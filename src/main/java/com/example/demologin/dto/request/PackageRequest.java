package com.example.demologin.dto.request;

import com.example.demologin.enums.PackageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageRequest {
    private PackageType packageType;
}
