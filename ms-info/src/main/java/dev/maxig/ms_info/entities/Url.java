package dev.maxig.ms_info.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class Url {
    String shortId;
    String longUrl;
    String userId;
    Long accessCount;
    Long createdAt;
    Long updatedAt;
    Long deletedAt;

}
