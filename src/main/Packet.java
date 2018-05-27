package main;

import java.nio.ByteBuffer;

public class Packet {
    
    short cksum; //16-bit 2-byte
    short length;    //16-bit 2-byte
    int ackno;    //32-bit 4-byte
    int seqno ;   //32-bit 4-byte Data packet Only
    byte[] data; //0-500 bytes. Data packet only. Variable
    boolean last;
    
    public Packet(){
        
    }
    public Packet(short cksum, short len, int ackno)
    {
        this.cksum = cksum;
        this.length = len;
        this.ackno = ackno;

        ByteBuffer b = ByteBuffer.allocate(8);
        b.putShort(0, cksum);
        b.putShort(2, len);
        b.putInt(4, ackno);
        byte seq[] = b.array();

        this.data = seq;
    }

    public Packet(short cksum, short len, int ackno, int seqno, byte[] data)
    {
        this.cksum = cksum;
        this.length = len;
        this.ackno = ackno;
        this.seqno = seqno;


        ByteBuffer b = ByteBuffer.allocate(12);
        b.putShort(0, cksum);
        b.putShort(2, len);
        b.putInt(4, ackno);
        b.putInt(8, seqno);
        byte seq[] = b.array();
        byte[] combined = new byte[seq.length + data.length];

        System.arraycopy(seq, 0, combined, 0, seq.length);
        System.arraycopy(data, 0, combined, seq.length, data.length);

        this.data = combined;
    }

    public short getCksum() {
        return this.cksum;
    }

    public void setCksum(short cksum) {
        this.cksum = cksum;
        if(cksum == 1) {
            data[1] = 1;
        }
        else {
            data[1] = 0;
        }
    }
    
    public String toString() {
        String dataString = "";
        for(int i = 0; i < length; i++) {
            dataString += (char) data[i];
        }
        return "Data: " + dataString + ". Length: " + length + ".";
    }


    public short getLength() {
        return this.length;
    }

    public void setLength(short len) {
        this.length = len;
    }

    public int getAckno() {
        return this.ackno;
    }

    public void setAckno(int ackno) {
        this.ackno = ackno;
    }

    public int getSeqno() {
        return this.seqno;
    }

    public void setSeqno(int seqno) {
        this.seqno = seqno;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean isLast() {
        return this.last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }
    
}