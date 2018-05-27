package receiver.thread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import main.Packet;
/**
 * Responsible for creating the ack and sending it
 *
 */
public class SenderThread implements Runnable {
    private int ackNumber;
    private DatagramPacket receivedPacket;
    private DatagramSocket receiverSocket;
    
    public SenderThread(int ackNumber, DatagramPacket receivedPacket, DatagramSocket receiverSocket) {
        super();
        this.ackNumber = ackNumber;
        this.receivedPacket = receivedPacket;
        this.receiverSocket = receiverSocket;
    }

    @Override
    public void run() {   
        //copy 152 to line to 163
        DatagramPacket ack = new DatagramPacket(ackPacket.getData(), ackPacket.getLength(),
                receivedPacket.getAddress(), receivedPacket.getPort());
        try {
            receiverSocket.send(ack);
            //put the logic from receiver line 173 to 179 
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
    }

}
