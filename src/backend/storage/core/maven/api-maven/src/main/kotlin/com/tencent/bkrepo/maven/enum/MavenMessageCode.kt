package com.tencent.bkrepo.maven.enum

import com.tencent.bkrepo.common.api.message.MessageCode

enum class MavenMessageCode(private val key: String) : MessageCode {
    CHECKSUM_CONFLICT("checksum.conflict")
    ;

    override fun getBusinessCode() = ordinal + 1
    override fun getKey() = key
    override fun getModuleCode() = 21
}
