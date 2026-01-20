package com.example.demologin.aspect;

import com.example.demologin.utils.AccountUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuthenticatedEndpointAspect {

    private final AccountUtils accountUtils;

    @Before("@annotation(com.example.demologin.annotation.AuthenticatedEndpoint)")
    public void checkAuthenticated() {
        // Nếu chưa login -> AccountUtils sẽ ném exception
        accountUtils.getCurrentUser();
    }
}
