package com.ai.base.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @Description: 登录审计记录实体。
 *
 * @ProjectName: ai-base
 * @Package: com.ai.base.infrastructure.persistence.entity
 * @ClassName: LoginAuditEntity
 * @Author: HUANGcong
 * @Date: Created in 2026/9/24
 * @Version: 1.0
 */
@Getter
@Setter
@TableName("login_audit")
public class LoginAuditEntity {
    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 全局用户 ID。 */
    private String userId;
    /** 全局租户 ID。 */
    private String tenantId;
    /** 身份类型。 */
    private String identityType;
    /** 身份标识值的 SHA-256 摘要。 */
    private String identityValueHash;
    /** 登录结果：1-成功，2-失败。 */
    private Integer loginResult;
    /** 失败码。 */
    private String failureCode;
    /** 登录 IP。 */
    private String loginIp;
    /** 用户代理。 */
    private String userAgent;
    /** 登录时间。 */
    private LocalDateTime loginAt;
    /** 创建时间。 */
    private LocalDateTime ctime;
}

