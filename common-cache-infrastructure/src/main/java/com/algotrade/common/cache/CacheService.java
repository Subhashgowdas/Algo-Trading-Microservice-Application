package com.algotrade.common.cache;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface CacheService {
	
	void set(String key, Object value, long ttl, TimeUnit unit);
	Optional<Object> get(String key);
	void delete(String key);
	boolean exists(String key);
	Long increment(String key);
	void expire(String key,long ttl,TimeUnit unit);
}
