package receiver.thread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
/**
 * Responsible to receiving packet and writing to file
 *
 */
public class ReceiverThread implements Runnable {

    private DatagramSocket socket;
    private byte[] data;
    private DatagramPacket receivePacket;
    
    public ReceiverThread(DatagramSocket socket, byte[] data){
        this.socket= socket;
        this.data= data;
    }
    
    @Override
    public void run() {
        //this thread of the receiver receive logic
        DatagramPacket receivePacket = new DatagramPacket(data, data.length);
        setReceivePacket(receivePacket);
        try {
            socket.receive(receivePacket);
            //put the code from receiver 73 line to 144 
            
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }

    }

    public DatagramPacket getReceivePacket() {
        return this.receivePacket;
    }

    public void setReceivePacket(DatagramPacket receivePacket) {
        this.receivePacket = receivePacket;
    }

}
