package de.fhkl.imst.i.cgma.raytracer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServerThread extends Thread {

    private TCPSocket tcpSocket;
    private String recieve;
    private String clientIP;
    private int xp;
    private String dat;
    private boolean terminate = false;
    private List<ColumnThreadWrapper> ctw = new ArrayList<ColumnThreadWrapper>();
    private boolean reachable = true;

    public boolean getReachable() {
        return reachable;
    }

    public ServerThread() {

    }

    public ServerThread(TCPSocket tcpSocket, String recieve) {
        this.tcpSocket = tcpSocket;
        this.recieve = recieve;
        this.start();
    }

    @Override
    public void run() {
        try {
            recieve = tcpSocket.receiveLine();
            if (recieve.equals("Are you a Server?")) {
                tcpSocket.sendLine("yes");
            }
            recieve = tcpSocket.receiveLine();
            clientIP = recieve;
            System.out.println("ClientIP = " + clientIP);
            //execute client requests
            while (terminate == false) {
                while (ctw.size() == 0) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        System.out.println("Unable to sleep");
                    }
                }
                while (ctw.size() > 0) {
                    int xp = ctw.get(0).getXp();
                    String dat = ctw.get(0).getDatString();
                    String recieve = sendData(xp, dat);
                    if (recieve != null) {
                        if (!recieve.equals("")) {
                            if (!recieve.equals("foo")) {
                                String[] recieveSplit = recieve.split("[;]");
                                int xpValue = 0;
                                for (int i = 0; i < recieveSplit.length; i++) {
                                    String[] split = recieveSplit[i].split("[,]");
                                    if (split.length == 3) {
                                        xpValue = Integer.parseInt(split[0]);
                                        if (!split[2].equals("null")) {
                                            int ypValue = Integer.parseInt(split[1]);
                                            int rgbValue = Integer.parseInt(split[2]);
                                            Raytracer06.INSTANCE.gui.setPixel(xpValue, ypValue, rgbValue);
                                        }
                                    }
                                }
                                Raytracer06.INSTANCE.removeValueFromIntList(xpValue);
                                ctw.remove(0);
                            } else {
                                if (Raytracer06.INSTANCE.countClient >= 1) {
                                    --Raytracer06.INSTANCE.countClient;
                                    Raytracer06.INSTANCE.howManyClients();
                                }
                                terminate = true;
                                break;
                            }
                        } else {
                            if (Raytracer06.INSTANCE.countClient >= 1) {
                                --Raytracer06.INSTANCE.countClient;
                                Raytracer06.INSTANCE.howManyClients();
                            }
                            terminate = true;
                            break;
                        }
                    } else {
                        terminate = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("serverthread run ");
        }

        System.out.println("Close Connection!");
        terminate = true;
        try {
            tcpSocket.close();
        } catch (IOException e) {
            System.out.println("close didn't work");
        }
    }

    public String sendData(int xp, String dat) {
        recieve = "foo";
        try {
            Integer xpInteger = new Integer(xp);
            String xpString = xpInteger.toString();
            tcpSocket.sendLine(xpString);
            recieve = tcpSocket.receiveLine();
        } catch (Exception e) {
            System.out.println("Can't reach client; xp: " + xp);
            recieve = "foo";
        }
        return recieve;
    }

    public void sendDatString(String datString) {
        try {
            tcpSocket.sendLine(datString);
        } catch (IOException e) {
            System.out.println("sendDatString didn't work! " + e);
        }
    }

    public void addToList(Connection con, int xp, String datString, int clientTmp, int countClient) {
        ctw.add(new ColumnThreadWrapper(con, xp, datString, clientTmp, countClient));
    }

    public void clearList() {
        ctw.clear();
    }

    public String getClientIP() {
        return clientIP;
    }

    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public String getDat() {
        return dat;
    }

    public void setDat(String dat) {
        this.dat = dat;
    }

    public boolean isTerminate() {
        return terminate;
    }

    public void setTerminate(boolean terminate) {
        this.terminate = terminate;
    }

    public TCPSocket getTcpSocket() {
        return tcpSocket;
    }

    public void setTcpSocket(TCPSocket tcpSocket) {
        this.tcpSocket = tcpSocket;
    }

    public String getRecieve() {
        return recieve;
    }

    public void setRecieve(String recieve) {
        this.recieve = recieve;
    }

    public List<ColumnThreadWrapper> getCtw() {
        return ctw;
    }

    public void setCtw(List<ColumnThreadWrapper> ctw) {
        this.ctw = ctw;
    }
}