package de.fhkl.imst.i.cgma.raytracer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class ConnectionThread implements Runnable {

    int i;
    String host;
    private static final int TIMEOUT = 500;
    List<String> reachableIPs = new ArrayList<String>();

    public ConnectionThread(String host, int i, List<String> reachableIPs) {
        this.i = i;
        this.host = host;
        this.reachableIPs = reachableIPs;
    }

    @Override
    public void run() {

        try {
            InetAddress ia = InetAddress.getByName(host);

            //            if (ia.isReachable(TIMEOUT) || !ia.getCanonicalHostName().equalsIgnoreCase(this.host)
            //                    || !ia.getHostName().equalsIgnoreCase(this.host)) {
            //                System.out.println("Reached " + this.host + "(" + ia.getCanonicalHostName() + ")");
            //                reachableIPs.add(host);
            //            }

            if (ia.isReachable(TIMEOUT)) {
                reachableIPs.add(host);
            }
        } catch (UnknownHostException e) {
//            e.printStackTrace();
        } catch (IOException e) {
//            e.printStackTrace();
        }
    }

    public List<String> getReachableIPs() {
        return reachableIPs;
    }

    public void setReachableIPs(List<String> reachableIPs) {
        this.reachableIPs = reachableIPs;
    }

    public static int getTimeout() {
        return TIMEOUT;
    }
}