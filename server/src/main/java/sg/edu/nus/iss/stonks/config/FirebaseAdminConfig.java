package sg.edu.nus.iss.stonks.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Configuration
public class FirebaseAdminConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseAdminConfig.class);

    @Autowired
    private Environment env;
    
    private FirebaseApp firebaseApp;

    @PostConstruct
    public void initializeFirebaseAdmin() {
        try {
            // check if Firebase App is already initialized
            if (!FirebaseApp.getApps().isEmpty()) {
                this.firebaseApp = FirebaseApp.getInstance();
                logger.info("Firebase Admin SDK already initialized (default app found).");
                return;
            }
            
            String firebaseCredentialsJson = env.getProperty("FIREBASE_CREDENTIALS");
            
            if (firebaseCredentialsJson != null && !firebaseCredentialsJson.isEmpty()) {
                // Use the full JSON from FIREBASE_CREDENTIALS environment variable
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(
                                new ByteArrayInputStream(firebaseCredentialsJson.getBytes())))
                        .build();

                this.firebaseApp = FirebaseApp.initializeApp(options);
                logger.info("Firebase Admin SDK initialized successfully using environment credentials.");
            } else {
                logger.error("Firebase credentials not found in environment variables.");
                // Fallback to default initialization for local testing
                try {
                    FirebaseOptions fallbackOptions = FirebaseOptions.builder()
                            .setProjectId("fintrenduser")
                            .build();
                    this.firebaseApp = FirebaseApp.initializeApp(fallbackOptions, "adminFallbackApp");
                    logger.info("Firebase Admin SDK initialized with fallback options for local testing.");
                } catch (Exception fallbackException) {
                    logger.error("Error initializing Firebase Admin fallback: {}", fallbackException.getMessage(), fallbackException);
                    throw fallbackException;
                }
            }
        } catch (IOException e) {
            logger.error("Error initializing Firebase Admin SDK: {}", e.getMessage(), e);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (this.firebaseApp == null) {
            logger.error("FirebaseApp not initialized. Cannot create FirebaseMessaging bean.");
            if (!FirebaseApp.getApps().isEmpty()) {
                // Use the default app or the fallback app if available
                FirebaseApp app = FirebaseApp.getApps().stream()
                        .filter(a -> a.getName().equals(FirebaseApp.DEFAULT_APP_NAME) || 
                                    a.getName().equals("adminFallbackApp"))
                        .findFirst()
                        .orElse(null);
                        
                if (app != null) {
                    return FirebaseMessaging.getInstance(app);
                }
            }
            return null;
        }
        
        try {
            return FirebaseMessaging.getInstance(this.firebaseApp);
        } catch (IllegalStateException e) {
            logger.error("Could not get FirebaseMessaging instance: {}", e.getMessage(), e);
            return null;
        }
    }
}