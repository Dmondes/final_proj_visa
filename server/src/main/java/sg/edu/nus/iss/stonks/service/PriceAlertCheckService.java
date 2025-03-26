package sg.edu.nus.iss.stonks.service;

import com.google.firebase.messaging.*; // Import all FCM types
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sg.edu.nus.iss.stonks.model.PriceAlert;
import sg.edu.nus.iss.stonks.model.StockPrice; // Import StockPrice model
import sg.edu.nus.iss.stonks.model.User;
import sg.edu.nus.iss.stonks.repo.MysqlRepo;

import java.text.DecimalFormat;
import java.util.HashMap; // Import HashMap
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PriceAlertCheckService {

    private static final Logger logger = LoggerFactory.getLogger(PriceAlertCheckService.class);
    private static final DecimalFormat df = new DecimalFormat("0.00");
    private final Map<String, Long> triggeredAlertCooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_PERIOD_MS = 6 * 60 * 60 * 1000; // 6 hours cooldown

    @Autowired
    private MysqlRepo mysqlRepo;

    private ScrapService scrapService;

    @Autowired(required = false)
    private FirebaseMessaging firebaseMessaging;

    @Scheduled(cron = "${scheduler.price_alert_check.cron}")
    public void checkPriceAlerts() {
        if (firebaseMessaging == null) {
            logger.warn("FirebaseMessaging not available. Skipping price alert check.");
            return;
        }
        logger.info("Running scheduled price alert check...");

        long currentTime = System.currentTimeMillis();

        List<User> usersWithAlerts = mysqlRepo.findAllUsersWithAlertsAndToken();
        if (usersWithAlerts.isEmpty()) {
            logger.info("No users found with active price alerts and FCM tokens.");
            return;
        }

        Set<String> tickersToCheck = new HashSet<>();
        for (User user : usersWithAlerts) {
            if (user.getPriceAlerts() != null) {
                tickersToCheck.addAll(user.getPriceAlerts().keySet());
            }
        }

        if (tickersToCheck.isEmpty()) {
            logger.info("No tickers found in active alerts across users.");
            return;
        }

        // --- Fetch prices using ScrapService ---
        logger.info("Fetching current prices via ScrapService for tickers: {}", tickersToCheck);
        Map<String, Double> currentPrices = new HashMap<>();
        for (String ticker : tickersToCheck) {
            try {
                StockPrice stockPrice = scrapService.getStockDetails(ticker);
                // Check if StockPrice object is valid and current price 'c' is positive
                if (stockPrice != null && stockPrice.getC() > 0) {
                    currentPrices.put(ticker, stockPrice.getC());
                } else {
                    logger.warn("Could not get valid current price for ticker {} via ScrapService. Received: {}", ticker, stockPrice);
                }
            } catch (Exception e) {
                // Catch potential exceptions from scrapService.getStockDetails
                logger.error("Error fetching details for ticker {} via ScrapService: {}", ticker, e.getMessage());
            }
        }

        if (currentPrices.isEmpty()) {
            logger.warn("Could not fetch any valid current prices via ScrapService for tickers: {}", tickersToCheck);
            return;
        }
        logger.debug("Fetched prices via ScrapService: {}", currentPrices);


        // --- Alert Checking Logic ---
        for (User user : usersWithAlerts) {
            if (user.getPriceAlerts() == null || user.getPriceAlerts().isEmpty() || user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
                continue;
            }

            for (Map.Entry<String, PriceAlert> entry : user.getPriceAlerts().entrySet()) {
                String ticker = entry.getKey();
                PriceAlert alert = entry.getValue();
                Double currentPrice = currentPrices.get(ticker); // Get price from the map we just populated

                if (currentPrice == null) {
                    // Price fetch failed for this specific ticker, skip check
                    continue;
                }

                String cooldownKey = user.getId() + ":" + ticker;

                // Cooldown Check (remains same)
                if (triggeredAlertCooldowns.containsKey(cooldownKey)) {
                     if (currentTime < triggeredAlertCooldowns.get(cooldownKey)) {
                          logger.trace("Alert for {}/{} is in cooldown for user {}.", ticker, alert.getCondition(), user.getEmail());
                          continue;
                     } else {
                         triggeredAlertCooldowns.remove(cooldownKey);
                         logger.info("Cooldown expired for {}/{} for user {}.", ticker, alert.getCondition(), user.getEmail());
                     }
                }

                // Trigger Check (remains same)
                boolean alertTriggered = false;
                if ("above".equalsIgnoreCase(alert.getCondition()) && currentPrice >= alert.getTargetPrice()) {
                    alertTriggered = true;
                    logger.debug("Trigger condition MET: {} >= {} (target) for Ticker {} (User {})", currentPrice, alert.getTargetPrice(), ticker, user.getEmail());
                } else if ("below".equalsIgnoreCase(alert.getCondition()) && currentPrice <= alert.getTargetPrice()) {
                    alertTriggered = true;
                    logger.debug("Trigger condition MET: {} <= {} (target) for Ticker {} (User {})", currentPrice, alert.getTargetPrice(), ticker, user.getEmail());
                } else {
                     logger.trace("Trigger condition NOT MET for Ticker {} (User {}): Current {}, Target {}, Condition {}", ticker, user.getEmail(), currentPrice, alert.getTargetPrice(), alert.getCondition());
                }


                if (alertTriggered) {
                    logger.info("ALERT TRIGGERED for user {} ({}) - Ticker: {}, Condition: {}, Target: {}, Current: {}",
                            user.getEmail(), user.getId(), ticker, alert.getCondition(), df.format(alert.getTargetPrice()), df.format(currentPrice));

                    // Send Notification (remains same)
                    sendFcmNotification(user, alert, currentPrice);

                    // Apply Cooldown (remains same)
                    triggeredAlertCooldowns.put(cooldownKey, currentTime + COOLDOWN_PERIOD_MS);
                    logger.info("Applied {}ms cooldown for {}/{} for user {}.", COOLDOWN_PERIOD_MS, ticker, alert.getCondition(), user.getEmail());

                    // Optional: Remove alert logic (remains same, commented out)
                }
            }
        }
        logger.info("Price alert check finished.");
    }

    // --- sendFcmNotification method (Remains the same) ---
    private void sendFcmNotification(User user, PriceAlert alert, double currentPrice) {
        // ... (Keep the implementation from the previous response) ...
        String title = String.format("%s Price Alert Triggered", alert.getTicker());
        String body = String.format("%s price is now %s your target of $%s. Current price: $%s",
                alert.getTicker(),
                alert.getCondition(),
                df.format(alert.getTargetPrice()),
                df.format(currentPrice));

        Notification notificationPayload = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setNotification(notificationPayload)
                .setToken(user.getFcmToken())
                .putData("ticker", alert.getTicker())
                .putData("targetPrice", df.format(alert.getTargetPrice()))
                .putData("currentPrice", df.format(currentPrice))
                .putData("condition", alert.getCondition())
                .putData("clickAction", "/stock/" + alert.getTicker())
                .build();

        try {
            String response = firebaseMessaging.send(message);
            logger.info("Successfully sent FCM alert to {} for {}: {}", user.getEmail(), alert.getTicker(), response);
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send FCM alert to {} for {}: Code={}, Message={}",
                         user.getEmail(), alert.getTicker(), e.getMessagingErrorCode(), e.getMessage());

            MessagingErrorCode errorCode = e.getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                logger.warn("FCM Token for user {} ({}) is invalid or unregistered. Removing token from DB.", user.getEmail(), user.getId());
                try {
                    mysqlRepo.removeFcmToken(user.getId());
                } catch (Exception removeEx) {
                     logger.error("Failed to remove invalid FCM token for user {}: {}", user.getId(), removeEx.getMessage());
                }
            }
        } catch (Exception e) {
             logger.error("Unexpected error sending FCM alert to {} for {}: {}", user.getEmail(), alert.getTicker(), e.getMessage(), e);
        }
    }
}