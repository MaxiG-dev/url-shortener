package dev.maxig.ms_redirect.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class RedisRepository {
    @Value("${config.application.cache.get-url-expire-time}")
    private int expireTime;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void save(String key, String value) {
        redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.MINUTES);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void saveNotFoundUrl(String key) {
        redisTemplate.opsForValue().set("notFoundedUrl-(" + key + ")", "", 1, TimeUnit.HOURS);
    }

    public Object getNotFoundUrl(String key) {
        return redisTemplate.opsForValue().get("notFoundedUrl-(" + key + ")");
    }

    public void updateUrlsRedirectCount(String key) {
        redisTemplate.opsForHash().increment("completeUrl-(" + key + ")", "accessCount", 1);
        redisTemplate.opsForHash().increment("globalStats-()", "urlsRedirect", 1);
    }
}
