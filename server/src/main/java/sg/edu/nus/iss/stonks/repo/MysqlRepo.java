package sg.edu.nus.iss.stonks.repo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import sg.edu.nus.iss.stonks.model.PriceAlert;
import sg.edu.nus.iss.stonks.model.Stock;
import sg.edu.nus.iss.stonks.model.Stocklist;
import sg.edu.nus.iss.stonks.model.User;

@Repository
public class MysqlRepo {

    private static final Logger logger = LoggerFactory.getLogger(MysqlRepo.class); 
    public static final String SQL_INSERT_STOCK = "INSERT INTO listing (symbol, company_name, market_cap, ipo_year, volume, sector, industry) VALUES (?, ?, ?, ?, ?, ?, ?)";
    public static final String SQL_COUNT_STOCKS = "select count(*) as count from listing";
    public static final String SQL_GET_ALL_STOCKLIST = "SELECT symbol, company_name FROM listing";
    public static final String SQ_GET_SYMBOLS = "SELECT symbol FROM listing";
    public static final String SQL_FIND_BY_EMAIL = "SELECT * FROM users WHERE email = ?";
    public static final String SQL_EMAIL_EXISTS = "SELECT COUNT(*) FROM users WHERE email = ?";
    public static final String SQL_UPDATE_WATCH = "UPDATE users SET watchlist = ? WHERE id = ?";
    public static final String SQL_INSERT_USER_FULL = "INSERT INTO users (email, password, watchlist, price_alerts, fcm_token) VALUES (?, ?, ?, ?, ?)";
    public static final String SQL_UPDATE_PRICE_ALERTS = "UPDATE users SET price_alerts = ? WHERE id = ?";
    public static final String SQL_UPDATE_FCM_TOKEN = "UPDATE users SET fcm_token = ? WHERE id = ?";
    public static final String SQL_FIND_USERS_WITH_ALERTS_AND_TOKEN = "SELECT * FROM users WHERE price_alerts IS NOT NULL AND price_alerts != '' AND fcm_token IS NOT NULL AND fcm_token != ''";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void saveStock(Stock stock) {
        jdbcTemplate.update(SQL_INSERT_STOCK, stock.getSymbol(), stock.getCompanyName(), stock.getMarketCap(),
                stock.getIpoYear(), stock.getVolume(), stock.getSector(), stock.getIndustry());
    }

    public int countStocks() {
        Integer count = jdbcTemplate.queryForObject(SQL_COUNT_STOCKS, Integer.class);
        if (null != count)
            return count;
        return 0;

    }

     public List<Stocklist> getAllStocklist() {
        List<Stocklist> stockList = new ArrayList<>();
        SqlRowSet rs = jdbcTemplate.queryForRowSet(SQL_GET_ALL_STOCKLIST);
        while (rs.next()) {
            stockList.add(new Stocklist(rs.getString("symbol"), rs.getString("company_name")));
        }
        return stockList;
    }

    public Set<String> getAllSymbols() {
        List<String> symbols = jdbcTemplate.queryForList(SQ_GET_SYMBOLS, String.class);
        return new HashSet<>(symbols);
    }

    public User findByEmail(String email) {
        try {
            SqlRowSet rs = jdbcTemplate.queryForRowSet(SQL_FIND_BY_EMAIL, email);
            if (rs.next()) {
                 return mapRowToUser(rs);
            } else {
                logger.debug("User not found with email: {}", email);
                return null;
            }
        } catch (Exception e) {
            logger.error("Database error when finding user by email {}: {}", email, e.getMessage(), e);
            return null;
        }
    }

    public void createUser(String email, String password) {
        try {
            System.out.println("Attempting to create user: " + email);
            int result;
            result = jdbcTemplate.update(SQL_INSERT_USER_FULL, email, password, "", "", null);
            System.out.println("User creation with full schema successful: " + result + " rows affected.");

        } catch (Exception e) {
            System.err.println("Database error creating user: " + e.getMessage());
            e.printStackTrace();
            throw e; 
        }
    }

    public Boolean emailExist(String email) {
        Integer count = jdbcTemplate.queryForObject(SQL_EMAIL_EXISTS, Integer.class, email);
        return (count != null && count > 0);
    }

    public void updateWatchlist(Long userId, Set<String> watchlist) {
        String watchString = String.join(",", watchlist);
        jdbcTemplate.update(SQL_UPDATE_WATCH, watchString, userId);
    }

    public void updatePriceAlerts(Long userId, Map<String, PriceAlert> priceAlerts) {
        StringBuilder sb = new StringBuilder();
        for (PriceAlert alert : priceAlerts.values()) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(alert.toString());
        }
        jdbcTemplate.update(SQL_UPDATE_PRICE_ALERTS, sb.toString(), userId);
    }

    public void updateFcmToken(Long userId, String fcmToken) {
        jdbcTemplate.update(SQL_UPDATE_FCM_TOKEN, fcmToken, userId);
    }
    public List<User> findAllUsersWithAlertsAndToken() {
        List<User> users = new ArrayList<>();
        SqlRowSet rs = null;
        try {
            rs = jdbcTemplate.queryForRowSet(SQL_FIND_USERS_WITH_ALERTS_AND_TOKEN);
            while (rs.next()) {
                try {
                    User user = mapRowToUser(rs); // Extract mapping logic
                    if (user != null) {
                       users.add(user);
                    }
                } catch (Exception e) {
                    logger.error("Error building user object during alert check for row (ID maybe {}): {}", rs.getLong("id"), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Database error executing findAllUsersWithAlertsAndToken: {}", e.getMessage(), e);
        }
        return users;
    }
    
    private User mapRowToUser(SqlRowSet rs) throws Exception { 
        User user = new User();
        user.setId(rs.getLong("id"));
        String email = rs.getString("email"); 
        user.setEmail(email);
        user.setPassword(rs.getString("password"));

        // Parse watchlist
        String watchlistStr = rs.getString("watchlist");
        Set<String> watchlist = new HashSet<>();
        if (watchlistStr != null && !watchlistStr.isEmpty()) {
            try {
                watchlist = new HashSet<>(Arrays.asList(watchlistStr.split(",")));
            } catch (Exception e) {
                logger.error("Error parsing watchlist for user {}: {}", email, e.getMessage());
            }
        }
        user.setWatchlist(watchlist);
        // Parse price alerts
        Map<String, PriceAlert> priceAlerts = new HashMap<>();
        try {
            String priceAlertsStr = rs.getString("price_alerts");
            if (priceAlertsStr != null && !priceAlertsStr.isEmpty()) {
                 String[] alertsArray = priceAlertsStr.split(";");
                 for (String alertStr : alertsArray) {
                     if (alertStr != null && !alertStr.trim().isEmpty()) {
                         PriceAlert alert = PriceAlert.fromString(alertStr.trim()); // Assumes PriceAlert.fromString exists
                         if (alert != null) {
                             priceAlerts.put(alert.getTicker(), alert);
                         } else {
                              logger.warn("Could not parse PriceAlert from string '{}' for user {}", alertStr, email);
                         }
                     }
                 }
            }
        } catch (Exception e) {
            // Check if column exists error or parsing error
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("price_alerts")) {
                 logger.warn("price_alerts column might be missing or inaccessible for user {}", email);
            } else {
                 logger.error("Error parsing price alerts for user {}: {}", email, e.getMessage());
            }
        }
        user.setPriceAlerts(priceAlerts);
        // Get FCM token
        try {
            String fcmToken = rs.getString("fcm_token");
            user.setFcmToken(fcmToken);
        } catch (Exception e) {
             if (e.getMessage() != null && e.getMessage().toLowerCase().contains("fcm_token")) {
                logger.warn("fcm_token column might be missing or inaccessible for user {}", email);
             } else {
                 logger.error("Error getting fcm_token for user {}: {}", email, e.getMessage());
             }
            user.setFcmToken(null);
        }
        return user;
    }
    public void removeFcmToken(Long userId) {
        try {
             jdbcTemplate.update(SQL_UPDATE_FCM_TOKEN, (String)null, userId); // Set token to null
             logger.info("Removed FCM token for user ID: {}", userId);
        } catch (Exception e) {
             logger.error("Failed to remove FCM token for user ID {}: {}", userId, e.getMessage());
        }
   }
}