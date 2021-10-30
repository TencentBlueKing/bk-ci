package com.tencent.devops.scm.pojo.p4

class P4TriggerProperties(
    /**
     * p4服务器地址
     * 如: localhost:1666
     * */
    val p4port: String,
    /**
     * 用户名，需要具备管理员权限
     * */
    val userName: String,
    /**
     * 密码
     * */
    val password: String,
    /**
     * p4服务触发器回调的webhook地址
     * */
    val webhookUrl: String,
    /**
     * 触发器执行的ticket
     * 因为脚本包含p4命令，所以需要通过p4服务端认证
     * */
    val triggerTicket: String
)
