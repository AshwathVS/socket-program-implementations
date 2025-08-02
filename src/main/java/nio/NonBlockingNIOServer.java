package nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.StandardCharsets;

@Slf4j
public class NonBlockingNIOServer implements Server {
    private final ServerSocketChannel serverSocketChannel;
    private volatile boolean active = true;

    public NonBlockingNIOServer(int port) throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.bind(new InetSocketAddress("localhost", port));
        this.serverSocketChannel.configureBlocking(false);
    }

    @Override
    public void start() throws IOException {
        while (active) {
            var socketChannel = serverSocketChannel.accept();
            if (socketChannel != null) {
                socketChannel.configureBlocking(false);
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                int bytesRead = socketChannel.read(buffer);
                if (bytesRead == -1) {
                    socketChannel.close();
                    continue;
                }

                buffer.flip();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                String clientMessage = new String(bytes, StandardCharsets.UTF_8).trim();

                if (clientMessage.equalsIgnoreCase("exit")) {
                    socketChannel.close();
                    this.close();
                    active = false;
                    break;
                }

                buffer.clear();
                buffer.put(("Hello " + clientMessage + "\n").getBytes(StandardCharsets.UTF_8));
                buffer.flip();
                socketChannel.write(buffer);
                socketChannel.close();
            }
        }
    }


    @Override
    public void close() throws IOException {
        if (this.active) {
            log.info("Closing server!");
            this.active = false;
            this.serverSocketChannel.close();
        }
    }
}
