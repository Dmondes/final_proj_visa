package sg.edu.nus.iss.stonks.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sg.edu.nus.iss.stonks.model.ScrapedPost;
import sg.edu.nus.iss.stonks.model.StockPrice;
import sg.edu.nus.iss.stonks.repo.MongoRepo;
import sg.edu.nus.iss.stonks.repo.ScrapRepo;

@Service
public class ScrapService {
    @Autowired
    private ScrapRepo scrapRepo;

    @Autowired
    private MongoRepo mongoRepo;
    
  public StockPrice getStockDetails(String ticker){
    return scrapRepo.getStockDetails(ticker);
  }

  public void scrapeRisingPosts(){
    scrapRepo.scrapeRisingPosts();
  }

   public Map<String, Integer> getTickerCounts(String timeframe){
    return mongoRepo.getTickerCounts(timeframe);
   }

  public List<ScrapedPost> getRecentPosts(String ticker, String timeframe){
    return mongoRepo.getRecentPosts(ticker, timeframe);
  }

  public void updateDailyTickerCounts() {
    scrapRepo.updateDailyTickerCounts();
  }
}
