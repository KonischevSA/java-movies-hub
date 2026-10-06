package ru.practicum.moviehub.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080"; // !!! добавьте базовую часть URL
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;


    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        if (store != null) {
            store.removeAllMovies();
        }
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("[]", body);
    }

    @Test
    void getMovies_whenNotEmpty_returnsMoviesArray() throws Exception {
        store.addMovie(new Movie("Первый", 1900));
        store.addMovie(new Movie("Второй", 1990));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("[{\"id\":1,\"title\":\"Первый\",\"year\":1900},{\"id\":2,\"title\":\"Второй\",\"year\":1990}]", body);
    }

    @Test
    void postMovies_whenNormalMovie_returnsNormalMovie() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{title:\"Первый\",year:1900}"))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("{\"id\":1,\"title\":\"Первый\",\"year\":1900}", body);
    }

    @Test
    void postMovies_whenEmptyTitle_returnsValidationError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{title:\"\",year:1900}"))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("{\"error\":\"Ошибка валидации\",\"details\":[\"Название должно быть от 1 до 100 символов.\"]}", body);
    }

    @Test
    void postMovies_whenTooLongTitle_returnsValidationError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{title:" + "Первый".repeat(20) + ",year:1900}"))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("{\"error\":\"Ошибка валидации\",\"details\":[\"Название должно быть от 1 до 100 символов.\"]}", body);
    }

    @Test
    void postMovies_whenYearBelow1888_returnsValidationError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{title:\"Первый\",year:1887}"))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("{\"error\":\"Ошибка валидации\",\"details\":[\"Год должен быть между 1888 и 2027 включительно.\"]}", body);
    }

    @Test
    void postMovies_whenYearMore2027_returnsValidationError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{title:\"Первый\",year:2028}"))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("{\"error\":\"Ошибка валидации\",\"details\":[\"Год должен быть между 1888 и 2027 включительно.\"]}", body);
    }

    @Test
    void postMovies_whenWrongContentType_returnsUnsupportedMediaTypeError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "app/js")
                .POST(HttpRequest.BodyPublishers.ofString("{title:\"Первый\",year:2028}"))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(415, resp.statusCode());
    }

    @Test
    void getMoviesById_whenExistingId_returnsMovie() throws Exception {
        store.addMovie(new Movie("Первый", 1900));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("{\"id\":1,\"title\":\"Первый\",\"year\":1900}", body);
    }

    @Test
    void getMoviesById_whenNotExistingId_returnsNotFoundError() throws Exception {
        store.addMovie(new Movie("Первый", 1900));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("\"Фильм не найден\"", body);
    }

    @Test
    void getMoviesById_whenIdNotNumber_returnsNotFoundError() throws Exception {
        store.addMovie(new Movie("Первый", 1900));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/один"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("\"Некорректный ID\"", body);
    }

    @Test
    void deleteMoviesById_whenExistingId_returnsNoContent() throws Exception {
        store.addMovie(new Movie("Первый", 1900));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode());
    }

    @Test
    void deleteMoviesById_whenIdNotFound_returnsNotFoundError() throws Exception {
        store.addMovie(new Movie("Первый", 1900));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("\"Фильм не найден\"", body);
    }

    @Test
    void deleteMoviesById_whenIdNotNumber_returnsNotFoundError() throws Exception {
        store.addMovie(new Movie("Первый", 1900));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/один"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("\"Некорректный ID\"", body);
    }

    @Test
    void getMoviesByYear_whenExistingYear_returnsMoviesArray() throws Exception {
        store.addMovie(new Movie("Первый", 1900));
        store.addMovie(new Movie("Второй", 1900));
        store.addMovie(new Movie("Третий", 1910));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1900"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("[{\"id\":1,\"title\":\"Первый\",\"year\":1900},{\"id\":2,\"title\":\"Второй\",\"year\":1900}]", body);
    }

    @Test
    void getMoviesByYear_whenNoMoviesWithSuchYear_returnsEmptyArray() throws Exception {
        store.addMovie(new Movie("Первый", 1900));
        store.addMovie(new Movie("Второй", 1900));
        store.addMovie(new Movie("Третий", 1910));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1950"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("[]", body);
    }

    @Test
    void getMoviesByYear_whenYearNotNumber_returnsEmptyArray() throws Exception {
        store.addMovie(new Movie("Первый", 1900));
        store.addMovie(new Movie("Второй", 1900));
        store.addMovie(new Movie("Третий", 1910));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=тыщадевятьисотый"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        String body = resp.body().trim();
        assertEquals("\"Некорректный параметр запроса — \\u0027year\\u0027\"", body);
    }
}