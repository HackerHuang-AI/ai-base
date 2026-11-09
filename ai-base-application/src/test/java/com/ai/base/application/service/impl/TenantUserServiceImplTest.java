package com.ai.base.application.service.impl;

import com.ai.base.application.bo.UserTenantBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.TenantTypeEnum;
import com.ai.base.application.model.request.RequestContext;
import com.ai.base.application.model.request.RequestContextHolder;
import com.ai.base.application.port.UserTenantRelationPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TenantUserServiceImplTest {
    private static final String USER_ID = "user-1";
    private UserTenantRelationPort relationPort;
    private TenantUserServiceImpl service;

    @BeforeEach
    void setUp() {
        relationPort = mock(UserTenantRelationPort.class);
        service = new TenantUserServiceImpl(relationPort);
        RequestContextHolder.set(new RequestContext("trace-1", "traceparent", USER_ID, "tenant-1", "session-1", "ai-base"));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void pageCurrentUsesContextUserAndPreservesPortOrderWithBusinessFields() {
        UserTenantBO enterprise = tenant("tenant-z", "企业空间", 2);
        UserTenantBO personal = tenant("tenant-a", "个人空间", 1);
        when(relationPort.countAvailable(USER_ID)).thenReturn(4_294_967_296L);
        when(relationPort.pageAvailable(USER_ID, 2L, 2)).thenReturn(List.of(enterprise, personal));

        var result = service.pageCurrent(2, 2);

        assertThat(result.getTotal()).isEqualTo(4_294_967_296L);
        assertThat(result.getPageNo()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.getRecords()).extracting(UserTenantBO::getTenantId).containsExactly("tenant-z", "tenant-a");
        assertThat(result.getRecords()).extracting(UserTenantBO::getTenantName).containsExactly("企业空间", "个人空间");
        assertThat(result.getRecords()).extracting(UserTenantBO::getTenantType).containsExactly(2, 1);
        assertThat(result.getRecords()).extracting(UserTenantBO::getTenantTypeDescription).containsExactly("企业租户", "个人空间");
        assertThat(UserTenantBO.class.getDeclaredFields()).extracting("name")
                .containsExactly("tenantId", "tenantName", "tenantType", "tenantTypeDescription");
        verify(relationPort).countAvailable(USER_ID);
        verify(relationPort).pageAvailable(USER_ID, 2L, 2);
        verifyNoMoreInteractions(relationPort);
    }

    @Test
    void pageCurrentWithoutContextDoesNotQueryPort() {
        RequestContextHolder.clear();

        assertThatThrownBy(() -> service.pageCurrent(1, 10)).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(relationPort);
    }

    @Test
    void pageCurrentUsesLongOffsetForMaximumPageNumber() {
        when(relationPort.pageAvailable(USER_ID, 214_748_364_600L, 100)).thenReturn(List.of());

        var result = service.pageCurrent(Integer.MAX_VALUE, 100);

        assertThat(result.getPageNo()).isEqualTo(Integer.MAX_VALUE);
        assertThat(result.getPageSize()).isEqualTo(100);
        assertThat(result.getTotal()).isZero();
        assertThat(result.getRecords()).isEmpty();
        verify(relationPort).pageAvailable(USER_ID, 214_748_364_600L, 100);
    }

    @Test
    void pageCurrentRejectsNullAndNonPositivePageNumbersBeforeQuerying() {
        for (Integer pageNo : new Integer[]{null, 0, -1, Integer.MIN_VALUE}) {
            assertThatThrownBy(() -> service.pageCurrent(pageNo, 10))
                    .isInstanceOfSatisfying(BizException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.PAGE_NUMBER_OUT_OF_RANGE));
        }
        verifyNoInteractions(relationPort);
    }

    @Test
    void pageCurrentRejectsNullAndOutOfRangePageSizesBeforeQuerying() {
        for (Integer pageSize : new Integer[]{null, 0, -1, 101, Integer.MAX_VALUE}) {
            assertThatThrownBy(() -> service.pageCurrent(1, pageSize))
                    .isInstanceOfSatisfying(BizException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.PAGE_SIZE_OUT_OF_RANGE));
        }
        verifyNoInteractions(relationPort);
    }

    @Test
    void pageCurrentAcceptsInclusivePageSizeBoundaries() {
        for (int pageSize : new int[]{1, 100}) {
            when(relationPort.pageAvailable(USER_ID, 0L, pageSize)).thenReturn(List.of());

            var result = service.pageCurrent(1, pageSize);

            assertThat(result.getPageNo()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(pageSize);
            assertThat(result.getRecords()).isEmpty();
            verify(relationPort).pageAvailable(USER_ID, 0L, pageSize);
        }
    }

    @Test
    void requireAvailableUsesExplicitUserAndReturnsBusinessObject() {
        when(relationPort.findAvailableRelation("user-2", "tenant-2"))
                .thenReturn(Optional.of(tenant("tenant-2", "个人空间", 1)));

        UserTenantBO result = service.requireAvailable("user-2", "tenant-2");

        assertThat(result.getTenantId()).isEqualTo("tenant-2");
        assertThat(result.getTenantName()).isEqualTo("个人空间");
        assertThat(result.getTenantType()).isEqualTo(1);
        assertThat(result.getTenantTypeDescription()).isEqualTo("个人空间");
        verify(relationPort).findAvailableRelation("user-2", "tenant-2");
        verifyNoMoreInteractions(relationPort);
    }

    @Test
    void requireAvailableRejectsNonCanonicalTenantIdsBeforeQuerying() {
        for (String tenantId : new String[]{"TEN_01A0EC2D-1234", "ten_01A0ec2d-1234", "ten_01a0ec2d-1234 ",
                "TEN_01a0ec2d-1234", "ten_空间", "ten.test"}) {
            assertThatThrownBy(() -> service.requireAvailable(USER_ID, tenantId))
                    .isInstanceOfSatisfying(BizException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.TENANT_UNAVAILABLE));
        }
        verifyNoInteractions(relationPort);
    }

    @Test
    void requireAvailableAcceptsLowercaseIdWithDigitsUnderscoresAndHyphens() {
        when(relationPort.findAvailableRelation(USER_ID, "ten_01a0ec2d-1234"))
                .thenReturn(Optional.of(tenant("ten_01a0ec2d-1234", "个人空间", 1)));

        assertThat(service.requireAvailable(USER_ID, "ten_01a0ec2d-1234").getTenantId())
                .isEqualTo("ten_01a0ec2d-1234");
    }

    @Test
    void requireAvailableRejectsMissingOrInaccessibleTenantWith1401001() {
        assertThatThrownBy(() -> service.requireAvailable(USER_ID, "unavailable"))
                .isInstanceOfSatisfying(BizException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.TENANT_UNAVAILABLE);
                    assertThat(e.getErrorCode().getCode()).isEqualTo("1401001");
                });
        verify(relationPort).findAvailableRelation(USER_ID, "unavailable");
        verifyNoMoreInteractions(relationPort);
    }

    @Test
    void tenantTypeDescriptionsMapPersonalAndEnterpriseSpaces() {
        assertThat(TenantTypeEnum.descriptionOf(1)).isEqualTo("个人空间");
        assertThat(TenantTypeEnum.descriptionOf(2)).isEqualTo("企业租户");
    }

    private UserTenantBO tenant(String tenantId, String tenantName, Integer tenantType) {
        UserTenantBO tenant = new UserTenantBO();
        tenant.setTenantId(tenantId);
        tenant.setTenantName(tenantName);
        tenant.setTenantType(tenantType);
        // 端口契约：BO 已含租户类型描述（由 infra 的 toTenant 用 TenantTypeEnum 填充）
        tenant.setTenantTypeDescription(TenantTypeEnum.descriptionOf(tenantType));
        return tenant;
    }
}
