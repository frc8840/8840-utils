package frc.team_8840_lib.utils.http;

public class IP {
    public static String getIP() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}