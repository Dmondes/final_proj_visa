package sg.edu.nus.iss.stonks.restcontroller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseAuthException;

import sg.edu.nus.iss.stonks.config.FirebaseAuthConfig;
import sg.edu.nus.iss.stonks.model.User;
import sg.edu.nus.iss.stonks.service.UserService;

@RestController
@RequestMapping("/api")
// @CrossOrigin(origins = "http://localhost:4200")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private FirebaseAuthConfig firebaseAuthConfig;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String email, @RequestParam String password, 
                                      @RequestHeader("Authorization") String idToken) {
        try {
            // Verify Firebase token
            if (idToken != null && idToken.startsWith("Bearer ")) {
                idToken = idToken.substring(7);
                String uid = firebaseAuthConfig.verifyToken(idToken);
                System.out.println("Token:" + uid);
                // Token is valid, proceed with user registration
                boolean newUser = userService.registerUser(email, password);
                if (newUser == false) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
                }
                return ResponseEntity.status(HttpStatus.CREATED).body("Registration successful");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid authentication token");
            }
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: " + e.getMessage());
        }
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email, 
                                               @RequestHeader("Authorization") String idToken) {
        try {
            // Verify Firebase token
            if (idToken != null && idToken.startsWith("Bearer ")) {
                idToken = idToken.substring(7);
                String uid = firebaseAuthConfig.verifyToken(idToken);
                System.out.println("Token:" + uid);
                // Token is valid, proceed with fetching user data
                User user = userService.findByEmail(email);
                if (user != null) {
                    return ResponseEntity.ok(user);
                } else {
                    return ResponseEntity.notFound().build();
                }
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/user/watchlist/add")
    public ResponseEntity<String> addToWatchlist(@RequestParam String email, @RequestParam String ticker,
                                                @RequestHeader("Authorization") String idToken) {
        try {
            // Verify Firebase token
            if (idToken != null && idToken.startsWith("Bearer ")) {
                idToken = idToken.substring(7);
                String uid = firebaseAuthConfig.verifyToken(idToken);
                System.out.println("Token:" + uid);
                // Token is valid, proceed with adding to watchlist
                userService.addToWatchlist(email, ticker);
                return ResponseEntity.ok("Added to watchlist");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid authentication token");
            }
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: " + e.getMessage());
        }
    }

    @PostMapping("/user/watchlist/remove")
    public ResponseEntity<String> removeFromWatchlist(@RequestParam String email, @RequestParam String ticker,
                                                    @RequestHeader("Authorization") String idToken) {
        try {
            // Verify Firebase token
            if (idToken != null && idToken.startsWith("Bearer ")) {
                idToken = idToken.substring(7);
                String uid = firebaseAuthConfig.verifyToken(idToken);
                System.out.println("Token:" + uid);
                // Token is valid, proceed with removing from watchlist
                userService.removeFromWatchlist(email, ticker);
                return ResponseEntity.ok("Removed from watchlist");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid authentication token");
            }
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: " + e.getMessage());
        }
    }
}