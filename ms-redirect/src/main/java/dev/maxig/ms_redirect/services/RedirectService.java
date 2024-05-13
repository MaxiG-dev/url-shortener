package dev.maxig.ms_redirect.services;

import java.util.concurrent.CompletableFuture;

public interface RedirectService {
    CompletableFuture<String> getLongUrl(String shortId, boolean redirect);
}
