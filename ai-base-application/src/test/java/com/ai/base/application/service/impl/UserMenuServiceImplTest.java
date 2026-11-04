package com.ai.base.application.service.impl;

import com.ai.base.application.bo.UserMenuBO;
import com.ai.base.application.bo.UserTenantBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.model.request.RequestContext;
import com.ai.base.application.model.request.RequestContextHolder;
import com.ai.base.application.service.TenantUserService;
import com.ai.base.infrastructure.persistence.dto.UserMenuRow;
import com.ai.base.infrastructure.persistence.mapper.extension.UserMenuExtensionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserMenuServiceImplTest {
    private static final String USER_ID = "user-1";
    private static final String TENANT_ID = "tenant-1";
    private TenantUserService tenantUserService;
    private UserMenuExtensionMapper mapper;
    private UserMenuServiceImpl service;

    @BeforeEach
    void setUp() {
        tenantUserService = mock(TenantUserService.class);
        mapper = mock(UserMenuExtensionMapper.class);
        service = new UserMenuServiceImpl(tenantUserService, mapper);
        setContext(TENANT_ID);
        available(TENANT_ID, 1);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void personalSpaceUsesSharedMenusAndHidesTechnicalRootAndEmptyDirectories() {
        when(mapper.selectPersonalMenus()).thenReturn(List.of(
                row(1, null, "ROOT", 1, true, 0), row(2, 1L, "HOME", 2, true, 1),
                row(3, 1L, "EMPTY", 1, true, 2), row(4, 1L, "AI", 1, true, 3),
                row(5, 4L, "MODELS", 2, true, 1)));

        List<UserMenuBO> tree = service.treeCurrent();

        assertThat(tree).extracting(UserMenuBO::getMenuCode).containsExactly("HOME", "AI");
        assertThat(tree.get(0).getChildren()).isEmpty();
        assertThat(tree.get(0).getDesc()).isEqualTo("HOME描述");
        assertThat(tree.get(1).getDesc()).isEqualTo("AI描述");
        assertThat(tree.get(1).getChildren()).extracting(UserMenuBO::getMenuCode).containsExactly("MODELS");
        assertThat(tree.get(1).getChildren().get(0).getRoutePath()).isEqualTo("/MODELS");
        assertThat(tree.get(1).getChildren().get(0).getDesc()).isEqualTo("MODELS描述");
        var order = inOrder(tenantUserService, mapper);
        order.verify(tenantUserService).requireAvailable(USER_ID, TENANT_ID);
        order.verify(mapper).selectPersonalMenus();
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void enterprisePageGrantAddsAncestorDirectoriesWithoutExposingSiblingPages() {
        available(TENANT_ID, 2);
        when(mapper.selectTenantMenus(USER_ID, TENANT_ID)).thenReturn(List.of(
                row(1, null, "ROOT", 1, false, 0), row(2, 1L, "AI", 1, false, 1),
                row(3, 2L, "MARKET", 1, false, 1), row(4, 3L, "MODELS", 2, true, 1),
                row(5, 3L, "COMPARE", 2, false, 2), row(6, 1L, "EMPTY_GRANTED", 1, true, 2)));

        List<UserMenuBO> tree = service.treeCurrent();

        assertThat(tree).extracting(UserMenuBO::getMenuCode).containsExactly("AI");
        assertThat(tree.get(0).getDesc()).isEqualTo("AI描述");
        assertThat(tree.get(0).getChildren().get(0).getChildren().get(0).getDesc()).isEqualTo("MODELS描述");
        assertThat(tree.get(0).getChildren()).extracting(UserMenuBO::getMenuCode).containsExactly("MARKET");
        assertThat(tree.get(0).getChildren().get(0).getChildren())
                .extracting(UserMenuBO::getMenuCode).containsExactly("MODELS");
        verify(mapper).selectTenantMenus(USER_ID, TENANT_ID);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void enterpriseWithNoVisiblePageReturnsEmptyTree() {
        available(TENANT_ID, 2);
        when(mapper.selectTenantMenus(USER_ID, TENANT_ID)).thenReturn(List.of(
                row(1, null, "ROOT", 1, true, 0), row(2, 1L, "AI", 1, true, 1),
                row(3, 2L, "MODELS", 2, false, 1)));

        assertThat(service.treeCurrent()).isEmpty();
    }

    @Test
    void childrenOfDisabledOrDeletedAncestorsAreNotPromotedToTopLevel() {
        when(mapper.selectPersonalMenus()).thenReturn(List.of(
                row(1, null, "ROOT", 1, true, 0), row(3, 2L, "ORPHAN_PAGE", 2, true, 1),
                row(4, 1L, "HOME", 2, true, 2)));

        assertThat(service.treeCurrent()).extracting(UserMenuBO::getMenuCode).containsExactly("HOME");
    }

    @Test
    void authorizedPageUnderUnauthorizedParentPageIsNotExposed() {
        available(TENANT_ID, 2);
        when(mapper.selectTenantMenus(USER_ID, TENANT_ID)).thenReturn(List.of(
                row(1, null, "ROOT", 1, false, 0), row(2, 1L, "HOME", 2, false, 1),
                row(3, 2L, "PROFILE", 2, true, 1)));

        assertThat(service.treeCurrent()).isEmpty();
    }

    @Test
    void pageNodesCanContainVisibleChildPages() {
        UserMenuRow profile = row(3, 2L, "PROFILE", 2, true, 1);
        profile.setDesc(null);
        when(mapper.selectPersonalMenus()).thenReturn(List.of(
                row(1, null, "ROOT", 1, true, 0), row(2, 1L, "HOME", 2, true, 1), profile));

        assertThat(service.treeCurrent().get(0).getChildren())
                .extracting(UserMenuBO::getMenuCode).containsExactly("PROFILE");
        assertThat(service.treeCurrent().get(0).getChildren().get(0).getDesc()).isNull();
    }

    @Test
    void orderingIsStableWithinEveryLevelEvenWhenInputRowsAreUnordered() {
        when(mapper.selectPersonalMenus()).thenReturn(List.of(
                row(8, 5L, "THIRD", 2, true, 2), row(4, 1L, "SECOND_ROOT", 2, true, 2),
                row(7, 5L, "SECOND", 2, true, 1), row(6, 5L, "FIRST", 2, true, 1),
                row(1, null, "ROOT", 1, true, 0), row(5, 1L, "FIRST_ROOT", 1, true, 1)));

        List<UserMenuBO> tree = service.treeCurrent();

        assertThat(tree).extracting(UserMenuBO::getMenuCode).containsExactly("FIRST_ROOT", "SECOND_ROOT");
        assertThat(tree.get(0).getChildren()).extracting(UserMenuBO::getMenuCode)
                .containsExactly("FIRST", "SECOND", "THIRD");
    }

    @Test
    void switchedContextUsesNewTenantWithoutReusingPreviousMenuTree() {
        when(mapper.selectPersonalMenus()).thenReturn(List.of(row(1, null, "PERSONAL_HOME", 2, true, 0)));
        available("tenant-2", 2);
        when(mapper.selectTenantMenus(USER_ID, "tenant-2"))
                .thenReturn(List.of(row(2, null, "ENTERPRISE_HOME", 2, true, 0)));

        assertThat(service.treeCurrent()).extracting(UserMenuBO::getMenuCode).containsExactly("PERSONAL_HOME");
        setContext("tenant-2");
        assertThat(service.treeCurrent()).extracting(UserMenuBO::getMenuCode).containsExactly("ENTERPRISE_HOME");
        verify(tenantUserService).requireAvailable(USER_ID, "tenant-2");
        verify(mapper).selectTenantMenus(USER_ID, "tenant-2");
    }

    @Test
    void unavailableTenantIsRejectedBeforeMenuQuery() {
        BizException failure = new BizException(ErrorCodeEnum.TENANT_UNAVAILABLE);
        when(tenantUserService.requireAvailable(USER_ID, TENANT_ID)).thenThrow(failure);

        assertThatThrownBy(service::treeCurrent).isSameAs(failure);
        verifyNoInteractions(mapper);
    }

    @Test
    void missingUserOrTenantContextCannotQueryMenus() {
        RequestContextHolder.clear();
        assertThatThrownBy(service::treeCurrent).isInstanceOf(IllegalStateException.class);
        RequestContextHolder.set(new RequestContext("trace-1", "traceparent", USER_ID, null, "session-1", "ai-base"));
        assertThatThrownBy(service::treeCurrent).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(tenantUserService, mapper);
    }

    @Test
    void unknownTenantTypeDoesNotFallBackToEitherMenuSource() {
        available(TENANT_ID, 3);

        assertThatThrownBy(service::treeCurrent).isInstanceOfSatisfying(BizException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.TENANT_UNAVAILABLE));
        verifyNoInteractions(mapper);
    }

    private void setContext(String tenantId) {
        RequestContextHolder.set(new RequestContext("trace-1", "traceparent", USER_ID, tenantId, "session-1", "ai-base"));
    }

    private void available(String tenantId, int type) {
        UserTenantBO tenant = new UserTenantBO();
        tenant.setTenantId(tenantId);
        tenant.setTenantType(type);
        when(tenantUserService.requireAvailable(USER_ID, tenantId)).thenReturn(tenant);
    }

    private UserMenuRow row(long id, Long parentId, String code, int type, boolean authorized, int sortNo) {
        UserMenuRow row = new UserMenuRow();
        row.setId(id);
        row.setParentId(parentId);
        row.setMenuCode(code);
        row.setResourceName(code + "菜单");
        row.setDesc(code + "描述");
        row.setResourceType(type);
        row.setRoutePath(type == 2 ? "/" + code : null);
        row.setIcon("icon-" + code);
        row.setSortNo(sortNo);
        row.setAuthorized(authorized);
        return row;
    }
}

