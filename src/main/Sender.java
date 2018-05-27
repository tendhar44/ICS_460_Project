package main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;

/**
 * This class builds packet sends to receiver and waits for acknowledgement
 *
 */
public class Sender implements Runnable{
    
    private int packetSize;
    private int timeout;
    private int windowSize;
    private int corrupotion;
    private int port;
    private String ipAddress; 
    
    public Sender(int packetSize, int timeout, int windowSize, int corrupotion, int port, String ipAddress) {
        super();
        this.packetSize = packetSize;
        this.timeout = timeout;
        this.windowSize = windowSize;
        this.corrupotion = corrupotion;
        this.port = port;
        this.ipAddress = ipAddress;
    }

    //public static void main(String[] args) throws IOException{
    @Override
    public void run(){
        
        //variables to keep track of packet
        Packet currentPacket = null;
        int packetSize = getPacketSize();
        int corruption = getCorrupotion();
        int timeout = getCorrupotion();
        int ackNumberValue = 1;
        String ipAddress = getIpAddress();
        int port= getPort();
        //variables to track of other ack and packets
        Integer prevAckNumber = 0;
        Integer delayedPacketNumber= 0;
        Integer prevPacketNumber= 0;
        
        //object to generate random number
        Random random = new Random();
                
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException ex1) {
            System.out.println("Invalid ipAddress entered!");
            disconnect();//exit out of program
        }
        
        // get data from command line
/*        if (args.length > 0){
        packetSize= Integer.parseInt(args[0]);
        timeout= Integer.parseInt(args[1]);
        corruption= Integer.parseInt(args[2]);
        ipAddress= args[3];
        port= Integer.parseInt(args[4]);
        }*/
        
        DatagramSocket socket= null;
        try {
            //create socket
            socket = new DatagramSocket(); 
            socket.setSoTimeout(timeout);
        } catch (SocketException ex1) {
            System.out.println("Error occured while creating socket!");
            disconnect();//exit out of program
        }
        //variable to store data in packets
        LinkedList<Packet> packets= new LinkedList<Packet>();
        
        try {
            //object to help build packet
            PacketBuilder packetBuilder= new PacketBuilder();
            //read data and put them in packets
            packets = packetBuilder.readFile(packetSize);
        } catch (FileNotFoundException ex1) {
            System.out.println("Error while creating packets!");
            disconnect();//exit out of program
        }
        
        //go thru all the packets while they exist
        while(!packets.isEmpty()) {
            try {
                //get the start time
                long startTime= System.currentTimeMillis();
                //variable to store checksum and ack
                String checksum = "";
                String ackNumber = "";
                int checksumValue = 0;
                short goodCheckSum= 0;
                short badCheckSum= 1;

                //check the packet and set its checksum
                currentPacket = packets.removeFirst();
                //by default the checksum will be good checksum of 0
                currentPacket.setCksum(goodCheckSum);
                
                //if user had specified packets to be corrupted, then packets 
                //will be error and delay
                if (corruption > 0) {
                    //randomly make this packet a bad packet
                    if (random.nextInt(5) == 2) {
                        // this packet is bad packet
                        System.out.println("[ERRR] packet # " + currentPacket.getSeqno()+ " is bad packet \n");
                        // assign bad checksum to packet
                        currentPacket.setCksum(badCheckSum);
                    } else if (random.nextInt(5) == 3) {
                        //randomly make this packet delayed
                        System.out.println("[DLYD] packet # " + currentPacket.getSeqno()+ "\n");
                        for (int z = 0; z <= timeout; z++) {
                            //do nothing just wait
                        }
                        //assign current packet as delayedPacket and timeout
                        delayedPacketNumber= currentPacket.getSeqno();
                        throw new SocketTimeoutException();
                    }
                }//end of if (corruption > 0)
                
                //create datagram packet which will be sent to receiver
                DatagramPacket output= new DatagramPacket(currentPacket.getData(), currentPacket.getLength(), ip, port);
                //send the packet
                try {
                    socket.send(output);
                } catch (IOException ex) {
                    // TODO Auto-generated catch block
                    ex.printStackTrace();
                }
                
                //check which msg to print 
                if (prevPacketNumber.equals(currentPacket.getSeqno()) && !delayedPacketNumber.equals(currentPacket.getSeqno())) {
                    //only need to do [RESEND] when packet was not a delayedPacket 
                    //and when packet was sent before but no ack was received  
                    System.out.println("[ReSend.]: packet # " + currentPacket.getSeqno() + " with datasize of " + currentPacket.getData().length + " -----> \n");
                } else {
                    long endTime= System.currentTimeMillis() - startTime;
                    //otherwise for delayedPackets and normalPackets print SENT
                    System.out.println("[SENDing]: packet # " + currentPacket.getSeqno() + " with datasize of " + currentPacket.getData().length + "\n");
                    System.out.println("[SENT] packet # "+ currentPacket.getSeqno() + " in "+ endTime + " ms ----->" + "\n");
                }
                
                //wait for Ack from receiver
                System.out.println("Waiting for [Ack] for packet # " + currentPacket.getSeqno() + "\n");
                //get the datagramPacket from receiver
                byte[] dataFromReceiver = new byte[1024];
                DatagramPacket receiverPacket = new DatagramPacket(dataFromReceiver, dataFromReceiver.length);
                try {
                    socket.receive(receiverPacket);
                } catch (IOException ex) {
                    System.err.println("Closing socket as no more incoming packets!");
                    socket.disconnect();
                    disconnect();//exit out of program 
                }

                //go thru packet from position 0 to 2 to get the checksum
                for(int i = 0; i < 3; i++) {
                    checksum = checksum + dataFromReceiver[i];
                }
                checksumValue = Integer.parseInt(checksum);
                //go thru packet from position 4 to 7 to get the ackNumber
                for(int i = 4; i < 8; i++) {
                    ackNumber = ackNumber + dataFromReceiver[i];
                }
                ackNumberValue = Integer.parseInt(ackNumber);
                
                //randomly drop the Ack that was sent to by receiver, test out maybe need to remove for project 1?????
                if (corruption > 0){
                    //randomly [DROP] the Ack
                    if (random.nextInt(5) == 4) {
                        System.out.println("[DROP] Ack for packet # " + ackNumberValue + "\n");
                        //note down the ackNumber that was dropped
                        prevAckNumber= ackNumberValue;
                        //note down which packet needs to resend since ack was dropped
                        prevPacketNumber= currentPacket.getSeqno();
                        //add this packet in front as it need to be resent
                        if (ackNumberValue == currentPacket.getSeqno()) {
                            packets.addFirst(currentPacket);
                            continue; //start from while loop again
                        }
                    }//end of  if (random.nextInt(5) == 4)
                }//end of if (corruption > 0)
                
                // if checksum came back as 0 from receiver then ack the packet
                if (checksumValue == 0) {
                    //if ack was received for this packet before then this is a Dupl Ack
                    if (prevAckNumber.equals(ackNumberValue)) {
                        System.out.println("[DuplAck] for packet # " + ackNumberValue + "\n"+ "\n");
                    } else {
                        //otherwise this is the first time we are receiving ack for this packet
                        System.out.println("[AckRcvd] for packet # " + ackNumberValue + "\n"+ "\n");
                    }
                }//end of if (checksumValue == 0)
                
            } catch (SocketTimeoutException ex) {
                //while waiting for receiver sender timed out
                System.out.println("[TimeOut] while waiting to receieve ACK for packet # "+ currentPacket.getSeqno()+ " \n");
                //note down this packet will have to resent
                prevPacketNumber= currentPacket.getSeqno();
                //add this packet in front as it need to be resent
                packets.addFirst(currentPacket);
            }//end of catch SocketTimeoutException

        }//end while
        //disconnect the socket
        if (socket != null)
            socket.disconnect();
        //exit out of program
        disconnect();
    }
    
    public int getPacketSize() {
        return this.packetSize;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getWindowSize() {
        return this.windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public int getCorrupotion() {
        return this.corrupotion;
    }

    public void setCorrupotion(int corrupotion) {
        this.corrupotion = corrupotion;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void disconnect(){
        System.exit(0);
    }
    public static void main(String args[]){      
     // get data from command line
//        if (args.length > 0){
//        int packetSize= Integer.parseInt(args[0]);
//        int timeout= Integer.parseInt(args[1]);
//        int corruption= Integer.parseInt(args[2]);
//        int windowSize= Integer.parseInt(args[3]);
//        String ipAddress= args[4];
//        int port= Integer.parseInt(args[5]);
        Scanner inputs = new Scanner(System.in);
        System.out.println("Please enter packetSize:");
        int packSize= inputs.nextInt();
        System.out.println("Please enter timeout:");
        int timeout= inputs.nextInt();
        System.out.println("Please enter corrutpion:");
        int corruption= inputs.nextInt();
        System.out.println("Please enter window size:");
        int window= inputs.nextInt();
//        System.out.println("Please enter ip adress:");
//        String ip= inputs.nextLine();
        System.out.println("Please enter port:");
        int port= inputs.nextInt();        
        
        Sender sender=new Sender(packSize, timeout, window, corruption, port, "localhost");
        Thread thread= new Thread(sender);
        thread.start();
//        }

    }

}
