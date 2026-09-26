package com.algotrade.common.commonlib;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StringUtils {

	private static Logger log = LoggerFactory.getLogger(StringUtils.class);

	private StringUtils() {
		// Prevent instantiation
	}

	/**
	 * Safely and efficiently retrieves a string value from a map.
	 *
	 * @param map The map to search.
	 * @param key The key to look up.
	 * @return The String value, or null if the map is null, the key is missing/invalid, or the value is null.
	 */

	public static String getStringValue(Map<?,?> map, String key) {
		if(map == null)
			return null;
		try {
			if(map.get(key) instanceof Object val)
				return val.toString();
		}catch (NullPointerException | ClassCastException e) {
			log.error("Exception occoured  StringUtils:getStringValue ",e);
		}
		return null;
	}
}
