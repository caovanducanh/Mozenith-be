package com.example.demologin.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CustomHealthControllerTest {
    @Test
    void testHealthWithSimpleHealthEmptyDetailsBranch() {
        // Health with empty details to cover the putAll line when details is empty
        Health health = Health.up().build();
        Mockito.when(healthEndpoint.health()).thenReturn(health);
        Object result = controller.health();
        Map<?,?> map = (Map<?,?>) result;
        assertEquals("UP", map.get("status"));
        assertEquals("Service is healthy", map.get("message"));
        Map<?,?> details = (Map<?,?>) map.get("details");
        assertTrue(details.isEmpty());
    }
    @Test
    void testHealthWithSimpleHealthDetailsBranch() {
        // This test is to force coverage of the 'else if (healthComponent instanceof Health simple)' branch
        Health health = Health.up().withDetail("foo", "bar").build();
        Mockito.when(healthEndpoint.health()).thenReturn(health);
        Object result = controller.health();
        Map<?,?> map = (Map<?,?>) result;
        assertEquals("UP", map.get("status"));
        assertEquals("Service is healthy", map.get("message"));
        Map<?,?> details = (Map<?,?>) map.get("details");
        assertTrue(details.containsKey("foo"));
        assertEquals("bar", details.get("foo"));
    }
    @Test
    void testHealthWithSimpleHealthNoDetails() {
        Health health = Health.up().build();
        Mockito.when(healthEndpoint.health()).thenReturn(health);
        Object result = controller.health();
        Map<?,?> map = (Map<?,?>) result;
        assertEquals("UP", map.get("status"));
        assertEquals("Service is healthy", map.get("message"));
        Map<?,?> details = (Map<?,?>) map.get("details");
        assertTrue(details.isEmpty());
    }
    @Test
    void testHealthWithCompositeHealthNonHealthComponent() {
        // Component không phải Health
        org.springframework.boot.actuate.health.HealthComponent fakeComponent = Mockito.mock(org.springframework.boot.actuate.health.HealthComponent.class);
        Mockito.when(fakeComponent.getStatus()).thenReturn(Status.UP);
        CompositeHealth composite = Mockito.mock(CompositeHealth.class);
        Mockito.when(composite.getStatus()).thenReturn(Status.UP);
        Mockito.when(composite.getComponents()).thenReturn(Collections.singletonMap("other", fakeComponent));
        Mockito.when(healthEndpoint.health()).thenReturn(composite);
        Object result = controller.health();
        Map<?,?> map = (Map<?,?>) result;
        assertEquals("UP", map.get("status"));
        Map<?,?> details = (Map<?,?>) map.get("details");
        assertTrue(details.containsKey("other"));
        Map<?,?> other = (Map<?,?>) details.get("other");
        assertEquals("UP", other.get("status"));
        assertNull(other.get("details"));
    }

    @Test
    void testHealthWithCompositeHealthEmptyComponents() {
        CompositeHealth composite = Mockito.mock(CompositeHealth.class);
        Mockito.when(composite.getStatus()).thenReturn(Status.UP);
        Mockito.when(composite.getComponents()).thenReturn(Collections.emptyMap());
        Mockito.when(healthEndpoint.health()).thenReturn(composite);
        Object result = controller.health();
        Map<?,?> map = (Map<?,?>) result;
        assertEquals("UP", map.get("status"));
        Map<?,?> details = (Map<?,?>) map.get("details");
        assertTrue(details.isEmpty());
    }

    @Test
    void testHealthWithCompositeHealthMixedComponents() {
        Health dbHealth = Health.up().withDetail("db", "ok").build();
        org.springframework.boot.actuate.health.HealthComponent fakeComponent = Mockito.mock(org.springframework.boot.actuate.health.HealthComponent.class);
        Mockito.when(fakeComponent.getStatus()).thenReturn(Status.UP);
        java.util.Map<String, org.springframework.boot.actuate.health.HealthComponent> components = new java.util.HashMap<>();
        components.put("db", dbHealth);
        components.put("other", fakeComponent);
        CompositeHealth composite = Mockito.mock(CompositeHealth.class);
        Mockito.when(composite.getStatus()).thenReturn(Status.UP);
        Mockito.when(composite.getComponents()).thenReturn(components);
        Mockito.when(healthEndpoint.health()).thenReturn(composite);
        Object result = controller.health();
        Map<?,?> map = (Map<?,?>) result;
        assertEquals("UP", map.get("status"));
        Map<?,?> details = (Map<?,?>) map.get("details");
        assertTrue(details.containsKey("db"));
        assertTrue(details.containsKey("other"));
    }
    private HealthEndpoint healthEndpoint;
    private CustomHealthController controller;

    @BeforeEach
    void setUp() {
        healthEndpoint = Mockito.mock(HealthEndpoint.class);
        controller = new CustomHealthController(healthEndpoint);
    }

    @Test
    void testHealthUpWithSimpleHealth() {
        Health health = Health.up().withDetail("db", "ok").build();
        Mockito.when(healthEndpoint.health()).thenReturn(health);
        Object result = controller.health();
        Map<?,?> map = (Map<?,?>) result;
        assertEquals("UP", map.get("status"));
        assertEquals("Service is healthy", map.get("message"));
        assertTrue(((Map<?,?>) map.get("details")).containsKey("db"));
    }

    @Test
    void testHealthDownWithSimpleHealth() {
        Health health = Health.down().withDetail("db", "fail").build();
        Mockito.when(healthEndpoint.health()).thenReturn(health);
        Object result = controller.health();
        Map<?,?> map = (Map<?,?>) result;
        assertEquals("DOWN", map.get("status"));
        assertEquals("Service is unavailable", map.get("message"));
        assertTrue(((Map<?,?>) map.get("details")).containsKey("db"));
    }

    @Test
    void testHealthWithCompositeHealth() {
        Health dbHealth = Health.up().withDetail("db", "ok").build();
        CompositeHealth composite = Mockito.mock(CompositeHealth.class);
        Mockito.when(composite.getStatus()).thenReturn(Status.UP);
        Mockito.when(composite.getComponents()).thenReturn(Collections.singletonMap("db", dbHealth));
        Mockito.when(healthEndpoint.health()).thenReturn(composite);
        Object result = controller.health();
        Map<?,?> map = (Map<?,?>) result;
        assertEquals("UP", map.get("status"));
        assertEquals("Service is healthy", map.get("message"));
        Map<?,?> details = (Map<?,?>) map.get("details");
        assertTrue(details.containsKey("db"));
    }
}
