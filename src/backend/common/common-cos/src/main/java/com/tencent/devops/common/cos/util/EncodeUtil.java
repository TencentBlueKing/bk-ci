package com.tencent.devops.common.cos.util;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

public class EncodeUtil {
    private static final String MAC_NAME = "HmacSHA1";
    private static final String ENCODING = "UTF-8";

    public static String SHA1Encrypt(final String encryptText) throws Exception {
        if(encryptText == null) {
            return null;
        }
        return SHA1Encrypt(encryptText.getBytes());
    }

    public static String SHA1Encrypt(final byte[] bytes) throws Exception {
        if(bytes == null)
            return null;

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(bytes);
        byte messageDigest[] = digest.digest();
        // Create Hex String
        StringBuilder hexString = new StringBuilder();
        // 字节数组转换为 十六进制 数
        for (byte aMessageDigest : messageDigest) {
            String shaHex = Integer.toHexString(aMessageDigest & 0xFF);
            if (shaHex.length() < 2) {
                hexString.append(0);
            }
            hexString.append(shaHex);
        }
        return hexString.toString();
    }

    /**
     * 使用 HMAC-SHA1 签名方法对对encryptText进行签名
     * @param encryptText 被签名的字符串
     * @param encryptKey 密钥
     * @return 返回被加密后的字符串
     * @throws Exception 异常
     */
    public static String HmacSHA1Encrypt(final String encryptText, final String encryptKey ) throws Exception {
        byte[] data = encryptKey.getBytes( ENCODING );
        // 根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
        SecretKey secretKey = new SecretKeySpec( data, MAC_NAME );
        // 生成一个指定 Mac 算法 的 Mac 对象
        Mac mac = Mac.getInstance( MAC_NAME );
        // 用给定密钥初始化 Mac 对象
        mac.init( secretKey );
        byte[] text = encryptText.getBytes( ENCODING );
        // 完成 Mac 操作
        byte[] digest = mac.doFinal( text );
        StringBuilder sBuilder = bytesToHexString( digest );
        return sBuilder.toString();
    }

    /**
     * 转换成Hex
     * @param bytesArray 字节集合
     */
    public static StringBuilder bytesToHexString(final byte[] bytesArray ){
        if (bytesArray == null){
            return null;
        }
        StringBuilder sBuilder = new StringBuilder();
        for ( byte b : bytesArray ){
            String hv = String.format("%02x", b);
            sBuilder.append( hv );
        }
        return sBuilder;
    }

}
