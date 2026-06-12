package com.steam.achievement;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class AchievementEvent {
    public static final String ORDER_PAID = "ORDER_PAID";
    public static final String REVIEW_CREATED = "REVIEW_CREATED";
    public static final String PLAYTIME_UPDATED = "PLAYTIME_UPDATED";
    public static final String LIBRARY_UPDATED = "LIBRARY_UPDATED";

    private String eventType;
    private Long userId;
    private Map<String, Object> data = new HashMap<>();

    public AchievementEvent(String eventType, Long userId) {
        this.eventType = eventType;
        this.userId = userId;
    }

    public AchievementEvent put(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }
}
