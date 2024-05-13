package dev.maxig.ms_delete.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class RedisRepository {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void saveNotFoundUrl(String key) {
        redisTemplate.opsForValue().set("notFoundedUrl-(" + key + ")", "", 1, TimeUnit.HOURS);
    }

    public Object getNotFoundUrl(String key) {
        return redisTemplate.opsForValue().get("notFoundedUrl-(" + key + ")");
    }

    public void delete(String key) {
        redisTemplate.opsForValue().getAndDelete(key);
    }
}
