package dev.maxig.ms_delete.services;

import java.util.concurrent.CompletableFuture;

public interface DeleteService {
    CompletableFuture<String> deleteUrl(String shortId);
}
