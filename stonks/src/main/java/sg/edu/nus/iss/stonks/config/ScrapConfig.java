package sg.edu.nus.iss.stonks.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScrapConfig {

    @Value("${reddit.client.id}")
    private String redditId;

    @Value("${reddit.client.secret}")
    private String redditSecret;

    @Value("${reddit.username}")
    private String redditUsername;

    @Value("${reddit.password}")
    private String redditPassword;

    @Value("${finnhub.api.key}")
    private String finnhubApiKey;

    public String getRedditId() {
        return redditId;
    }

    public String getRedditSecret() {
        return redditSecret;
    }

    public String getRedditUsername() {
        return redditUsername;
    }

    public String getRedditPassword() {
        return redditPassword;
    }

    public String getFinnhubApiKey() {
        return finnhubApiKey;
    }
    
}
