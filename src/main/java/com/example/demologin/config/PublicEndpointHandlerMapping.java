package com.example.demologin.config;

import com.example.demologin.annotation.PublicEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Component để quét và thu thập tất cả các endpoint được đánh dấu với @PublicEndpoint.
 * Các endpoint này sẽ được tự động permit all trong SecurityFilterChain.
 */
@Component
public class PublicEndpointHandlerMapping {

    private final List<String> publicEndpoints = new ArrayList<>();

    /**
     * Inject bean mặc định của Spring MVC để tránh xung đột với bean của Actuator.
     */
    @Autowired
    public PublicEndpointHandlerMapping(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {

        // Quét tất cả các handler methods trong ứng dụng
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();

            // Kiểm tra nếu method có annotation @PublicEndpoint
            if (handlerMethod.hasMethodAnnotation(PublicEndpoint.class)) {
                RequestMappingInfo mappingInfo = entry.getKey();

                // Lấy tất cả các URL patterns (Spring < 6)
                if (mappingInfo.getPatternsCondition() != null) {
                    Set<String> patterns = mappingInfo.getPatternsCondition().getPatterns();
                    publicEndpoints.addAll(patterns);
                }

                // Hỗ trợ cho Spring 6+ sử dụng PathPatternsCondition
                if (mappingInfo.getPathPatternsCondition() != null) {
                    mappingInfo.getPathPatternsCondition().getPatterns()
                            .forEach(pattern -> publicEndpoints.add(pattern.getPatternString()));
                }
            }
        }

        System.out.println("Đã tìm thấy " + publicEndpoints.size() + " public endpoints: " + publicEndpoints);
    }

    /**
     * Trả về danh sách tất cả các public endpoints.
     * @return List các URL patterns được đánh dấu @PublicEndpoint
     */
    public List<String> getPublicEndpoints() {
        return new ArrayList<>(publicEndpoints);
    }
}
