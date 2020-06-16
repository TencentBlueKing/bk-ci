package com.tencent.devops.common.util.crypto

import com.google.common.collect.Lists
import com.tencent.devops.common.util.Base64Util
import org.apache.commons.codec.binary.Base64
import java.io.*
import java.security.*
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher


/**
 * RSAUtils 加解密
 */
object RSAUtils {
    private const val KEY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"
    private const val BEGIN_ENCRYPTED_PRIVATE_KEY = "-----BEGIN ENCRYPTED PRIVATE KEY-----"
    private const val END_ENCRYPTED_PRIVATE_KEY = "-----END ENCRYPTED PRIVATE KEY-----"
    private const val BEGIN_RSA_PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----"
    private const val END_RSA_PRIVATE_KEY = "-----END RSA PRIVATE KEY-----"
    private const val BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----"
    private const val END_PRIVATE_KEY = "-----END PRIVATE KEY-----"
    private const val BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----"
    private const val END_PUBLIC_KEY = "-----END PUBLIC KEY-----"
    private const val CHARSET_NAME = "UTF-8"
    private const val LINE_SPLIT = "\n"
    private val SKIP_STR: List<String> = Lists.newArrayList(
            BEGIN_PRIVATE_KEY, END_PRIVATE_KEY, BEGIN_PUBLIC_KEY, END_PUBLIC_KEY,
            BEGIN_ENCRYPTED_PRIVATE_KEY, END_ENCRYPTED_PRIVATE_KEY, BEGIN_RSA_PRIVATE_KEY, END_RSA_PRIVATE_KEY
    )

    @Throws(IOException::class)
    private fun getPermKey(permFile: File): String {
        val strKeyPEM = StringBuilder(2048)
        BufferedReader(FileReader(permFile)).use { br ->
            var line: String = ""
            while (br.readLine().also { line = it } != null) {
                if (SKIP_STR.contains(line)) {
                    continue
                }
                strKeyPEM.append(line).append(LINE_SPLIT)
            }
        }
        return strKeyPEM.toString()
    }

    @Throws(IOException::class)
    private fun getPermKey(permBase64: String): String {
        val perm = Base64Util.base64DecodeContentToStr(permBase64)
        val strKeyPEM = StringBuilder(2048)
        BufferedReader(StringReader(perm)).use { br ->
            var line: String = ""
            while (br.readLine().also { line = it } != null) {
                if (SKIP_STR.contains(line)) {
                    continue
                }
                strKeyPEM.append(line).append(LINE_SPLIT)
            }
        }
        return strKeyPEM.toString()
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun getPrivateKey(rsaPrivatePermFile: File): RSAPrivateKey {
        val privateKeyPEM = getPermKey(rsaPrivatePermFile)
        val encoded = Base64.decodeBase64(privateKeyPEM)
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(PKCS8EncodedKeySpec(encoded)) as RSAPrivateKey
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun getPrivateKey(rsaPrivateKeyBase64: String): RSAPrivateKey {
        val privateKeyPEM = getPermKey(rsaPrivateKeyBase64)
        val encoded = Base64.decodeBase64(privateKeyPEM)
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(PKCS8EncodedKeySpec(encoded)) as RSAPrivateKey
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun getPublicKey(rsaPublicPermFile: File): RSAPublicKey {
        val publicKeyPEM = getPermKey(rsaPublicPermFile)
        val encoded = Base64.decodeBase64(publicKeyPEM)
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(X509EncodedKeySpec(encoded)) as RSAPublicKey
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun getPublicKey(rsaPublicKeyBase64: String): RSAPublicKey {
        val encoded = Base64.decodeBase64(getPermKey(rsaPublicKeyBase64))
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(X509EncodedKeySpec(encoded)) as RSAPublicKey
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class, SignatureException::class, UnsupportedEncodingException::class)
    fun sign(privateKey: PrivateKey?, message: String): String {
        val sign = Signature.getInstance(SIGNATURE_ALGORITHM)
        sign.initSign(privateKey)
        sign.update(message.toByteArray(charset(CHARSET_NAME)))
        return String(Base64.encodeBase64(sign.sign()), charset(CHARSET_NAME))
    }

    @Throws(SignatureException::class, NoSuchAlgorithmException::class, UnsupportedEncodingException::class, InvalidKeyException::class)
    fun verify(publicKey: PublicKey?, message: String, signature: String): Boolean {
        val sign = Signature.getInstance(SIGNATURE_ALGORITHM)
        sign.initVerify(publicKey)
        sign.update(message.toByteArray(charset(CHARSET_NAME)))
        return sign.verify(Base64.decodeBase64(signature.toByteArray(charset(CHARSET_NAME))))
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun encrypt(rawText: String, publicKey: PublicKey?): String {
        val cipher = Cipher.getInstance(KEY_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return Base64.encodeBase64String(cipher.doFinal(rawText.toByteArray(charset(CHARSET_NAME))))
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun decrypt(cipherText: String?, privateKey: PrivateKey?): String {
        val cipher = Cipher.getInstance(KEY_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return String(cipher.doFinal(Base64.decodeBase64(cipherText)), charset(CHARSET_NAME))
    }
}