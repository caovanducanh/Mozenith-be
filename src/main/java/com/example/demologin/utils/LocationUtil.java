package com.example.demologin.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class LocationUtil {

    // Primary API: ip-api.com (free, no API key needed, 45 requests/minute)
    private static final String IP_API_URL = "http://ip-api.com/json/{ip}?fields=status,message,country,countryCode,regionName,city,lat,lon,isp,org,as,query";

    // Fallback API: ipapi.co (free tier available with API key)
    private static final String IPAPI_CO_URL = "https://ipapi.co/{ip}/json/";

    private final RestTemplate restTemplate;

    public LocationUtil(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(Duration.ofMillis(1500));
                    factory.setReadTimeout(Duration.ofMillis(3000));
                    return factory;
                })
                .build();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class LocationInfo {
        private String city;
        private String region;
        private String country;
        private String countryCode;
    }

    @Cacheable(value = "ipLocations", unless = "#result.city == null || #result.city.equals('Unknown')")
    public LocationInfo getLocationFromIP(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return unknownLocation();
        }

        // Handle local/private IPs
        if (isLocalOrPrivateIP(ipAddress)) {
            return localLocation();
        }

        // Try primary API first
        LocationInfo locationInfo = tryIpApi(ipAddress);
        if (!"Unknown".equals(locationInfo.getCity())) {
            return locationInfo;
        }

        // If primary fails, try fallback API
        locationInfo = tryIpApiCo(ipAddress);
        if (!"Unknown".equals(locationInfo.getCity())) {
            return locationInfo;
        }

        return unknownLocation();
    }

    private LocationInfo tryIpApi(String ipAddress) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    IP_API_URL.replace("{ip}", ipAddress),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = response.getBody();

                if ("success".equals(data.get("status"))) {
                    return new LocationInfo(
                            getStringOrUnknown(data, "city"),
                            getStringOrUnknown(data, "regionName"),
                            getStringOrUnknown(data, "country"),
                            getStringOrUnknown(data, "countryCode")
                    );
                } else {
                    log.warn("IP-API error for {}: {}", ipAddress, data.get("message"));
                }
            }
        } catch (Exception e) {
            log.debug("IP-API request failed for {}: {}", ipAddress, e.getMessage());
        }
        return unknownLocation();
    }

    private LocationInfo tryIpApiCo(String ipAddress) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    IPAPI_CO_URL.replace("{ip}", ipAddress),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = response.getBody();

                if (!Boolean.TRUE.equals(data.get("error"))) {
                    return new LocationInfo(
                            getStringOrUnknown(data, "city"),
                            getStringOrUnknown(data, "region"),
                            getStringOrUnknown(data, "country_name"),
                            getStringOrUnknown(data, "country_code")
                    );
                }
            }
        } catch (Exception e) {
            log.debug("ipapi.co request failed for {}: {}", ipAddress, e.getMessage());
        }
        return unknownLocation();
    }

    private String getStringOrUnknown(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : "Unknown";
    }

    private boolean isLocalOrPrivateIP(String ipAddress) {
        if (ipAddress == null) return true;

        // Remove port and localhost info if exists
        String ip = ipAddress.split("\\s+")[0].split(":")[0].trim();

        // Check for localhost
        if (ip.equals("127.0.0.1") || ip.equals("::1") || ip.equalsIgnoreCase("localhost")) {
            return true;
        }

        // Check for private IP ranges
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;

        try {
            int firstOctet = Integer.parseInt(parts[0]);
            int secondOctet = Integer.parseInt(parts[1]);

            // 10.0.0.0 - 10.255.255.255
            if (firstOctet == 10) return true;

            // 172.16.0.0 - 172.31.255.255
            if (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31) return true;

            // 192.168.0.0 - 192.168.255.255
            if (firstOctet == 192 && secondOctet == 168) return true;

            // 169.254.0.0/16 (APIPA)
            if (firstOctet == 169 && secondOctet == 254) return true;

        } catch (NumberFormatException e) {
            return false;
        }

        return false;
    }

    public static String formatLocationInfo(LocationInfo locationInfo) {
        if (locationInfo == null) {
            return "Unknown Location";
        }

        if ("LOCAL".equals(locationInfo.getCountryCode())) {
            return "Local Network";
        }

        StringBuilder location = new StringBuilder();

        if (!"Unknown".equals(locationInfo.getCity())) {
            location.append(locationInfo.getCity());
        }

        if (!"Unknown".equals(locationInfo.getRegion()) &&
                !locationInfo.getRegion().equals(locationInfo.getCity())) {
            if (location.length() > 0) location.append(", ");
            location.append(locationInfo.getRegion());
        }

        if (!"Unknown".equals(locationInfo.getCountry())) {
            if (location.length() > 0) location.append(", ");
            location.append(locationInfo.getCountry());
        }

        return location.length() > 0 ? location.toString() : "Unknown Location";
    }

    private LocationInfo unknownLocation() {
        return new LocationInfo("Unknown", "Unknown", "Unknown", "Unknown");
    }

    private LocationInfo localLocation() {
        return new LocationInfo("Local", "Local Network", "Local", "LOCAL");
    }

    // For testing purposes
    public static LocationInfo testLocation(String city, String region, String country, String countryCode) {
        return new LocationInfo(city, region, country, countryCode);
    }
}