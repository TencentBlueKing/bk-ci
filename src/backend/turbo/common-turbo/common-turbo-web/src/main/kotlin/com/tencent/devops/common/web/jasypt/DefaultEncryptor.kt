package com.tencent.devops.common.web.jasypt

import com.tencent.devops.common.util.AESUtil
import org.jasypt.encryption.StringEncryptor

class DefaultEncryptor(private val key: String) : StringEncryptor {

    override fun decrypt(message: String): String {
        return AESUtil.decrypt(key, message)
    }

    override fun encrypt(message: String): String {
        return AESUtil.encrypt(key, message)
    }
}
