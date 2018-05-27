package sender.thread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.Random;

import javax.swing.plaf.SliderUI;

import main.Packet;
/**
 * Thread that called by sender to send packets
 *
 */
public class ThreadOne implements Runnable {

    private LinkedList<Packet> packets;
    private int corruption;
    private DatagramSocket socket;
    private int timeout;
    private InetAddress ip;
    private int port;
    private int windowSize;
    //track the packets
    private Integer previousPacketNumber;
    private Integer delayedPacketNumber;
    private Packet currentPacket;
    //variable to know when to send next packet
    private Integer nextPacketNumber= 1;
    private Integer ackedPacket;
    private Integer incrementValue;
    /**
     * Constructor
     * @param packets2
     * @param windowSize
     * @param corruption2
     * @param socket2
     * @param timeout2
     * @param ip2
     * @param port2
     */
    public ThreadOne(LinkedList<Packet> packets2, int windowSize, int corruption2, DatagramSocket socket2, int timeout2, InetAddress ip2, int port2) {
        this.packets = packets2;
        this.corruption = corruption2;
        this.socket = socket2;
        this.timeout = timeout2;
        this.ip= ip2;
        this.port= port2;
        this.windowSize= windowSize;
    }


    @Override
    public void run() {
        System.out.println("Inside run method for threadOne");
        // get the start time
        long startTime = System.currentTimeMillis();
        // variable to store checksum and ack
        short badCheckSum = 1;
        Packet currentPacket = null;
        Integer delayedPacketNumber = 0;
        Integer prevPacketNumber = 0;
        Random random = new Random();

            // check the packet and set its checksum
            while (!packets.isEmpty()){
                
                try {
                    
                    LinkedList<Packet> packetsInTransit = choosePackets(windowSize, packets);
                    
                    while (!packetsInTransit.isEmpty()){
                       
                        currentPacket = packetsInTransit.removeFirst();                            
                        System.out.println("Getting ready packet "+currentPacket.getSeqno());
                        
                        if (corruption > 0) {
                            // randomly make this packet a bad packet
                            if (random.nextInt(5) == 2) {
                                // this packet is bad packet
                                System.out.println("[ERRR] packet # " + currentPacket.getSeqno() + " is bad packet  " + System.currentTimeMillis() % 1000 + "ms \n");
                                // assign bad checksum to packet
                                currentPacket.setCksum(badCheckSum);
                            } else if (random.nextInt(5) == 3) {
                                // randomly make this packet delayed
                                System.out.println("[DLYD] packet # " + currentPacket.getSeqno() + "  " + System.currentTimeMillis() % 1000 + "ms \n");
                                for (int z = 0; z <= timeout; z++) {
                                    // do nothing just wait
                                }
                                // assign current packet as delayedPacket and timeout
                                delayedPacketNumber = currentPacket.getSeqno();
                                throw new SocketTimeoutException();
                            }
                        }// end of if (corruption > 0)

                        // create datagram packet which will be sent to receiver
                        sendPacket(currentPacket);
                        
                        if (prevPacketNumber.equals(currentPacket.getSeqno()) && !delayedPacketNumber.equals(currentPacket.getSeqno())) {
                            // only need to do [RESEND] when packet was not a
                            // delayedPacket
                            // and when packet was sent before but no ack was received
                            System.out.println("[ReSend.]: packet # " + currentPacket.getSeqno() + " with datasize of " + currentPacket.getData().length + "  " + System.currentTimeMillis() % 1000 + "ms  -----> \n");
                        } else {
                            long endTime = System.currentTimeMillis() - startTime;
                            // otherwise for delayedPackets and normalPackets print SENT
                            System.out.println("[SENDing]: packet # " + currentPacket.getSeqno() + " with datasize of " + currentPacket.getData().length + "\n");
                            System.out.println("[SENT] packet # " + currentPacket.getSeqno() + " in " + endTime + " ms  " +  System.currentTimeMillis() % 1000 + "ms  -----> \n");
                            setCurrentPacket(currentPacket);
                            System.out.println("Data in the packet was "+ convertFrom(currentPacket.getData()));
                        }
                        
                    }//end of  while (!packetsInTransit.isEmpty())
                    
                }catch (SocketTimeoutException ex) {
                    // while waiting for receiver sender timed out
                    System.out.println("[TimeOut] while sending packet # " + currentPacket.getSeqno() + " \n");
                    // note down this packet will have to resent
                    prevPacketNumber = currentPacket.getSeqno();
                    setPreviousPacketNumber(prevPacketNumber);
                    // add this packet in front as it need to be resent
                    packets.addFirst(currentPacket);
                }
            }//end of while (!packets.isEmpty())
            return;//end the thread
    }//end of run 
            
    private static String convertFrom(byte[] source) {
        return source == null ? null : new String(source);
    }
          

    private synchronized LinkedList<Packet> choosePackets(int windowSize, LinkedList<Packet> allPreparedPackets) {
        LinkedList<Packet> packetsToSend = new LinkedList<Packet>();
        if (allPreparedPackets != null && !allPreparedPackets.isEmpty()) {
            if (windowSize > 1 && allPreparedPackets.size() >= windowSize) {
                Packet topPacket = allPreparedPackets.getFirst();
                if (topPacket.getSeqno() == 1) {
                    System.out.println("Window size is " + windowSize);
                    addPacketsToSend(windowSize, allPreparedPackets, packetsToSend);
                } else if ( topPacket.getSeqno() > 1){
                    System.out.println(" Need wait to send packet " + topPacket.getSeqno() + " until we receive ack for previous packet");
                    waitUntilAckRecv();
                    addPacketsToSend(getIncrementValue(), allPreparedPackets, packetsToSend);
                }
                
            } else {
                Packet packet = allPreparedPackets.getFirst();
                if (packet.getSeqno() > 1) {
                    System.out.println(" Need wait for packet " + packet.getSeqno() + " until we receive ack for previous packet");
                    waitUntilAckRecv();
                }
                packetsToSend.add(allPreparedPackets.removeFirst());
                System.out.println(" window size is just one so adding packet " + packet.getSeqno() + " and packets left to send are " + allPreparedPackets.size());
            }
        }
        if (allPreparedPackets.isEmpty() && packets.size() == getAckedPacket()){
//            Packet emptyPacket= new Packet();
//            emptyPacket.setLast(true);
//            packetsToSend.add(emptyPacket);
            System.out.println("The last ack packet was "+ getAckedPacket()+ " same as the packets we needed to send "+ packets.size()+ " so exiting the system");
            System.exit(0);
        }
        return packetsToSend;
    }
    
    private void waitUntilAckRecv(){
        try {
            wait();
        } catch (InterruptedException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
    }

    private void addPacketsToSend(int windowSize, LinkedList<Packet> allPreparedPackets, LinkedList<Packet> packetsToSend){
        for (int i = 0; i < windowSize; i++) {
            System.out.println("When i is " + i + " the packet to remove is " + allPreparedPackets.getFirst().getSeqno());
            packetsToSend.add(allPreparedPackets.removeFirst());
            System.out.println(" packetsToSend size is " + packetsToSend.size() + " and the packet that was added was " + packetsToSend.get(i).getSeqno());
        }
    }
    private void sendPacket(Packet currentPacket) {
        DatagramPacket output = new DatagramPacket(currentPacket.getData(), currentPacket.getLength(), ip, port);
        // send the packet
        try {
            socket.send(output);
            System.out.println("Inside SendPacket at "+ System.currentTimeMillis() % 1000);
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }

    }

    public synchronized void setAckedPacket(int ackedPacket){
        System.out.println("Notified that we have recieved ack and can [MoveWnd]");
        setIncrementValue(getCurrentPacket().getSeqno() - ackedPacket);
        notify();
        this.ackedPacket= ackedPacket;
    }

    public Integer getAckedPacket() {
        return this.ackedPacket;
    }

    public Integer getIncrementValue() {
        return this.incrementValue;
    }


    public void setIncrementValue(Integer diff) {
        if (diff.equals(0)){
            System.out.println("increment the window by windowSize since diff was "+ diff);
            incrementValue= windowSize;
        } else {
            System.out.println("increment the window by 1 since diff was "+ diff);
            incrementValue= 1;
        }
        this.incrementValue = incrementValue;
    }


    public Integer getNextPacketNumber() {
        return this.nextPacketNumber;
    }


    public void setNextPacketNumber(Integer nextPacketNumber) {
        this.nextPacketNumber = nextPacketNumber;
    }


    public Integer getPreviousPacketNumber() {
        return this.previousPacketNumber;
    }


    public void setPreviousPacketNumber(Integer previousPacketNumber) {
        this.previousPacketNumber = previousPacketNumber;
    }


    public Integer getDelayedPacketNumber() {
        return this.delayedPacketNumber;
    }


    public void setDelayedPacketNumber(Integer delayedPacketNumber) {
        this.delayedPacketNumber = delayedPacketNumber;
    }


    public  Packet getCurrentPacket() {
        return this.currentPacket;
    }


    public  void setCurrentPacket(Packet currentPacket) {
        this.currentPacket = currentPacket;
    }


    public LinkedList<Packet> getPackets() {
        return this.packets;
    }


    public void setPackets(LinkedList<Packet> packets) {
        this.packets = packets;
    }

}
