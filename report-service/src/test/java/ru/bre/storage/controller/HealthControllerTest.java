package ru.bre.storage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthControllerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new HealthController(dataSource)).build();
    }

    @Test
    void testLiveness() throws Exception {
        mockMvc.perform(get("/healthcheck/liveness"))
                .andExpect(status().isOk());
    }

    @Test
    void testReadinessWhenDatabaseUp() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(true);

        mockMvc.perform(get("/healthcheck/readiness"))
                .andExpect(status().isOk());
    }

    @Test
    void testReadinessWhenDatabaseDown() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("unavailable"));

        mockMvc.perform(get("/healthcheck/readiness"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void testReadinessWhenConnectionInvalid() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(false);

        mockMvc.perform(get("/healthcheck/readiness"))
                .andExpect(status().isServiceUnavailable());
    }
}
