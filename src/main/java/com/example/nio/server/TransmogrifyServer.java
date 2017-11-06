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

/**
 * Implementation from http://www.baeldung.com/java-nio-selector
 *
 */
public class TransmogrifyServer {

	private static String END_OF_MESSAGE = "EOM";

	public static void main(String[] args) throws IOException {
		Selector selector = Selector.open();
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.bind(new InetSocketAddress("localhost", 9090));
		ssc.configureBlocking(false);
		// server channel is for accepting incoming connections
		ssc.register(selector, SelectionKey.OP_ACCEPT);
		ByteBuffer buffer = ByteBuffer.allocate(256);

		while (true) {
			selector.select();
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			Iterator<SelectionKey> it = selectedKeys.iterator();
			while (it.hasNext()) {
				SelectionKey key = it.next();
				if (key.isAcceptable()) {
					// IO ready on server socket channel
					// accept this client connection
					registerClientWithSelector(selector, ssc);
				}

				if (key.isReadable()) {
					// IO ready on client's socket channel
					readAndTransmogrify(key, buffer);
				}

				it.remove();
			}
		}
	}

	private static void readAndTransmogrify(SelectionKey key, ByteBuffer buffer) throws IOException {
		SocketChannel client = (SocketChannel) key.channel();
		buffer.clear();
		client.read(buffer);
		byte[] array = Arrays.copyOf(buffer.array(), buffer.position());
		if (new String(array).trim().equals(END_OF_MESSAGE)) {
			// close this client , no more msg from it
			client.close();
			System.out.println("This client is closed");
			return;
		}
		// now whatever is read, transmogrify it and send to the client
		buffer.flip();
		for (int i = 0; i < buffer.limit(); i++) {
			int ch = buffer.get(i);
			if (Character.isLetter(ch))
				buffer.put(i, (byte) (' ' ^ ch));
			else
				buffer.put(i, (byte) ch);
		}
		client.write(buffer);
	}

	private static void registerClientWithSelector(Selector selector, ServerSocketChannel ssc) throws IOException {
		SocketChannel client = ssc.accept();
		// server socket channel is non blocking so will return immediately
		// so check for null
		if (client != null) {
			client.configureBlocking(false);
			client.register(selector, SelectionKey.OP_READ);
		}
	}

}
