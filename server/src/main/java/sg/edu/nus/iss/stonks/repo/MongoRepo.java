package sg.edu.nus.iss.stonks.repo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import sg.edu.nus.iss.stonks.model.ScrapedPost;

@Repository
public class MongoRepo {

    @Autowired
    private MongoTemplate mongoTemplate;

    public void save(ScrapedPost post) {
        mongoTemplate.insert(post, "posts");
    }

    public void updateTickerCounts(String timeframe, LocalDateTime startTime, LocalDateTime endTime) {
        // Create base document if new timeframe
        Document timeframeDoc = mongoTemplate.findOne(
                new Query(Criteria.where("timeframe").is(timeframe)),
                Document.class,
                "ticker_counts");

        if (timeframeDoc == null) {
            Document newDoc = new Document("timeframe", timeframe)
                    .append("tickers", new Document());
            mongoTemplate.insert(newDoc, "ticker_counts");
        }
        // Get posts within timeframe
        Query postsQuery = new Query(Criteria.where("createdTime").gte(startTime).lt(endTime));
        mongoTemplate.find(postsQuery, ScrapedPost.class, "posts").forEach(post -> {
            String ticker = post.getTicker();
            if (ticker != null && !ticker.isEmpty() && !postCountCheck(post.getRedditId())) {
                // Update ticker count
                Query query = new Query(Criteria.where("timeframe").is(timeframe));
                Update update = new Update()
                        .inc("tickers." + ticker + ".count", 1)
                        .set("tickers." + ticker + ".last_updated", LocalDateTime.now());
                mongoTemplate.upsert(query, update, "ticker_counts");

                // Mark as processed
                checkedPost(post.getRedditId());
            }
        });

    }

    public Map<String, Integer> getTickerCounts(String timeframe) {
        Map<String, Integer> counts = new HashMap<>();
        Document timeframeDoc = mongoTemplate.findOne(
                new Query(Criteria.where("timeframe").is(timeframe)),
                Document.class,
                "ticker_counts");

        if (timeframeDoc != null) {
            Document tickersDoc = timeframeDoc.get("tickers", Document.class);
            tickersDoc.forEach((ticker, data) -> {
                counts.put(ticker, ((Document) data).getInteger("count"));
            });
        }
        return counts;
    }

    public List<ScrapedPost> getRecentPosts(String ticker, String timeframe) {
       LocalDateTime startTime;
           LocalDateTime now = LocalDateTime.now();
           if ("24h".equals(timeframe)) {
               startTime = now.minusHours(24);
           } else if ("7d".equals(timeframe)) {
               startTime = now.minusDays(7);
           } else {
            System.out.println("invalid timeframe");
               return new ArrayList<>(); 
           }

           return mongoTemplate.find(
                   new Query()
                           .addCriteria(Criteria.where("ticker").is(ticker))
                           .addCriteria(Criteria.where("createdTime").gte(startTime)) // filter time
                           .with(Sort.by(Sort.Direction.DESC, "createdTime"))
                           .limit(5),
                   ScrapedPost.class,
                   "posts");
       }

    public boolean idExist(String redditId) {
        Query query = new Query(Criteria.where("redditId").is(redditId));
        return mongoTemplate.exists(query, ScrapedPost.class, "posts");
    }

    public boolean postCountCheck(String redditId) {
        Query query = new Query(Criteria.where("redditId").is(redditId));
        return mongoTemplate.exists(query, "countedPosts");
    }

    public void checkedPost(String redditId) {
        Document doc = new Document("redditId", redditId);
        mongoTemplate.insert(doc, "countedPosts");
    }
}