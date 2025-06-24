package org.example;

import java.io.BufferedOutputStream;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        final var validPaths = List.of("/", "/hello");
        final var server = new Server(9999, validPaths);

        // Регистрируем обработчик для GET-запроса на путь '/hello'
        server.addHandler("GET", "/hello", new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream outputStream) throws Exception {
                String lastParam = request.getQueryParam("last");
                String responseText = "Параметр last равен: " + lastParam;
                byte[] content = responseText.getBytes("UTF-8");

                outputStream.write("HTTP/1.1 200 OK\r\n".getBytes());
                outputStream.write("Content-Type: text/plain; charset=UTF-8\r\n".getBytes());
                outputStream.write("Content-Length: ".getBytes());
                outputStream.write(Integer.toString(content.length).getBytes());
                outputStream.write("\r\n".getBytes());
                outputStream.write("Connection: close\r\n".getBytes());
                outputStream.write("\r\n".getBytes());
                outputStream.write(content);
                outputStream.flush();
            }
        });

        // Начинаем слушание соединений
        server.start();
    }
}