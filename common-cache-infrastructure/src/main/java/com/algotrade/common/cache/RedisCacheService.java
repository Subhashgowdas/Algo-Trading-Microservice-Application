package com.algotrade.common.cache;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheService implements CacheService{

	private final RedisTemplate<String, Object> redisTemplate;

	public RedisCacheService(RedisTemplate<String, Object> redisTemplate) {
		super();
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void set(String key, Object value, long ttl, TimeUnit unit) {
		redisTemplate.opsForValue().set(key, value,ttl,unit);

	}

	@Override
	public Optional<Object> get(String key) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(key));
	}

	@Override
	public void delete(String key) {
		redisTemplate.delete(key);
	}

	@Override
	public boolean exists(String key) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	@Override
	public Long increment(String key) {
		return redisTemplate.opsForValue().increment(key);
	}

	@Override
	public void expire(String key, long ttl, TimeUnit unit) {
		redisTemplate.expire(key, ttl,unit);
	}

}
