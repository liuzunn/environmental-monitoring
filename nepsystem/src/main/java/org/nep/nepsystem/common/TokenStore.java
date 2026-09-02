package org.nep.nepsystem.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录令牌存储（BUG-003 修复）：
 * 登录时签发并登记（24 小时有效），拦截器校验 Authorization: Bearer token 是否有效；
 * 登出移除。静态实现便于测试注入。
 */
public final class TokenStore {

    /** 令牌有效期（毫秒）：24 小时 */
    public static final long TTL_MS = 24L * 3600 * 1000;

    /** 令牌类型：ADMIN=admins 表；USER=users 表 */
    public enum Kind { ADMIN, USER }

    public static final class Entry {
        public final Kind kind;
        public final Integer id;
        public final long expireAt;
        public Entry(Kind kind, Integer id, long expireAt) {
            this.kind = kind;
            this.id = id;
            this.expireAt = expireAt;
        }
    }

    private static final Map<String, Entry> TOKENS = new ConcurrentHashMap<>();

    private TokenStore() {
    }

    /** 签发令牌并登记；返回令牌串 */
    public static String issue(Kind kind, Integer id) {
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        TOKENS.put(token, new Entry(kind, id, System.currentTimeMillis() + TTL_MS));
        return token;
    }

    /** 校验令牌：有效返回 Entry，无效/过期返回 null（过期同时清理） */
    public static Entry validate(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        Entry e = TOKENS.get(token);
        if (e == null) {
            return null;
        }
        if (System.currentTimeMillis() > e.expireAt) {
            TOKENS.remove(token);
            return null;
        }
        return e;
    }

    /** 登出移除 */
    public static void remove(String token) {
        if (token != null) {
            TOKENS.remove(token);
        }
    }

    /** 当前有效令牌数（测试/监控用） */
    public static int size() {
        return TOKENS.size();
    }

    /** 清理（测试用） */
    public static void clear() {
        TOKENS.clear();
    }
}
