package Machine;
import java.io.*;  
import java.nio.file.Files;
import java.util.*;
import java.net.*; 
import Message.*; 
//import org.apache.commons.lang3.ArrayUtils;

public class Machine{
    //private InetAddress selfAddress;
    String ac_address, clientIP;
    int ac_port;
    Queue<Packet> buffer;
    Queue<Packet> receiveBuffer;

    public Machine(String ac_address, int ac_port, String clientIP) throws IOException{
        //selfAddress  = Binder.getAddress();
        this.ac_address = ac_address;
        this.clientIP = clientIP;
        this.ac_port = ac_port;
        this.buffer = new LinkedList<Packet>();
        this.receiveBuffer = new LinkedList<Packet>();
    }
    
    public static List<byte[]> divideFileIntoPackets(File file, int payload_size) throws IOException {
        List<byte[]> packets = new ArrayList<>();
        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[payload_size];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] packet = new byte[bytesRead];
                System.arraycopy(buffer, 0, packet, 0, bytesRead);
                packets.add(packet);
            }
        }
        return packets;
    }
    
    public static void convertPacketsToFile(Vector<byte[]> packets, File file) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(file)) {
            for (byte[] packet : packets) {
                outputStream.write(packet);
            }
        }
    }
    
    public static void writeByteArrayToFile(byte[] data, String filename) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(filename)) {
            outputStream.write(data);
        }
    }

    public void initiate() throws IOException{
    	Scanner sc = new Scanner(System.in);
        try{

            Socket s = new Socket(ac_address, ac_port);
            
            ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
            System.out.printf("Connected to server %s:%d\n", ac_address, ac_port);
            
            
            String certID;
            SecurityCertificate cert = new SecurityCertificate();
            
            System.out.printf("Enter username: ");
//            cert.username = sc.next();
            cert.username = "marvy";
            
            System.out.printf("Enter password: ");
//            cert.password = sc.next();
            cert.password = "admin";
            
            oos.writeObject(cert);
            
    		while (true) {
    			try {
    				cert = (SecurityCertificate) ois.readObject();
    				if (cert != null) break;
    			} catch(IOException e) {
    				
    			} catch(ClassNotFoundException e) {
    				
    			}
    		}
    		
    		if (cert.CertificateID.equals("NULL")) {
    			System.out.println("Wrong Credentials provided!!");
    			return;
    		}	
            
    		certID = cert.CertificateID;
    		System.out.printf("Security ID: %sn\n",certID);
            
            System.out.printf("1.Send file\n2.Recieve file\n\n>>");
            int x = sc.nextInt();
            sc.nextLine();
            
            int payload_size = 100;

            if (x == 1) {
            	//send file
            	System.out.printf("Enter file name: ");
//            	String fileName = sc.nextLine();
            	System.out.printf("Enter destination IP: ");
//            	String destIP = sc.nextLine();
            	String destIP = "192.168.1.21";
            	String fileName = "file.txt";
            	
            	File file = new File(fileName);
            	List<byte[]> byteArray = divideFileIntoPackets(file, payload_size);
            	System.out.println("Number of packets: " + byteArray.size());
            	System.out.println("Payload size: " + payload_size);
            	List<Packet> packets = new ArrayList<Packet>();
            	int cnt = 0;
            	for (byte[] packet: byteArray) {
            		Packet p = new Packet();
            		p.payload = packet;
            		p.client_name = "marvy";
            		p.client_ip = clientIP;
            		p.destination_ip = destIP;
            		p.pkt_no = cnt;
            		p.pkt_id = cnt;
            		p.checksum = Checksum.computeChecksum(packet);
            		p.msg_name = fileName;
            		p.cert_id = certID;
            		cnt+=1;
            		packets.add(p);
            	}
            	
            	Packet firstPacket = new Packet();
            	firstPacket.payload = null;
            	firstPacket.client_name = "marvy";
            	firstPacket.client_ip = clientIP;
            	firstPacket.destination_ip = destIP;
            	firstPacket.pkt_no = packets.size();
        		firstPacket.pkt_id = 0;
        		firstPacket.checksum = -1;
        		firstPacket.msg_name = fileName;
        		firstPacket.cert_id = certID;
        		
        		oos.writeObject(firstPacket);
        		
            	
            	Queue<Integer> sendBuffer = new LinkedList<Integer>();
            	
            	Runnable sender = new Runnable() {
            		@Override
            		public void run() {
            			while (true) {
            				Packet p;
            				if (sendBuffer.isEmpty()) continue;
            				synchronized(sendBuffer) {
            					System.out.printf("Sending packet again..");
            					p = packets.get(sendBuffer.poll());
            				}
            				try{
            					oos.writeObject(p);
            				} catch(IOException e) {
            					e.printStackTrace();
            				}
            			}
            		}
            	};
            	
            	Runnable reciever = new Runnable() {
            		@Override
            		public void run() {
            			while (true) {
            				try {
            					Packet p = (Packet) ois.readObject();
            					if (p.pkt_id == packets.size()) {
            						System.out.printf("File Send Successfully!");
            						return;
            					}
            					synchronized(sendBuffer) {
            						sendBuffer.add(p.pkt_id);
            					}
            					
            					
            				} catch (IOException e) {
            					e.printStackTrace();
            				} catch (ClassNotFoundException e) {
            					e.printStackTrace();
            				}
            				
            				
            			}
            		}
            	};
            	
            	Thread recv = new Thread(reciever);
                recv.start();
                
                Thread send = new Thread(sender);
                send.start();
                
                System.out.printf("Sending packets..\n");
        		
        		for (Packet packet: packets) {
//        			System.out.printf("Sending  %d..\n", packet.pkt_id);
        			oos.writeObject(packet);
        		}
        		
        		System.out.printf("Sent packets..\n");
            	
            }
            else if (x == 2) {
            	System.out.printf("Waiting for file..\n");
            	Packet firstPacket = (Packet) ois.readObject();
            	int totalPackets = firstPacket.pkt_no;
            	String fileName = firstPacket.msg_name;
            	String destIP = firstPacket.client_ip;
            	System.out.printf("Ready to recieve %s\n", firstPacket.msg_name);
            	
            	Vector<byte[]> packets = new Vector<byte[]>();
            	packets.setSize(totalPackets);
            	
            	Queue<Integer> sendBuffer = new LinkedList<Integer>();
            	final Counter recievedPackets = new Counter();
            	
            	Runnable sender = new Runnable() {
            		@Override
            		public void run() {
            			while (true) {
            				if (sendBuffer.isEmpty()) continue;
            				Packet p = new Packet();
            				p.client_ip = clientIP;
            				p.destination_ip = destIP;
            				p.pkt_no = -1;
            				p.msg_name = "resend";
            				p.cert_id = certID;
 
        					synchronized(sendBuffer) {
        						p.pkt_id = sendBuffer.poll();
            				}
            			
            				try{
            					oos.writeObject(p);
            				} catch(IOException e) {
            					e.printStackTrace();
            				}
            			}
            		}
            	};
            	
            	Runnable reciever = new Runnable() {
            		@Override
            		public void run() {
            			while (true) {
	            			try {
	            				Packet p = (Packet) ois.readObject();
	            				if (!Checksum.verifyChecksum(p.payload, p.checksum)) {
	            					System.out.printf("Requesting for packet %d again as checksum verification failed!", p.pkt_no);
	            					synchronized(buffer) {
	            						sendBuffer.add(p.pkt_no);
	            					}
	            				}
	            				else {
	            					packets.set(p.pkt_no, p.payload);
	            					recievedPackets.inc();
	            					System.out.println(recievedPackets.get());
	            					if (recievedPackets.get() == totalPackets) {
	            						Packet finalPacket = new Packet();
	            						finalPacket.client_ip = clientIP;
	            						finalPacket.destination_ip = destIP;
	            						finalPacket.cert_id = certID;
	            						finalPacket.pkt_id = totalPackets;
	            						oos.writeObject(finalPacket);
	            						System.out.printf("Recieved all packets successfully!\n");
	            						
	            						File outFile = new File(fileName);
	            						
	            						convertPacketsToFile(packets, outFile);
	            						writeByteArrayToFile(Files.readAllBytes(outFile.toPath()), fileName);
	            						
	            						System.out.printf("Saved to disk!");
	            						return;
	            					}
	            				}
	            			} catch (IOException e) {
	        					e.printStackTrace();
	        				} catch (ClassNotFoundException e) {
	        					e.printStackTrace();
	        				}
            			}
            		}
            	};
            	
            	Thread recv = new Thread(reciever);
                recv.start();
                
                Thread send = new Thread(sender);
                send.start();

            }
            else {
            	oos.close();
            	ois.close();
            	s.close();
            	return;
            }
                		

        }catch(Exception e){
        	System.out.printf("There was an error connecting to the server: ");
            e.printStackTrace();
        }
    }
}
