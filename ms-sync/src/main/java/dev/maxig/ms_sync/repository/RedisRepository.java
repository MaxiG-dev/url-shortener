package dev.maxig.ms_sync.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisRepository {
    @Value("${config.application.cache.urls-expire-time}")
    private int expireTime;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void saveCacheUrl(String key, String value) {
        redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.MINUTES);
    }

    public void saveGlobalStats(HashMap<String, String> hashMap) {
        redisTemplate.opsForHash().putAll("globalStats-()", hashMap);
    }

}