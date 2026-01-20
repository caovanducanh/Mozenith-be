package com.example.demologin.aspect;

import com.example.demologin.utils.AccountUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class AuthenticatedEndpointAspectTest {
    @Mock
    AccountUtils accountUtils;

    @InjectMocks
    AuthenticatedEndpointAspect aspect;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aspect = new AuthenticatedEndpointAspect(accountUtils);
    }

    @Test
    void testCheckAuthenticated_callsGetCurrentUser() {
        aspect.checkAuthenticated();
        verify(accountUtils, times(1)).getCurrentUser();
    }
}
