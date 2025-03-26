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
import java.util.stream.Collectors;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(ScrapRepo.class);
    private static List<Stocklist> stockList = new ArrayList<>();
    private static Map<String, String> tickerToCompanyNameMap = new HashMap<>();
    private static Set<String> validTickersSet = new HashSet<>();

    private static final Set<String> TICKER_CONTEXT_KEYWORDS = Set.of(
            "stock", "stocks", "shares", "share", "ticker", "buy", "sell", "buying",
            "selling", "hold", "holding", "short", "shorting", "long", "calls", "puts",
            "options", "equity", "dividend", "earnings", "market", "cap", "trading",
            "invest", "investment", "portfolio", "bullish", "bearish", "moon", "diamond",
            "hands", "tendies", "yolo", "dd", "due diligence");

    @PostConstruct
    public void init() {
        if (sqlRepo.countStocks() == 0) {
            csvFile(); // Only load if DB is empty
        }
        validTickersSet = sqlRepo.getAllSymbols(); // Load from DB
        List<Stocklist> stocks = sqlRepo.getAllStocklist();
        tickerToCompanyNameMap = stocks.stream()
                .collect(Collectors.toMap(Stocklist::getSymbol, Stocklist::getCompanyName,
                        (existing, replacement) -> existing));
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
            String url = apiRequestUrl + "r/" + subreddit + "/rising.json?limit=15";
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

    private static final int SCORE_DOLLAR_PREFIX = 100; // Very strong signal
    private static final int SCORE_ALL_CAPS = 25; // Strong signal
    private static final int SCORE_CONTEXT_KEYWORD = 8; // Moderate signal per keyword
    private static final int SCORE_COMPANY_NAME_MATCH = 15; // Moderate signal
    private static final int MAX_CONTEXT_SCORE = 24; // Cap context score
    private static final int MIN_CONFIDENCE_THRESHOLD = 20;


    public String filterTicker(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Map<String, Integer> tickerScores = new HashMap<>();
        String textLower = text.toLowerCase();
        String[] words = text.split("[\\s\\p{Punct}&&[^$]]+");
        List<String> wordList = Arrays.asList(words);

        String potentialDollarTicker = null;
        int dollarTickerScore = 0;

        for (int i = 0; i < wordList.size(); i++) {
            String originalWord = wordList.get(i);
            if (originalWord == null || originalWord.isEmpty()) {
                logger.trace("Skipping null or empty word at index {}", i);
                continue;
            }

            int currentScore = 0;
            String potentialTicker;
            boolean isDollarPrefixed = false;

            // 1. Check for $ Prefix (Highest Priority)
            if (originalWord != null && originalWord.startsWith("$") && originalWord.length() > 1) {
                potentialTicker = originalWord.substring(1).replaceAll("[^A-Za-z]", ""); // Clean after $
                if (potentialTicker.isEmpty()) {continue;}
                potentialTicker = potentialTicker.toUpperCase();
                isDollarPrefixed = true;

                if (isValidTicker(potentialTicker)) {
                    currentScore = SCORE_DOLLAR_PREFIX;
                    // Store this separately as it's a very strong candidate 
                    if (currentScore > dollarTickerScore) {
                        dollarTickerScore = currentScore;
                        potentialDollarTicker = potentialTicker;
                    }
                    // Add score to main map as well, in case context reinforces it
                    tickerScores.put(potentialTicker, tickerScores.getOrDefault(potentialTicker, 0) + currentScore);
                } 
            } else {
                // 2. Process regular words (potential tickers)
                potentialTicker = originalWord.replaceAll("[^A-Za-z]", "").toUpperCase();
                if (potentialTicker.isEmpty() || !isValidTicker(potentialTicker)) {
                    continue; 
                }

                // 3. Score based on Case (All Caps?)
                String cleanedWord = originalWord.replaceAll("[^A-Za-z]", "");
                if (!cleanedWord.isEmpty() && isAllUpperCase(cleanedWord) && cleanedWord.equals(potentialTicker)) {
                    currentScore += SCORE_ALL_CAPS;

                }

                // 4. Score based on Context Keywords
                int contextScore = 0;
                int start = Math.max(0, i - 5); // Check 5 words before
                int end = Math.min(wordList.size(), i + 6); // Check 5 words after (inclusive of current word's potential context)
                for (int j = start; j < end; j++) {
                    if (i == j)
                        continue;
                    String contextWord = wordList.get(j);
                    if (contextWord != null && !contextWord.isEmpty() && TICKER_CONTEXT_KEYWORDS.contains(contextWord.toLowerCase())) {
                        contextScore += SCORE_CONTEXT_KEYWORD;
                        if (contextScore >= MAX_CONTEXT_SCORE) {
                            break;
                        }
                    }
                }
                currentScore += contextScore;

                // 5. Score based on Company Name Match
                String companyName = tickerToCompanyNameMap.get(potentialTicker);
                if (companyName != null && !companyName.isEmpty()) {
                    // check if company name (partially) exists in the lowercased text
                    if (textLower.contains(companyName.toLowerCase())) {
                        currentScore += SCORE_COMPANY_NAME_MATCH;
                    }
                }

                // Add score to the map
                if (currentScore > 0) {
                    tickerScores.put(potentialTicker, tickerScores.getOrDefault(potentialTicker, 0) + currentScore);
                }
            }
        } // End word loop

        // Determine the best ticker
        String bestTicker = null;
        int highestScore = 0;

        // If a $ticker was found and scored highly, it's a strong contender
        if (potentialDollarTicker != null) {
            bestTicker = potentialDollarTicker;
            highestScore = tickerScores.getOrDefault(bestTicker, 0); // Get its total score including context etc.
            logger.trace("Dollar-prefixed ticker {} found with initial score {}", bestTicker, dollarTickerScore);
        }

        // Compare with other potential tickers
        for (Map.Entry<String, Integer> entry : tickerScores.entrySet()) {
            // If this entry has a higher score than the current best (or the $ticker)
            if (entry.getValue() > highestScore) {
                highestScore = entry.getValue();
                bestTicker = entry.getKey();
            }
            // Tie-breaking: If scores are equal, prefer the $ticker if it exists
            else if (entry.getValue() == highestScore && potentialDollarTicker != null
                    && !entry.getKey().equals(potentialDollarTicker)) {
                // Keep the potentialDollarTicker as bestTicker if scores are equal
                logger.trace("Ticker {} tied with dollar-ticker {} score {}, preferring dollar-ticker.", entry.getKey(),
                        potentialDollarTicker, highestScore);
            }
        }

        // Final Decision based on Threshold
        if (bestTicker != null && highestScore >= MIN_CONFIDENCE_THRESHOLD) {
            logger.debug("Determined best ticker: {} with score: {} from text snippet: \"{}\"",
                    bestTicker, highestScore, text.length() > 100 ? text.substring(0, 100) + "..." : text);
            return bestTicker;
        } else {
            if (bestTicker != null) {
                logger.trace("Ticker {} found, but score {} is below threshold {}. Discarding.", bestTicker,
                        highestScore, MIN_CONFIDENCE_THRESHOLD);
            } else {
                logger.trace("No confident ticker found in text snippet: \"{}\"",
                        text.length() > 100 ? text.substring(0, 100) + "..." : text);
            }
            return null; // Return null if no ticker meets the confidence threshold
        }
    }

    private boolean isAllUpperCase(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (char c : s.toCharArray()) {
            if (!Character.isUpperCase(c)) {
                return false;
            }
        }
        // check for letters
        return s.matches(".*[A-Z].*");
    }

    private boolean isValidTicker(String ticker) {
        return ticker != null && !ticker.isEmpty() && ticker.length() <= 5 && validTickersSet.contains(ticker);
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
