
package com.example.demologin;
import org.junit.jupiter.api.Disabled;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;



@Disabled("Ignore main contextLoads test for CI/CD and Jacoco")
@SpringBootTest
class DemoLoginApplicationTests {

    @Test
    void contextLoads() {
        // Ignored for CI/CD and Jacoco coverage
    }
}
