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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WordReversalReactiveServer {

	private static final int WORKER_THREADS = 10;

	private int port;

	private ServerSocketChannel serverChannel;

	private Selector selector;

	private ExecutorService executorService;

	public WordReversalReactiveServer(int port) {
		this.port = port;
		try {
			selector = Selector.open();
			serverChannel = ServerSocketChannel.open();
			serverChannel.bind(new InetSocketAddress("localhost", this.port));
			serverChannel.configureBlocking(false);
			SelectionKey key = serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			key.attach(new Acceptor());
			executorService = Executors.newFixedThreadPool(WORKER_THREADS);
		} catch (IOException ex) {
			// exception handling
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.println("No port provided, provide a valid port");
			return;
		}
		int port = Integer.valueOf(args[0]);
		WordReversalReactiveServer server = new WordReversalReactiveServer(port);
		server.start();
	}

	private void start() {

		try {
			// Event loop
			while (true) {
				// selector.select();
				int selectNow = selector.selectNow();
				if (selectNow == 0)
					continue;
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while (it.hasNext()) {
					SelectionKey key = it.next();
					it.remove();
					// the attachment is either an acceptor for new client
					// connection
					// or read coming from a client
					if (key.isValid()) {
						Runnable r = (Runnable) key.attachment();
						if (r != null) {
							r.run();
						}
					}
				}
			}
		} catch (IOException ex) {
			// handle exceptions
			ex.printStackTrace();
		}
	}

	private class Acceptor implements Runnable {

		@Override
		public void run() {
			try {
				SocketChannel channel = serverChannel.accept();
				if (channel != null) {
					new ReadWriteHandler(selector, channel);
				}
			} catch (IOException ex) {
				// handle exceptions
				ex.printStackTrace();
			}
		}

	}

	private class ReadWriteHandler implements Runnable {

		private Selector selector;
		private SocketChannel channel;
		private SelectionKey selectionKey;

		private static final int READ_BUF_SIZE = 1024;
		private static final int WRITE_BUF_SIZE = 1024;

		private ByteBuffer readBuffer = ByteBuffer.allocate(READ_BUF_SIZE);
		private ByteBuffer writeBuffer = ByteBuffer.allocate(WRITE_BUF_SIZE);

		public ReadWriteHandler(Selector s, SocketChannel sc) {
			selector = s;
			channel = sc;
			try {
				channel.configureBlocking(false);
				selectionKey = channel.register(selector, SelectionKey.OP_READ);
				selectionKey.attach(this);
				selector.wakeup();
			} catch (IOException ex) {
				// handle exceptions
				ex.printStackTrace();
			}
		}

		@Override
		public void run() {
			if (selectionKey.isReadable()) {
				doProcessing();
			}
		}

		private void doProcessing() {
			executorService.execute(() -> {
				try {
					readWriteProcessing();
				} catch (Exception e) {
					e.printStackTrace();
				}
				selector.wakeup();
			});
		}

		private synchronized void readWriteProcessing() throws IOException {
			try {
				int numBytes = channel.read(readBuffer);
				String input = new String(Arrays.copyOf(readBuffer.array(), readBuffer.position()));
				byte[] output = doWordByWordReversal(input);
				writeBuffer = ByteBuffer.wrap(output);
				channel.write(writeBuffer);
				readBuffer.clear();
				writeBuffer.clear();
			} catch (IOException ex) {
				// handle exceptions
				ex.printStackTrace();
				selectionKey.cancel();
				channel.close();
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

}
