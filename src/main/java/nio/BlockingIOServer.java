package nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
public class BlockingIOServer implements Server {
    private final ServerSocket serverSocket;
    private boolean active;

    public BlockingIOServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        this.active = false;
    }

    public void start() throws IOException {
        log.info("Starting JavaIOBlockingServer");
        active = true;
        while (active) {
            log.info("Waiting for connection...");
            Socket socket = serverSocket.accept(); // blocking
            log.info("socket information available");
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            int read;
            byte[] bytes = new byte[1024];
            while ((read = is.read(bytes)) != -1) { // blocking
                if (new String(bytes, 0, read).equals("exit\n")) {
                    active = false;
                    os.write("Shutting down server".getBytes());
                    break;
                }
                os.write("Hello ".getBytes());
                os.write(bytes, 0, read); // blocking
            }
            socket.close();
        }
    }


    public void stop() throws IOException {
        active = false;
        serverSocket.close();
    }

    @Override
    public void close() throws IOException {
        log.info("Closing server!");
        stop();
    }
}
