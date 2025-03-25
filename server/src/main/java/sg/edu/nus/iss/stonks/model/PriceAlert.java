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
        
        String[] parts = str.split(":");
        if (parts.length < 3) {
            return null;
        }
        
        PriceAlert alert = new PriceAlert();
        alert.setTicker(parts[0]);
        alert.setTargetPrice(Double.parseDouble(parts[1]));
        alert.setCondition(parts[2]);
        
        // If createdAt was included
        if (parts.length > 3) {
            alert.setCreatedAt(Long.parseLong(parts[3]));
        } else {
            alert.setCreatedAt(new Date().getTime());
        }
        
        return alert;
    }
}