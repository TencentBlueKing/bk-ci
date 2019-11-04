package com.tencent.devops.common.notify.pojo

class RtxNotifyPost : BaseNotifyPost() {
    var sender: String = ""
    var receiver: String = ""
    var title: String = ""
    var msgInfo: String = ""
}