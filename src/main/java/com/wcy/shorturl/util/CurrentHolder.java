package com.wcy.shorturl.util;

public class CurrentHolder {

    private static final ThreadLocal<Long> CURRENT_LOCAL = new ThreadLocal<>();

    public static void setCurrentId(Long employeeId) {
        CURRENT_LOCAL.set(employeeId);
    }

    public static Long getCurrentId() {
        return CURRENT_LOCAL.get();
    }

    public static void remove() {
        CURRENT_LOCAL.remove();
    }
}
