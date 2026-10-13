package com.tutr.backend.facade;

import com.tutr.backend.dto.connection.ConnectionRequest;
import com.tutr.backend.dto.connection.ConnectionResponse;
import com.tutr.backend.model.entity.TutorStudentConnection;
import com.tutr.backend.service.ConnectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// ===================================== 2 TESTS ============================
@ExtendWith(MockitoExtension.class)
class ConnectionFacadeTest {

    @Mock private ConnectionService connectionService;
    @InjectMocks private ConnectionFacade connectionFacade;

    // 1. requestConnection — delegates + converts
    @Test
    void requestConnection_delegates() {
        ConnectionRequest req = new ConnectionRequest();
        TutorStudentConnection conn = TutorStudentConnection.builder().id(200L).build();
        ConnectionResponse resp = ConnectionResponse.builder().connectionId(200L).build();

        when(connectionService.requestConnection(req)).thenReturn(conn);
        when(connectionService.convertToResponse(conn)).thenReturn(resp);

        ConnectionResponse result = connectionFacade.requestConnection(req);

        assertThat(result.getConnectionId()).isEqualTo(200L);
        verify(connectionService).requestConnection(req);
        verify(connectionService).convertToResponse(conn);
    }

    // 2. getStudentConnections — passes through
    @Test
    void getStudentConnections_passesThrough() {
        when(connectionService.getStudentConnections(1L)).thenReturn(java.util.List.of());

        connectionFacade.getStudentConnections(1L);

        verify(connectionService).getStudentConnections(1L);
    }
}