package dev.maxig.api_gateway.dtos;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateUrlDTO {
    @Nonnull
    private String longUrl;

    @Nonnull
    private String userId;
}
