package com.tencent.devops.monitoring.pojo.enums

enum class GrafanaNotifyTypeEnum {
    RTX, // 企业微信
    WECHAT, // 微信
    EMAIL, // 邮件
    NOC, // NOC语音告警
    RTX_WECHAT, // 企业微信和微信
    RTX_WECHAT_EMIAL, // 企业微信、微信和邮件
    RTX_WECHAT_NOC, // 企业微信、微信和NOC语音告警
    EMAIL_NOC, // 邮件和NOC语音告警
    ALL; // 企业微信、微信、邮件和NOC语音告警
}