package vn.iotstar;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import vn.iotstar.entity.User;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.repository.RoleRepository;
import vn.iotstar.repository.UserRepository;
import vn.iotstar.service.EmailService;

/** Đăng ký + OTP, quên mật khẩu + OTP, và phân quyền sửa/xóa product. EmailService được mock để lấy OTP. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:webst9flow;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@ActiveProfiles("h2")
class OtpAndProductFlowTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private EmailService emailService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String lastOtp(String email) {
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtp(eq(email), otp.capture(), anyString());
        return otp.getValue();
    }

    @Test
    void registerThenVerifyOtpThenLogin() throws Exception {
        String email = "newbie@example.com";

        mvc.perform(post("/register").with(csrf())
                        .param("username", "newbie").param("email", email)
                        .param("fullName", "Người Mới")
                        .param("password", "secret1").param("confirmPassword", "secret1"))
                .andExpect(status().is3xxRedirection());

        // chưa nhập OTP => chưa đăng nhập được
        mvc.perform(formLogin("/login").user("newbie").password("secret1"))
                .andExpect(unauthenticated());

        // OTP sai
        mvc.perform(post("/verify-otp").with(csrf()).param("email", email).param("otp", "000000"))
                .andExpect(status().isOk());

        mvc.perform(post("/verify-otp").with(csrf()).param("email", email).param("otp", lastOtp(email)))
                .andExpect(redirectedUrl("/login"));

        mvc.perform(formLogin("/login").user("newbie").password("secret1"))
                .andExpect(authenticated().withUsername("newbie"));
    }

    @Test
    void forgotPasswordThenResetWithOtp() throws Exception {
        String email = "resetme@example.com";
        createUser("resetme", email);

        mvc.perform(post("/forgot-password").with(csrf()).param("email", email))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/reset-password").with(csrf())
                        .param("email", email).param("otp", lastOtp(email))
                        .param("password", "newpass1").param("confirmPassword", "newpass1"))
                .andExpect(redirectedUrl("/login"));

        mvc.perform(formLogin("/login").user("resetme").password("newpass1"))
                .andExpect(authenticated().withUsername("resetme"));
        mvc.perform(formLogin("/login").user("resetme").password("123456"))
                .andExpect(unauthenticated());
    }

    @Test
    void onlyOwnerOrAdminCanEditProduct() throws Exception {
        createUser("user02", "user02@gmail.com");

        MockHttpSession owner = login("user01");
        MockHttpSession other = login("user02");
        MockHttpSession admin = login("admin01");

        mvc.perform(multipart("/products/create").session(owner).with(csrf())
                        .param("name", "San pham test").param("price", "199000"))
                .andExpect(redirectedUrl("/products"));

        Long id = productRepository.search("San pham test", PageRequest.of(0, 1)).getContent().get(0).getId();

        mvc.perform(get("/products/edit/" + id).session(owner)).andExpect(status().isOk());
        mvc.perform(get("/products/edit/" + id).session(admin)).andExpect(status().isOk());
        mvc.perform(get("/products/edit/" + id).session(other)).andExpect(status().isForbidden());
        mvc.perform(post("/products/delete/" + id).session(other).with(csrf())).andExpect(status().isForbidden());

        mvc.perform(post("/products/delete/" + id).session(owner).with(csrf()))
                .andExpect(redirectedUrl("/products"));
    }

    @Test
    void usersPageIsAdminOnly() throws Exception {
        mvc.perform(get("/users").session(login("user01"))).andExpect(status().isForbidden());
        mvc.perform(get("/users").session(login("admin01"))).andExpect(status().isOk());
    }

    private void createUser(String username, String email) {
        userRepository.save(User.builder()
                .username(username).email(email)
                .password(passwordEncoder.encode("123456")).fullName("Test " + username)
                .role(roleRepository.findByName("ROLE_USER").orElseThrow()).enabled(true).build());
    }

    private MockHttpSession login(String username) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin("/login").user(username).password("123456"))
                .andReturn().getRequest().getSession(false);
    }
}
