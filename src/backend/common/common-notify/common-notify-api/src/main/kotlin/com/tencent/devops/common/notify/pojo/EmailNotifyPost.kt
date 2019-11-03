package com.tencent.devops.common.notify.pojo

import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.enums.EnumEmailType

class EmailNotifyPost : BaseNotifyPost() {
    var from: String = ""
    var to: String = ""
    var cc: String = ""
    var bcc: String = ""
    var title: String = ""
    var content: String = ""
    var emailType: Int = EnumEmailType.OUTER_MAIL.getValue()
    var bodyFormat: Int = EnumEmailFormat.PLAIN_TEXT.getValue()
}