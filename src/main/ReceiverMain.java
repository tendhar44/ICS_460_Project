package main;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import receiver.thread.ThreadFour;
import receiver.thread.ThreadThree;
/**
 * Main class for receiver that creates threads
 *
 */
public class ReceiverMain {

    public static void main(String args[]){
        //get data from command line
//      if (args.length > 0) {
//          int windowSize= Integer.parseInt(args[0]);
//          int corruption = Integer.parseInt(args[1]);
//          String hostname = args[2];
//          int port = Integer.parseInt(args[3]);
        
            Scanner inputs = new Scanner(System.in);
          //Gathering program inputs
          System.out.println("Please enter hostanme");
          String hostname = inputs.nextLine();
          System.out.println("Please enter window");
          int windowSize = inputs.nextInt();
          System.out.println("Please enter corruption:");
          int corruption = inputs.nextInt();
          System.out.println("Please enter timeout:");
          int timeout= inputs.nextInt();
          System.out.println("Please enter port");
          int port = inputs.nextInt();
          inputs.close();
          
          if (timeout == 0)
              timeout= 2000;
          DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(port, InetAddress.getByName(hostname));
            socket.setSoTimeout(timeout);
        } catch (SocketException ex) {
           System.out.println("Error while creating socket");
           System.exit(0);
        } catch (UnknownHostException ex) {
            System.out.println("Error while getting hostname");
            System.exit(0);
        }
          ThreadThree threadThree= new ThreadThree(windowSize, corruption, port, hostname, socket);
          Thread threadR = new Thread(threadThree);
          threadR.start();
          
          ThreadFour threadFour = new ThreadFour(corruption, socket, threadThree);
          Thread threadS= new Thread(threadFour);
          threadS.start();

    }

}
