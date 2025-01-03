# GraphFeign: A Feign-Like Client for GraphQL Queries and Mutations

GraphFeign is a declarative client for GraphQL APIs built with Spring Boot and WebClient. Inspired by the ease of use of Feign Clients in REST APIs, GraphFeign provides a simple way to make GraphQL queries and mutations in a declarative, strongly-typed, and intuitive manner.

This repository contains the core library for integrating **GraphFeign** into your Spring Boot application, allowing you to interact with GraphQL APIs in a seamless and efficient way.

---

## Features

- **Declarative GraphQL Client**: Make GraphQL requests like REST using a Feign-like syntax.
- **Easy Setup**: Works out of the box with Spring Boot, WebClient, and Feign integration.
- **Query and Mutation Support**: Easily map GraphQL queries and mutations to Java methods.
- **Advanced Configurations**: Custom headers, interceptors, dynamic query generation, and more.
- **GraphQL Subscriptions**: Built-in support for GraphQL subscriptions for real-time updates.
- **Strongly Typed**: Automatically map GraphQL responses to Java POJOs.

---

## Getting Started

Follow these steps to get started with GraphFeign in your Spring Boot application:

### Prerequisites

- Java 17 or later
- Spring Boot 2.7.x project
- Maven build tool

### Installation

To use GraphFeign, add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.skycstech.graphclient</groupId>
    <artifactId>graphclient-core</artifactId>
    <version>0.0.1</version>
</dependency>
```

---

## Configuration

### Step 1: Enable GraphFeign in Your Application

In your Spring Boot main application class, add the `@EnableGraphFeignClients` annotation to enable GraphFeign.

```java
@EnableGraphFeignClients
@SpringBootApplication
public class GraphFeignApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphFeignApplication.class, args);
    }
}
```

### Step 2: Define Your GraphQL Client Interface

Create a Java interface for your GraphQL client. Annotate it with `@GraphFeignClient` and define methods for the GraphQL queries you want to perform.

```java
@GraphFeignClient(name = "graphClient", 
                  url = "https://localhost:4351/graphql", 
                  configuration = GQLClientConfiguration.class)
public interface GraphQLClient {

    @GraphFeignRequest(documentName = "FetchBookByAuthorIDQuery", retrievePath = "books")
    List<Book> getBooksByAuthorID(@GraphFeignVariable("request") Long authorID) throws GraphFeignException;

    @GraphFeignRequest(documentName = "FetchBookByIDQuery", retrievePath = "book")
    Book getBookByID(@GraphFeignVariable("request") Long bookId) throws GraphFeignException;
}
```

### Step 3: Custom Configuration (Optional)

If you need to configure custom headers, interceptors, or document sources, implement a configuration class that implements `GraphFeignClientConfiguration`.

```java
public class GQLClientConfiguration implements GraphFeignClientConfiguration {

    @Override
    public Consumer<HttpHeaders> headersConsumer(Method method) {
        return headers -> {
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        };
    }

    // Optional configurations for interceptors or document source
    /*
    @Override
    public Consumer<List<GraphQlClientInterceptor>> interceptorsConsumer(Method method) {
        return interceptors -> {
            // Add custom interceptors if needed
        };
    }

    @Override
    public DocumentSource documentSource(Method method) {
        return null; // Provide custom document source if needed
    }
    */
}
```

### Step 4: Inject and Use the Client

In your service class, inject the GraphQL client and start making GraphQL requests.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphQLService {

    @Resource // added this to avoid IDE error - Spring could resolve the bean without this at Runtime.
    private final GraphQLClient graphQLClient;

    public List<Book> getBooksByAuthorID(Long authorID) {
        try {
            return graphQLClient.getBooksByAuthorID(authorID);
        } catch (GraphFeignException e) {
            log.error("Error occurred while calling GraphQL API", e);
        }
        return Collections.emptyList();
    }
}
```

---

## Advanced Usage

### Handling Multiple Operations in a Single Document

If a GraphQL document contains multiple operations, you can specify the operation name:

```java
@GraphFeignRequest(documentName = "FetchBookByAuthorIDQuery", 
                   operationName = "SomeOperationName", 
                   retrievePath = "books")
List<Book> getBooksByAuthorID(@GraphFeignVariable("request") Long authorID) throws GraphFeignException;
```

### Dynamically Loading GraphQL Queries

You can pass the GraphQL query document dynamically as a string or load it from a custom source:

```java
@GraphFeignRequest(retrievePath = "books")
List<Book> getBooksByAuthorID(@GraphFeignDocument String document, 
                              @GraphFeignVariable("request") Long authorID);
```

### Using ClientGraphQlResponse for Raw Response

You can get the raw response as a `ClientGraphQlResponse` to have more control over the response processing.

```java
@GraphFeignRequest(documentName = "FetchBookByAuthorIDQuery")
Mono<ClientGraphQlResponse> getBooksByAuthorID(@GraphFeignVariable("request") Long authorID) 
    throws GraphFeignException;
```

### GraphQL Subscriptions

GraphFeign supports GraphQL subscriptions, which allow you to listen for real-time updates:

```java
@GraphFeignRequest(documentName = "FetchBookByIDQuery", 
                   retrievePath = "book", 
                   isSubscription = true)
Flux<Book> getBookByID(@GraphFeignVariable("request") Long bookId) throws GraphFeignException;
```

---

## Contributing

We welcome contributions! If you'd like to contribute to **GraphFeign**, feel free to fork the repository, create a pull request, and submit your changes. Please make sure to follow the existing code style and add tests for new features.

---

Feel free to adapt this README as needed. Happy coding!