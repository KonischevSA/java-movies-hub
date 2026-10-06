package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {

    private MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        Endpoint endpoint = getEndpoint(ex.getRequestMethod(), ex.getRequestURI());
        switch (endpoint) {
            case GET_MOVIES -> handleGetMovies(ex);
            case GET_MOVIES_ID -> handleGetMoviesById(ex);
            case GET_MOVIES_YEAR -> handleGetMoviesByYear(ex);
            case POST_MOVIES -> handlePostMovies(ex);
            case DELETE_MOVIES_ID -> handleDeleteMoviesById(ex);
            case UNKNOWN -> sendJson(ex, 404, "Такого эндпоинта не существует");
        }
    }

    private Endpoint getEndpoint(String method, URI uri) {
        String parms = uri.getQuery();
        String[] pathParts = uri.getPath().split("/");

        switch (method) {
            case "GET" -> {
                if (pathParts.length == 2 && pathParts[1].equalsIgnoreCase("movies")) {
                    if (parms == null) {
                        return Endpoint.GET_MOVIES;
                    } else if (getParmValue(parms, "year") != null) {
                        return Endpoint.GET_MOVIES_YEAR;
                    }
                } else if (pathParts.length == 3 && pathParts[1].equalsIgnoreCase("movies")) {
                    return Endpoint.GET_MOVIES_ID;
                }
            }
            case "POST" -> {
                if (pathParts.length == 2 && pathParts[1].equalsIgnoreCase("movies")) {
                    return Endpoint.POST_MOVIES;
                }
            }
            case "DELETE" -> {
                if (pathParts.length == 3 && pathParts[1].equalsIgnoreCase("movies")) {
                    return Endpoint.DELETE_MOVIES_ID;
                }
            }
        }

        return Endpoint.UNKNOWN;
    }

    private String getParmValue(String parms, String parmName) {
        if (parms == null || !parms.contains(parmName)) {
            return null;
        }

        String[] parmPairs = parms.split("&");
        for (String pair : parmPairs) {
            if (pair.indexOf(parmName) == 0) {
                return pair.split("=")[1];
            }
        }

        return null;
    }

    private void handleGetMovies(HttpExchange ex) throws IOException {
        Gson gson = new Gson();
        String jsonResp = gson.toJson(store.getMovies());

        sendJson(ex, 200, jsonResp);
    }

    private void handleGetMoviesById(HttpExchange ex) throws IOException {
        Gson gson = new Gson();
        String jsonResp;
        int id;

        try {
            id = Integer.parseInt(ex.getRequestURI().getPath().split("/")[2]);
            Movie movie = store.getMovieById(id);

            if (movie != null) {
                jsonResp = gson.toJson(movie);
                sendJson(ex, 200, jsonResp);
            } else {
                jsonResp = gson.toJson("Фильм не найден");
                sendJson(ex, 404, jsonResp);
            }
        } catch (Exception e) {
            jsonResp = gson.toJson("Некорректный ID");
            sendJson(ex, 400, jsonResp);
        }
    }

    private void handleGetMoviesByYear(HttpExchange ex) throws IOException {
        Gson gson = new Gson();
        String jsonResp;

        try {
            int year = Integer.parseInt(getParmValue(ex.getRequestURI().getQuery(), "year"));
            jsonResp = gson.toJson(store.getMoviesByYear(year));
            sendJson(ex, 200, jsonResp);
        } catch (Exception e) {
            jsonResp = gson.toJson("Некорректный параметр запроса — year");
            sendJson(ex, 400, jsonResp);
        }
    }

    private void handlePostMovies(HttpExchange ex) throws IOException {
        List<String> contentTypeValues = ex.getRequestHeaders().get("Content-type");

        if (contentTypeValues == null || !contentTypeValues.contains("application/json")) {
            sendNoContent(ex, 415);
            return;
        }

        Gson gson = new Gson();
        String jsonResp;

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonElement jsonElement = JsonParser.parseString(body);

        List<String> errors = new ArrayList<>();
        if (!jsonElement.isJsonObject()) {
            errors.add("Не удалось получить объект Java. Проверьте правильность переданных данных");
        } else {
            String title = "";
            int year = 0;
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            JsonElement titleJs = jsonObject.get("title");

            if (titleJs != null) {
                title = titleJs.getAsString();
                if (title.isEmpty() || title.length() > 100) {
                    errors.add("Название должно быть от 1 до 100 символов.");
                }
            } else {
                errors.add("Отсутствует название фильма в теле запроса.");
            }

            JsonElement yearJs = jsonObject.get("year");

            if (yearJs != null) {
                try {
                    year = yearJs.getAsInt();
                    if (year < 1888 || year > 2027) {
                        errors.add("Год должен быть между 1888 и 2027 включительно.");
                    }
                } catch (Exception e) {
                    errors.add("Год должен быть числом между 1888 и 2027 включительно.");
                }

            } else {
                errors.add("Отсутствует год выпуска фильма в теле запроса.");
            }

            if (errors.isEmpty()) {
                Movie movie = new Movie(title, year);
                store.addMovie(movie);

                jsonResp = gson.toJson(movie);
                sendJson(ex, 201, jsonResp);
            } else {
                jsonResp = gson.toJson(new ErrorResponse("Ошибка валидации", errors));
                sendJson(ex, 422, jsonResp);
            }
        }
    }

    private void handleDeleteMoviesById(HttpExchange ex) throws IOException {
        Gson gson = new Gson();
        String jsonResp;
        int id;

        try {
            id = Integer.parseInt(ex.getRequestURI().getPath().split("/")[2]);
            Movie movie = store.getMovieById(id);

            if (movie != null) {
                store.removeMovieById(id);
                sendNoContent(ex, 204);
            } else {
                jsonResp = gson.toJson("Фильм не найден");
                sendJson(ex, 404, jsonResp);
            }
        } catch (Exception e) {
            jsonResp = gson.toJson("Некорректный ID");
            sendJson(ex, 400, jsonResp);
        }
    }
}
