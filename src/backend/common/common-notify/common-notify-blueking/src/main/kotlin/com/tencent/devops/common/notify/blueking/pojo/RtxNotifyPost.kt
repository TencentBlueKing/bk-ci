package com.tencent.devops.common.notify.blueking.pojo

class RtxNotifyPost : BaseNotifyPost() {
    var sender: String = ""
    var receiver: String = ""
    var title: String = ""
    var msgInfo: String = ""
}