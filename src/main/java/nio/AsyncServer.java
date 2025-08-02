package nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class AsyncServer implements Server {
    private final int port;
    private final AsynchronousServerSocketChannel serverSocketChannel;
    private final CountDownLatch latch = new CountDownLatch(1);

    public AsyncServer(int port) {
        try {
            this.port = port;
            this.serverSocketChannel = AsynchronousServerSocketChannel.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() throws IOException {
        this.serverSocketChannel.bind(new InetSocketAddress("localhost", port));
        AcceptCompletionHandler handler = new AcceptCompletionHandler(this.serverSocketChannel);
        log.info("Server started");
        this.serverSocketChannel.accept(null, handler);

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        this.latch.countDown();
        this.serverSocketChannel.close();
    }

    private record AcceptCompletionHandler(
        AsynchronousServerSocketChannel serverSocketChannel) implements CompletionHandler<AsynchronousSocketChannel, Void> {

        @Override
            public void completed(AsynchronousSocketChannel socketChannel, Void attachment) {
                this.serverSocketChannel.accept(attachment, this);
                log.info("Client connected");
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                ReadCompletionHandler readCompletionHandler = new ReadCompletionHandler(socketChannel, byteBuffer);
                socketChannel.read(byteBuffer, attachment, readCompletionHandler);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {

            }
        }

    private record ReadCompletionHandler(
        AsynchronousSocketChannel socketChannel, ByteBuffer buffer
    ) implements CompletionHandler<Integer, Void> {

        @Override
        public void completed(Integer result, Void attachment) {
            if (result == -1) {
                closeSocket();
                return;
            }

            buffer.flip();
            String input = new String(buffer.array(), 0, result).trim();
            log.info("Received from client: {}", input);

            // Echo back the reversed input
            String response = new StringBuilder(input).reverse().append("\n").toString();

            buffer.clear();
            buffer.put(response.getBytes());
            buffer.flip();

            socketChannel.write(buffer, null, new WriteCompletionHandler(socketChannel, buffer, !input.equalsIgnoreCase("exit")));
        }

        @Override
        public void failed(Throwable exc, Void attachment) {
            closeSocket();
        }

        private void closeSocket() {
            try {
                socketChannel.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    private record WriteCompletionHandler(
        AsynchronousSocketChannel socketChannel, ByteBuffer buffer, boolean keepAlive) implements CompletionHandler<Integer, Void> {
        @Override
        public void completed(Integer result, Void attachment) {
            if (keepAlive) {
                log.info("Client keep alive");
                // re-read again
                buffer.clear();
                socketChannel.read(buffer, null, new ReadCompletionHandler(socketChannel, buffer));
            } else {
                try {
                    log.info("Closing client connection");
                    socketChannel.close();
                }  catch (IOException e) {
                    // do nothing
                }
            }
        }

        @Override
        public void failed(Throwable exc, Void attachment) {

        }
    }
}
