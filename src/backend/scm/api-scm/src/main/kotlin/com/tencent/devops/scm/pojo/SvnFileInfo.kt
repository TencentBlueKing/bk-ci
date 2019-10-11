package com.tencent.devops.scm.pojo

import com.tencent.devops.scm.pojo.enums.SvnFileType

data class SvnFileInfo(
    val type: SvnFileType,
    val name: String
)
