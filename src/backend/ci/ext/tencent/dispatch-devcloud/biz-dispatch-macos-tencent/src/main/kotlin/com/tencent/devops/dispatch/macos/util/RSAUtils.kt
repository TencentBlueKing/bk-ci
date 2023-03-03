/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.macos.util

import org.jolokia.util.Base64Util
import java.io.ByteArrayOutputStream
import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.Cipher

object RSAUtils {
    val transformation = "RSA"
    val ENCRYPT_MAX_SIZE = 117
    val DECRYPT_MAX_SIZE = 256
    /**
     * 私钥加密
     */
    fun encryptByPrivateKey(str: String, privateKey: PrivateKey): String {
        val byteArray = str.toByteArray()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, privateKey)

        // 定义缓冲区
        var temp: ByteArray? = null
        // 当前偏移量
        var offset = 0

        val outputStream = ByteArrayOutputStream()

        while (byteArray.size - offset > 0) {
            // 剩余的部分大于最大加密字段，则加密117个字节的最大长度
            if (byteArray.size - offset >= ENCRYPT_MAX_SIZE) {
                temp = cipher.doFinal(byteArray, offset, ENCRYPT_MAX_SIZE)
                // 偏移量增加117
                offset += ENCRYPT_MAX_SIZE
            } else {
                // 如果剩余的字节数小于117，则加密剩余的全部
                temp = cipher.doFinal(byteArray, offset, (byteArray.size - offset))
                offset = byteArray.size
            }
            outputStream.write(temp)
        }
        outputStream.close()
        return Base64Util.encode(outputStream.toByteArray())
    }

    /**
     * 公钥加密
     */
    fun encryptByPublicKey(str: String, publicKey: PublicKey): String {
        val byteArray = str.toByteArray()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        var temp: ByteArray? = null
        var offset = 0

        val outputStream = ByteArrayOutputStream()

        while (byteArray.size - offset > 0) {
            if (byteArray.size - offset >= ENCRYPT_MAX_SIZE) {
                temp = cipher.doFinal(byteArray, offset, ENCRYPT_MAX_SIZE)
                offset += ENCRYPT_MAX_SIZE
            } else {
                temp = cipher.doFinal(byteArray, offset, (byteArray.size - offset))
                offset = byteArray.size
            }
            outputStream.write(temp)
        }

        outputStream.close()
        return Base64Util.encode(outputStream.toByteArray())
    }

    /**
     * 私钥解密
     * 注意Exception in thread "main" javax.crypto.IllegalBlockSizeException:
     * Data must not be longer than 256 bytes
     * 关于到底是128个字节还是256个，我也很迷糊了，我写成128的时候就报这个错误，改成256后就没事了
     */
    fun decryptByPrivateKey(str: String, privateKey: PrivateKey): String {
        val byteArray = Base64Util.decode(str)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)

        // 定义缓冲区
        var temp: ByteArray? = null
        // 当前偏移量
        var offset = 0

        val outputStream = ByteArrayOutputStream()

        while (byteArray.size - offset > 0) {
            // 剩余的部分大于最大解密字段，则加密限制的最大长度
            if (byteArray.size - offset >= DECRYPT_MAX_SIZE) {
                temp = cipher.doFinal(byteArray, offset, DECRYPT_MAX_SIZE)
                // 偏移量增加128
                offset += DECRYPT_MAX_SIZE
            } else {
                // 如果剩余的字节数小于最大长度，则解密剩余的全部
                temp = cipher.doFinal(byteArray, offset, (byteArray.size - offset))
                offset = byteArray.size
            }
            outputStream.write(temp)
        }
        outputStream.close()
        return String(outputStream.toByteArray())
    }

    /**
     * 公钥解密
     */
    fun decryptByPublicKey(str: String, publicKey: PublicKey): String {
        val byteArray = Base64Util.decode(str)
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, publicKey)

        var temp: ByteArray? = null
        var offset = 0

        val outputStream = ByteArrayOutputStream()

        while (byteArray.size - offset > 0) {
            if (byteArray.size - offset >= DECRYPT_MAX_SIZE) {
                temp = cipher.doFinal(byteArray, offset, DECRYPT_MAX_SIZE)
                offset += DECRYPT_MAX_SIZE
            } else {
                temp = cipher.doFinal(byteArray, offset, (byteArray.size - offset))
                offset = byteArray.size
            }
            outputStream.write(temp)
        }
        outputStream.close()
        return String(outputStream.toByteArray())
    }
}
//
// fun main(args: Array<String>) {
//    val devcloudPassword =
//        "ivtR7OeVw68+dWDRsHZkTWjXhaWV0Cfb6AmoW1ZcwtGB3+Y9h3b1myO7t/HAu62TrInSjmlsJxyurXVKENSFoGwAbWkoO9FWjx0W5uzHN/oepY4Cb2xSRl3XsJ8ZJ4Cm4Jfu+nKYCwWNUo/xV5xxOAU5vVd1ctGfRa/q7Y8ccVpVVli1D0GNcmrbsdg+ugcZU88kroDY4MIFJGOl/UWRgqf103F/gfgqg00gc9PJPGSx2xtQEhybEAqlpGIBvXuDwbM69IpogDIcRnUukupUAnpyJF5HdlyPpzRI2tBoiaTQMzhqX6e2+orCYrulIZdXojOKUbko+e1LWw9iXoThjw=="
//    val rsaPrivateKey = """MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDJuYCLdzQ+a7EJ
// EO84f6cSIj6jhEhyBFdA+TFpp2EUy8pdqd/XHRZCYVSNKe4JWWzkgHviQABoqenN
// d8kQJW/SbExxZH5CX5DznvzUC3uapVn76XTNW43rNd9odW6O3a7KiHblASbKtpl9
// oe5CJ+Klbc8CjsYB8eFIKMmsJm9U+INH1MC/a0K7WIoZL5XPhEf/AaClbo3n3zK+
// 4IG4T+kSHTuAQ+UoYG24jmsMmYMPv3ZlBf9p2zWRpLSOaf/77A5aekAx4N6fCavh
// F7ft2g4sclwSYm26AMbWfYR748d6C62qfkWdhZL4puTkbmZyRZtgY1B9mXafKhDr
// hT+xUq9bAgMBAAECggEBAJxMkg/9tcojsRFAQMp7Bh1j0TrrNSK6wEyEiEe/u9Xc
// LRkzlU9cpzGZrmSON3ShCGFoOBTO2EJAXPqptG1BRIo7BWPapIWS/IH3DxjRKN1i
// /vzUH386UBUOMeMDEj26AEPHRrAbm033+6e/PxedRdw93+awbzV0j8RUbiZnxV2E
// qblyQ/JoRO1hGO6UA+HrDswUPq+dD3nJGTky7md6xUxSaCvJ/igcf3GhsM6UBcVK
// SzKHtDOF6Mx3xjZEyVM4YxN087jLS0NS7Q9Ms544nj1wXUOvgzVdLvefWYhltdSx
// uHJF23G3Vw4vQV9T1zIO8pvBfLawhD4WvYXDllBS8UkCgYEA/9r8uNyEgJfnb+iQ
// tGLmenexvrqQKAWdom1DQe/rIzK9rulH8A+3Gr1AvEcACeSXsMY+RthMNo1PYyFl
// whApqNsIRaVSE4JE5/7fAjYRHAO/DFapQAc95Y0r6wLjCGT7sL8e4TNvUg8vGulY
// IuVC/v4fAhi12WkXwlGQcN+g+Y8CgYEAydavKGKcROkWO8r4VXQkw5YMSbEDqK8R
// PH+CyJSgil4oPzpJQh/Ksh5HxuYfbt4H5wflHS9OHGv823c3Jww+Ls8VvpEuQ6+L
// aBfAvR/LQz7VnCbCdI/EPAiXBQDMXVORk+0XGWZTvDkqrVjym6p2Ls8xJsCTrqQ6
// NQIoVMaqz3UCgYBUFoHGs9rsdIBA3FtqnbKH/3ywZZppPOdA8c4QBqzuBYgny020
// TUXtiNOUzXTqy3E7TPw8xmvN5gV4XAqYjD1U3J/MW/gspzzETpS2olRfM6//ex/n
// BjpGjm4nr+wz3xSFU1rvwrrMamcd3iD0Xwr0yUoho0No78w6NhpVrnm2awKBgHJf
// jhfzcRv3fRra1EgQLXkLgOENRsaCleDTXLfuyN4/pszVaYfn3gN69y91XaCEfrPj
// HGiiUr/2TW3Lq1wcKux5epfnSvEd+4A73YMODi+H3qHCsPQ+N3PRZkp2flfwMBFE
// C4/gFVkWh+F9AtVFDf1OeUq9W7Jd64H4PAR2I3q1AoGAdk8ImlF+/aL2Gy9WJkxi
// AY86s4dRV7n+ZAKFxlOqwHDkWleGw4v4xlFhgk/rPq2kHKVaw1ai5leNNXd6sn4f
// pZqEPhHE+ioLjXb1zOpiQfRuA84Fed8JNJNY5S7pyf5OdIuqYu9wfVYYne5HVxGv
// +GYPeYy6sPaY45J7l/nTSDs=
// """
//    val keyFactory = KeyFactory.getInstance("RSA")
//    val tmp1 = Base64Util.decode(rsaPrivateKey.replace("\n",""))
//    val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(tmp1))
//    val passwordDevcloud = RSAUtils.decryptByPrivateKey(devcloudPassword, privateKey)
//    System.out.println(passwordDevcloud)
// }
