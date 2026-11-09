package com.ai.base.infrastructure.store;

import com.ai.base.application.model.SessionRecord;
import com.ai.base.application.port.SessionStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 会话存储端口实现：承载会话相关的 Redis key 结构、Lua 脚本与 JSON 序列化。
 *
 * <p>key 约定（与登录服务保持一致）：
 * ai-base:auth:session:{tokenHash}、ai-base:auth:session-id:{sessionId}、
 * ai-base:auth:user-sessions:{userId}（hash：deviceId -&gt; sessionId）。</p>
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Component
public class RedisSessionStore implements SessionStore {

    private static final String SESSION_KEY_PREFIX = "ai-base:auth:session:{";
    private static final String SESSION_ID_KEY_PREFIX = "ai-base:auth:session-id:{";
    private static final String USER_SESSIONS_KEY_PREFIX = "ai-base:auth:user-sessions:{";
    private static final String DEVICE_LIMIT_EXCEEDED = "DEVICE_LIMIT_EXCEEDED";
    private static final RedisScript<String> REPLACE_DEVICE_SESSION_SCRIPT = new DefaultRedisScript<>("local oldSessionId = redis.call('HGET', KEYS[1], ARGV[1])\nif not oldSessionId and redis.call('HLEN', KEYS[1]) >= tonumber(ARGV[2]) then return 'DEVICE_LIMIT_EXCEEDED' end\nredis.call('HSET', KEYS[1], ARGV[1], ARGV[3])\nredis.call('PEXPIRE', KEYS[1], ARGV[4])\nreturn oldSessionId or ''", String.class);
    private static final RedisScript<Long> ROLLBACK_DEVICE_SESSION_SCRIPT = new DefaultRedisScript<>("if redis.call('HGET', KEYS[1], ARGV[1]) ~= ARGV[2] then return 0 end\nif ARGV[3] == '' then return redis.call('HDEL', KEYS[1], ARGV[1]) end\nreturn redis.call('HSET', KEYS[1], ARGV[1], ARGV[3])", Long.class);
    private static final RedisScript<Long> SWITCH_TENANT_SCRIPT = new DefaultRedisScript<>("""
            local value = redis.call('GET', KEYS[1])
            local ttl = redis.call('PTTL', KEYS[1])
            if not value or ttl <= 0 then return 0 end
            local session = cjson.decode(value)
            if session.userId ~= ARGV[1] or session.sessionId ~= ARGV[2] then return 0 end
            if session.tenantId ~= ARGV[3] then return -1 end
            session.tenantId = ARGV[4]
            redis.call('SET', KEYS[1], cjson.encode(session), 'PX', ttl)
            return 1
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisSessionStore(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public DeviceSlot replaceDeviceSlot(String userId, String deviceId, String sessionId, Duration ttl, int maxDevices) {
        String result = stringRedisTemplate.execute(REPLACE_DEVICE_SESSION_SCRIPT, List.of(userSessionsKey(userId)),
                deviceId, String.valueOf(maxDevices), sessionId, String.valueOf(ttl.toMillis()));
        if (DEVICE_LIMIT_EXCEEDED.equals(result)) {
            return new DeviceSlot(null, true);
        }
        return new DeviceSlot(result == null ? "" : result, false);
    }

    @Override
    public void rollbackDeviceSlot(String userId, String deviceId, String sessionId, String oldSessionId) {
        stringRedisTemplate.execute(ROLLBACK_DEVICE_SESSION_SCRIPT, List.of(userSessionsKey(userId)),
                deviceId, sessionId, oldSessionId);
    }

    @Override
    public void saveSession(SessionRecord session, String tokenHash) {
        String value = serialize(session);
        stringRedisTemplate.opsForValue().set(sessionKey(tokenHash), value, Duration.ofMillis(session.ttlMillis()));
        stringRedisTemplate.opsForValue().set(sessionIdKey(session.sessionId()), tokenHash,
                Duration.ofMillis(session.ttlMillis()));
    }

    @Override
    public Optional<SessionRecord> findSession(String tokenHash) {
        String value = stringRedisTemplate.opsForValue().get(sessionKey(tokenHash));
        return Optional.ofNullable(deserialize(value));
    }

    @Override
    public Optional<String> findTokenHash(String sessionId) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(sessionIdKey(sessionId)));
    }

    @Override
    public void deleteSession(String tokenHash, String sessionId) {
        stringRedisTemplate.delete(sessionKey(tokenHash));
        stringRedisTemplate.delete(sessionIdKey(sessionId));
    }

    @Override
    public List<String> listSessionIds(String userId) {
        List<Object> values = stringRedisTemplate.opsForHash().values(userSessionsKey(userId));
        return values == null ? List.of() : values.stream().map(Object::toString).toList();
    }

    @Override
    public void removeDevice(String userId, String deviceId) {
        stringRedisTemplate.opsForHash().delete(userSessionsKey(userId), deviceId);
    }

    @Override
    public TenantSwitchResult switchTenant(String tokenHash, String userId, String sessionId,
                                           String currentTenantId, String newTenantId) {
        Long result = stringRedisTemplate.execute(SWITCH_TENANT_SCRIPT, List.of(sessionKey(tokenHash)),
                userId, sessionId, currentTenantId, newTenantId);
        if (Long.valueOf(-1).equals(result)) {
            return TenantSwitchResult.TENANT_MISMATCH;
        }
        return Long.valueOf(1).equals(result) ? TenantSwitchResult.OK : TenantSwitchResult.SESSION_INVALID;
    }

    private String sessionKey(String tokenHash) {
        return SESSION_KEY_PREFIX + tokenHash + "}";
    }

    private String sessionIdKey(String sessionId) {
        return SESSION_ID_KEY_PREFIX + sessionId + "}";
    }

    private String userSessionsKey(String userId) {
        return USER_SESSIONS_KEY_PREFIX + userId + "}";
    }

    private String serialize(SessionRecord session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize session", exception);
        }
    }

    private SessionRecord deserialize(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, SessionRecord.class);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
