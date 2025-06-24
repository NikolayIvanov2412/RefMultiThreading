package org.example;


import org.apache.hc.core5.net.URLEncodedUtils;
import org.apache.hc.core5.http.NameValuePair;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final byte[] body;
    private final Map<String, List<String>> parameters;

    public Request(String method, String rawPath, Map<String, String> headers, byte[] body) {
        this.method = method;
        // Отделяем путь от параметров запроса
        int questionMarkPos = rawPath.indexOf('?');
        if (questionMarkPos >= 0) {
            this.path = rawPath.substring(0, questionMarkPos);
            String queryString = rawPath.substring(questionMarkPos + 1);
            // Получаем список NameValuePair
            List<NameValuePair> parsedParameters = URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8);
            // Преобразуем в Map<String, List<String>>
            this.parameters = toMultiValueMap(parsedParameters);
        } else {
            this.path = rawPath;
            this.parameters = Map.of(); // пустая неизменяемая карта
        }
        this.headers = headers;
        this.body = body;
    }

    private Map<String, List<String>> toMultiValueMap(List<NameValuePair> params) {
        Map<String, List<String>> multiValueMap = new LinkedHashMap<>();
        for (NameValuePair param : params) {
            String key = param.getName();
            String value = param.getValue();
            multiValueMap.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return multiValueMap;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    /**
     * Возвращает первый параметр с указанным именем
     */
    public String getQueryParam(String name) {
        List<String> values = parameters.get(name);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    /**
     * Возвращает все параметры запроса
     */
    public Map<String, List<String>> getQueryParams() {
        return parameters;
    }
}