package dev.maxig.ms_core.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class RedisRepository {
    @Value("${config.application.cache.urls-expire-time}")
    private int expireTime;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void save(String key, String value) {
        redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.MINUTES);
    }

}
