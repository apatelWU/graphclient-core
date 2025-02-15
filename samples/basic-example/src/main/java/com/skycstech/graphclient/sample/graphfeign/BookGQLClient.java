package com.skycstech.graphclient.sample.graphfeign;

import com.skycstech.graphclient.core.annotation.GraphFeignClient;
import com.skycstech.graphclient.core.annotation.GraphFeignRequest;
import com.skycstech.graphclient.core.annotation.GraphFeignVariable;
import com.skycstech.graphclient.core.exception.GraphFeignException;
import com.skycstech.graphclient.sample.service.Book;

@GraphFeignClient(name = "bookGQLClient", url = "http://localhost:8080/graphql", configuration = BookGQLClientConfiguration.class)
public interface BookGQLClient {

    @GraphFeignRequest(documentName = "bookQuery", retrievePath = "bookById")
    BookAuthorView getBookById(@GraphFeignVariable("bookId") String bookId) throws GraphFeignException;

}
