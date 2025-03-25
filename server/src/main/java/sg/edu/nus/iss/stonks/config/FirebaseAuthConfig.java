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
// import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
// import java.io.InputStream;

@Configuration
public class FirebaseAuthConfig {

    @Autowired
    private Environment env;

    @Bean
    public FirebaseAuth firebaseAuth() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                // (InputStream serviceAccount = new
                // ClassPathResource("serviceAccountKey.json").getInputStream())
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(
                                new ByteArrayInputStream(createFirebaseCredentialsJson().getBytes())))
                        .setProjectId(env.getProperty("firebase.project.id"))
                        .build();

                FirebaseApp.initializeApp(options);

            } catch (IOException e) {
                // Log the exception properly. Don't just swallow it.
                System.err.println("Error initializing Firebase: " + e.getMessage());
                e.printStackTrace(); // Print the stack trace for debugging.

                // Fallback for development (optional, but good for local testing)
                // Remove the fallback if you *only* want to use the service account.
                try {
                    FirebaseOptions fallbackOptions = FirebaseOptions.builder()
                            .setProjectId("fintrenduser") // Set project ID for fallback as well.
                            // .setApplicationId("1:431029665236:web:d922b37d16b2a9bda6aa41") // Usually not
                            // needed with Admin SDK
                            .build();
                    FirebaseApp.initializeApp(fallbackOptions, "fallbackApp"); // Use a different name for the fallback
                                                                               // app
                } catch (Exception fallbackException) {
                    System.err.println("Error initializing Firebase fallback: " + fallbackException.getMessage());
                    fallbackException.printStackTrace();
                    throw fallbackException; // Re-throw to halt application startup if initialization fails
                }
            }
        }

        // Return the *default* instance if using service account, otherwise return
        // fallback.
        return (FirebaseApp.getApps().size() > 1 && FirebaseApp.getInstance("fallbackApp") != null)
                ? FirebaseAuth.getInstance(FirebaseApp.getInstance("fallbackApp"))
                : FirebaseAuth.getInstance();
    }

    public String verifyToken(String idToken) throws FirebaseAuthException {
        // Get the correct FirebaseAuth instance. If the fallback was used,
        // we need to use the fallback instance; otherwise, use the default.
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