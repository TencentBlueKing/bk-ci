package com.tencent.devops.common.api.digest.enc

import org.bouncycastle.jcajce.provider.asymmetric.dh.KeyAgreementSpi
import java.math.BigInteger

/**
 * bcprov 1.46以前,不会对生成的密钥进行填充,但是bcprov 1.5以后会进行填充,导致加解密时会报错
 * 详情查看: https://github.com/bcgit/bc-java/issues/413
 * 为了能够兼容性升级,这里重写bigIntToBytes方法,如果客户端使用的旧版本,则使用旧版本的bigIntToBytes方法
 */
class KeyAgreementNoPaddingSpi : KeyAgreementSpi() {
    override fun bigIntToBytes(r: BigInteger): ByteArray {
        val tmp = r.toByteArray()
        if (tmp[0].toInt() == 0) {
            val ntmp = ByteArray(tmp.size - 1)
            System.arraycopy(tmp, 1, ntmp, 0, ntmp.size)
            return ntmp
        }
        return tmp
    }
}
