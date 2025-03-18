package sg.edu.nus.iss.stonks.repo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

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
    public static final String SQL_INSERT_USER = "INSERT INTO users (email, password, watchlist) VALUES ( ?, ?, ?)";

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

    // public Set<String> getWatchList(String email) {
    //     User user = findByEmail(email);
    //     if (user != null) {
    //         return user.getWatchlist();
    //     }
    //     return new HashSet<>();
    // }

    public User findByEmail(String email) { // also get User
        if (emailExist(email)) {
            SqlRowSet rs = jdbcTemplate.queryForRowSet(SQL_FIND_BY_EMAIL, email);
            if (rs.next()) {

                User user = new User();
                user.setId(rs.getLong("id"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));

                String watchlistStr = rs.getString("watchlist");
                Set<String> watchlist = (watchlistStr != null && !watchlistStr.isEmpty())
                        ? new HashSet<>(Arrays.asList(watchlistStr.split(",")))
                        : new HashSet<>();
                user.setWatchlist(watchlist);
                return user;
            }
        }
        return null;

    }

    public void createUser(String email, String password) {
        jdbcTemplate.update(SQL_INSERT_USER, email, password, "");
    }

    public Boolean emailExist(String email) {
        Integer count = jdbcTemplate.queryForObject(SQL_EMAIL_EXISTS, Integer.class, email);
        return (count != null && count > 0);
    }

    public void updateWatchlist(Long userId, Set<String> watchlist) {
        String watchString = String.join(",", watchlist);
        jdbcTemplate.update(SQL_UPDATE_WATCH, watchString, userId);
    }

}
