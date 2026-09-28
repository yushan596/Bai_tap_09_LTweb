package vn.iotstar;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Kiểm tra toàn bộ chuỗi phiên bản chạy cùng nhau: Boot 4.1 + Security 7 + Hibernate 7
 * + Thymeleaf (+ security dialect + layout dialect) + MapStruct + Lombok, trên H2.
 */
@SpringBootTest
@ActiveProfiles("h2")
class LoginFlowTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginPageRenders() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Username hoặc Email")));
    }

    @Test
    void loginWithUsername() throws Exception {
        mvc.perform(formLogin("/login").user("user01").password("123456"))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("user01"));
    }

    @Test
    void loginWithEmail() throws Exception {
        mvc.perform(formLogin("/login").user("user01@gmail.com").password("123456"))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("user01"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(formLogin("/login").user("user01").password("sai-mat-khau"))
                .andExpect(redirectedUrl("/login?error=true"))
                .andExpect(unauthenticated());
    }

    @Test
    void headerShowsFullNameAndAvatarAfterLogin() throws Exception {
        MvcResult login = mvc.perform(formLogin("/login").user("user01").password("123456"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        String html = mvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertTrue(html.contains("Nguyễn Hữu Trung"), "fullName"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(html.contains("/images/user.png"), "avatar"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(html.contains("ROLE_USER"), "role"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(html.contains("Copyright"), "layout footer"));
    }

    @Test
    void roleBasedAccess() throws Exception {
        MockHttpSession user = (MockHttpSession) mvc
                .perform(formLogin("/login").user("user01").password("123456"))
                .andReturn().getRequest().getSession(false);
        MockHttpSession admin = (MockHttpSession) mvc
                .perform(formLogin("/login").user("admin01").password("123456"))
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/admin/dashboard").session(user)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/dashboard").session(admin)).andExpect(status().isOk());
    }

    @Test
    void logoutClearsSession() throws Exception {
        MockHttpSession session = (MockHttpSession) mvc
                .perform(formLogin("/login").user("user01").password("123456"))
                .andReturn().getRequest().getSession(false);

        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?logout=true"));
    }
}
