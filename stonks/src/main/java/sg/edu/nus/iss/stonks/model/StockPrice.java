package sg.edu.nus.iss.stonks.model;

public class StockPrice {
    private double c; // Current price
    private double d; // Absolute change
    private double dp; // Percent change
    private double h; // High price
    private double l; // Low price
    private double o; // Open price
    private double pc; // Previous close price
    private double t; // Timestamp

    public StockPrice() {
    }

    public StockPrice(double c, double d, double dp, double h, double l,
            double o, double pc, double t) {
        this.c = c;
        this.d = d;
        this.dp = dp;
        this.h = h;
        this.l = l;
        this.o = o;
        this.pc = pc;
        this.t = t;
    }

    public double getC() {
        return c;
    }

    public void setC(double c) {
        this.c = c;
    }

    public double getD() {
        return d;
    }

    public void setD(double d) {
        this.d = d;
    }

    public double getDp() {
        return dp;
    }

    public void setDp(double dp) {
        this.dp = dp;
    }

    public double getH() {
        return h;
    }

    public void setH(double h) {
        this.h = h;
    }

    public double getL() {
        return l;
    }

    public void setL(double l) {
        this.l = l;
    }

    public double getO() {
        return o;
    }

    public void setO(double o) {
        this.o = o;
    }

    public double getPc() {
        return pc;
    }

    public void setPc(double pc) {
        this.pc = pc;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }
}
