package Machine;
import java.io.*;
import java.net.*;
import java.util.*;

public class run {
    public static void main (String args[]) throws IOException{
    	
    	String clientIP = "192.168.1.21";
        
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isUp() && (iface.getName().startsWith("w") || iface.getName().startsWith("enp0s3")) ) { // filter by WiFi interfaces
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr.getAddress().length == 4) { // filter IPv4 addresses
                            clientIP = addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("Error getting network interfaces: " + e.getMessage());
        }
        
    	Scanner sc = new Scanner(System.in);
    	System.out.printf("Enter IP Address of the server: ");
    	String ac_address = sc.next();
       
        System.out.printf("Enter port of the server: ");
    	int port = sc.nextInt();
        
        Machine m = new Machine(ac_address, port, clientIP);
        m.initiate();
    }
}
