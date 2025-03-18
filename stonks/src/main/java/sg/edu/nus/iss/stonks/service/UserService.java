package sg.edu.nus.iss.stonks.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sg.edu.nus.iss.stonks.model.User;
import sg.edu.nus.iss.stonks.repo.MysqlRepo;

@Service
public class UserService {

    @Autowired
    private MysqlRepo sqlRepo;

    // public Set<String> getWatchList(String email){
    //     return sqlRepo.getWatchList(email);
    // }
   
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
        }
    }
    public User findByEmail(String email) { //also get user
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
