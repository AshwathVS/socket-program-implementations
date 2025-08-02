import lombok.extern.slf4j.Slf4j;
import nio.AsyncServer;
import nio.BlockingIOServer;
import nio.BlockingNIOServer;
import nio.MultiplexingNIOServer;
import nio.NonBlockingNIOServer;
import nio.Server;

import java.io.IOException;
import java.util.Scanner;

@Slf4j
public class Main {
    public static void main(String[] args) {
        int port = 7000;
        log.info("Enter server type (1 - 5): ");
        Scanner scanner = new Scanner(System.in);
        int serverType = scanner.nextInt();

        try (Server server = getServer(serverType, port)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    log.info("Shutting down server...");
                    server.close();
                } catch (IOException e) {
                    log.error("Error shutting down server", e);
                }
            }));
            server.start();
        } catch (Exception e) {
            log.error("Unexpected error", e);
        }
    }

    static Server getServer(int type, int port) throws IOException {
        return switch (type) {
            case 1 -> new BlockingIOServer(port);
            case 2 -> new BlockingNIOServer(port);
            case 3 -> new NonBlockingNIOServer(port);
            case 4 -> new MultiplexingNIOServer(port);
            case 5 -> new AsyncServer(port);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }
}
