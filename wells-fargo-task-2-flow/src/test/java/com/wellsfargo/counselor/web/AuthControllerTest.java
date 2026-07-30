package com.wellsfargo.counselor.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wellsfargo.counselor.dto.AuthResponse;
import com.wellsfargo.counselor.dto.LoginRequest;
import com.wellsfargo.counselor.dto.RegisterRequest;
import com.wellsfargo.counselor.entity.Role;
import com.wellsfargo.counselor.security.AuthService;
import com.wellsfargo.counselor.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-layer slice test for {@link AuthController} (security filters disabled; service mocked). */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private AuthService authService;
    @MockBean
    private JwtService jwtService;

    @Test
    void loginReturnsAccessAndRefreshTokens() throws Exception {
        given(authService.login(any())).willReturn(AuthResponse.of("access-1", "refresh-1", "admin", Role.ADMIN, 900));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin12345"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-1"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-1"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void registerReturnsCreated() throws Exception {
        given(authService.register(any())).willReturn(AuthResponse.of("access-9", "refresh-9", "grace", Role.VIEWER, 900));

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("grace", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("VIEWER"));
    }

    @Test
    void blankCredentialsAreRejected() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("", ""))))
                .andExpect(status().isBadRequest());
    }
}
