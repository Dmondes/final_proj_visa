package sg.edu.nus.iss.stonks.model;

public class Stock {
    private String symbol;
    private String companyName;
    private Double marketCap;
    private int ipoYear;
    private Double volume;
    private String sector;
    private String industry;

    public Stock() {
    }

    public Stock(String symbol, String companyName, Double marketCap, int ipoYear, Double volume, String sector,
            String industry) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.marketCap = marketCap;
        this.ipoYear = ipoYear;
        this.volume = volume;
        this.sector = sector;
        this.industry = industry;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Double getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(Double marketCap) {
        this.marketCap = marketCap;
    }

    public int getIpoYear() {
        return ipoYear;
    }

    public void setIpoYear(int ipoYear) {
        this.ipoYear = ipoYear;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

}
