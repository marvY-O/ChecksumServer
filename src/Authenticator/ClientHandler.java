package Authenticator;
import java.io.*;
import java.util.Random;
import Database.*;
import Message.*;
import java.net.*;
import java.util.HashMap;
import java.util.Queue;
import java.security.SecureRandom;

class ClientHandler implements Runnable {
    
    Socket s;
    public HashMap<InetAddress, Queue<Packet>> buffer;
    public HashMap<InetAddress, String> certIDStore;
	String serverIP;
      
    public ClientHandler(Socket sc, HashMap<InetAddress, Queue<Packet>> buffer, HashMap<InetAddress, String> certIDStore, String serverIP) throws IOException{
        s = sc;
        this.buffer = buffer;
        this.certIDStore = certIDStore;
		this.serverIP = serverIP;
    }

    public static String generate() {
    	String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        SecureRandom RANDOM = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
    
    public static byte[] tamperByteArray(byte[] data) {
    	if (data == null) return null;
        Random random = new Random();
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (data[i] ^ random.nextInt(256));
        }
        return data;
    }
  
    @Override
    public void run() {
    	ObjectOutputStream oos;
    	ObjectInputStream ois;

        try{
    		
    		oos = new ObjectOutputStream(s.getOutputStream());
    		ois = new ObjectInputStream(s.getInputStream());
    		
    		SecurityCertificate cert;
    		while (true) {
    			try {
    				cert = (SecurityCertificate) ois.readObject();
    				if (cert != null) break;
    			} catch(IOException e) {
    				
    			} catch(ClassNotFoundException e) {
    				
    			}
    		}
    		
    		if (dbUsers.verify(cert.username, cert.password)) {
    			cert.CertificateID = generate();
    			certIDStore.put(s.getInetAddress(), cert.CertificateID);
    		}
    		else {
    			cert.CertificateID = "NULL";
    		}

			System.out.printf("%s -> %s\n", s.getInetAddress().getHostAddress(), cert.CertificateID);
    		
    		oos.writeObject(cert);
    		
    		Random random = new Random();
    		int total = random.nextInt(1) + 0;
    		Counter cnt = new Counter();
    		
    		System.out.printf("Will tamper %d packets\n", total);
    		
    		Runnable receiver = new Runnable() {
                @Override
                public void run() {

                	while (true) {
                			Packet p;
							try {
								p = (Packet) ois.readObject();
								if (p.pkt_id == -1){
									System.out.printf("%s sending file to %s\n", p.client_ip, p.destination_ip);
									dbUsers.addEntry(p.client_ip, p.destination_ip, p.pkt_no);
								}
								else if (p.pkt_id == -2) {
									System.out.printf("Fetch request of %s from %s to %s\n", p.msg_name, p.client_ip, p.destination_ip);
								}
								InetAddress destAddr = InetAddress.getByName(p.destination_ip);
								synchronized (buffer) {
									if (buffer.containsKey(destAddr)) {
										buffer.get(destAddr).add(p);
									}
								}
							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								//e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								//e.printStackTrace();
							}
                	}
                	
                }
            };
            
            Runnable sender = new Runnable() {
                @Override
                public void run() {

                	while (true) {
                		if (buffer.get(s.getInetAddress()).size() == 0) continue;
                		Packet curPacket;
                		synchronized (buffer) {
	            			curPacket = buffer.get(s.getInetAddress()).poll();
                		}
            			try {
            				curPacket.cert_id = certIDStore.get(s.getInetAddress());
//            				System.out.printf("Sending to %s\n", curPacket.destination_ip);
            				
            				if (!curPacket.msg_name.equals("resend") && cnt.get() < total) {
								int dec = random.nextInt(2);
								if (dec == 1) {
									curPacket.payload = tamperByteArray(curPacket.payload);
//									System.out.printf("Tampered %d from %s to %s\n", curPacket.pkt_no, curPacket.client_ip, curPacket.destination_ip);
									cnt.inc();
								}
							}
            				
//            				if (curPacket.msg_name.equals("resend")) {
//            					System.out.printf("Resend request from %s\n", curPacket.client_ip);
//            				}
            				oos.writeObject(curPacket);
            			} catch(IOException e) {
            				e.printStackTrace();
            			}
                		
                	}
                	
                }
            };
            
            
            Thread recv = new Thread(receiver);
            recv.start();
            
            Thread send = new Thread(sender);
            send.start();
             
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }
       
    
                
}