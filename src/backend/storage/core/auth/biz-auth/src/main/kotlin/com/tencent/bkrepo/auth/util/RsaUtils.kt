package com.tencent.bkrepo.auth.util

import cn.hutool.crypto.asymmetric.KeyType
import cn.hutool.crypto.asymmetric.RSA

/**
 * RSA 非对称加密工具类
 */
object RsaUtils {
    private val rsa = RSA()
    var privateKey = rsa.privateKeyBase64!!
    var publicKey = rsa.publicKeyBase64!!

    /**
     * 公钥加密
     * @param password 需要解密的密码
     */
    fun encrypt(password: String): String {
        return rsa.encryptBcd(password, KeyType.PublicKey)
    }

    /**
     * 私钥解密，返回解密后的密码
     * @param password 前端加密后的密码
     */
    fun decrypt(password: String): String {
        return rsa.decryptStr(password, KeyType.PrivateKey)
    }
}
