package dev.maxig.ms_info.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class Stats {
    Long urlsCount;
    Long urlsRedirect;
}
