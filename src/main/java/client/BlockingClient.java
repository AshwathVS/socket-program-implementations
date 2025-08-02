package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Scanner;

class BlockingClient implements Client {
    public void start(int serverPort, Scanner scanner) {
        try (var socket = new java.net.Socket("localhost", serverPort);
             var writer = new PrintWriter(socket.getOutputStream(), true);
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            System.out.println("Connected to server!");
            for (String input; (input = scanner.nextLine()) != null;) {
                if (input.isEmpty()) {
                    break;
                }
                writer.println(input);
                System.out.println(reader.readLine());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
