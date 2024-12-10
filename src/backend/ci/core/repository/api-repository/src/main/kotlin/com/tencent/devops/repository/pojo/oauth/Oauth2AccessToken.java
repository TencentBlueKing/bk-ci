package com.tencent.devops.repository.pojo.oauth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Oauth2AccessToken {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private String refreshToken;
    private Long createTime;
}