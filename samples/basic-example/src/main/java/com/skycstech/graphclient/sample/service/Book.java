package com.skycstech.graphclient.sample.service;

import java.util.Arrays;
import java.util.List;

public record Book(String id, String name, int pageCount, String authorId) {

    private static List<Book> books = Arrays.asList(
            new Book("book-1", "The Art of war", 223, "author-1"),
            new Book("book-2", "Atomic Habits", 635, "author-2"),
            new Book("book-3", "The Alchemist", 371, "author-3")
    );

    public static Book getById(String id) {
        return books.stream().filter(book -> book.id().equals(id)).findFirst().orElse(null);
    }

}