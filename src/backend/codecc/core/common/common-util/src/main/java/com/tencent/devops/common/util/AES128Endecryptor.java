package com.tencent.devops.common.util;

import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class AES128Endecryptor {
    private static Logger logger = LoggerFactory.getLogger(AES128Endecryptor.class);
    /**
     * 算法名称
     */
    public static final String KEY_ALGORITHM = "AES";

    /**
     * 算法名称/加密模式/填充方式
     * AES共有四种工作模式-->>ECB：电子密码本模式、CBC：加密分组链接模式、CFB：加密反馈模式、OFB：输出反馈模式
     */
    public static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5PADDING";

    /**
     * AES加密
     *
     * @param key
     * @param initVector
     * @param value
     * @return
     */
    public static String encrypt(String key, String initVector, String value) {
        String encryptStr;

        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(StandardCharsets.UTF_8.name()));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8.name()), KEY_ALGORITHM);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());

            encryptStr = Base64.encodeBase64String(encrypted);
        } catch (Exception e) {
            logger.error("encrypt fail.", e);
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }

        return encryptStr;
    }

    /**
     * AES解密
     *
     * @param key
     * @param initVector
     * @param encrypted
     * @return
     */
    public static String decrypt(String key, String initVector, String encrypted)
    {
        String decryptStr;
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(StandardCharsets.UTF_8.name()));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8.name()), KEY_ALGORITHM);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));

            decryptStr = new String(original);
        } catch (Exception e) {
            logger.error("decrypt fail.", e);
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }

        return decryptStr;
    }

    /**
     * AES解密
     *
     * @param key
     * @param initVector
     * @param encrypted
     * @return
     */
    public static byte[] decryptAndReturnByte(String key, String initVector, String encrypted)
    {
        byte[] encryptedByte = Base64.decodeBase64(encrypted);
        return decryptAndReturnByte(key, initVector, encryptedByte);
    }

    /**
     * AES解密
     *
     * @param key
     * @param initVector
     * @param encrypted
     * @return
     */
    public static byte[] decryptAndReturnByte(String key, String initVector, byte[] encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(StandardCharsets.UTF_8.name()));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8.name()), KEY_ALGORITHM);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            logger.error("decrypt fail.", e);
            throw new CodeCCException(CommonMessageCode.SYSTEM_ERROR);
        }
    }
}
