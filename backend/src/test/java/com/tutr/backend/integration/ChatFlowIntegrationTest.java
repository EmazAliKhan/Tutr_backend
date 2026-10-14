package com.tutr.backend.integration;

import com.tutr.backend.IntegrationTestBase;
import com.tutr.backend.model.entity.*;
import com.tutr.backend.model.enums.*;
import com.tutr.backend.repository.*;
import com.tutr.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ===================================== 6 TESTS ============================
class ChatFlowIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private TutorProfileRepository tutorProfileRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private TutorStudentConnectionRepository connectionRepository;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private JwtUtil jwtUtil;

    private User studentUser;
    private User tutorUser;
    private StudentProfile student;
    private TutorProfile tutor;
    private String studentJwt;

    @BeforeEach
    void setup() {
        chatRoomRepository.deleteAll();
        connectionRepository.deleteAll();
        courseRepository.deleteAll();
        tutorProfileRepository.deleteAll();
        studentProfileRepository.deleteAll();
        userRepository.deleteAll();

        tutorUser = userRepository.saveAndFlush(User.builder()
                .email("tutor@tutr.com").passwordHash("h")
                .role(Role.TUTOR).accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true).registrationStep(4).build());
        tutor = tutorProfileRepository.saveAndFlush(TutorProfile.builder()
                .user(tutorUser).firstName("Ahmed").lastName("Khan").build());

        studentUser = userRepository.saveAndFlush(User.builder()
                .email("student@tutr.com").passwordHash("h")
                .role(Role.STUDENT).accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true).registrationStep(2).build());
        student = studentProfileRepository.saveAndFlush(StudentProfile.builder()
                .user(studentUser).firstName("Ali").lastName("Raza").build());

        Course course = courseRepository.saveAndFlush(Course.builder()
                .tutorProfile(tutor).subject("Math")
                .isAvailable(true).price(5000.0).build());

        // Confirmed connection (required for chat)
        connectionRepository.saveAndFlush(TutorStudentConnection.builder()
                .tutor(tutor).student(student).course(course)
                .status(ConnectionStatus.CONFIRMED)
                .originalPrice(5000.0).agreedPrice(4500.0)
                .isActive(true)
                .build());

        studentJwt = jwtUtil.generateToken(studentUser.getId(), studentUser.getEmail(), studentUser.getRole().name());
    }

    // ============================================================
    // 1. Get or create shared chat room
    // ============================================================
    @Test
    void getOrCreateSharedChatRoom_success() throws Exception {
        mockMvc.perform(get("/api/chat/shared-room")
                        .header("Authorization", "Bearer " + studentJwt)
                        .param("studentId", String.valueOf(studentUser.getId()))
                        .param("tutorId", String.valueOf(tutorUser.getId()))
                        .param("userId", String.valueOf(studentUser.getId())))
                .andExpect(status().isOk());

        assertThat(chatRoomRepository.findAll()).hasSize(1);
    }

    // ============================================================
    // 2. Chat with no connection → 400 (no confirmed connection)
    // ============================================================
    @Test
    void getOrCreateChat_noConnection_returns400() throws Exception {
        connectionRepository.deleteAll();

        mockMvc.perform(get("/api/chat/shared-room")
                        .header("Authorization", "Bearer " + studentJwt)
                        .param("studentId", String.valueOf(studentUser.getId()))
                        .param("tutorId", String.valueOf(tutorUser.getId()))
                        .param("userId", String.valueOf(studentUser.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("You must have at least one confirmed connection to chat with this tutor"));
    }

    // ============================================================
    // 3. Get user chat rooms
    // ============================================================
    @Test
    void getUserChatRooms_returnsList() throws Exception {
        // Create a chat room directly
        chatRoomRepository.saveAndFlush(ChatRoom.builder()
                .roomId("shared_" + studentUser.getId() + "_" + tutorUser.getId())
                .studentUserId(studentUser.getId())
                .tutorUserId(tutorUser.getId())
                .isActive(true)
                .build());

        mockMvc.perform(get("/api/chat/rooms/" + studentUser.getId())
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ============================================================
    // 4. Send message persists to DB
    // ============================================================
    @Test
    void sendMessage_persistsToDb() throws Exception {
        ChatRoom room = chatRoomRepository.saveAndFlush(ChatRoom.builder()
                .roomId("shared_room")
                .studentUserId(studentUser.getId())
                .tutorUserId(tutorUser.getId())
                .isActive(true)
                .build());

        String json = """
                {
                    "chatRoomId": %d,
                    "senderId": %d,
                    "recipientId": %d,
                    "content": "Hello tutor!"
                }
                """.formatted(room.getId(), studentUser.getId(), tutorUser.getId());

        mockMvc.perform(post("/api/chat/messages/send")
                        .header("Authorization", "Bearer " + studentJwt)
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello tutor!"));
    }

    // ============================================================
    // 5. Get unread count
    // ============================================================
    @Test
    void getUnreadCount_returnsZero() throws Exception {
        mockMvc.perform(get("/api/chat/unread-count/" + studentUser.getId())
                        .header("Authorization", "Bearer " + studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    // ============================================================
    // 6. User not part of chat room → 403
    // ============================================================
    @Test
    void chatRoom_userNotPart_returns403() throws Exception {
        // Create a random user not in this chat
        User stranger = userRepository.saveAndFlush(User.builder()
                .email("stranger@tutr.com").passwordHash("h")
                .role(Role.STUDENT).accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true).registrationStep(2).build());

        String strangerJwt = jwtUtil.generateToken(stranger.getId(), stranger.getEmail(), stranger.getRole().name());

        mockMvc.perform(get("/api/chat/shared-room")
                        .header("Authorization", "Bearer " + strangerJwt)
                        .param("studentId", String.valueOf(studentUser.getId()))
                        .param("tutorId", String.valueOf(tutorUser.getId()))
                        .param("userId", String.valueOf(stranger.getId())))
                .andExpect(status().isForbidden());
    }
}