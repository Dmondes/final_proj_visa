package sg.edu.nus.iss.stonks.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public class User {
    private Long id;
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters")
    private String password;
    private Set<String> watchlist = new HashSet<>();

    public User() {
    }

    public User(Long id, String email, String password, Set<String> watchlist) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.watchlist = watchlist;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getWatchlist() {
        return watchlist;
    }

    public void setWatchlist(Set<String> watchlist) {
        this.watchlist = watchlist;
    }

    

}
