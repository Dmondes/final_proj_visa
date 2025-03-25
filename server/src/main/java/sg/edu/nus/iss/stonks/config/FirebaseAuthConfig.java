package sg.edu.nus.iss.stonks.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.io.IOException;

// import org.springframework.core.io.ClassPathResource;
// import java.io.InputStream;

@Configuration
public class FirebaseAuthConfig {

    @Autowired
    private Environment env;

    @Bean
    public FirebaseAuth firebaseAuth() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                // (InputStream serviceAccount =
                // newClassPathResource("serviceAccountKey.json").getInputStream())

                // FirebaseOptions options = FirebaseOptions.builder()
                // .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                // .setProjectId("fintrenduser")
                // .build();

                String firebaseCredentialsJson = env.getProperty("FIREBASE_CREDENTIALS");
                FirebaseOptions options;

                if (firebaseCredentialsJson != null && !firebaseCredentialsJson.isEmpty()) {
                    // Use the full JSON from FIREBASE_CREDENTIALS
                    options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(
                                    new ByteArrayInputStream(firebaseCredentialsJson.getBytes())))
                            .build();

                    FirebaseApp.initializeApp(options);
                }

            } catch (IOException e) { // Fallback to local run for test
                System.err.println("Error initializing Firebase: " + e.getMessage());
                e.printStackTrace(); // Print the stack trace for debugging.
                try {
                    FirebaseOptions fallbackOptions = FirebaseOptions.builder()
                            .setProjectId("fintrenduser")
                            .build();
                    FirebaseApp.initializeApp(fallbackOptions, "fallbackApp");
                } catch (Exception fallbackException) {
                    System.err.println("Error initializing Firebase fallback: " + fallbackException.getMessage());
                    fallbackException.printStackTrace();
                    throw fallbackException;
                }
            }
        }
        return (FirebaseApp.getApps().size() > 1 && FirebaseApp.getInstance("fallbackApp") != null)
                ? FirebaseAuth.getInstance(FirebaseApp.getInstance("fallbackApp"))
                : FirebaseAuth.getInstance();
    }

    public String verifyToken(String idToken) throws FirebaseAuthException {
        FirebaseAuth auth = (FirebaseApp.getApps().size() > 1 && FirebaseApp.getInstance("fallbackApp") != null)
                ? FirebaseAuth.getInstance(FirebaseApp.getInstance("fallbackApp"))
                : FirebaseAuth.getInstance();

        FirebaseToken decodedToken = auth.verifyIdToken(idToken);
        return decodedToken.getUid();
    }

    private String createFirebaseCredentialsJson() {
        // Create a JSON string from the individual properties
        return "{\n" +
                "  \"type\": \"" + env.getProperty("firebase.credentials.type") + "\",\n" +
                "  \"project_id\": \"" + env.getProperty("firebase.credentials.project.id") + "\",\n" +
                "  \"private_key_id\": \"" + env.getProperty("firebase.credentials.private.key.id") + "\",\n" +
                "  \"private_key\": \"" + env.getProperty("firebase.credentials.private.key") + "\",\n" +
                "  \"client_email\": \"" + env.getProperty("firebase.credentials.client.email") + "\",\n" +
                "  \"client_id\": \"" + env.getProperty("firebase.credentials.client.id") + "\",\n" +
                "  \"auth_uri\": \"" + env.getProperty("firebase.credentials.auth.uri") + "\",\n" +
                "  \"token_uri\": \"" + env.getProperty("firebase.credentials.token.uri") + "\",\n" +
                "  \"auth_provider_x509_cert_url\": \""
                + env.getProperty("firebase.credentials.auth.provider.x509.cert.url") + "\",\n" +
                "  \"client_x509_cert_url\": \"" + env.getProperty("firebase.credentials.client.x509.cert.url") + "\"\n"
                +
                "}";
    }
}