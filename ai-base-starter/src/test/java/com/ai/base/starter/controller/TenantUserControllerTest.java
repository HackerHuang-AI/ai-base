package com.ai.base.starter.controller;

import com.ai.base.application.bo.BasePageOutBO;
import com.ai.base.application.bo.UserTenantBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.enums.ResultCodeEnum;
import com.ai.base.application.service.LoginService;
import com.ai.base.application.service.TenantUserService;
import com.ai.base.starter.vo.BasePageInVO;
import com.ai.base.starter.vo.TenantSwitchVO;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TenantUserControllerTest {
    private TenantUserService tenantUserService;
    private LoginService loginService;
    private TenantUserController controller;

    @BeforeEach
    void setUp() {
        tenantUserService = mock(TenantUserService.class);
        loginService = mock(LoginService.class);
        controller = new TenantUserController(tenantUserService, loginService);
    }

    @Test
    void pageDelegatesOnlyPaginationAndReturnsBusinessData() {
        BasePageInVO request = new BasePageInVO();
        request.setPageNo(3);
        request.setPageSize(20);
        BasePageOutBO<UserTenantBO> page = new BasePageOutBO<>();
        page.setPageNo(3);
        page.setPageSize(20);
        page.setTotal(41);
        page.setRecords(List.of(new UserTenantBO()));
        when(tenantUserService.pageCurrent(3, 20)).thenReturn(page);

        var result = controller.page(request);

        assertThat(result.getCode()).isEqualTo(ResultCodeEnum.SUCCESS);
        assertThat(result.getData()).isSameAs(page);
        assertThat(BasePageInVO.class.getDeclaredFields()).extracting("name").containsExactly("pageNo", "pageSize");
        verify(tenantUserService).pageCurrent(3, 20);
        verifyNoMoreInteractions(tenantUserService);
        verifyNoInteractions(loginService);
    }

    @Test
    void pagePassesExplicitNullParametersToServiceWithoutDefaulting() {
        BasePageInVO request = new BasePageInVO();
        request.setPageNo(null);
        request.setPageSize(null);
        BizException failure = new BizException(ErrorCodeEnum.PAGE_NUMBER_OUT_OF_RANGE);
        when(tenantUserService.pageCurrent(null, null)).thenThrow(failure);

        assertThatThrownBy(() -> controller.page(request)).isSameAs(failure);

        verify(tenantUserService).pageCurrent(null, null);
        verifyNoMoreInteractions(tenantUserService);
        verifyNoInteractions(loginService);
    }

    @Test
    void pagePreservesBusinessException() {
        BizException failure = new BizException(ErrorCodeEnum.PAGE_SIZE_OUT_OF_RANGE);
        when(tenantUserService.pageCurrent(1, 10)).thenThrow(failure);

        assertThatThrownBy(() -> controller.page(new BasePageInVO())).isSameAs(failure);
    }

    @Test
    void pageMapsUnexpectedExceptionToSystemError() {
        when(tenantUserService.pageCurrent(1, 10)).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> controller.page(new BasePageInVO()))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.SYSTEM_ERROR));
    }

    @Test
    void switchTenantDelegatesTenantIdToLoginServiceAndReturnsBusinessData() {
        TenantSwitchVO request = new TenantSwitchVO();
        request.setTenantId("tenant-2");
        UserTenantBO tenant = new UserTenantBO();
        tenant.setTenantId("tenant-2");
        tenant.setTenantName("企业空间");
        tenant.setTenantType(2);
        tenant.setTenantTypeDescription("企业租户");
        when(loginService.switchTenant("tenant-2")).thenReturn(tenant);

        var result = controller.switchTenant(request);

        assertThat(result.getCode()).isEqualTo(ResultCodeEnum.SUCCESS);
        assertThat(result.getData()).isSameAs(tenant);
        verify(loginService).switchTenant("tenant-2");
        verifyNoMoreInteractions(loginService);
        verifyNoInteractions(tenantUserService);
    }

    @Test
    void switchTenantPreservesBusinessException() {
        TenantSwitchVO request = new TenantSwitchVO();
        request.setTenantId("unavailable");
        BizException failure = new BizException(ErrorCodeEnum.TENANT_UNAVAILABLE);
        when(loginService.switchTenant("unavailable")).thenThrow(failure);

        assertThatThrownBy(() -> controller.switchTenant(request)).isSameAs(failure);
    }

    @Test
    void switchTenantMapsUnexpectedExceptionToSystemError() {
        TenantSwitchVO request = new TenantSwitchVO();
        request.setTenantId("tenant-2");
        when(loginService.switchTenant("tenant-2")).thenThrow(new IllegalStateException("session store unavailable"));

        assertThatThrownBy(() -> controller.switchTenant(request))
                .isInstanceOfSatisfying(BizException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.SYSTEM_ERROR));
    }

    @Test
    void switchTenantValidationRejectsNullEmptyAndWhitespaceIds() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (String tenantId : new String[]{null, "", " ", "\t\n"}) {
                TenantSwitchVO request = new TenantSwitchVO();
                request.setTenantId(tenantId);

                var violations = validator.validate(request);

                assertThat(violations).hasSize(1);
                assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("tenantId");
                assertThat(violations).extracting(v -> v.getMessage()).containsExactly("1001003");
            }
        }
        verifyNoInteractions(tenantUserService, loginService);
    }

    @Test
    void switchTenantValidationAccepts128CharactersAndRejects129() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            TenantSwitchVO request = new TenantSwitchVO();
            request.setTenantId("a".repeat(128));
            assertThat(validator.validate(request)).isEmpty();

            request.setTenantId("a".repeat(129));
            var violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations).extracting(v -> v.getPropertyPath().toString()).containsExactly("tenantId");
            assertThat(violations).extracting(v -> v.getMessage()).containsExactly("1001004");
        }
        verifyNoInteractions(tenantUserService, loginService);
    }
}

