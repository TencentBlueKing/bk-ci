package com.tencent.devops.common.notify.blueking.pojo

import com.tencent.devops.common.notify.blueking.enums.EnumNotifyPriority

open class BaseNotifyPost {
    var priority: String = EnumNotifyPriority.LOW.getValue()
    var contentMd5: String = ""
    var frequencyLimit: Int = 0
    var fromSysId: String = ""
    var tofSysId: String = ""
}