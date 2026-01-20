package com.example.demologin.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UserAgentUtil {

    @Getter
    @Setter
    @AllArgsConstructor
    public static class DeviceInfo {
        private String browser;
        private String browserVersion;
        private String operatingSystem;
        private String device;
        private String deviceType;
    }

    public static DeviceInfo parseUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return new DeviceInfo("Unknown", "Unknown", "Unknown", "Unknown", "Unknown");
        }

        String browser = extractBrowser(userAgent);
        String browserVersion = extractBrowserVersion(userAgent, browser);
        String operatingSystem = extractOperatingSystem(userAgent);
        String device = extractDevice(userAgent);
        String deviceType = extractDeviceType(userAgent);

        return new DeviceInfo(browser, browserVersion, operatingSystem, device, deviceType);
    }

    private static String extractBrowser(String userAgent) {
        if (userAgent.contains("Edg/")) {
            return "Microsoft Edge";
        } else if (userAgent.contains("Chrome/") && !userAgent.contains("Chromium/")) {
            return "Google Chrome";
        } else if (userAgent.contains("Firefox/")) {
            return "Mozilla Firefox";
        } else if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) {
            return "Safari";
        } else if (userAgent.contains("Opera/") || userAgent.contains("OPR/")) {
            return "Opera";
        } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/")) {
            return "Internet Explorer";
        } else if (userAgent.contains("Chromium/")) {
            return "Chromium";
        }
        return "Unknown Browser";
    }

    private static String extractBrowserVersion(String userAgent, String browser) {
        Pattern pattern = null;
        
        switch (browser) {
            case "Google Chrome":
                pattern = Pattern.compile("Chrome/([0-9.]+)");
                break;
            case "Mozilla Firefox":
                pattern = Pattern.compile("Firefox/([0-9.]+)");
                break;
            case "Safari":
                pattern = Pattern.compile("Version/([0-9.]+).*Safari/");
                break;
            case "Microsoft Edge":
                pattern = Pattern.compile("Edg/([0-9.]+)");
                break;
            case "Opera":
                if (userAgent.contains("OPR/")) {
                    pattern = Pattern.compile("OPR/([0-9.]+)");
                } else {
                    pattern = Pattern.compile("Opera/([0-9.]+)");
                }
                break;
            case "Internet Explorer":
                if (userAgent.contains("MSIE")) {
                    pattern = Pattern.compile("MSIE ([0-9.]+)");
                } else {
                    pattern = Pattern.compile("rv:([0-9.]+)");
                }
                break;
        }

        if (pattern != null) {
            Matcher matcher = pattern.matcher(userAgent);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "Unknown";
    }

    private static String extractOperatingSystem(String userAgent) {
        if (userAgent.contains("Android")) {
            Pattern pattern = Pattern.compile("Android ([0-9.]+)");
            Matcher matcher = pattern.matcher(userAgent);
            if (matcher.find()) {
                return "Android " + matcher.group(1);
            }
            return "Android";
        } else if (userAgent.contains("iPhone OS") || userAgent.contains("iOS")) {
            Pattern pattern = Pattern.compile("OS ([0-9_]+)");
            Matcher matcher = pattern.matcher(userAgent);
            if (matcher.find()) {
                return "iOS " + matcher.group(1).replace("_", ".");
            }
            return "iOS";
        } else if (userAgent.contains("Windows NT 10.0")) {
            return "Windows 10/11";
        } else if (userAgent.contains("Windows NT 6.3")) {
            return "Windows 8.1";
        } else if (userAgent.contains("Windows NT 6.2")) {
            return "Windows 8";
        } else if (userAgent.contains("Windows NT 6.1")) {
            return "Windows 7";
        } else if (userAgent.contains("Windows NT")) {
            return "Windows";
        } else if (userAgent.contains("Mac OS X")) {
            Pattern pattern = Pattern.compile("Mac OS X ([0-9_]+)");
            Matcher matcher = pattern.matcher(userAgent);
            if (matcher.find()) {
                return "macOS " + matcher.group(1).replace("_", ".");
            }
            return "macOS";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        }
        return "Unknown OS";
    }

    private static String extractDevice(String userAgent) {
        if (userAgent.contains("iPhone")) {
            return "iPhone";
        } else if (userAgent.contains("iPad")) {
            return "iPad";
        } else if (userAgent.contains("Android")) {
            if (userAgent.contains("Mobile")) {
                return "Android Phone";
            } else {
                return "Android Tablet";
            }
        } else if (userAgent.contains("Windows Phone")) {
            return "Windows Phone";
        } else if (userAgent.contains("BlackBerry")) {
            return "BlackBerry";
        } else if (userAgent.contains("Mac")) {
            return "Mac Computer";
        } else if (userAgent.contains("Windows")) {
            return "Windows Computer";
        } else if (userAgent.contains("Linux")) {
            return "Linux Computer";
        }
        return "Unknown Device";
    }

    private static String extractDeviceType(String userAgent) {
        if (userAgent.contains("Mobile") || userAgent.contains("iPhone") || 
            userAgent.contains("Android") && userAgent.contains("Mobile")) {
            return "Mobile";
        } else if (userAgent.contains("Tablet") || userAgent.contains("iPad") ||
                   userAgent.contains("Android") && !userAgent.contains("Mobile")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }

    // Utility method để format thông tin device thành string đẹp
    public static String formatDeviceInfo(DeviceInfo deviceInfo) {
        return String.format("%s %s on %s (%s)", 
            deviceInfo.getBrowser(), 
            deviceInfo.getBrowserVersion(),
            deviceInfo.getOperatingSystem(),
            deviceInfo.getDeviceType()
        );
    }
}
