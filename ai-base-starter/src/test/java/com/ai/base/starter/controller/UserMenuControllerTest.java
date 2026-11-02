package com.ai.base.starter.controller;

import com.ai.base.application.bo.UserMenuBO;
import com.ai.base.application.common.BizException;
import com.ai.base.application.enums.ErrorCodeEnum;
import com.ai.base.application.service.UserMenuService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserMenuControllerTest {
    private UserMenuService service;
    private UserMenuController controller;

    @BeforeEach
    void setUp() {
        service = mock(UserMenuService.class);
        controller = new UserMenuController(service);
    }

    @Test
    void postWithoutRequestBodyReturnsNavigationFieldsAndEmptyChildren() throws Exception {
        UserMenuBO node = new UserMenuBO();
        node.setMenuCode("HOME");
        node.setResourceName("首页");
        node.setResourceType(2);
        node.setRoutePath("/home");
        node.setIcon("home");
        node.setSortNo(1);
        node.setChildren(List.of());
        when(service.treeCurrent()).thenReturn(List.of(node));

        var response = MockMvcBuilders.standaloneSetup(controller).build()
                .perform(post("/api/user/menu/tree")).andExpect(status().isOk()).andReturn();
        var json = new ObjectMapper().readTree(response.getResponse().getContentAsByteArray());

        assertThat(json.get("data").size()).isEqualTo(1);
        var menu = json.get("data").get(0);
        assertThat(menu.get("menuCode").asText()).isEqualTo("HOME");
        assertThat(menu.get("resourceName").asText()).isEqualTo("首页");
        assertThat(menu.get("resourceType").asInt()).isEqualTo(2);
        assertThat(menu.get("routePath").asText()).isEqualTo("/home");
        assertThat(menu.get("children").isArray()).isTrue();
        assertThat(menu.get("children").size()).isZero();
        assertThat(menu.has("id")).isFalse();
        assertThat(menu.has("authorized")).isFalse();
        assertThat(menu.has("tenantId")).isFalse();
        verify(service).treeCurrent();
        verifyNoMoreInteractions(service);
    }

    @Test
    void noMenusReturnsEmptyListRatherThanNull() {
        when(service.treeCurrent()).thenReturn(List.of());

        assertThat(controller.tree().getData()).isEmpty();
    }

    @Test
    void businessFailureIsPreserved() {
        BizException failure = new BizException(ErrorCodeEnum.TENANT_UNAVAILABLE);
        when(service.treeCurrent()).thenThrow(failure);

        assertThatThrownBy(controller::tree).isSameAs(failure);
    }

    @Test
    void unexpectedFailureIsMappedToSystemError() {
        when(service.treeCurrent()).thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(controller::tree).isInstanceOfSatisfying(BizException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCodeEnum.SYSTEM_ERROR));
    }
}

