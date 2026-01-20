package com.example.demologin.annotation;

import com.example.demologin.enums.ActivityType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UserActivity {
    ActivityType activityType();
    String details() default "";
    boolean logUserId() default true; // true = log current user ID
}
