package dev.maxig.ms_info.repository;

import dev.maxig.ms_info.entities.Stats;
import dev.maxig.ms_info.entities.Url;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisRepository {
    @Value("${config.application.cache.get-url-expire-time}")
    private int expireTime;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Object getNotFoundUrl(String key) {
        return redisTemplate.opsForValue().get("notFoundedUrl-(" + key + ")");
    }

    public void save(String key, String value) {
        redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.MINUTES);
    }

    public void saveNotFoundUrl(String key) {
        redisTemplate.opsForValue().set("notFoundedUrl-(" + key + ")", "", 1, TimeUnit.HOURS);
    }

    public Url getCompleteUrl(String key) {
        try {
            Object longUrl = redisTemplate.opsForHash().get("completeUrl-(" + key + ")", "longUrl");
            Object userId = redisTemplate.opsForHash().get("completeUrl-(" + key + ")", "userId");
            Object accessCount = redisTemplate.opsForHash().get("completeUrl-(" + key + ")", "accessCount");
            Object createdAt = redisTemplate.opsForHash().get("completeUrl-(" + key + ")", "createdAt");
            Object updatedAt = redisTemplate.opsForHash().get("completeUrl-(" + key + ")", "updatedAt");
            Object deletedAt = redisTemplate.opsForHash().get("completeUrl-(" + key + ")", "deletedAt");

            return Url.builder()
                    .shortId(key)
                    .longUrl(longUrl.toString())
                    .userId(userId.toString())
                    .accessCount(Long.valueOf(accessCount.toString()))
                    .createdAt(Long.valueOf(createdAt.toString()))
                    .updatedAt(Long.valueOf(updatedAt.toString()))
                    .deletedAt(Long.valueOf(deletedAt.toString()))
                    .build();}
        catch (Exception e) {
            return null;
        }

    }

    public void saveCompleteUrl(String key, Url url) {
        HashMap<String, String> hashUrl = new HashMap<>();
        hashUrl.put("longUrl", url.getLongUrl());
        hashUrl.put("userId", url.getUserId());
        hashUrl.put("accessCount", String.valueOf(url.getAccessCount()));
        hashUrl.put("createdAt", String.valueOf(url.getCreatedAt()));
        hashUrl.put("updatedAt", String.valueOf(url.getUpdatedAt()));
        hashUrl.put("deletedAt", String.valueOf(url.getDeletedAt()));

        redisTemplate.opsForHash().putAll("completeUrl-(" + key + ")", hashUrl);
    }

    public Stats getGlobalStats() {
        Object urlsCount = redisTemplate.opsForHash().get("globalStats-()", "urlsCount");
        Object urlsRedirect = redisTemplate.opsForHash().get("globalStats-()", "urlsRedirect");
        if (urlsCount == null || urlsRedirect == null) {
            return null;
        }
        return Stats.builder()
                .urlsCount(Long.valueOf(urlsCount.toString()))
                .urlsRedirect(Long.valueOf(urlsRedirect.toString()))
                .build();
    }

    public void saveGlobalStats(HashMap<String, String> hashMap) {
        redisTemplate.opsForHash().putAll("globalStats-()", hashMap);
    }
}
