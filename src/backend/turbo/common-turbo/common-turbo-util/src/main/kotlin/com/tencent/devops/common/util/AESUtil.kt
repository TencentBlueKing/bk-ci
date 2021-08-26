package com.tencent.devops.common.util

import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PKCS7Padding
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import java.security.Security
import java.util.Base64
import javax.crypto.KeyGenerator

object AESUtil {

    private const val UTF8 = "UTF-8"
    private const val AES = "AES"
    private const val SEED = 256

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    private fun generateKeyParameter(key: String): KeyParameter {
        val keyGenerator: KeyGenerator = KeyGenerator.getInstance(AES)
        val secureRandom: SecureRandom = SecureRandom.getInstance("SHA1PRNG")
        secureRandom.setSeed(key.toByteArray(charset(UTF8)))

        keyGenerator.init(SEED, secureRandom)
        val secretKey = keyGenerator.generateKey()
        val encoded = secretKey.encoded
        return KeyParameter(encoded)
    }

    private fun processData(encrypt: Boolean, keyParameter: KeyParameter, bytes: ByteArray): ByteArray {
        val blockCipherPadding = PKCS7Padding()
        val blockCipher = CBCBlockCipher(AESEngine())
        val paddedBufferedBlockCipher = PaddedBufferedBlockCipher(blockCipher, blockCipherPadding)
        paddedBufferedBlockCipher.init(encrypt, keyParameter)

        val output = ByteArray(paddedBufferedBlockCipher.getOutputSize(bytes.size))
        val offset = paddedBufferedBlockCipher.processBytes(bytes, 0, bytes.size, output, 0)
        val outputLength = paddedBufferedBlockCipher.doFinal(output, offset)
        return output.copyOf(offset + outputLength)
    }

    fun encrypt(key: String, content: String): String {
        val bytes = content.toByteArray(charset(UTF8))
        val keyParameter = generateKeyParameter(key)
        val output = processData(true, keyParameter, bytes)
        return Base64.getEncoder().encodeToString(output)
    }

    fun decrypt(key: String, content: String): String {
        val bytes = Base64.getDecoder().decode(content)
        val keyParameter = generateKeyParameter(key)
        val output = processData(false, keyParameter, bytes)
        return output.toString(charset(UTF8))
    }

    fun encrypt(key: String, content: ByteArray): ByteArray {
        val keyParameter = generateKeyParameter(key)
        return processData(true, keyParameter, content)
    }

    fun decrypt(key: String, content: ByteArray): ByteArray {
        val keyParameter = generateKeyParameter(key)
        return processData(false, keyParameter, content)
    }
}
