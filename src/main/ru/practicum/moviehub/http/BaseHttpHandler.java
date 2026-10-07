package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        boolean haveBody = json != null && !json.isEmpty();

        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, haveBody ? 0 : -1);
        if (haveBody) {
            try (OutputStream os = ex.getResponseBody()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    protected void sendError(HttpExchange ex, int status, String error, List<String> details) throws IOException {
        String json = null;

        if (error != null) {
            Gson gson = new Gson();
            ErrorResponse body = new ErrorResponse(error, details);
            json = gson.toJson(body);
        }

        sendJson(ex, status, json);
    }
}