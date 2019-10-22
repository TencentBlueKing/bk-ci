package com.tencent.devops.common.notify.pojo

class WechatNotifyPost : BaseNotifyPost() {
    var sender: String = ""
    var receiver: String = ""
    var msgInfo: String = ""
}