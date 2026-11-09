package com.ai.base.infrastructure.repository;

import com.ai.base.application.model.LoginAuditEntry;
import com.ai.base.application.port.LoginAuditPort;
import com.ai.base.infrastructure.persistence.entity.LoginAuditEntity;
import com.ai.base.infrastructure.persistence.mapper.LoginAuditMapper;
import org.springframework.stereotype.Component;

/**
 * 登录审计落库端口实现。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
@Component
public class LoginAuditRepository implements LoginAuditPort {

    private final LoginAuditMapper loginAuditMapper;

    public LoginAuditRepository(LoginAuditMapper loginAuditMapper) {
        this.loginAuditMapper = loginAuditMapper;
    }

    @Override
    public void record(LoginAuditEntry entry) {
        LoginAuditEntity audit = new LoginAuditEntity();
        audit.setUserId(entry.userId());
        audit.setTenantId(entry.tenantId());
        audit.setIdentityType(entry.identityType());
        audit.setIdentityValueHash(entry.identityValueHash());
        audit.setLoginResult(entry.loginResult());
        audit.setLoginIp(entry.loginIp());
        audit.setUserAgent(entry.userAgent());
        audit.setLoginAt(entry.loginAt());
        loginAuditMapper.insert(audit);
    }
}
