package com.tencent.devops.remotedev.utils

import org.junit.jupiter.api.Test
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64

class RsaUtilTest {
    @Test
    fun test() {
        val publicKey = "LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUlHZk1BMEdDU3FHU0liM0RRRUJBUVVBQTRHTkFEQ0JpUUtCZ1FEcFAxUkFTTzBoV2N1MXNHdzJkbkVZNlU4cgplUVVIaXdrNVVhWWNyVDRzMGdyL21XMWJaaWZqSVZmL2p4YnFDMFFNT0dxY3RXNUlYRGo4TDZWWTFjYnppeHNOClkxOXp4MFNvbVdzak9FMXkvTVE1QlVwdjZYdmUrM256enhSbHQya1F0OHhldHlEc3Uzbi9FcndQcnNQZEdOTk8KeUtNUkdlNnMvQ0RJMWJoNEh3SURBUUFCCi0tLS0tRU5EIFBVQkxJQyBLRVktLS0tLQo="
        val text = "噢~我是加密的哟"
        val privateKey = "LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tCk1JSUNkd0lCQURBTkJna3Foa2lHOXcwQkFRRUZBQVNDQW1Fd2dnSmRBZ0VBQW9HQkFPay9WRUJJN1NGWnk3V3cKYkRaMmNSanBUeXQ1QlFlTENUbFJwaHl0UGl6U0N2K1piVnRtSitNaFYvK1BGdW9MUkF3NGFweTFia2hjT1B3dgpwVmpWeHZPTEd3MWpYM1BIUktpWmF5TTRUWEw4eERrRlNtL3BlOTc3ZWZQUEZHVzNhUkMzekY2M0lPeTdlZjhTCnZBK3V3OTBZMDA3SW94RVo3cXo4SU1qVnVIZ2ZBZ01CQUFFQ2dZRUFpUGNiTGpTa3FyVGtIbWplNG94aWxWSkwKbnllTmdJUndnaXdqOHlyNEc3R3JxN1FZdDFjaGpRcURkaG84Um1zZkpsM3FuT01kUTNpRUxmSjhrZEhXNVFWMwpWL1BxeUI2blpHL3djZVM3WFBDamRwdHZHaUdaaUFzZTBkZ0pDUmRLNEFHN0xxdkl3bkU5QU5TQVFMUDQvN0RCCk0wdGZSQU9MMG9FWVdhTllyZ0VDUVFEdTJ2SlRHUnZLQUVEZEN5a2ljNTFBejI4cjQwY0dZNFlTUG1jTUhGUy8KU2tUU1pMZzd2M0ZxOTRiME1BQzRZaTVpTTNaMUFhWkc3MG9SbmdWL0ZTMUJBa0VBK2YxVjdTajN2S3BFdVRxdgpyaVkrUjJIQlVMUTFHSUFpejgxd0NEWXdobER1ZE50QzY0TTdwMTBFY1QwdTFuNUo3M3NTWWUzSm8xZks3Q1E3ClNwaHRYd0pCQUxxb1VYek0vMkZ4cHo3V2JQUG1ZN3AxSUl5c2xTR1Ivd0VjMFF5dXl5K2VDNEJiZzNuMWx0MmYKeUUvbGYzcVlCMlZva0NiSi9qWXE0N2cyeEZiV3BzRUNRRWVLWTBPNmZLTW1Td0tETS9GdmlsVWRPWmhoNmV2NApCMzVXZVdBd09kVEdabWRVdENMMzdHTnA4REtENHRxSlM1bFlMQnVRVkNzRm5kSFVVSTk1YlpzQ1FEMmJtYXNCCmFXcUpvRitzRnMzS3JLL1ozeDEwZUIvblhEeWVpeEpNMjZyWEJUa2lXZXQzbkZzb2hCZXQ1NGxMUnIxampaa0oKb1ZwMlJTOXVIM1VJQUJzPQotLS0tLUVORCBQUklWQVRFIEtFWS0tLS0tCg=="

        val rsaPublicKey: RSAPublicKey = RsaUtil.generatePublicKey(Base64.getDecoder().decode(publicKey))
        val rsaPrivateKey: RSAPrivateKey = RsaUtil.generatePrivateKey(Base64.getDecoder().decode(privateKey))

        val encryptText = RsaUtil.rsaEncrypt(text, rsaPublicKey)
        System.out.printf("【%s】经过【RSA】加密后：%s\n", text, encryptText)

        val decryptText = RsaUtil.rsaDecrypt(encryptText, rsaPrivateKey)
        System.out.printf("【%s】经过【RSA】解密后：%s\n", encryptText, decryptText)

        val signature = RsaUtil.sign(text, rsaPrivateKey, "MD5")
        System.out.printf("【%s】经过【RSA】签名后：%s\n", text, signature)

        val result = RsaUtil.verify(text, rsaPublicKey, signature, "MD5")
        System.out.printf("【%s】的签名【%s】经过【RSA】验证后结果是：$result", text, signature)
    }
}
