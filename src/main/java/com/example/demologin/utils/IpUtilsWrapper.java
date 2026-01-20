package com.example.demologin.utils;


import org.springframework.stereotype.Component;

@Component
public class IpUtilsWrapper {
    public String getClientIpAddress() {
        return IpUtils.getClientIpAddress();
    }

    public String getUserAgent() {
        return IpUtils.getUserAgent();
    }


}