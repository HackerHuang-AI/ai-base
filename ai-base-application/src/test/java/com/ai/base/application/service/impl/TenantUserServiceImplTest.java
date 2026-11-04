package com.ai.base.application.service.impl;

import com.ai.base.application.bo.UserTenantBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.model.request.RequestContext;
import com.ai.base.application.model.request.RequestContextHolder;
import com.ai.base.infrastructure.persistence.dto.UserTenantRow;
import com.ai.base.infrastructure.persistence.mapper.extension.TenantUserExtensionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TenantUserServiceImplTest {
    private static final String USER_ID = "user-1";
    private TenantUserExtensionMapper mapper;
    private TenantUserServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(TenantUserExtensionMapper.class);
        service = new TenantUserServiceImpl(mapper);
        RequestContextHolder.set(new RequestContext("trace-1", "traceparent", USER_ID, "tenant-1", "session-1", "ai-base"));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void pageCurrentUsesContextUserAndPreservesMapperOrderWithBusinessFields() {
        UserTenantRow enterprise = new UserTenantRow();
        enterprise.setUserId(USER_ID);
        enterprise.setTenantId("tenant-z");
        enterprise.setTenantName("企业空间");
        enterprise.setTenantType(2);
        UserTenantRow personal = new UserTenantRow();
        personal.setUserId(USER_ID);
        personal.setTenantId("tenant-a");
        personal.setTenantName("个人空间");
        personal.setTenantType(1);
        when(mapper.countAvailableByUserId(USER_ID)).thenReturn(4_294_967_296L);
        when(mapper.selectAvailablePageByUserId(USER_ID, 2L, 2)).thenReturn(List.of(enterprise, personal));

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
        verify(mapper).countAvailableByUserId(USER_ID);
        verify(mapper).selectAvailablePageByUserId(USER_ID, 2L, 2);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void pageCurrentWithoutContextDoesNotQueryMapper() {
        RequestContextHolder.clear();

        assertThatThrownBy(() -> service.pageCurrent(1, 10)).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(mapper);
    }

    @Test
    void pageCurrentUsesLongOffsetForMaximumPageNumber() {
        when(mapper.selectAvailablePageByUserId(USER_ID, 214_748_364_600L, 100)).thenReturn(List.of());

        var result = service.pageCurrent(Integer.MAX_VALUE, 100);

        assertThat(result.getPageNo()).isEqualTo(Integer.MAX_VALUE);
        assertThat(result.getPageSize()).isEqualTo(100);
        assertThat(result.getTotal()).isZero();
        assertThat(result.getRecords()).isEmpty();
        verify(mapper).selectAvailablePageByUserId(USER_ID, 214_748_364_600L, 100);
    }

    @Test
    void pageCurrentRejectsNullAndNonPositivePageNumbersBeforeQuerying() {
        for (Integer pageNo : new Integer[]{null, 0, -1, Integer.MIN_VALUE}) {
            assertThatThrownBy(() -> service.pageCurrent(pageNo, 10))
                    .isInstanceOfSatisfying(BizException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.PAGE_NUMBER_OUT_OF_RANGE));
        }
        verifyNoInteractions(mapper);
    }

    @Test
    void pageCurrentRejectsNullAndOutOfRangePageSizesBeforeQuerying() {
        for (Integer pageSize : new Integer[]{null, 0, -1, 101, Integer.MAX_VALUE}) {
            assertThatThrownBy(() -> service.pageCurrent(1, pageSize))
                    .isInstanceOfSatisfying(BizException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.PAGE_SIZE_OUT_OF_RANGE));
        }
        verifyNoInteractions(mapper);
    }

    @Test
    void pageCurrentAcceptsInclusivePageSizeBoundaries() {
        for (int pageSize : new int[]{1, 100}) {
            when(mapper.selectAvailablePageByUserId(USER_ID, 0L, pageSize)).thenReturn(List.of());

            var result = service.pageCurrent(1, pageSize);

            assertThat(result.getPageNo()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(pageSize);
            assertThat(result.getRecords()).isEmpty();
            verify(mapper).selectAvailablePageByUserId(USER_ID, 0L, pageSize);
        }
    }

    @Test
    void requireAvailableUsesExplicitUserAndReturnsBusinessObject() {
        UserTenantRow row = new UserTenantRow();
        row.setUserId("user-2");
        row.setTenantId("tenant-2");
        row.setTenantName("个人空间");
        row.setTenantType(1);
        when(mapper.selectAvailableByUserIdAndTenantId("user-2", "tenant-2")).thenReturn(row);

        UserTenantBO result = service.requireAvailable("user-2", "tenant-2");

        assertThat(result.getTenantId()).isEqualTo("tenant-2");
        assertThat(result.getTenantName()).isEqualTo("个人空间");
        assertThat(result.getTenantType()).isEqualTo(1);
        assertThat(result.getTenantTypeDescription()).isEqualTo("个人空间");
        verify(mapper).selectAvailableByUserIdAndTenantId("user-2", "tenant-2");
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void requireAvailableRejectsCaseInsensitiveDatabaseMatches() {
        UserTenantRow row = new UserTenantRow();
        row.setTenantId("ten_01a0ec2d-1234");
        for (String tenantId : new String[]{"TEN_01A0EC2D-1234", "ten_01A0ec2d-1234", "ten_01a0ec2d-1234 "}) {
            when(mapper.selectAvailableByUserIdAndTenantId(USER_ID, tenantId)).thenReturn(row);

            assertThatThrownBy(() -> service.requireAvailable(USER_ID, tenantId))
                    .isInstanceOfSatisfying(BizException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.TENANT_UNAVAILABLE));
        }
    }

    @Test
    void requireAvailableRejectsNonCanonicalStoredTenantIds() {
        for (String tenantId : new String[]{"TEN_01a0ec2d-1234", "ten_空间", "ten.test"}) {
            UserTenantRow row = new UserTenantRow();
            row.setTenantId(tenantId);
            when(mapper.selectAvailableByUserIdAndTenantId(USER_ID, tenantId)).thenReturn(row);

            assertThatThrownBy(() -> service.requireAvailable(USER_ID, tenantId))
                    .isInstanceOfSatisfying(BizException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.TENANT_UNAVAILABLE));
        }
    }

    @Test
    void requireAvailableAcceptsLowercaseIdWithDigitsUnderscoresAndHyphens() {
        UserTenantRow row = new UserTenantRow();
        row.setTenantId("ten_01a0ec2d-1234");
        when(mapper.selectAvailableByUserIdAndTenantId(USER_ID, row.getTenantId())).thenReturn(row);

        assertThat(service.requireAvailable(USER_ID, row.getTenantId()).getTenantId()).isEqualTo(row.getTenantId());
    }

    @Test
    void requireAvailableRejectsMissingOrInaccessibleTenantWith1401001() {
        assertThatThrownBy(() -> service.requireAvailable(USER_ID, "unavailable"))
                .isInstanceOfSatisfying(BizException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.TENANT_UNAVAILABLE);
                    assertThat(e.getErrorCode().getCode()).isEqualTo("1401001");
                });
        verify(mapper).selectAvailableByUserIdAndTenantId(USER_ID, "unavailable");
        verifyNoMoreInteractions(mapper);
    }
}

