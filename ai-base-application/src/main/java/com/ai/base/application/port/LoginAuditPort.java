package com.ai.base.application.port;

import com.ai.base.application.model.LoginAuditEntry;

/**
 * 登录审计落库端口。
 *
 * @author HUANGcong
 * @since 2026-11-07
 */
public interface LoginAuditPort {

    /** 写入一条登录审计记录。 */
    void record(LoginAuditEntry entry);
}
