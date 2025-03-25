package sg.edu.nus.iss.stonks.restcontroller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<?> register(@RequestBody(required = false) Map<String, String> requestBody,
                                      @RequestParam(required = false) String email, 
                                      @RequestParam(required = false) String password,
                                      @RequestHeader("Authorization") String idToken) {
        try {
            // Try to get email and password from request body first, fall back to request params
            String userEmail = (requestBody != null && requestBody.get("email") != null) 
                               ? requestBody.get("email") : email;
            String userPassword = (requestBody != null && requestBody.get("password") != null) 
                                 ? requestBody.get("password") : password;
            
            if (userEmail == null || userPassword == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email and password are required");
            }
            
            // Verify Firebase token
            if (idToken != null && idToken.startsWith("Bearer ")) {
                idToken = idToken.substring(7);
                String uid = firebaseAuthConfig.verifyToken(idToken);
                System.out.println("Token:" + uid);
                System.out.println("Registering user: " + userEmail);
                
                // Token is valid, proceed with user registration
                try {
                    boolean newUser = userService.registerUser(userEmail, userPassword);
                    if (newUser == false) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
                    }
                    return ResponseEntity.status(HttpStatus.CREATED).body("Registration successful");
                } catch (Exception e) {
                    System.err.println("Error registering user: " + e.getMessage());
                    e.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body("Registration failed: " + e.getMessage());
                }
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid authentication token");
            }
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during registration: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Registration failed due to an unexpected error");
        }
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email, 
                                               @RequestHeader("Authorization") String idToken) {
        System.out.println("Received request for user: " + email);
        try {
            // Verify Firebase token
            if (idToken != null && idToken.startsWith("Bearer ")) {
                idToken = idToken.substring(7);
                try {
                    String uid = firebaseAuthConfig.verifyToken(idToken);
                    System.out.println("Firebase token verified. UID: " + uid);
                    
                    // Token is valid, proceed with fetching user data
                    try {
                        User user = userService.findByEmail(email);
                        if (user != null) {
                            System.out.println("User found: " + user.getEmail());
                            return ResponseEntity.ok(user);
                        } else {
                            System.out.println("User not found: " + email);
                            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .header("X-Error-Message", "User not found in database")
                                .build();
                        }
                    } catch (Exception dbException) {
                        System.err.println("Database error when finding user " + email + ": " + dbException.getMessage());
                        dbException.printStackTrace();
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .header("X-Error-Message", "Database error: " + dbException.getMessage())
                            .build();
                    }
                } catch (FirebaseAuthException e) {
                    System.err.println("Firebase authentication error for user " + email + ": " + e.getMessage());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header("X-Error-Message", "Firebase auth error: " + e.getMessage())
                        .build();
                }
            } else {
                System.err.println("Invalid Authorization header for user " + email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("X-Error-Message", "Invalid authorization header")
                    .build();
            }
        } catch (Exception e) {
            System.err.println("Unexpected error processing request for user " + email + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Error-Message", "Unexpected error: " + e.getMessage())
                .build();
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
    
    @PostMapping("/user/price-alert/set")
    public ResponseEntity<String> setPriceAlert(@RequestParam String email, 
                                               @RequestBody Map<String, Object> payload,
                                               @RequestHeader("Authorization") String idToken) {
        try {
            // Verify Firebase token
            if (idToken != null && idToken.startsWith("Bearer ")) {
                idToken = idToken.substring(7);
                String uid = firebaseAuthConfig.verifyToken(idToken);
                System.out.println("Token:" + uid);
                
                // Extract parameters from request body
                String ticker = (String) payload.get("ticker");
                Double targetPrice = ((Number) payload.get("targetPrice")).doubleValue();
                String condition = (String) payload.get("condition");
                
                if (ticker == null || targetPrice == null || condition == null) {
                    return ResponseEntity.badRequest().body("Missing required parameters");
                }
                
                // Token is valid, proceed with setting price alert
                userService.setPriceAlert(email, ticker, targetPrice, condition);
                return ResponseEntity.ok("Price alert set");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid authentication token");
            }
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        }
    }
    
    @PostMapping("/user/price-alert/remove")
    public ResponseEntity<String> removePriceAlert(@RequestParam String email, @RequestParam String ticker,
                                                 @RequestHeader("Authorization") String idToken) {
        try {
            // Verify Firebase token
            if (idToken != null && idToken.startsWith("Bearer ")) {
                idToken = idToken.substring(7);
                String uid = firebaseAuthConfig.verifyToken(idToken);
                System.out.println("Token:" + uid);
                // Token is valid, proceed with removing price alert
                userService.removePriceAlert(email, ticker);
                return ResponseEntity.ok("Price alert removed");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid authentication token");
            }
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/user/fcm-token")
    public ResponseEntity<String> updateFcmToken(@RequestParam String email, 
                                               @RequestBody Map<String, String> payload,
                                               @RequestHeader("Authorization") String idToken) {
        try {
            // Verify Firebase token
            if (idToken != null && idToken.startsWith("Bearer ")) {
                idToken = idToken.substring(7);
                String uid = firebaseAuthConfig.verifyToken(idToken);
                System.out.println("Token:" + uid);
                
                // Extract FCM token from request body
                String fcmToken = payload.get("token");
                if (fcmToken == null) {
                    return ResponseEntity.badRequest().body("Missing FCM token");
                }
                
                // Token is valid, proceed with updating FCM token
                userService.updateFcmToken(email, fcmToken);
                return ResponseEntity.ok("FCM token updated");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid authentication token");
            }
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed: " + e.getMessage());
        }
    }
}