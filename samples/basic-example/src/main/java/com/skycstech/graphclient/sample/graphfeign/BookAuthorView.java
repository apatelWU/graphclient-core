package com.skycstech.graphclient.sample.graphfeign;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class BookAuthorView {

    private String id;
    private String name;
    private int pageCount;
    private Author author;

    @Getter
    @Setter
    @Accessors(chain = true)
    @ToString
    public static class Author {
        private String id;
        private String firstName;
        private String lastName;
    }
}
