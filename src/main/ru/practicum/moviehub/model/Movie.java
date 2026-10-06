package ru.practicum.moviehub.model;

public class Movie {
    private static int moviesCount = 1;
    private int id;
    private String title;
    private int year;

    public Movie(String title, int year) {
        this.title = title;
        this.year = year;
        id = moviesCount++;
    }

    /*Для тестов, чтобы корректно отрабатывали при массовом запуске, а не только по одному*/
    public static void resetIdCounter() {
        moviesCount = 1;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }
}