package com.example.demologin.dto.response;

import com.example.demologin.enums.PackageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaResponse {
    private PackageType packageType;
    /** remaining calls today; -1 means unlimited */
    private int remainingToday;
    private LocalDate quotaResetDate;
    /**
     * When a premium subscription expires, null if not premium or no expiry set.
     */
    private LocalDate premiumExpiryDate;
}
