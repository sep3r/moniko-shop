package com.monikoshop.audit;

/** نوع رویدادهای امنیتی/مدیریتی که در جدول security_event ثبت می‌شوند. */
public final class AuditEvent {

    private AuditEvent() { }

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILURE = "LOGIN_FAILURE";
    public static final String LOGIN_BLOCKED = "LOGIN_BLOCKED";
    public static final String REGISTER = "REGISTER";
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String PASSWORD_CHANGE_FAILED = "PASSWORD_CHANGE_FAILED";
    public static final String PASSWORD_RESET_REQUESTED = "PASSWORD_RESET_REQUESTED";
    public static final String PASSWORD_RESET_DONE = "PASSWORD_RESET_DONE";
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_BLOCKED = "ORDER_BLOCKED";
    public static final String ADMIN_PRODUCT_CREATE = "ADMIN_PRODUCT_CREATE";
    public static final String ADMIN_PRODUCT_UPDATE = "ADMIN_PRODUCT_UPDATE";
    public static final String ADMIN_PRODUCT_DELETE = "ADMIN_PRODUCT_DELETE";
    public static final String ADMIN_CATEGORY_CREATE = "ADMIN_CATEGORY_CREATE";
    public static final String ADMIN_CATEGORY_UPDATE = "ADMIN_CATEGORY_UPDATE";
    public static final String ADMIN_CATEGORY_DELETE = "ADMIN_CATEGORY_DELETE";
    public static final String ADMIN_ORDER_STATUS = "ADMIN_ORDER_STATUS";
    public static final String ADMIN_USER_STATUS = "ADMIN_USER_STATUS";
    public static final String ADMIN_USER_ROLES = "ADMIN_USER_ROLES";
}
