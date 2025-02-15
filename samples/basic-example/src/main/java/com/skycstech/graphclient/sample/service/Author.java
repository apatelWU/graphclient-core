package com.skycstech.graphclient.sample.service;

import java.util.Arrays;
import java.util.List;

public record Author(String id, String firstName, String lastName) {

    private static List<Author> authors = Arrays.asList(
            new Author("author-1", "Sun", "Zu"),
            new Author("author-2", "James", "Clear"),
            new Author("author-3", "Paulo", "Coelho")
    );

    public static Author getById(String id) {
        return authors.stream().filter(author -> author.id().equals(id)).findFirst().orElse(null);
    }

}
