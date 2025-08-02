package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class NonBlockingClient implements Client {
    public void start(int port, Scanner scanner) {
        try (SocketChannel socket = SocketChannel.open()) {
            socket.configureBlocking(false);
            socket.connect(new InetSocketAddress("localhost", port));

            Selector selector = Selector.open();

            if (!socket.finishConnect()) {
                socket.register(selector, SelectionKey.OP_CONNECT);
            } else {
                socket.register(selector, SelectionKey.OP_WRITE);
            }

            while (true) {
                if (selector.select() > 0) {
                    var keys = selector.selectedKeys();
                    for (var key : keys) {
                        if (key.isConnectable()) {
                            handleConnect(key);
                        } else if (key.isWritable()) {
                            boolean exit = write(key, scanner);
                            if (exit) {
                                break;
                            }
                        } else if (key.isReadable()) {
                            read(key);
                        }
                    }
                    keys.clear();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleConnect(SelectionKey key) throws IOException {
        var socket = (SocketChannel) key.channel();
        if (socket.finishConnect()) {
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private boolean write(SelectionKey key, Scanner scanner) throws IOException {
        var socketChannel = (SocketChannel) key.channel();
        String input = scanner.nextLine().trim();
        if (input.equalsIgnoreCase("exit")) {
            socketChannel.close();
            return false;
        }
        socketChannel.write(ByteBuffer.wrap(input.getBytes()));
        key.interestOps(SelectionKey.OP_READ);
        return true;
    }

    private void read(SelectionKey key) throws IOException {
        var socketChannel = (SocketChannel) key.channel();
        var buffer = ByteBuffer.allocate(1024);
        int bytesRead = socketChannel.read(buffer);

        if (bytesRead == -1) {
            socketChannel.close();
            return;
        }

        String response = new String(buffer.array(), 0, bytesRead);
        System.out.println("Response from server: " + response);
        key.interestOps(SelectionKey.OP_WRITE);
    }
}
