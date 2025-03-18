package sg.edu.nus.iss.stonks.repo;

import java.time.LocalDateTime;
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
        // Get posts within timeframe
        Query postsQuery = new Query(Criteria.where("createdTime").gte(startTime).lt(endTime));
        mongoTemplate.find(postsQuery, ScrapedPost.class, "posts").forEach(post -> {
            String ticker = post.getTicker();
            if (ticker != null && !ticker.isEmpty() && !postCountCheck(post.getRedditId(), timeframe)) {
                
                // Update ticker count
                Query query = new Query(Criteria.where("timeframe").is(timeframe));
                Update update = new Update()
                        .inc("tickers." + ticker + ".count", 1)
                        .set("tickers." + ticker + ".last_updated", LocalDateTime.now());
                mongoTemplate.upsert(query, update, "ticker_counts");
                
                // Mark as processed
                checkedPost(post.getRedditId(), timeframe);
            }
        });
    
        // Create base document if new timeframe
        Document timeframeDoc = mongoTemplate.findOne(
            new Query(Criteria.where("timeframe").is(timeframe)), 
            Document.class, 
            "ticker_counts"
        );
        
        if (timeframeDoc == null) {
            Document newDoc = new Document("timeframe", timeframe)
                .append("tickers", new Document());
            mongoTemplate.insert(newDoc, "ticker_counts");
        }
    }

    public Map<String, Integer> getTickerCounts(String timeframe) {
        Map<String, Integer> counts = new HashMap<>();
        Document timeframeDoc = mongoTemplate.findOne(
            new Query(Criteria.where("timeframe").is(timeframe)), 
            Document.class, 
            "ticker_counts"
        );

        if (timeframeDoc != null) {
            Document tickersDoc = timeframeDoc.get("tickers", Document.class);
            tickersDoc.forEach((ticker, data) -> {
                counts.put(ticker, ((Document) data).getInteger("count"));
            });
        }
        return counts;
    }

    public List<ScrapedPost> getRecentPosts(String ticker) {
        return mongoTemplate.find(
            new Query()
                .addCriteria(Criteria.where("ticker").is(ticker))
                .with(Sort.by(Sort.Direction.DESC, "createdTime"))
                .limit(5),
            ScrapedPost.class,
            "posts"
        );
    }

    public boolean idExist(String redditId){
        Query query = new Query(Criteria.where("redditId").is(redditId));
        return mongoTemplate.exists(query, ScrapedPost.class, "posts");
    }

    public boolean postCountCheck(String redditId, String timeframe) {
        Query query = new Query(Criteria.where("redditId").is(redditId).and("timeframe").is(timeframe));
        return mongoTemplate.exists(query, "countedPosts");
    }
    
    public void checkedPost(String redditId, String timeframe) {
        Document doc = new Document("redditId", redditId)
                        .append("timeframe", timeframe);
        mongoTemplate.insert(doc, "countedPosts");
    }
}