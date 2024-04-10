package com.tencent.devops.remotedev.utils

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.security.Security
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import javax.crypto.Cipher

object RsaUtil {
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    /**
     * RSA公钥 PKCS8 格式
     *
     * @param publicKey
     * 公钥
     * @return 公钥
     * @throws Exception
     * 加密过程中的异常信息
     */
    fun generatePublicKey(publicKey: ByteArray): RSAPublicKey {
        val bais = ByteArrayInputStream(publicKey)
        val reader = PEMReader(InputStreamReader(bais)) { "".toCharArray() }
        return reader.use { it.readObject() as RSAPublicKey }
    }

    /**
     * RSA私钥
     *
     * @param privateKey PKCS8 格式
     * 私钥
     * @return 私钥
     * @throws Exception
     * 解密过程中的异常信息
     */
    fun generatePrivateKey(privateKey: ByteArray): RSAPrivateKey {
        val bais = ByteArrayInputStream(privateKey)
        val reader = PEMReader(InputStreamReader(bais)) { "".toCharArray() }
        return reader.use { it.readObject() as RSAPrivateKey }
    }

    /**
     * RSA公钥加密
     *
     * @param value
     * 加密字符串
     * @param publicKey
     * 公钥
     * @return 密文
     * @throws Exception
     * 加密过程中的异常信息
     */
    fun rsaEncrypt(value: String, publicKey: RSAPublicKey): String {
        // RSA加密
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val buffer = cipher.doFinal(value.toByteArray(charset("utf-8")))
        return String(Base64.getEncoder().encode(buffer))
    }

    /**
     * RSA私钥解密
     *
     * @param value
     * 加密字符串
     * @param privateKey
     * 私钥
     * @return 明文
     * @throws Exception
     * 解密过程中的异常信息
     */
    @Throws(Exception::class)
    fun rsaDecrypt(value: String, privateKey: RSAPrivateKey): String {
        var buffer = Base64.getDecoder().decode(value)

        // RSA解密
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        buffer = cipher.doFinal(buffer)
        return String(buffer)
    }

    /**
     * RSA签名
     *
     * @param value
     * 加密字符串
     * @param privateKey
     * 私钥
     * @param alg
     * 加密算法，如MD5, SHA1, SHA256, SHA384, SHA512等
     * @return 签名
     * @throws Exception
     * 签名过程中的异常信息
     */
    fun sign(value: String, privateKey: RSAPrivateKey, alg: String): String {
        val s = Signature.getInstance(if (alg.toUpperCase().endsWith("WithRSA")) alg else alg + "WithRSA")
        s.initSign(privateKey)
        s.update(value.toByteArray(charset("utf-8")))
        val buffer: ByteArray = s.sign()

        // 使用hex格式输出公钥
        val result = StringBuffer()
        for (i in buffer.indices) {
            result.append(String.format("%02x", buffer[i]))
        }
        return result.toString()
    }

    /**
     * RSA签名验证
     *
     * @param value
     * 加密字符串
     * @param publicKey
     * 公钥
     * @param alg
     * 加密算法，如MD5, SHA1, SHA256, SHA384, SHA512等
     * @return 签名合法则返回true，否则返回false
     * @throws Exception
     * 验证过程中的异常信息
     */
    fun verify(value: String, publicKey: RSAPublicKey, signature: String, alg: String): Boolean {
        val s = Signature.getInstance(if (alg.toUpperCase().endsWith("WithRSA")) alg else alg + "WithRSA")
        s.initVerify(publicKey)
        s.update(value.toByteArray(charset("utf-8")))
        val buffer = ByteArray(signature.length / 2)
        for (i in buffer.indices) {
            buffer[i] = signature.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return s.verify(buffer)
    }
}
