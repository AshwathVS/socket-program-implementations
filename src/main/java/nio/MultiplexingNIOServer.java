package nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MultiplexingNIOServer implements Server {
    private final Map<SocketChannel, ByteBuffer> openSocketChannels = new HashMap<>();
    private final int port;

    public MultiplexingNIOServer(int port) {
        this.port = port;
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = openSocketChannels.get(clientChannel);

        int bytesRead = clientChannel.read(buffer);
        if (bytesRead == -1) {
            closeSocketChannel(clientChannel);
            return;
        }

        buffer.flip();
        String message = new String(buffer.array(), 0, buffer.limit()).trim();

        if (message.equalsIgnoreCase("exit")) {
            closeSocketChannel(clientChannel);
            return;
        }

        buffer.clear();
        processClientMessage(key, message);
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void write(SelectionKey key) throws IOException {
        var socketChannel = (SocketChannel) key.channel();
        var buffer = openSocketChannels.get(socketChannel);
        buffer.clear();
        buffer.put(key.attachment().toString().getBytes());
        buffer.flip();
        while (buffer.hasRemaining()) {
            socketChannel.write(buffer);
        }
        buffer.clear();
        key.interestOps(SelectionKey.OP_READ);
    }

    private void register(Selector selector, ServerSocketChannel serverSocketChannel) throws IOException {
        var socketChannel = serverSocketChannel.accept();
        if (socketChannel != null) {
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            openSocketChannels.put(socketChannel, ByteBuffer.allocate(1024));
        }
    }

    private void closeSocketChannel(SocketChannel socketChannel) throws IOException {
        socketChannel.close();
        openSocketChannels.remove(socketChannel);
    }

    private void processClientMessage(SelectionKey selectionKey, String inputString) {
        log.info("Received message from client: {}",  inputString);
        selectionKey.attach(new StringBuilder(inputString).reverse());
    }

    @Override
    public void close() throws IOException {
        for (var socketChannel : openSocketChannels.keySet()) {
            socketChannel.close();
        }
    }

    @Override
    public void start() throws IOException {
        try (
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            Selector selector = Selector.open()
        ) {
            serverSocketChannel.bind(new InetSocketAddress("localhost", port));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                var cnt = selector.select();
                if (cnt > 0) {
                    var keys = selector.selectedKeys();
                    for (var key : keys) {
                        if (key.isAcceptable()) {
                            register(selector, serverSocketChannel);
                        } else if (key.isReadable()) {
                            read(key);
                        } else if (key.isWritable()) {
                            write(key);
                        }
                    }
                    keys.clear();
                }
            }

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
