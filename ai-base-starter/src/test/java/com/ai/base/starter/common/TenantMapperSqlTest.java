package com.ai.base.starter.common;

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
                "mapper/extension/UserMenuExtensionMapper.xml"}) {
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
                        "1 AS authorized", "ORDER BY sort_no, id")
                .doesNotContain("tenant_id", "menu_template", "user_role");
        assertThat(query.getParameterMappings()).isEmpty();
    }

    @Test
    void enterpriseMenuQueryScopesEveryJoinAndRespectsGrantLifetimeAndPathBoundaries() {
        BoundSql query = sql("com.ai.base.infrastructure.persistence.mapper.extension.UserMenuExtensionMapper.selectTenantMenus",
                Map.of("userId", "user-1", "tenantId", "tenant-1"));
        String text = query.getSql().replaceAll("\\s+", " ").trim();

        assertThat(text).contains("EXISTS (", "r.tenant_id = ur.tenant_id AND r.id = ur.role_id",
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

    private BoundSql sql(String statement, Map<String, Object> parameters) {
        return configuration.getMappedStatement(statement).getBoundSql(parameters);
    }
}

