package com.tutr.backend.controller.favorite;

import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.course.FavoriteCourse;
import com.tutr.backend.service.FavoriteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 5 TESTS ============================
@WebMvcTest(FavoriteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class FavoriteControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private FavoriteService favoriteService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. POST /{id}/add/{courseId}
    @Test
    void addToFavorites_returns200() throws Exception {
        when(favoriteService.addToFavorites(1L, 50L)).thenReturn("Added");

        mockMvc.perform(post("/api/student/favorites/1/add/50"))
                .andExpect(status().isOk());

        verify(favoriteService).addToFavorites(1L, 50L);
    }

    // 2. DELETE /{id}/remove/{courseId}
    @Test
    void removeFromFavorites_returns200() throws Exception {
        when(favoriteService.removeFromFavorites(1L, 50L)).thenReturn("Removed");

        mockMvc.perform(delete("/api/student/favorites/1/remove/50"))
                .andExpect(status().isOk());
    }

    // 3. DELETE /remove/{favoriteId}
    @Test
    void removeFavoriteById_returns200() throws Exception {
        when(favoriteService.removeFavoriteById(100L)).thenReturn("Removed");

        mockMvc.perform(delete("/api/student/favorites/remove/100"))
                .andExpect(status().isOk());
    }

    // 4. GET /{studentId} — returns list
    @Test
    void getStudentFavorites_returnsList() throws Exception {
        when(favoriteService.getStudentFavorites(1L))
                .thenReturn(List.of(FavoriteCourse.builder().courseId(50L).build()));

        mockMvc.perform(get("/api/student/favorites/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value(50));
    }

    // 5. GET /{studentId}/check/{courseId}
    @Test
    void checkIsFavorite_returnsBoolean() throws Exception {
        when(favoriteService.isFavorite(1L, 50L)).thenReturn(true);

        mockMvc.perform(get("/api/student/favorites/1/check/50"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}