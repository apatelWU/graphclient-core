package com.skycstech.graphclient.sample.graphfeign;

import com.skycstech.graphclient.sample.service.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@RequiredArgsConstructor
public class BookFeignService {

    @Resource
    private final BookGQLClient bookGQLClient;

    @EventListener(ApplicationReadyEvent.class)
    public void getBookById() {
        try {
            BookAuthorView bookAuthorView = bookGQLClient.getBookById("book-1");
            System.out.println(bookAuthorView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
