package com.example.demologin.controller;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PingControllerTest {
    @Test
    void ping() {
        PingController controller = new PingController();
        Map<String, Object> result = controller.ping();
        assertEquals("UP", result.get("status"));
        assertNotNull(result.get("timestamp"));
        assertTrue(result.get("timestamp") instanceof String);
    }
}
