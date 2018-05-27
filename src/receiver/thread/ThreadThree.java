package receiver.thread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Random;

import main.Packet;
/**
 * Thread called by receiver that will receive data packets
 *
 */
public class ThreadThree implements Runnable {

    private int windowSize;
    private int corruption;
    private int port;
    private String ipAddress;
    private DatagramSocket socket;
    private FileOutputStream output = null;
    private int receivedPacketSeqNum;
    //variable to be used by threadFour
    private DatagramPacket packet;
    private int ackNumber;
    private int expectedPacketNumber;
    private int checksumValue;
    private int oldPacketNumber;
    
    public ThreadThree(int windowSize, int corruption, int port, String ipAddress, DatagramSocket socket) {
        super();
        this.windowSize = windowSize;
        this.corruption = corruption;
        this.port = port;
        this.ipAddress = ipAddress;
        this.socket= socket;
    }

    public int getWindowSize() {
        return this.windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public int getCorruption() {
        return this.corruption;
    }

    public void setCorruption(int corruption) {
        this.corruption = corruption;
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
    
    public DatagramSocket getSocket() {
        return this.socket;
    }

    public void setSocket(DatagramSocket socket) {
        this.socket = socket;
    }

    public int getReceivedPacketSeqNum() {
        return this.receivedPacketSeqNum;
    }

    public void setReceivedPacketSeqNum(int receivedPacketSeqNum) {
        this.receivedPacketSeqNum = receivedPacketSeqNum;
    }

    public synchronized DatagramPacket getPacket() {
            System.out.println("ThreadFour waits until threadThree recv a packet"); 
            try {
                wait();
            } catch (InterruptedException ex) {
                // TODO Auto-generated catch block
                ex.printStackTrace();
            }            
        return this.packet;
    }

    public synchronized void setPacket(DatagramPacket packet) {
        System.out.println("Notify threadThree got packet and handoff to threadFour to send ack");
        notify();
        this.packet = packet;
    }

    public int getAckNumber() {
        return this.ackNumber;
    }

    public void setAckNumber(int ackNumber) {
        this.ackNumber = ackNumber;
    }

    public int getExpectedPacketNumber() {
        return this.expectedPacketNumber;
    }

    public synchronized void setExpectedPacketNumber(int expectedPacketNumber) {
        System.out.println("Notify Ack was sent by threadFour and threadThree can receive next packet "+ expectedPacketNumber);
        notify();
        this.expectedPacketNumber = expectedPacketNumber;
    }

    public int getChecksumValue() {
        return this.checksumValue;
    }

    public void setChecksumValue(int checksumValue) {
        this.checksumValue = checksumValue;
    }

    public int getOldPacketNumber() {
        return this.oldPacketNumber;
    }

    public void setOldPacketNumber(int oldPacketNumber) {
        this.oldPacketNumber = oldPacketNumber;
    }

    @Override
    public void run() {
        // variable to keep track of packets that arrived and are coming
        int oldPacketNumber = 0;
        int expectedPacketNumber = 1;
        // packet and ack number that will be used keep track of packets
        int currentPacketNumber = 1;
        int ackNumber = 1;
     // object to generate random number
        Random random = new Random();
        // supported character type
        String characterSet = "UTF-8";

        // variable to to store package properties
        String seqNumber = "";
        String packageString = "";
        // checksum value coming from sender
        int cksumValue = 0;
        while(true){
            System.out.println("Inside run method for threadThree");
            
        byte[] packageByte;
        // variable to store data that comes from sender
        byte[] data = new byte[1024];
        
        // print out what packet is coming
        System.out.println("Waiting on packet # " + expectedPacketNumber + "\n");
        //setExpectedPacketNumber(expectedPacketNumber);
        // variable to extract checksum and seq
        String cksum = "";
        seqNumber = "";
        // packet received from sender
        DatagramPacket receivePacket = new DatagramPacket(data, data.length);
        try {
            socket.receive(receivePacket);
        } catch (IOException ex) {
            System.out.println("Timeout while trying to receive packet "+ expectedPacketNumber + "  " + System.currentTimeMillis() % 1000 + "ms");
           continue;//start from while loop again
            // System.exit(0);
        }

        // parse the data to get checksum
        for (int i = 0; i < 3; i++) {
            cksum = cksum + data[i];
        }
        // convert checksum into a number
        cksumValue = Integer.parseInt(cksum);

        // parse data to get current packet number
        for (int i = 8; i < 12; i++) {
            seqNumber = seqNumber + data[i];
        }
        // convert seq into a number
        currentPacketNumber = Integer.parseInt(seqNumber);
        setReceivedPacketSeqNum(currentPacketNumber);

        // check whether we have received packet before
        if (oldPacketNumber != currentPacketNumber) {
            // if this packet has not arrived before then we
            // receiving it for first time
            System.out.println("[RECV] Packet # " + currentPacketNumber + "  " + System.currentTimeMillis() % 1000 + "ms \n");
        } else {
            // otherwise this packet came before
            System.out.println("[RECV] [DUPL] Packet # " + currentPacketNumber + "  " + System.currentTimeMillis() % 1000 + "ms \n");
        }
        
        // if the cksumValue is not zero packet is [CRPT] exit out
        if (cksumValue != 0) {
            System.out.println("[CRPT] packet # " + currentPacketNumber + " and need to recieve again  <-----  " + System.currentTimeMillis() % 1000 +  "ms \n");
            // note down this packetNumber since it arrived but was
            // could not proceed further
            oldPacketNumber = currentPacketNumber;
            continue;// start from the while loop again
        }
        // we need to randomly [DROP] packet and exit out
        if (corruption > 0) {
            if (random.nextInt(10) == 5) {
                System.out.println("[DROP] packet # " + currentPacketNumber + " <-----  " + System.currentTimeMillis() % 1000 + "ms \n");
                // note down this packetNumber since it arrived but was
                // could not proceed further
                oldPacketNumber = currentPacketNumber;
                continue;// start from the while loop again
            }
        }
        System.out.println("Getting data from packet "+ currentPacketNumber);
        // get the data that was sent
        packageByte = new byte[receivePacket.getLength() - 12];
        for (int i = 0; i < packageByte.length; i++) {
            packageByte[i] = data[i + 12];
        }

        // if checksum value equals zero and no error occurred above
        // proceed with creating ack
        if (cksumValue == 0) {
            System.out.println("Setting checksumValue for packet"+ currentPacketNumber);
            // assign ack for this packetNumber
            ackNumber = currentPacketNumber;
            setChecksumValue(cksumValue);

            try {
                // write the data in the new file
                if (currentPacketNumber == 1) {
                    System.out.println("Writing the title for packet");
                    // if its the first packet then it has the name of file
                    // get name of file and write to output file
                    if (!packageString.equals(new String(packageByte, characterSet))) {
                        packageString = new String(packageByte, characterSet);
                        writeToFile(packageString);
                    }
                } else {
                    System.out.println("Writing the rest of data of packet");
                    // otherwise the packet has content of file, get the
                    // content and write to output file
                    if (!packageString.equals(new String(packageByte, characterSet))) {
                        packageString = new String(packageByte, characterSet);
                        writeToFile(packageByte);
                    }
                }
                System.out.println("Setting ackNumber for packet"+ ackNumber);
                setAckNumber(ackNumber);
                setPacket(receivePacket);//set the packet that was received
                expectedPacketNumber= ackNumber +1;//we should not receive next packet
                waitUntilAckSent(ackNumber);

            } catch (IOException ex) {
                System.out.println("Error happened while writing to file  " + System.currentTimeMillis() % 1000 + "ms");
            }//end of catch
        }//end of if (cksumValue == 0)
        }//end of while true
    }
   
    
    private synchronized void waitUntilAckSent(int ackNumber){
        System.out.println("ThreadThree wait till threadFour sends Ack for "+ ackNumber);
        try {
            wait();
        } catch (InterruptedException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }        
    }
    /**
     * Write the name of file that was sent by sender to output file
     * 
     * @param packageString
     * @throws IOException
     */
    public void writeToFile(String packageString) throws IOException {
        output = new FileOutputStream(new File("output_" + packageString));
    }


    /**
     * Write content of file that was sent by sender to output file
     * 
     * @param packageByte
     * @throws IOException
     */
    public void writeToFile(byte[] packageByte) throws IOException {
        output.write(packageByte);
    }


}
