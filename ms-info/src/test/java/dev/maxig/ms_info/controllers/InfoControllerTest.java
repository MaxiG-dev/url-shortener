package dev.maxig.ms_info.controllers;

import dev.maxig.ms_info.entities.Stats;
import dev.maxig.ms_info.services.InfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
public class InfoControllerTest {

    @Mock
    private InfoService service;

    @InjectMocks
    private InfoController controller;

    private final String validApiKey = "test-api-key";
    private final String invalidApiKey = "wrong-api-key";

    @BeforeEach
    void setUp() {
        controller = new InfoController(service);
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getLongUrl_WithValidApiKey_ReturnsUrl() {
        String shortUrl = "abc123";
        CompletableFuture<String> expectedFuture = CompletableFuture.completedFuture("http://example.com");
        when(service.getLongUrl(shortUrl)).thenReturn(expectedFuture);

        CompletableFuture<ResponseEntity<String>> result = controller.getLongUrl(shortUrl, validApiKey);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(HttpStatus.OK, result.join().getStatusCode()),
                () -> assertEquals("http://example.com", result.join().getBody())
        );
    }

    @Test
    void getLongUrl_WithInvalidApiKey_ReturnsForbidden() {
        String shortUrl = "abc123";

        CompletableFuture<ResponseEntity<String>> result = controller.getLongUrl(shortUrl, invalidApiKey);

        assertEquals(HttpStatus.FORBIDDEN, result.join().getStatusCode());
    }

    @Test
    void getGlobalStats_WithValidApiKey_ReturnsStats() {
        Long urlsCount = 10L;
        Long urlsRedirect = 20L;
        CompletableFuture<Stats> statsFuture = CompletableFuture.completedFuture(new Stats(urlsCount, urlsRedirect));
        when(service.getGlobalStats()).thenReturn(statsFuture);

        CompletableFuture<ResponseEntity<Stats>> result = controller.getGlobalStats(validApiKey);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(HttpStatus.OK, result.join().getStatusCode()),
                () -> assertEquals(10, Objects.requireNonNull(result.join().getBody()).getUrlsCount()),
                () -> assertEquals(20, Objects.requireNonNull(result.join().getBody()).getUrlsRedirect())
        );
    }

    @Test
    void getGlobalStats_WithInvalidApiKey_ReturnsForbidden() {
        CompletableFuture<ResponseEntity<Stats>> result = controller.getGlobalStats(invalidApiKey);

        assertEquals(HttpStatus.FORBIDDEN, result.join().getStatusCode());
    }
}
