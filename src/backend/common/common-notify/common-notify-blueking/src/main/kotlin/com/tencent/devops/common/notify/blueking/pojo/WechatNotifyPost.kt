package com.tencent.devops.common.notify.blueking.pojo

class WechatNotifyPost : BaseNotifyPost() {
    var sender: String = ""
    var receiver: String = ""
    var msgInfo: String = ""
}