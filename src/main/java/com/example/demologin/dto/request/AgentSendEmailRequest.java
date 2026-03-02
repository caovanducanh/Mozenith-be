package com.example.demologin.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AgentSendEmailRequest {
    private String toEmail;
    private String subject;
    private String message;
    private String ccEmail;
}
