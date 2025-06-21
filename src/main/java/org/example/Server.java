package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Класс Server, содержащий основные методы сервера
public class Server {
    private final int port;
    private final List<String> validPaths;
    private final ExecutorService threadPool;

    public Server(int port, List<String> validPaths) {
        this.port = port;
        this.validPaths = validPaths;
        this.threadPool = Executors.newFixedThreadPool(64); // создаем пул из 64 потоков
    }

    // Метод для запуска сервера
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порте " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(() -> handleClient(clientSocket, validPaths)); // запускаем новую сессию в отдельном потоке
            }
        } catch (IOException e) {
            System.err.println("Ошибка запуска сервера: " + e.getMessage());
        }
    }

    // Метод для обработки отдельной сессии
    private void handleClient(Socket clientSocket, List<String> validPaths) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedOutputStream writer = new BufferedOutputStream(clientSocket.getOutputStream())
        ) {
            String requestLine = reader.readLine();
            String[] parts = requestLine.split(" ");

            if (parts.length != 3 || !parts[0].equals("GET")) {
                sendError(writer, "400 Bad Request");
                return;
            }

            String requestedPath = parts[1];
            if (!validPaths.contains(requestedPath)) {
                sendError(writer, "404 Not Found");
                return;
            }

            Path filePath = Path.of(".", "public", requestedPath);
            String mimeType = Files.probeContentType(filePath);

            if (requestedPath.equals("/classic.html")) {
                String template = Files.readString(filePath);
                String replacedTemplate = template.replace("{time}", LocalDateTime.now().toString());
                sendResponse(writer, "200 OK", mimeType, replacedTemplate.getBytes());
            } else {
                byte[] content = Files.readAllBytes(filePath);
                sendResponse(writer, "200 OK", mimeType, content);
            }
        } catch (IOException e) {
            System.err.println("Ошибка обработки запроса: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Ошибка закрытия сокета: " + e.getMessage());
            }
        }
    }

    // Метод для формирования и отправки стандартного HTTP-ответа
    private void sendResponse(BufferedOutputStream writer, String statusCode, String contentType, byte[] content) throws IOException {
        writer.write(("HTTP/1.1 " + statusCode + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + content.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n").getBytes());
        writer.write(content);
        writer.flush();
    }

    // Вспомогательный метод для отправки сообщений об ошибке
    private void sendError(BufferedOutputStream writer, String errorCode) throws IOException {
        sendResponse(writer, errorCode, "", "".getBytes());
    }
}

