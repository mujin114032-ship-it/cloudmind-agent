package com.lablink.cloudmind.common.util;

public final class UserContext {

    private static final Long DEFAULT_DEV_USER_ID = 1L;

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static Long getCurrentUserId() {
        Long userId = CURRENT_USER_ID.get();
        return userId == null ? DEFAULT_DEV_USER_ID : userId;
    }

    public static void setCurrentUserId(Long userId) {
        if (userId == null) {
            clear();
            return;
        }
        CURRENT_USER_ID.set(userId);
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}