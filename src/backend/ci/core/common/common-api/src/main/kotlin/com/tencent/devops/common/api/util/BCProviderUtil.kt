package com.tencent.devops.common.api.util

import com.tencent.devops.common.api.digest.enc.KeyAgreementNoPaddingSpi
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

object BCProviderUtil {

    const val PACKAGE_NAME = "com.tencent.devops.common.api.digest.enc."

    init {
        val provider = BouncyCastleProvider()
        provider.addAlgorithm("KeyAgreement.DHNP", PACKAGE_NAME + KeyAgreementNoPaddingSpi::class.simpleName)
        Security.addProvider(provider)
    }
}
