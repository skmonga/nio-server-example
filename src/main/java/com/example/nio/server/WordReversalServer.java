package com.example.nio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class WordReversalServer {

	private int port;
	
	public WordReversalServer(int port) {
		this.port = port;
	}
	
	public static void main(String[] args) throws IOException {
		if(args.length < 1) {
			System.err.println("No port provided, provide a valid port");
			return;
		}
		int port = Integer.valueOf(args[0]);
		WordReversalServer server = new WordReversalServer(port);
		server.start();
	}

	private void start() throws IOException {
		Selector selector = Selector.open();
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.bind(new InetSocketAddress("localhost", this.port));
		ssc.configureBlocking(false);
		// server channel is for accepting incoming connections
		ssc.register(selector, SelectionKey.OP_ACCEPT);
		//create thread pool for processing client requests
		Executor executor = Executors.newFixedThreadPool(10);
		while(true) {
			selector.select();
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			Iterator<SelectionKey> it = selectedKeys.iterator();
			while (it.hasNext()) {
				SelectionKey key = it.next();
				if (key.isValid() && key.isAcceptable()) {
					// IO ready on server socket channel
					// accept this client connection
					executor.execute(new ClientRegisterHandler(selector, ssc));
				}

				if (key.isValid() && key.isReadable()) {
					// IO ready on client's socket channel
					executor.execute(new ReadClientHandler((SocketChannel) key.channel()));
				}

				it.remove();
			}
		}
		
	}
	
	private class ReadClientHandler implements Runnable {

		SocketChannel channel;
		
		public ReadClientHandler(SocketChannel ch) {
			this.channel = ch;
		}
		
		@Override
		public void run() {
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			try {
			channel.read(buffer);
			String input = new String(Arrays.copyOf(buffer.array(), buffer.position()));
			byte[] output = doWordByWordReversal(input);
			buffer.clear();
			ByteBuffer op = ByteBuffer.wrap(output);
			channel.write(op);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		private byte[] doWordByWordReversal(String input) {
			StringBuilder builder = new StringBuilder();
			String[] splits = input.split("\\W+");
			for (String s : splits) {
				builder.append(new StringBuilder(s).reverse().toString());
				builder.append(" ");
			}
			int len = builder.length();
			if (len > 0) {
				builder.replace(len - 1, len, "");
			}
			return builder.toString().getBytes();
		}
		
	}
	
	private class ClientRegisterHandler implements Runnable {

		Selector selector;
		ServerSocketChannel serverChannel;

		public ClientRegisterHandler(Selector s, ServerSocketChannel ssc) {
			this.selector = s;
			this.serverChannel = ssc;
		}

		@Override
		public void run() {
			try {
				SocketChannel client = serverChannel.accept();
				// server socket channel is non blocking so will return
				// immediately
				if (client != null) {
					client.configureBlocking(false);
					client.register(selector, SelectionKey.OP_READ);
				}
			} catch (IOException ex) {
				// handle exception
			}
		}

	}
	
}


