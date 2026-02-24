package com.example.demologin.service;

import com.example.demologin.dto.response.QuotaResponse;
import com.example.demologin.enums.PackageType;

public interface QuotaService {
    QuotaResponse getQuota(Long userId);
    QuotaResponse consumeQuota(Long userId);
    void setPackage(Long userId, PackageType packageType);
}
