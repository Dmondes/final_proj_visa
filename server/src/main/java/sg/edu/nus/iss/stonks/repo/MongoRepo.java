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
        System.out.println(
                "Update ticker counts for timeframe: " + timeframe + " between " + startTime + " and " + endTime);
        Query postsQuery = new Query(Criteria.where("createdTime").gte(startTime).lt(endTime)
                .and("ticker").ne(null).ne(""));
        List<ScrapedPost> postsInTimeframe = mongoTemplate.find(postsQuery, ScrapedPost.class, "posts");

        Map<String, Integer> newCounts = new HashMap<>();
        for (ScrapedPost post : postsInTimeframe) {
            newCounts.put(post.getTicker(), newCounts.getOrDefault(post.getTicker(), 0) + 1);
        }
        System.out.println("New counts for " + timeframe + ": " + newCounts.size() + " tickers.");
        Document tickersUpdateDoc = new Document();
        for (Map.Entry<String, Integer> entry : newCounts.entrySet()) {
            tickersUpdateDoc.put(entry.getKey(),
                    new Document("count", entry.getValue())
                            .append("last_updated", LocalDateTime.now()) // Update timestamp here
            );
        }

        // 4. Update the specific timeframe document in ticker_counts
        Query query = new Query(Criteria.where("timeframe").is(timeframe));
        Update update = new Update()
                .set("tickers", tickersUpdateDoc); // Replace the entire tickers map

        mongoTemplate.upsert(query, update, "ticker_counts");
        System.out.println("Successfully updated ticker_counts for timeframe: " + timeframe);
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