package com.example.nio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class WordReversalClient {

	private SocketChannel channel;
	
	private ByteBuffer buffer;
	
	private String serverHost;
	
	private int serverPort;
	
	public WordReversalClient(String sHost, int sPort) {
		//serverHost and serverPort taken as fields as if connection
		//is disconnected , then retry can be done via these fields by client itself
		this.serverHost = sHost;
		this.serverPort = sPort;
		try {
		this.channel = SocketChannel.open(new InetSocketAddress(sHost, sPort));
		this.buffer = ByteBuffer.allocate(256);
		} catch (IOException ex) {
			//handle exceptions
		}
	}
	
	public void sendMessage(String message) {
		buffer = ByteBuffer.wrap(message.getBytes());
        try {
            channel.write(buffer);
            buffer.clear();
            channel.read(buffer);
            String response = new String(buffer.array()).trim();
            System.out.println(response);
            buffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public static void main(String[] args) {
		WordReversalClient client = new WordReversalClient("localhost", 9090);
		client.sendMessage("Hi This is Sumit");
		client.sendMessage("Just trying out some luck");
	}
	
}
