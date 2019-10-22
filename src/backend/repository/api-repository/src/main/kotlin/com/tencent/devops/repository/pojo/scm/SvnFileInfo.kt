package com.tencent.devops.repository.pojo.scm

import com.tencent.devops.repository.pojo.scm.enums.SvnFileType

data class SvnFileInfo(
        val type: SvnFileType,
        val name: String
)
