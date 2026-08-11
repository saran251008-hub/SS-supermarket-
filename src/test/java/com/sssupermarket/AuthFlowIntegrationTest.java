package com.sssupermarket;

import com.sssupermarket.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductService productService;

    private MockHttpSession registerBuyer(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/register")
                        .param("name", "Integration Buyer")
                        .param("email", email)
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "BUYER")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private MockHttpSession loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .param("username", username)
                        .param("password", password)
                        .with(csrf()))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void publicPages_areAccessible() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SS")));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Laptop")));

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedPage_redirectsAnonymousUserToLogin() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        mockMvc.perform(get("/checkout"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void buyerRegistration_logsInAndRedirectsToProducts() throws Exception {
        MockHttpSession session = registerBuyer("integration-buyer@example.com");

        mockMvc.perform(get("/cart").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void sellerRegistration_redirectsToSellerDashboard() throws Exception {
        MvcResult result = mockMvc.perform(post("/register")
                        .param("name", "Integration Seller")
                        .param("email", "integration-seller@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "SELLER")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mockMvc.perform(get("/seller").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Seller Dashboard")));
    }

    @Test
    void registration_cannotBeAdmin() throws Exception {
        mockMvc.perform(post("/register")
                        .param("name", "Fake Admin")
                        .param("email", "fake-admin@example.com")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "ADMIN")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Registration as ADMIN is not allowed")));
    }

    @Test
    void adminLogin_redirectsToAdminDashboard() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .param("username", "admin@sssupermarket.com")
                        .param("password", "Admin@123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin Dashboard")));
    }

    @Test
    void buyerCannotAccessAdminOrSellerPages() throws Exception {
        MockHttpSession session = registerBuyer("restricted-buyer@example.com");

        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/seller").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_withBadCredentials_showsError() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "admin@sssupermarket.com")
                        .param("password", "wrong-password")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void logout_invalidatesSession() throws Exception {
        MockHttpSession session = loginAs("admin@sssupermarket.com", "Admin@123");

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void apiProducts_arePublicJson() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        mockMvc.perform(get("/api/products").param("search", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    void apiCart_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiCart_worksForAuthenticatedBuyer() throws Exception {
        MockHttpSession session = registerBuyer("api-buyer@example.com");

        Long laptopId = productService.listProducts("laptop", null).get(0).getId();

        mockMvc.perform(post("/api/cart").session(session)
                        .contentType("application/json")
                        .content("{\"productId\":" + laptopId + ",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void apiOrders_fullCheckoutFlow() throws Exception {
        MockHttpSession session = registerBuyer("api-order-buyer@example.com");

        Long laptopId = productService.listProducts("laptop", null).get(0).getId();
        Long keyboardId = productService.listProducts("keyboard", null).get(0).getId();

        mockMvc.perform(post("/api/cart").session(session)
                        .contentType("application/json")
                        .content("{\"productId\":" + laptopId + ",\"quantity\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/cart").session(session)
                        .contentType("application/json")
                        .content("{\"productId\":" + keyboardId + ",\"quantity\":2}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders").session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(get("/api/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void roleBasedAccess_throughRequestPostProcessor() throws Exception {
        mockMvc.perform(get("/admin").with(user("buyer@example.com")
                        .password("x").roles("BUYER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin").with(user("admin@example.com")
                        .password("x").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/orders").with(user("admin@example.com")
                        .password("x").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
