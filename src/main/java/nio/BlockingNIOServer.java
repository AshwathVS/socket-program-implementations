package nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

@Slf4j
public class BlockingNIOServer implements Server {
    private final ServerSocketChannel serverSocketChannel;
    private boolean active = true;

    public BlockingNIOServer(int port) throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.bind(new InetSocketAddress("localhost", port));
        this.serverSocketChannel.configureBlocking(true);
    }

    @Override
    public void start() throws IOException {
        while (active) {
            SocketChannel socketChannel = serverSocketChannel.accept(); // blocking
            log.info("accept new client");
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            int bytesRead = socketChannel.read(buffer); // blocking
            if (bytesRead == -1) {
                socketChannel.close();
                continue;
            }

            buffer.flip();
            byte[] received = new byte[buffer.remaining()];
            buffer.get(received);
            String clientMessage = new String(received, StandardCharsets.UTF_8).trim();

            if (clientMessage.equalsIgnoreCase("exit")) {
                socketChannel.close();
                this.close();
                active = false;
                break;
            }

            // Prepare response
            buffer.clear();
            buffer.put(("Hello " + clientMessage + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            socketChannel.write(buffer);
            socketChannel.close();
        }
    }

    @Override
    public void close() throws IOException {
        log.info("Closing server!");
        this.active = false;
        this.serverSocketChannel.close();
    }
}
