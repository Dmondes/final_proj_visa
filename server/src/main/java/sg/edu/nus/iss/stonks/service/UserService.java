package sg.edu.nus.iss.stonks.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sg.edu.nus.iss.stonks.model.PriceAlert;
import sg.edu.nus.iss.stonks.model.User;
import sg.edu.nus.iss.stonks.repo.MysqlRepo;

@Service
public class UserService {

    @Autowired
    private MysqlRepo sqlRepo;
   
    public void addToWatchlist(String email, String ticker) {
        User user = sqlRepo.findByEmail(email);
        if (user != null) {
            Set<String> watchlist = user.getWatchlist();
            if (watchlist == null) {
                watchlist = new HashSet<>();
            }
            watchlist.add(ticker);
            sqlRepo.updateWatchlist(user.getId(), watchlist);
        }
    }

    public void removeFromWatchlist(String email, String ticker) {
        User user = sqlRepo.findByEmail(email);
        if (user != null) {
            Set<String> watchlist = user.getWatchlist();
            if (watchlist != null) {
                watchlist.remove(ticker);
                sqlRepo.updateWatchlist(user.getId(), watchlist);
            }
            
            // Also remove any price alerts for this ticker
            if (user.getPriceAlerts() != null && user.getPriceAlerts().containsKey(ticker)) {
                removePriceAlert(email, ticker);
            }
        }
    }
    
    public void setPriceAlert(String email, String ticker, double targetPrice, String condition) {
        User user = sqlRepo.findByEmail(email);
        if (user != null) {
            Map<String, PriceAlert> priceAlerts = user.getPriceAlerts();
            if (priceAlerts == null) {
                priceAlerts = new HashMap<>();
            }
            
            PriceAlert alert = new PriceAlert(ticker, targetPrice, condition);
            priceAlerts.put(ticker, alert);
            
            sqlRepo.updatePriceAlerts(user.getId(), priceAlerts);
        }
    }
    
    public void removePriceAlert(String email, String ticker) {
        User user = sqlRepo.findByEmail(email);
        if (user != null) {
            Map<String, PriceAlert> priceAlerts = user.getPriceAlerts();
            if (priceAlerts != null && priceAlerts.containsKey(ticker)) {
                priceAlerts.remove(ticker);
                sqlRepo.updatePriceAlerts(user.getId(), priceAlerts);
            }
        }
    }
    
    public void updateFcmToken(String email, String token) {
        User user = sqlRepo.findByEmail(email);
        if (user != null) {
            sqlRepo.updateFcmToken(user.getId(), token);
        }
    }
    
    public User findByEmail(String email) {
        return sqlRepo.findByEmail(email);
    }
    
    public boolean registerUser(String email, String password) {
        User existingUser = sqlRepo.findByEmail(email);
        if (existingUser != null) {
            System.out.println("User already exists");
            return false; // User already exists
        }
        sqlRepo.createUser(email, password);
        return true;
    }
}