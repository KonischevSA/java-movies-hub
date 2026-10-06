package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.stream.Collectors.toCollection;

public class MoviesStore {
    private HashMap<Integer, Movie> movies = new HashMap<>();

    public List<Movie> getMovies() {
        return movies.values().stream().toList();
    }

    public void addMovie(Movie movie) {
        movies.put(movie.getId(), movie);
    }

    public Movie getMovieById(int id) {
        return movies.get(id);
    }

    public void removeMovieById(int id) {
        movies.remove(id);
    }

    public void removeAllMovies() {
        movies.clear();
        Movie.resetIdCounter();
    }

    public List<Movie> getMoviesByYear(int year) {
        return movies.values().stream()
                .filter(movie -> movie.getYear() == year)
                .collect(toCollection(ArrayList::new));
    }
}