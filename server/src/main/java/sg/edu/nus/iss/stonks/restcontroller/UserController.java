package sg.edu.nus.iss.stonks.restcontroller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sg.edu.nus.iss.stonks.model.User;
import sg.edu.nus.iss.stonks.service.UserService;

@RestController
@RequestMapping("/api")
// @CrossOrigin(origins = "http://localhost:4200")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String email, @RequestParam String password) {
        boolean newUser = userService.registerUser(email, password);
        if (newUser == false) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("Registration successful");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        User existUser = userService.findByEmail(user.getEmail());
        if (existUser != null && existUser.getPassword().equals(user.getPassword())) {
            return ResponseEntity.ok("Login successful");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    @PostMapping("/user/watchlist/add")
    public ResponseEntity<String> addToWatchlist(@RequestParam String email, @RequestParam String ticker) {
        userService.addToWatchlist(email, ticker);
        return ResponseEntity.ok("Added to watchlist");
    }

    @PostMapping("/user/watchlist/remove")
    public ResponseEntity<String> removeFromWatchlist(@RequestParam String email, @RequestParam String ticker) {
        userService.removeFromWatchlist(email, ticker);
        return ResponseEntity.ok("Removed from watchlist");
    }

    @GetMapping("/user/{email}") //also get User
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        User user = userService.findByEmail(email);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
