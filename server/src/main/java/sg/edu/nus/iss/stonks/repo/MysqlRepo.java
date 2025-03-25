package sg.edu.nus.iss.stonks.repo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import sg.edu.nus.iss.stonks.model.PriceAlert;
import sg.edu.nus.iss.stonks.model.Stock;
import sg.edu.nus.iss.stonks.model.User;

@Repository
public class MysqlRepo {

    public static final String SQL_INSERT_STOCK = "INSERT INTO listing (symbol, company_name, market_cap, ipo_year, volume, sector, industry) VALUES (?, ?, ?, ?, ?, ?, ?)";
    public static final String SQL_COUNT_STOCKS = "select count(*) as count from listing";
    public static final String SQ_GET_SYMBOLS = "SELECT symbol FROM listing";
    public static final String SQL_FIND_BY_EMAIL = "SELECT * FROM users WHERE email = ?";
    public static final String SQL_EMAIL_EXISTS = "SELECT COUNT(*) FROM users WHERE email = ?";
    public static final String SQL_UPDATE_WATCH = "UPDATE users SET watchlist = ? WHERE id = ?";
    public static final String SQL_INSERT_USER = "INSERT INTO users (email, password, watchlist, price_alerts, fcm_token) VALUES (?, ?, ?, ?, ?)";
    public static final String SQL_UPDATE_PRICE_ALERTS = "UPDATE users SET price_alerts = ? WHERE id = ?";
    public static final String SQL_UPDATE_FCM_TOKEN = "UPDATE users SET fcm_token = ? WHERE id = ?";

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

    public Set<String> getAllSymbols() {
        List<String> symbols = jdbcTemplate.queryForList(SQ_GET_SYMBOLS, String.class);
        return new HashSet<>(symbols);
    }

    public User findByEmail(String email) { // also get User
        if (emailExist(email)) {
            SqlRowSet rs = jdbcTemplate.queryForRowSet(SQL_FIND_BY_EMAIL, email);
            if (rs.next()) {

                User user = new User();
                user.setId(rs.getLong("id"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));

                // Parse watchlist
                String watchlistStr = rs.getString("watchlist");
                Set<String> watchlist = (watchlistStr != null && !watchlistStr.isEmpty())
                        ? new HashSet<>(Arrays.asList(watchlistStr.split(",")))
                        : new HashSet<>();
                user.setWatchlist(watchlist);

                // Parse price alerts
                String priceAlertsStr = rs.getString("price_alerts");
                Map<String, PriceAlert> priceAlerts = new HashMap<>();
                if (priceAlertsStr != null && !priceAlertsStr.isEmpty()) {
                    String[] alertsArray = priceAlertsStr.split(";");
                    for (String alertStr : alertsArray) {
                        PriceAlert alert = PriceAlert.fromString(alertStr);
                        if (alert != null) {
                            priceAlerts.put(alert.getTicker(), alert);
                        }
                    }
                }
                user.setPriceAlerts(priceAlerts);

                // Get FCM token
                String fcmToken = rs.getString("fcm_token");
                user.setFcmToken(fcmToken);

                return user;
            }
        }
        return null;
    }

    public void createUser(String email, String password) {
        jdbcTemplate.update(SQL_INSERT_USER, email, password, "", "", null);
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
}