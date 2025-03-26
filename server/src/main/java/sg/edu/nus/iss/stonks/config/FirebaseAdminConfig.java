package sg.edu.nus.iss.stonks.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct; // Use jakarta for Spring Boot 3+
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader; // To load based on path prefix (classpath:, file:)

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseAdminConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseAdminConfig.class);

    @Value("${app.firebase.service-account-key-path}")
    private String serviceAccountKeyPath;

    private final ResourceLoader resourceLoader;
    private FirebaseApp firebaseApp;

    // Inject ResourceLoader to handle classpath: or file: prefixes
    public FirebaseAdminConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initializeFirebaseAdmin() {
        try {
            Resource resource = resourceLoader.getResource(serviceAccountKeyPath);
            if (!resource.exists()) {
                 logger.error("Firebase service account key file not found at path: {}", serviceAccountKeyPath);
                 throw new IOException("Service account key file not found: " + serviceAccountKeyPath);
            }

            InputStream serviceAccount = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    // Optional: Add database URL if using RTDB/Firestore Admin features
                    // .setDatabaseUrl("https://<YOUR_PROJECT_ID>.firebaseio.com")
                    .build();

            // Initialize the default app if it hasn't been initialized yet
            if (FirebaseApp.getApps().isEmpty()) {
                this.firebaseApp = FirebaseApp.initializeApp(options);
                logger.info("Firebase Admin SDK initialized successfully.");
            } else {
                // If default app exists, try to get it. This might happen if another config init'd it.
                this.firebaseApp = FirebaseApp.getInstance();
                 logger.info("Firebase Admin SDK already initialized (default app found).");
                 // Optional: You could check if the existing app used the same credentials if needed.
            }

        } catch (IOException e) {
            logger.error("Error initializing Firebase Admin SDK: {}", e.getMessage(), e);
            // Depending on your app's requirements, you might want to throw a runtime exception
            // to prevent the application from starting without Firebase Admin.
            // throw new RuntimeException("Failed to initialize Firebase Admin SDK", e);
        } catch (IllegalStateException e) {
             logger.warn("Firebase Admin SDK initialization attempt encountered existing apps: {}", e.getMessage());
             // Attempt to get the default instance if available
             if (!FirebaseApp.getApps().isEmpty()) {
                  this.firebaseApp = FirebaseApp.getInstance();
                  logger.info("Using pre-existing default Firebase App instance for Admin SDK.");
             } else {
                 logger.error("Could not obtain Firebase App instance for Admin SDK.", e);
             }
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (this.firebaseApp == null) {
             logger.error("FirebaseApp not initialized. Cannot create FirebaseMessaging bean.");
             // Return null or throw an exception based on how critical FCM is at startup
             // throw new IllegalStateException("FirebaseApp not initialized.");
             return null; // Or handle appropriately
        }
        try {
            return FirebaseMessaging.getInstance(this.firebaseApp);
        } catch (IllegalStateException e) {
             logger.error("Could not get FirebaseMessaging instance: {}", e.getMessage(), e);
             return null; // Or handle appropriately
        }
    }
}