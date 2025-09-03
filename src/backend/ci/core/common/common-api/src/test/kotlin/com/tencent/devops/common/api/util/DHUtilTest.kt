package com.tencent.devops.common.api.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Base64
import javax.crypto.BadPaddingException

class DHUtilTest {

    companion object {
        private const val secret = "123456"
    }

    @Test
    @DisplayName("同时填充")
    fun encryptAndDecrypt1() {
        for (i in 0 until 1000) {
            val pair = DHUtil.initKey()
            val (serverBase64PublicKey, encryptedSecret) = serverEncrypt(
                clientPublicKey = pair.publicKey,
                padding = true
            )
            val decryptedSecret = clientDecrypt(
                encryptedSecret = encryptedSecret,
                serverPublicKey = serverBase64PublicKey,
                clientPrivateKey = pair.privateKey,
                padding = true
            )
            Assertions.assertEquals(secret, decryptedSecret)
        }
    }

    @Test
    @DisplayName("同时不填充")
    fun encryptAndDecrypt2() {
        for (i in 0 until 1000) {
            val pair = DHUtil.initKey()
            val (serverBase64PublicKey, encryptedSecret) = serverEncrypt(
                clientPublicKey = pair.publicKey,
                padding = false
            )
            val decryptedSecret = clientDecrypt(
                encryptedSecret = encryptedSecret,
                serverPublicKey = serverBase64PublicKey,
                clientPrivateKey = pair.privateKey,
                padding = false
            )
            Assertions.assertEquals(secret, decryptedSecret)
        }
    }

    @DisplayName("客户端不填充,服务端填充")
    fun encryptAndDecrypt3() {
        Assertions.assertThrows(BadPaddingException::class.java) {
            for (i in 0 until 1000) {
                val pair = DHUtil.initKey()
                val (serverBase64PublicKey, encryptedSecret) = serverEncrypt(
                    clientPublicKey = pair.publicKey,
                    padding = true
                )
                val decryptedSecret = clientDecrypt(
                    encryptedSecret = encryptedSecret,
                    serverPublicKey = serverBase64PublicKey,
                    clientPrivateKey = pair.privateKey,
                    padding = false
                )
                Assertions.assertEquals(secret, decryptedSecret)
            }
        }
    }

    @DisplayName("客户端填充,服务端不填充")
    fun encryptAndDecrypt4() {
        Assertions.assertThrows(BadPaddingException::class.java) {
            for (i in 0 until 1000) {
                val pair = DHUtil.initKey()
                val (serverBase64PublicKey, encryptedSecret) = serverEncrypt(
                    clientPublicKey = pair.publicKey,
                    padding = false
                )
                val decryptedSecret = clientDecrypt(
                    encryptedSecret = encryptedSecret,
                    serverPublicKey = serverBase64PublicKey,
                    clientPrivateKey = pair.privateKey,
                    padding = true
                )
                Assertions.assertEquals(secret, decryptedSecret)
            }
        }
    }

    private fun serverEncrypt(clientPublicKey: ByteArray, padding: Boolean): Pair<String, String> {
        val serverDHKeyPair = DHUtil.initKey(clientPublicKey)
        val serverPublicKeyByteArray = serverDHKeyPair.publicKey
        val serverPrivateKeyByteArray = serverDHKeyPair.privateKey
        val serverBase64PublicKey = String(Base64.getEncoder().encode(serverPublicKeyByteArray))
        val encryptedSecret = DHUtil.encrypt(
            data = secret.toByteArray(),
            partAPublicKey = clientPublicKey,
            partBPrivateKey = serverPrivateKeyByteArray,
            padding = padding
        )
        return Pair(serverBase64PublicKey, String(Base64.getEncoder().encode(encryptedSecret)))
    }

    private fun clientDecrypt(
        encryptedSecret: String,
        serverPublicKey: String,
        clientPrivateKey: ByteArray,
        padding: Boolean
    ): String {
        val decoder = Base64.getDecoder()
        return String(
            DHUtil.decrypt(
                data = Base64.getDecoder().decode(encryptedSecret),
                partBPublicKey = decoder.decode(serverPublicKey),
                partAPrivateKey = clientPrivateKey,
                padding = padding
            )
        )
    }
}
