package de.fhkl.imst.i.cgma.raytracer;

public class ColumnThreadWrapper {

    private Connection con;
    private int xp;
    private String datString;
    private int clientTmp;
    private int countClient;

    public ColumnThreadWrapper(Connection con, int xp, String datString, int clientTmp, int countClient) {
        this.con = con;
        this.xp = xp;
        this.datString = datString;
        this.clientTmp = clientTmp;
        this.countClient = countClient;
    }

    public Connection getCon() {
        return con;
    }

    public void setCon(Connection con) {
        this.con = con;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public String getDatString() {
        return datString;
    }

    public void setDatString(String datString) {
        this.datString = datString;
    }

    public int getClientTmp() {
        return clientTmp;
    }

    public void setClientTmp(int clientTmp) {
        this.clientTmp = clientTmp;
    }

    public int getCountClient() {
        return countClient;
    }

    public void setCountClient(int countClient) {
        this.countClient = countClient;
    }
}
