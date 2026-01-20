package com.example.demologin.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserAgentUtilTest {

    @Test
    void testParseUserAgent_Chrome() {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
        
        UserAgentUtil.DeviceInfo deviceInfo = UserAgentUtil.parseUserAgent(userAgent);
        
        assertEquals("Google Chrome", deviceInfo.getBrowser());
        assertEquals("91.0.4472.124", deviceInfo.getBrowserVersion());
        assertEquals("Windows 10/11", deviceInfo.getOperatingSystem());
        assertEquals("Windows Computer", deviceInfo.getDevice());
        assertEquals("Desktop", deviceInfo.getDeviceType());
    }

    @Test
    void testParseUserAgent_Firefox() {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0";
        
        UserAgentUtil.DeviceInfo deviceInfo = UserAgentUtil.parseUserAgent(userAgent);
        
        assertEquals("Mozilla Firefox", deviceInfo.getBrowser());
        assertEquals("89.0", deviceInfo.getBrowserVersion());
        assertEquals("Windows 10/11", deviceInfo.getOperatingSystem());
        assertEquals("Windows Computer", deviceInfo.getDevice());
        assertEquals("Desktop", deviceInfo.getDeviceType());
    }

    @Test
    void testParseUserAgent_Safari() {
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15";
        
        UserAgentUtil.DeviceInfo deviceInfo = UserAgentUtil.parseUserAgent(userAgent);
        
        assertEquals("Safari", deviceInfo.getBrowser());
        assertEquals("14.1.1", deviceInfo.getBrowserVersion());
        assertEquals("macOS 10.15.7", deviceInfo.getOperatingSystem());
        assertEquals("Mac Computer", deviceInfo.getDevice());
        assertEquals("Desktop", deviceInfo.getDeviceType());
    }

    @Test
    void testParseUserAgent_AndroidChrome() {
        String userAgent = "Mozilla/5.0 (Linux; Android 11; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36";
        
        UserAgentUtil.DeviceInfo deviceInfo = UserAgentUtil.parseUserAgent(userAgent);
        
        assertEquals("Google Chrome", deviceInfo.getBrowser());
        assertEquals("91.0.4472.120", deviceInfo.getBrowserVersion());
        assertEquals("Android 11", deviceInfo.getOperatingSystem());
        assertEquals("Android Phone", deviceInfo.getDevice());
        assertEquals("Mobile", deviceInfo.getDeviceType());
    }

    @Test
    void testParseUserAgent_iPhone() {
        String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Mobile/15E148 Safari/604.1";
        
        UserAgentUtil.DeviceInfo deviceInfo = UserAgentUtil.parseUserAgent(userAgent);
        
        assertEquals("Safari", deviceInfo.getBrowser());
        assertEquals("14.1.1", deviceInfo.getBrowserVersion());
        assertEquals("iOS 14.6", deviceInfo.getOperatingSystem());
        assertEquals("iPhone", deviceInfo.getDevice());
        assertEquals("Mobile", deviceInfo.getDeviceType());
    }

    @Test
    void testParseUserAgent_Edge() {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.59";
        
        UserAgentUtil.DeviceInfo deviceInfo = UserAgentUtil.parseUserAgent(userAgent);
        
        assertEquals("Microsoft Edge", deviceInfo.getBrowser());
        assertEquals("91.0.864.59", deviceInfo.getBrowserVersion());
        assertEquals("Windows 10/11", deviceInfo.getOperatingSystem());
        assertEquals("Windows Computer", deviceInfo.getDevice());
        assertEquals("Desktop", deviceInfo.getDeviceType());
    }

    @Test
    void testParseUserAgent_NullOrEmpty() {
        UserAgentUtil.DeviceInfo deviceInfo1 = UserAgentUtil.parseUserAgent(null);
        UserAgentUtil.DeviceInfo deviceInfo2 = UserAgentUtil.parseUserAgent("");
        
        assertEquals("Unknown", deviceInfo1.getBrowser());
        assertEquals("Unknown", deviceInfo1.getOperatingSystem());
        
        assertEquals("Unknown", deviceInfo2.getBrowser());
        assertEquals("Unknown", deviceInfo2.getOperatingSystem());
    }

    @Test
    void testFormatDeviceInfo() {
        UserAgentUtil.DeviceInfo deviceInfo = new UserAgentUtil.DeviceInfo(
            "Google Chrome", "91.0.4472.124", "Windows 10/11", "Windows Computer", "Desktop"
        );
        
        String formatted = UserAgentUtil.formatDeviceInfo(deviceInfo);
        assertEquals("Google Chrome 91.0.4472.124 on Windows 10/11 (Desktop)", formatted);
    }
}
