package com.tencent.devops.common.notify.pojo

import com.tencent.devops.common.notify.enums.EnumNotifyPriority

open class BaseNotifyPost {
    var priority: String = EnumNotifyPriority.HIGH.getValue()
    var contentMd5: String = ""
    var frequencyLimit: Int = 0
    var fromSysId: String = ""
    var tofSysId: String = ""
}