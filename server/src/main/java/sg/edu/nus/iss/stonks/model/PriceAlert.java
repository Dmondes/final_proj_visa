package sg.edu.nus.iss.stonks.model;

import java.util.Date;

public class PriceAlert {
    private String ticker;
    private double targetPrice;
    private String condition; // "above" or "below"
    private long createdAt;

    public PriceAlert() {
    }

    public PriceAlert(String ticker, double targetPrice, String condition) {
        this.ticker = ticker;
        this.targetPrice = targetPrice;
        this.condition = condition;
        this.createdAt = new Date().getTime();
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public double getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(double targetPrice) {
        this.targetPrice = targetPrice;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return ticker + ":" + targetPrice + ":" + condition;
    }

    /**
     * Parse a string representation back into a PriceAlert object
     */
    public static PriceAlert fromString(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        
        try {
            String[] parts = str.split(":");
            if (parts.length < 3) {
                return null;
            }
            
            PriceAlert alert = new PriceAlert();
            alert.setTicker(parts[0]);
            
            try {
                alert.setTargetPrice(Double.parseDouble(parts[1]));
            } catch (NumberFormatException e) {
                System.err.println("Error parsing target price: " + parts[1] + " - " + e.getMessage());
                return null;
            }
            
            alert.setCondition(parts[2]);
            
            if (parts.length > 3) {
                try {
                    alert.setCreatedAt(Long.parseLong(parts[3]));
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing createdAt timestamp: " + parts[3] + " - " + e.getMessage());
                    // Use current time as fallback
                    alert.setCreatedAt(new Date().getTime());
                }
            } else {
                alert.setCreatedAt(new Date().getTime());
            }
            
            return alert;
        } catch (Exception e) {
            System.err.println("Unexpected error parsing price alert from string: " + str + " - " + e.getMessage());
            return null;
        }
    }
}