package com.tutr.backend.controller.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutr.backend.admin.config.TestSecurityConfig;
import com.tutr.backend.dto.chat.ChatRoomResponse;
import com.tutr.backend.dto.chat.MessageResponse;
import com.tutr.backend.dto.chat.SendMessageRequest;
import com.tutr.backend.facade.ChatFacade;
import com.tutr.backend.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 9 TESTS ============================
@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class ChatControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ChatFacade chatFacade;
    @MockBean private FileStorageService fileStorageService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u@tutr.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    // 1. GET /shared-room — success
    @Test
    void getOrCreateSharedRoom_success() throws Exception {
        ChatRoomResponse resp = ChatRoomResponse.builder().id(100L).build();
        when(chatFacade.getOrCreateSharedChatRoom(10L, 20L, 10L)).thenReturn(resp);

        mockMvc.perform(get("/api/chat/shared-room")
                        .param("studentId", "10")
                        .param("tutorId", "20")
                        .param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }

    // 2. GET /shared-room — no confirmed connection → 404
    @Test
    void getOrCreateSharedRoom_noConnection_returns404() throws Exception {
        when(chatFacade.getOrCreateSharedChatRoom(anyLong(), anyLong(), anyLong()))
                .thenThrow(new RuntimeException("No confirmed connection found"));

        mockMvc.perform(get("/api/chat/shared-room")
                        .param("studentId", "10")
                        .param("tutorId", "20")
                        .param("userId", "10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NO_CONFIRMED_CONNECTION"));
    }

    // 3. GET /shared-room — user not part → 403
    @Test
    void getOrCreateSharedRoom_notPart_returns403() throws Exception {
        when(chatFacade.getOrCreateSharedChatRoom(anyLong(), anyLong(), anyLong()))
                .thenThrow(new RuntimeException("User is not part of this chat room"));

        mockMvc.perform(get("/api/chat/shared-room")
                        .param("studentId", "10")
                        .param("tutorId", "20")
                        .param("userId", "999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("NOT_PART_OF_CHAT"));
    }

    // 4. GET /rooms/{userId}
    @Test
    void getUserChatRooms_returnsList() throws Exception {
        when(chatFacade.getUserChatRooms(10L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/chat/rooms/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // 5. POST /messages/send — success
    @Test
    void sendMessage_success() throws Exception {
        SendMessageRequest req = new SendMessageRequest();
        req.setChatRoomId(100L);
        req.setSenderId(10L);
        req.setRecipientId(20L);
        req.setContent("Hello");

        when(chatFacade.sendMessage(any()))
                .thenReturn(MessageResponse.builder().id(500L).content("Hello").build());

        mockMvc.perform(post("/api/chat/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello"));
    }

    // 6. GET /messages/{roomId}
    @Test
    void getMessages_returnsList() throws Exception {
        when(chatFacade.getMessages(eq(100L), eq(10L), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/chat/messages/100")
                        .param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // 7. PATCH /rooms/{roomId}/read-all
    @Test
    void markAllAsRead_returns200() throws Exception {
        mockMvc.perform(patch("/api/chat/rooms/100/read-all")
                        .param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(chatFacade).markAllAsRead(100L, 10L);
    }

    // 8. GET /unread-count/{userId}
    @Test
    void getUnreadCount_returnsCount() throws Exception {
        when(chatFacade.getUnreadCount(10L)).thenReturn(3L);

        mockMvc.perform(get("/api/chat/unread-count/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    // 9. POST /upload/audio — success
    @Test
    void uploadAudio_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "audio.aac",
                "audio/aac", "bytes".getBytes());

        when(fileStorageService.storeAudioFile(any(), eq(10L)))
                .thenReturn("/uploads/audio/test.aac");

        mockMvc.perform(multipart("/api/chat/upload/audio")
                        .file(file)
                        .param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audioUrl").value("/uploads/audio/test.aac"));
    }
}