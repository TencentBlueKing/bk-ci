package com.tencent.bk.codecc.task.service

interface DevopsNotifyService {

    /**
     * 发送邮件
     */
    fun sendMail(
        from: String,
        receivers: Set<String>,
        cc: Set<String>,
        bcc: Set<String>,
        title: String,
        content: String,
        priority: String,
        bodyFormat: String,
        attaches: Map<String, String>
    )

    /**
     * 发送企业微信
     */
    fun sendRtx(
        receivers: Set<String>,
        body : String = "",
        from : String,
        title : String,
        priority: String
    )
}