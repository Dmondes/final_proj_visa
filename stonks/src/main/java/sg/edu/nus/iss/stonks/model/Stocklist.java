package sg.edu.nus.iss.stonks.model;

public class Stocklist {
    private String symbol;
    private String companyName;

    public Stocklist() {
    }

    public Stocklist(String symbol, String companyName) {
        this.symbol = symbol;
        this.companyName = companyName;
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

}
