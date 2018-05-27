package main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
/**
 * Class responsible of reading data from text file and putting data in packets
 *
 */
public class PacketBuilder {
    
    private final String defaultPath= "C:\\DevApps\\GitLocal\\ICS_460_Project\\src\\resources\\";
	//private final String defaultPath= "C:\\Users\\pg676_000.TENDHAR-PC\\Desktop\\ComputerNetworkClass\\ICS_460_Project\\src\\resources\\";
    private String fileName= "test.txt";
    /**
     * Read data from file and puts data in packets
     * @param fileName
     * @param packetLength
     * @return
     * @throws FileNotFoundException 
     */
    public LinkedList<Packet> readFile(int packetLength) throws FileNotFoundException {
        // create a new data file from default path
        LinkedList<Packet> packets = null;
        BufferedReader bufferedReader = null;

        FileReader fileReader = new FileReader(defaultPath + fileName);
        if (fileReader != null) {
            // store the content of the file
            StringBuilder contents = new StringBuilder();
            String line;

            try {
                bufferedReader = new BufferedReader(fileReader);
                // read the lines from the files and add it to content
                while ((line = bufferedReader.readLine()) != null) {
                    contents.append(line + "\n");
                }
                // once all data is present in content create the packets
                packets = buildPackets(fileName, contents.toString(), packetLength);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    // close out the bufferedReader and fileReader
                    if (bufferedReader != null)
                        bufferedReader.close();
                    if (fileReader != null)
                        fileReader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }//end of finally try/catch block
            }//end of finally

        }//end of fileReader != null
        return packets;
    }
    
    /**
     * build the packet based on packent length
     * @param fileName
     * @param dataFromFile
     * @param packetSize
     * @return
     */
    private LinkedList<Packet> buildPackets(String fileName, String dataFromFile, int packetSize) {
        //create list to store data and packets
        LinkedList<byte[]> dataList = new LinkedList<byte[]>();
        LinkedList<Packet> packetList = new LinkedList<Packet>();
        //get the data in bytes
        byte[] fullData = dataFromFile.getBytes();
        //do modulo and see what what data is left over and will not fill up the entire packet
        int remainingData = fullData.length % packetSize;
        //check how many packets can be filled up entirely with data
        int fullPacketCount = fullData.length / packetSize;
        int packetNumber = 1;
        int location = 0;
        short checkSum= 0;
        int destinationStartIndex= 0;
        //create the packet that has the name of the file 
        Packet namePacket = new Packet(checkSum, (short) (12 + fileName.length()), packetNumber, packetNumber, fileName.getBytes());
        //increment packet number
        packetNumber++;
        //add packet to list
        packetList.add(namePacket);
        //go thru all the data that needs to be sent to receiver 
        for(int i = 0; i < fullPacketCount; i++) {
            //create a new byte[] to store the data in chunks of packetSize
            byte[] destination = new byte[packetSize];
            //copy the full data in chunks of packetSize into a new destination
            System.arraycopy(fullData, packetSize * i, destination, destinationStartIndex, destination.length);
            //adding the chunked data to list
            dataList.add(destination);
            //keeping track of the position of when we stopped copying
            location = packetSize * (i + 1);
        }
        //if there are still some data remaining from full data copy it
        if(remainingData != 0) {
            //create byte[] to store the data that is remaining
            byte[] otherDestination = new byte[remainingData];
            //copy the data that is remaining from full data to destinaiton
            System.arraycopy(fullData, location, otherDestination, destinationStartIndex, otherDestination.length);
            //add remaining data with rest of the data list
            dataList.add(otherDestination);
        }
        //once i am done putting all packet to list 
        while(!dataList.isEmpty()) {
            //remove the last packet
            byte[] data = dataList.remove();
            //create packet object
            Packet packet = new Packet(checkSum, (short) (12 + data.length), packetNumber, packetNumber, data);
            //increment packet number
            packetNumber++;
            //add packet to list
            packetList.add(packet);
        }

        return packetList;
    }

}
