package sg.edu.nus.iss.stonks.restcontroller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import sg.edu.nus.iss.stonks.model.ScrapedPost;
import sg.edu.nus.iss.stonks.model.StockPrice;
import sg.edu.nus.iss.stonks.service.ScrapService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class TicketController {

    @Autowired
    private ScrapService scrapService;
    
    RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/trending") // List of trending tickers
    public ResponseEntity<Map<String, Integer>> getTrendingTickers(
            @RequestParam(required = false, defaultValue = "24h") String timeframe) {
        Map<String, Integer> counts = scrapService.getTickerCounts(timeframe);
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/scrape") // manual run instead of cron
    public ResponseEntity<String> runScrapeNow() {
        scrapService.scrapeRisingPosts();
        scrapService.updateDailyTickerCounts();
        return ResponseEntity.ok(" Manual Scraping completed");
    }
    
    @GetMapping("/stock/{ticker}") //get stock details
    public ResponseEntity<StockPrice> getStockDetails(@PathVariable String ticker) {
        StockPrice stock = scrapService.getStockDetails(ticker);
        return ResponseEntity.ok(stock);
    }

     @GetMapping("/recentposts/{ticker}") // recent 5 post for ticker
    public ResponseEntity<List<ScrapedPost>> getRecentPosts(@PathVariable String ticker) {
        List<ScrapedPost> posts = scrapService.getRecentPosts(ticker);
        return ResponseEntity.ok(posts);
    }

}
