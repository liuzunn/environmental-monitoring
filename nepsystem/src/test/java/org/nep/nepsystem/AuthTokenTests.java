package org.nep.nepsystem;

import org.junit.jupiter.api.Test;
import org.nep.nepsystem.common.TokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BUG-003 回归：Token 签发/校验/登出移除/无效令牌拒绝
 */
@SpringBootTest(properties = {"simulator.enabled=false", "quality.scan.enabled=false", "alert.auto-resolve-hold-ms=0"})
@Transactional
@AutoConfigureMockMvc
class AuthTokenTests {

    @Autowired private MockMvc mockMvc;

    @Test
    void loginIssuesValidToken() throws Exception {
        String resp = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adminCode\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp).path("data").path("token").asText();
        assertTrue(token.length() >= 20, "应签发 token");
        // 带有效 token 访问受控接口 → 放行
        mockMvc.perform(get("/api/stats/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void invalidTokenRejected() throws Exception {
        // 乱 token → 401
        mockMvc.perform(get("/api/stats/overview").header("Authorization", "Bearer deadbeef-invalid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
        // 无 token → 兼容放行（Service 层按身份头/归属继续校验）
        mockMvc.perform(get("/api/stats/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void logoutInvalidatesToken() throws Exception {
        String resp = mockMvc.perform(post("/api/auth/login-public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"zhang_san\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp).path("data").path("token").asText();
        // 登出（带 token）
        mockMvc.perform(post("/api/auth/logout-public").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        // 登出后 token 失效 → 401
        mockMvc.perform(get("/api/stats/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /** BUG-004 回归：兜底异常不泄露内部信息 */
    @Test
    void sanitizedFallback() throws Exception {
        mockMvc.perform(post("/api/data/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceCode\":\"DEV-AIR-001\",\"items\":\"not-an-array\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统繁忙，请稍后重试"));
    }

    @Test
    void tokenStoreUnit() {
        String t = TokenStore.issue(TokenStore.Kind.ADMIN, 1);
        assertNotNull(TokenStore.validate(t));
        assertNull(TokenStore.validate("nope"));
        TokenStore.remove(t);
        assertNull(TokenStore.validate(t));
        TokenStore.clear();
    }
}