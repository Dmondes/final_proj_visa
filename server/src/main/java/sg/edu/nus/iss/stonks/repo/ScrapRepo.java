package sg.edu.nus.iss.stonks.repo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import sg.edu.nus.iss.stonks.config.ScrapConfig;
import sg.edu.nus.iss.stonks.model.ScrapedPost;
import sg.edu.nus.iss.stonks.model.Stock;
import sg.edu.nus.iss.stonks.model.StockPrice;
import sg.edu.nus.iss.stonks.model.Stocklist;

@Repository
public class ScrapRepo {

    @Autowired
    private ScrapConfig scrapConfig;

    @Autowired
    private MongoRepo mongoRepo;

    @Autowired
    private MysqlRepo sqlRepo;

    RestTemplate restTemplate = new RestTemplate();

    private static final String accessTokenUrl = "https://www.reddit.com/api/v1/access_token";
    private static final String apiRequestUrl = "https://oauth.reddit.com/";
    private static final String finnhubUrl = "https://finnhub.io/api/v1/";
    private static List<Stocklist> stockList = new ArrayList<>();
    private static Set<String> validTickersSet = new HashSet<>();
    private static final Set<String> commonWordTickersSet = new HashSet<>(Arrays.asList(
            "A", "ACT", "ADD", "AKA", "ALL", "AM", "AMC", "AN", "AND", "ANY", "ARE", "AS", "AT",
            "BACK", "BE", "BC", "BETA", "BIO", "BOOK", "BROS", "BY",
            "CALM", "CAN", "CAP", "CAR", "CARS", "CCL", "CHART", "CLOSE", "COST",
            "DAY", "DOW", "DTE","EDIT", "EVER","F", "FIVE", "FOR", "FUND",
            "GOLD", "GO", "GOOD", "GT", "HAS", "HE", "HIGH", "HI", "HOLD", "ICU", "IRS", "IT",
            "JOB", "K", "LMT", "LOW", "LUCK","M", "MAIN", "MAR", "ME", "MSN",
            "NAT", "NET", "NEW", "NOW","OLD", "ON", "ONE", "OPEN", "OR", "OUT",
            "P/E", "PER", "PLAY", "POST", "PRICE", "QUOTE",  "RAY", "REAL", "CASH",
            "S", "SAFE", "SAT", "SAY", "SEE", "SELF", "SELL", "SHARE", "SO", "SOLD", "STOCK", 
            "T", "THE", "TICKER", "TRADE", "TSLA", "TWO","UP", "USE", "EU", "NEXT", "HIT", "FAST",
            "VALUE", "VS","WAY", "WELL", "WHO","XP","YIELD", "YOU"));

    @PostConstruct
    public void init() {
        if (sqlRepo.countStocks() == 0) {
            csvFile(); // Only load if DB is empty
        }
        validTickersSet = sqlRepo.getAllSymbols(); // Load from DB
    }

    private String getRedditAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(
                scrapConfig.getRedditId(),
                scrapConfig.getRedditSecret());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("User-Agent", "Scaper/1.0 by " + scrapConfig.getRedditUsername());
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    accessTokenUrl,
                    new HttpEntity<>(body, headers),
                    Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                System.out.println("Successfully authenticated with Reddit API");
                return (String) response.getBody().get("access_token");

            } else {
                System.err.println("Reddit API error: " + response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            return null;
        }

    }

    public void scrapeRisingPosts() {
        String token = getRedditAccessToken(); // Get OAuth2 access token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("User-Agent", "Scaper/1.0 by " + scrapConfig.getRedditUsername());
        List<String> subreddits = Arrays.asList("stocks", "wallstreetbets", "investing", "StockMarket",
                "DeepFuckingValue");
        subreddits.forEach(subreddit -> {
            String url = apiRequestUrl + "r/" + subreddit + "/rising.json?limit=10";
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                processedPosts(response.getBody());
            }
        });

    }

    private void processedPosts(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject listing = reader.readObject();
            JsonArray posts = listing.getJsonObject("data").getJsonArray("children");
            for (JsonValue post : posts) {
                JsonObject postData = ((JsonObject) post).getJsonObject("data");
                ScrapedPost redditPost = new ScrapedPost();
                redditPost.setRedditId(postData.getString("id"));
                if (mongoRepo.idExist(redditPost.getRedditId())) {
                    continue;
                }
                redditPost.setTitle(postData.getString("title"));
                redditPost.setSelfText(postData.getString("selftext", ""));
                redditPost.setSubReddit(postData.getString("subreddit"));
                redditPost.setAuthor(postData.getString("author"));
                redditPost.setScore(postData.getInt("score"));
                redditPost.setUpvoteRatio((float) postData.getJsonNumber("upvote_ratio").doubleValue());
                redditPost.setNumComments(postData.getInt("num_comments"));
                Instant instant = Instant.ofEpochSecond(postData.getJsonNumber("created_utc").longValue());
                redditPost.setCreatedTime(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
                redditPost.setFlairText(postData.getString("link_flair_text", ""));
                redditPost.setPostUrl("https://www.reddit.com/" + postData.getString("permalink"));
                String ticker = filterTicker(postData.getString("title") + " " + postData.getString("selftext"));
                redditPost.setTicker(ticker);
                mongoRepo.save(redditPost);

                // System.out.println("\nReddit Post Details: " +
                // "\nID: " + redditPost.getRedditId() +
                // "\nTitle: " + redditPost.getTitle() +
                // "\nSelfText: " + redditPost.getSelfText() +
                // "\nSubReddit: " + redditPost.getSubReddit() +
                // "\nAuthor: " + redditPost.getAuthor() +
                // "\nScore: " + redditPost.getScore() +
                // "\nUpvote Ratio: " + redditPost.getUpvoteRatio() +
                // "\nNumber of Comments: " + redditPost.getNumComments() +
                // "\nCreated Time: " + redditPost.getCreatedTime() +
                // "\nFlair Text: " + redditPost.getFlairText() +
                // "\nPost URL: " + redditPost.getPostUrl() +
                // "\nTicker: " + redditPost.getTicker());

            }
        } catch (Exception e) {
            System.err.println("Failed to parse JSON: " + e.getMessage());
        }
    }

    public String filterTicker(String text) {
        // check for $ prefix
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (word.startsWith("$")) {
                String processed = word.substring(1).replaceAll("[^A-Za-z]", "").toUpperCase().trim();
                // Skip words with mixed letters and numbers
                if (!word.substring(1).matches(".*\\d+.*") && validTickersSet.contains(processed)
                        && processed.length() <= 5) {
                    return processed; // best case for exact match
                }
            }
        }
        // word score counter
        Map<String, Integer> tickerScores = new HashMap<>();
    
        for (String word : words) {
            // Skip words that contain any digits
            if (word.matches(".*\\d+.*")) {
                continue;
            }
            String potentialTicker = word.replaceAll("[^A-Za-z]", "").toUpperCase().trim();
            // Skip invalid tickers
            if (!validTickersSet.contains(potentialTicker) || potentialTicker.length() > 5
                    || potentialTicker.isEmpty()) {
                continue;
            }
            int score = 0;
            // Common words minus pts
            if (commonWordTickersSet.contains(potentialTicker)) {
                score -= 5;
            } else {
                score += 2; // word not in common
            }
            // Check for company name matches in the text
            for (Stocklist stock : stockList) {
                if (potentialTicker.equals(stock.getSymbol())) {
                    String companyName = stock.getCompanyName().toLowerCase();
                    // Full company name match
                    if (text.toLowerCase().contains(companyName)) {
                        score += 10;
                        break;
                    }
                    // Partial name matches
                    String[] nameParts = companyName.split("\\s+");
                    int matchedParts = 0;
                    for (String part : nameParts) {
                        if (part.length() > 2 && text.toLowerCase().contains(part)) {
                            matchedParts++;
                        }
                    }
                    if (matchedParts > 0) {
                        score += Math.min(matchedParts * 2, 6); // Cap at 6 points
                    }
                }
            }
            // Check for word frequency
            int wordFrequency = 0;
            for (String w : words) {
                if (!w.matches(".*\\d+.*") && w.replaceAll("[^A-Za-z]", "").equalsIgnoreCase(potentialTicker)) {
                    wordFrequency++;
                }
            }
            score += Math.min(wordFrequency - 1, 3);
    
            // Add to scores map
            tickerScores.put(potentialTicker, score);
        }
        // Return the ticker with the highest score
        String bestTicker = null;
        int highestScore = 0;
    
        for (Map.Entry<String, Integer> entry : tickerScores.entrySet()) {
            if (entry.getValue() > highestScore) {
                highestScore = entry.getValue();
                bestTicker = entry.getKey();
            }
        }
        // Only return if the score> 3
        if (highestScore >= 3) {
            System.out.println(" Found ticker: " + bestTicker + " in text: " + text
                    + " with score: " + highestScore);
            return bestTicker;
        } else {
            System.out.println(" No ticker found in text: " + text);
            return null;
        }
    }

    public void csvFile() {
        InputStream is = getClass().getResourceAsStream("/nasdaq_tickers.csv");
        if (is == null) {
            System.err.println("Could not find nasdaq_tickers.csv in resources.");
            return;
        }
        try (InputStream inputStream = is) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            line = reader.readLine(); // skip headers
            while ((line = reader.readLine()) != null) {
                // System.out.println("Test output: " + line);
                String[] parts = line.split(",");
                if (parts.length == 11 && !parts[0].equals("Symbol")) {
                    String symbol = parts[0].trim();
                    String name = parts[1].trim();
                    String capMarket = parts[5].trim();
                    Double marketCap = 0.0;
                    if (capMarket != null && !capMarket.isEmpty()) {
                        marketCap = Double.parseDouble(capMarket);
                    }
                    String yearIPO = parts[7].trim();
                    int ipoYear = 0;
                    if (yearIPO != null && !yearIPO.isEmpty()) {
                        ipoYear = Integer.parseInt(yearIPO);
                    }
                    Double volume = Double.parseDouble(parts[8].trim());
                    String sector = parts[9].trim();
                    String industry = parts[10].trim();
                    stockList.add(new Stocklist(symbol, name));
                    sqlRepo.saveStock(new Stock(symbol, name, marketCap, ipoYear, volume, sector, industry));
                    validTickersSet.add(symbol);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Number of tickers in HashSet: " + validTickersSet.size());

    }

    public void updateDailyTickerCounts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        mongoRepo.updateTickerCounts("24h", twentyFourHoursAgo, now);
    }

    public void updateWeeklyTickerCounts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        mongoRepo.updateTickerCounts("7d", sevenDaysAgo, now);
    }

    public StockPrice getStockDetails(String ticker) {
        String url = finnhubUrl + "quote?symbol=" + ticker + "&token=" + scrapConfig.getFinnhubApiKey();
        ResponseEntity<StockPrice> response = restTemplate.getForEntity(url, StockPrice.class);
        return response.getBody();
    }

}
