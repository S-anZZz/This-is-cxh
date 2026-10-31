package com.cxh.blog.user.domain;

/**
 * 账号角色。只区分普通用户与管理员，够用且不引入多余的权限模型复杂度。
 */
public enum UserRole {

    /** 普通用户：可以发文、点赞。 */
    USER("普通用户"),

    /** 管理员：可进入后台、管理全部文章与站点设置。 */
    ADMIN("管理员");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
