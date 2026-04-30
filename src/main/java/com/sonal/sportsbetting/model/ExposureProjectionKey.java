package com.sonal.sportsbetting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ExposureProjectionKey implements Serializable {

    public static final String SCOPE_GLOBAL = "GLOBAL";
    public static final String SCOPE_USER = "USER";

    @Column(name = "scope", nullable = false, length = 16)
    private String scope;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    public ExposureProjectionKey() {
    }

    public ExposureProjectionKey(String scope, String userId) {
        this.scope = scope;
        this.userId = userId;
    }

    public static ExposureProjectionKey global() {
        return new ExposureProjectionKey(SCOPE_GLOBAL, "");
    }

    public static ExposureProjectionKey forUser(String userId) {
        return new ExposureProjectionKey(SCOPE_USER, userId);
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExposureProjectionKey that = (ExposureProjectionKey) o;
        return Objects.equals(scope, that.scope) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, userId);
    }
}
