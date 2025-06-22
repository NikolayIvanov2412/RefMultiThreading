package org.example;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    private final List<String> validPaths;
    private final ExecutorService threadPool;
    private final Map<String, Map<String, Handler>> handlers;

    public Server(int port, List<String> validPaths) {
        this.port = port;
        this.validPaths = validPaths;
        this.threadPool = Executors.newFixedThreadPool(64); // Пул из 64 потоков
        this.handlers = new HashMap<>();
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, m -> new HashMap<>()).put(path, handler);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedOutputStream out = new BufferedOutputStream(clientSocket.getOutputStream())
        ) {
            // Читаем строку запроса
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                writeResponse(out, 400, "Bad Request");
                return;
            }

            final var method = parts[0]; // Метод запроса (GET, POST и т.д.)
            final var path = parts[1];   // Путь запрашиваемого ресурса

            if (!validPaths.contains(path)) {
                writeResponse(out, 404, "Not Found");
                return;
            }

            // Формируем объект Request
            Request request = parseRequest(in, method, path);

            // Получаем обработчик для текущего запроса
            Handler handler = getHandler(method, path);
            if (handler != null) {
                handler.handle(request, out);
            } else {
                writeResponse(out, 404, "Not Found");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Request parseRequest(BufferedReader in, String method, String path) throws IOException {
        // Заглушка для упрощённого примера
        return new Request(method, path, Map.of(), new byte[0]);
    }

    private Handler getHandler(String method, String path) {
        Map<String, Handler> methodHandlers = handlers.get(method);
        return methodHandlers != null ? methodHandlers.get(path) : null;
    }

    private void writeResponse(BufferedOutputStream out, int statusCode, String reasonPhrase) throws IOException {
        out.write((
                "HTTP/1.1 " + statusCode + " " + reasonPhrase + "\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
}