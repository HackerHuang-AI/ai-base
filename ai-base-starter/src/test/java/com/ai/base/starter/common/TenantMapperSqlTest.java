package com.ai.base.starter.common;

import com.ai.base.infrastructure.persistence.entity.UserEntity;
import com.ai.base.infrastructure.persistence.entity.UserInfoEntity;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TenantMapperSqlTest {
    private Configuration configuration;

    @BeforeEach
    void setUp() throws Exception {
        configuration = new Configuration();
        for (String resource : new String[]{"mapper/extension/TenantUserExtensionMapper.xml", "mapper/UserTenantPreferenceMapper.xml",
                "mapper/extension/UserMenuExtensionMapper.xml", "mapper/extension/UserExtensionMapper.xml"}) {
            try (var stream = Resources.getResourceAsStream(resource)) {
                new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
    }

    @Test
    void availableSpaceQueriesUseIdenticalAdmissionConditionsAndStableOrdering() {
        String namespace = "com.ai.base.infrastructure.persistence.mapper.extension.TenantUserExtensionMapper.";
        Map<String, Object> parameters = Map.of("userId", "user-1", "tenantId", "tenant-1",
                "offset", 214_748_364_600L, "pageSize", 100);
        for (String method : new String[]{"countAvailableByUserId", "selectAvailablePageByUserId", "selectAvailableByUserIdAndTenantId"}) {
            String sql = sql(namespace + method, parameters).getSql().replaceAll("\\s+", " ").trim();
            assertThat(sql).contains("tu.user_id = ?", "tu.status = 1", "tu.valid = 1", "t.status = 1", "t.valid = 1");
        }
        BoundSql page = sql(namespace + "selectAvailablePageByUserId", parameters);
        assertThat(page.getSql().replaceAll("\\s+", " ")).contains("ORDER BY t.tenant_type, tu.id LIMIT ?, ?");
        assertThat(page.getParameterMappings()).extracting("property").containsExactly("userId", "offset", "pageSize");
        assertThat(sql(namespace + "selectAvailableByUserIdAndTenantId", parameters).getSql())
                .contains("AND tu.tenant_id = ?");
    }

    @Test
    void preferenceUpdatesChangeOnlyRequestedFieldAndSupportNullDefault() {
        String namespace = "com.ai.base.infrastructure.persistence.mapper.UserTenantPreferenceMapper.";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", "user-1");
        parameters.put("tenantId", null);
        parameters.put("usedAt", LocalDateTime.of(2026, 10, 27, 10, 0, 0, 123456000));
        BoundSql defaultUpdate = sql(namespace + "updateDefaultTenantId", parameters);
        String defaultSql = defaultUpdate.getSql().replaceAll("\\s+", " ");
        assertThat(defaultSql).contains("SET default_tenant_id = ?, version = version + 1", "WHERE user_id = ? AND valid = 1")
                .doesNotContain("last_used_tenant_id", "last_used_at");
        assertThat(defaultUpdate.getParameterMappings().get(0).getJdbcType().name()).isEqualTo("VARCHAR");
        BoundSql lastUsedUpdate = sql(namespace + "updateLastUsedTenantId", parameters);
        String lastUsedSql = lastUsedUpdate.getSql().replaceAll("\\s+", " ");
        assertThat(lastUsedSql).contains("SET last_used_tenant_id = ?, last_used_at = ?, version = version + 1",
                        "WHERE user_id = ? AND valid = 1", "AND (last_used_at IS NULL OR last_used_at < ?)")
                .doesNotContain("default_tenant_id", "update_time", "<=");
        assertThat(lastUsedUpdate.getParameterMappings()).extracting("property")
                .containsExactly("tenantId", "usedAt", "userId", "usedAt");
        assertThat(lastUsedUpdate.getParameterMappings().get(1).getJdbcType().name()).isEqualTo("TIMESTAMP");
    }

    @Test
    void personalMenuQueryUsesSharedEnabledNavigationWithoutTenantOrTemplateLookup() {
        BoundSql query = sql("com.ai.base.infrastructure.persistence.mapper.extension.UserMenuExtensionMapper.selectPersonalMenus", Map.of());
        String text = query.getSql().replaceAll("\\s+", " ").trim();

        assertThat(text).contains("FROM personal_tenant_menu", "status = 1 AND valid = 1 AND resource_type IN (1, 2)",
                        "resource_name AS resourceName, `desc`, resource_type AS resourceType",
                        "1 AS authorized", "ORDER BY sort_no, id")
                .doesNotContain("tenant_id", "menu_template", "user_role");
        assertThat(query.getParameterMappings()).isEmpty();
    }

    @Test
    void enterpriseMenuQueryScopesEveryJoinAndRespectsGrantLifetimeAndPathBoundaries() {
        BoundSql query = sql("com.ai.base.infrastructure.persistence.mapper.extension.UserMenuExtensionMapper.selectTenantMenus",
                Map.of("userId", "user-1", "tenantId", "tenant-1"));
        String text = query.getSql().replaceAll("\\s+", " ").trim();

        assertThat(text).contains("m.resource_name AS resourceName, m.`desc`, m.resource_type AS resourceType",
                        "EXISTS (", "r.tenant_id = ur.tenant_id AND r.id = ur.role_id",
                        "rm.tenant_id = r.tenant_id AND rm.role_id = r.id",
                        "granted.tenant_id = rm.tenant_id AND granted.id = rm.tenant_menu_id",
                        "ur.user_id = ? AND ur.tenant_id = ?", "m.tenant_id = ?",
                        "ur.valid = 1 AND ur.effective_start_time <= CURRENT_TIMESTAMP",
                        "ur.effective_end_time IS NULL OR ur.effective_end_time > CURRENT_TIMESTAMP",
                        "r.status = 1 AND r.valid = 1 AND rm.valid = 1",
                        "granted.status = 1 AND granted.valid = 1", "granted.id = m.id",
                        "rm.include_descendants = 1",
                        "LEFT(m.men_path, CHAR_LENGTH(granted.men_path) + 1) = CONCAT(granted.men_path, ';')",
                        "m.status = 1 AND m.valid = 1 AND m.resource_type IN (1, 2)", "ORDER BY m.sort_no, m.id")
                .doesNotContain(" LIKE ", "TENANT_ADMIN", "role_code", "menu_template");
        assertThat(query.getParameterMappings()).extracting("property").containsExactly("userId", "tenantId", "tenantId");
    }

    @Test
    void userQueriesMatchCreationTimeMappingWithAndWithoutBounds() {
        String namespace = "com.ai.base.infrastructure.persistence.mapper.extension.UserExtensionMapper.";
        for (boolean hasStart : new boolean[]{false, true}) {
            for (boolean hasEnd : new boolean[]{false, true}) {
                UserEntity user = new UserEntity();
                user.setCreateTime(hasStart ? LocalDateTime.of(2026, 10, 1, 0, 0) : null);
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("user", user);
                parameters.put("userInfo", new UserInfoEntity());
                parameters.put("createdEndTime", hasEnd ? LocalDateTime.of(2026, 11, 1, 0, 0) : null);
                parameters.put("offset", 0L);
                parameters.put("pageSize", 20L);
                for (String method : new String[]{"countByQuery", "selectPageByQuery"}) {
                    BoundSql query = sql(namespace + method, parameters);
                    String text = query.getSql().replaceAll("\\s+", " ").trim();
                    assertThat(text).doesNotContain("ctime", "utime");
                    assertThat(text.contains("AND u.create_time >= ?")).isEqualTo(hasStart);
                    assertThat(text.contains("AND u.create_time <= ?")).isEqualTo(hasEnd);
                    assertThat(query.getParameterMappings().stream().anyMatch(mapping -> mapping.getProperty().equals("user.createTime")))
                            .isEqualTo(hasStart);
                    assertThat(query.getParameterMappings().stream().anyMatch(mapping -> mapping.getProperty().equals("createdEndTime")))
                            .isEqualTo(hasEnd);
                    if (method.equals("selectPageByQuery")) {
                        assertThat(text).contains("ORDER BY u.create_time DESC, u.id DESC LIMIT ?, ?");
                    }
                }
            }
        }
    }

    private BoundSql sql(String statement, Map<String, Object> parameters) {
        return configuration.getMappedStatement(statement).getBoundSql(parameters);
    }
}

