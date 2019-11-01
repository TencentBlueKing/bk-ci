package com.tencent.devops.common.notify.pojo

class SmsNotifyPost : BaseNotifyPost() {
    var sender: String = ""
    var receiver: String = ""
    var msgInfo: String = ""
}