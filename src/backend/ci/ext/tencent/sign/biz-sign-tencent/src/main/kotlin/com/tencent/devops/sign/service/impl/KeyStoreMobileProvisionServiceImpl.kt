package com.tencent.devops.sign.service.impl

import com.dd.plist.NSArray
import com.dd.plist.NSDictionary
import com.dd.plist.NSString
import com.dd.plist.PropertyListParser
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.sign.Constants.KEYSTORE_CATEGORY_PROVISION
import com.tencent.devops.sign.Constants.KEYSTORE_HTTP_HEADER_AUTH
import com.tencent.devops.sign.Constants.KEYSTORE_HTTP_HEADER_IP
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.MobileProvisionService
import com.tencent.devops.sign.utils.EncryptUtil
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

@Service
class KeyStoreMobileProvisionServiceImpl @Autowired constructor() : MobileProvisionService {
    @Value("\${keystore.url:}")
    private val keyStoreUrl = "https://proxy.test.keystore.oa.com"

    @Value("\${keystore.certListUrl:}")
    private val keyStoreCertListUrl = "/api/auth/getCertList?appId=%s"

    @Value("\${keystore.certUrl:}")
    private val keyStoreCertUrl = "/api/auth/getCert?id=%s&category=%s"

    @Value("\${keystore.authId:}")
    private val keyStoreAuthId = "devops"

    @Value("\${keystore.authSecret:}")
    private val keyStoreAuthSecret = "a21c218df41f6d7fd032535fe20394e2"

    @Value("\${bkci.sign.keyChainGroups:}")
    private val keyChainGroups: String = ""

    @Value("\${bkci.sign.wildcardMobileProvisionId:}")
    private val wildcardMobileProvisionId = ""

    private val TEAM_IDENTIFIER_KEY = "com.apple.developer.team-identifier"

    private val KEYCHAIN_ACCESS_GROUPS_KEY = "keychain-access-groups"

    companion object {
        private val logger = LoggerFactory.getLogger(KeyStoreMobileProvisionServiceImpl::class.java)
        private val pairKey = DHUtil.initKey()
        private val privateKey = pairKey.privateKey
        private val publicKey = String(Base64.getEncoder().encode(pairKey.publicKey))
        private var token: String? = null
        private val teamIdentifier = "com.apple.developer.team-identifier"
    }

    override fun downloadMobileProvision(mobileProvisionDir: File, projectId: String, mobileProvisionId: String): File {
        // 从keystore下载文件
        val mobileProvisionFile = File("${mobileProvisionDir.canonicalPath}/$mobileProvisionId.mobileprovision")
        val url = String.format(keyStoreUrl + keyStoreCertUrl, mobileProvisionId, KEYSTORE_CATEGORY_PROVISION)
        val headers = mutableMapOf<String, String>()
        headers[KEYSTORE_HTTP_HEADER_AUTH] = token ?: ""
        headers[KEYSTORE_HTTP_HEADER_IP] = InetAddress.getLocalHost().hostAddress
        logger.info("Keystore download mobileprovision url:$url")
        logger.info("Keystore download mobileprovision header:$headers")
        OkhttpUtils.doGet(url, headers).use { resp ->
            if (resp.code() != 200 || resp.body() == null) {
                throw ErrorCodeException(
                        errorCode = SignMessageCode.ERROR_MP_NOT_EXIST,
                        defaultMessage = "KeyStore描述文件($mobileProvisionId)不存在或者下载失败。"
                )
            }
            val decryptedMobileProvisionEncrypt = resp.body()!!.bytes()
            logger.info("Keystore decrypt decryptedMobileProvisionEncrypt:$decryptedMobileProvisionEncrypt")
            logger.info("Keystore decrypt keyStoreAuthSecret:$keyStoreAuthSecret")
            val decryptedMobileProvisionDecrypt = EncryptUtil.decrypt(decryptedMobileProvisionEncrypt, keyStoreAuthSecret)
            mobileProvisionFile.writeBytes(decryptedMobileProvisionDecrypt)
        }
        return mobileProvisionFile
    }

    override fun handleEntitlement(entitlementFile: File) {
        val rootDict = PropertyListParser.parse(entitlementFile) as NSDictionary

        // 处理keychain-access-groups中无用的com.apple.token
        if (rootDict.containsKey(KEYCHAIN_ACCESS_GROUPS_KEY)) {
            val keychainArray = (rootDict.objectForKey(KEYCHAIN_ACCESS_GROUPS_KEY) as NSArray).array.withIndex()
            for((index,e) in keychainArray){
                if(e.toString() == "com.apple.token") {
                    val removeKeyChainGroupCMD = "plutil -remove keychain-access-groups.$index ${entitlementFile.canonicalPath}"
                    CommandLineUtils.execute(removeKeyChainGroupCMD, entitlementFile.parentFile, true)
                    break
                }
            }
        }

        if (keyChainGroups.isNullOrBlank()) {
            return
        }
        val keyChainGroupsList = keyChainGroups.split(";")
        // 解析entitlement文件
        try {

            // entitlement
            if (rootDict.containsKey(TEAM_IDENTIFIER_KEY) && rootDict.containsKey(KEYCHAIN_ACCESS_GROUPS_KEY)) {
                val teamId = (rootDict.objectForKey(TEAM_IDENTIFIER_KEY) as NSString).toString()
                if (!teamId.isNullOrBlank() && keyChainGroupsList.isNotEmpty()) {
                    keyChainGroupsList.forEach {
                        if (it.isNotBlank()) {
                            val insertKeyChainGroupCMD = "plutil -insert keychain-access-groups.0 -string '$teamId.$it' ${entitlementFile.canonicalPath}"
                            CommandLineUtils.execute(insertKeyChainGroupCMD, entitlementFile.parentFile, true)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("插入entitlement文件(${entitlementFile.canonicalPath})的keychain-access-groups失败。")
            throw ErrorCodeException(
                    errorCode = SignMessageCode.ERROR_INSERT_KEYCHAIN_GROUPS,
                    defaultMessage = "entitlement插入keychain失败"
            )
        }
    }

    override fun downloadWildcardMobileProvision(mobileProvisionDir: File, ipaSignInfo: IpaSignInfo): File? {
        return downloadMobileProvision(
                mobileProvisionDir = mobileProvisionDir,
                projectId = ipaSignInfo.projectId,
                mobileProvisionId = wildcardMobileProvisionId
        )
    }

    private fun generateToken() {
        val claims = mutableMapOf<String, Any>()
        claims["authId"] = keyStoreAuthId
        claims["timeMillis"] = Instant.now().epochSecond
        val key = Keys.hmacShaKeyFor(keyStoreAuthSecret.toByteArray(StandardCharsets.UTF_8))
        token = Jwts.builder().setClaims(claims)
                .signWith(SignatureAlgorithm.HS256, key).compact()
    }

    /*
    * keystore的jwt有效期为3分钟，设置为
    * */
    @Scheduled(fixedDelay = 1 * 60 * 1000)
    fun refreshToken() {
        logger.info("Refresh keystore jwt token")
        generateToken()
    }
}
//
// fun main(args: Array<String>) {
//    val keyStoreAuthId = "devops"
//    val keyStoreAuthSecret = "a21c218df41f6d7fd032535fe20394e2"
//    val mobileProvisionDir  = File("/data/enterprise_sign_tmp/freyzheng/11111111/222222222/test.ipa.mobileProvisionDir")
//    val mobileProvisionId = "16722ba2-6fd1-45ef-a009-f94b20bc0d4f"
//    val keyStoreUrl = "https://proxy.test.keystore.oa.com"
//    val keyStoreCertUrl = "/api/auth/getCert?id=%s&category=%s"
//
//
//    val claims = mutableMapOf<String, Any>()
//    claims["authId"] = keyStoreAuthId
//    claims["timeMillis"] = Instant.now().epochSecond
//    val key = Keys.hmacShaKeyFor(keyStoreAuthSecret.toByteArray(StandardCharsets.UTF_8))
//    val token = Jwts.builder().setClaims(claims)
//            .signWith(key, SignatureAlgorithm.HS256).compact()
//    // 从keystore下载文件
//    val mobileProvisionFile = File("${mobileProvisionDir.canonicalPath}/$mobileProvisionId.mobileprovision")
//    val url = String.format(keyStoreUrl + keyStoreCertUrl, mobileProvisionId, KEYSTORE_CATEGORY_PROVISION)
//    val request = Request.Builder()
//            .url(url)
//            .addHeader(KEYSTORE_HTTP_HEADER_AUTH, token)
//            .addHeader(KEYSTORE_HTTP_HEADER_IP, InetAddress.getLocalHost().hostAddress)
//            .build()
//    val headers = mutableMapOf<String, String>()
//    headers[KEYSTORE_HTTP_HEADER_AUTH] = token ?:""
//    headers[KEYSTORE_HTTP_HEADER_IP] = InetAddress.getLocalHost().hostAddress
//    OkhttpUtils.doGet(url, headers).use { resp ->
//        if(resp.code() != 200 || resp.body() == null) {
//            throw ErrorCodeException(
//                    errorCode = SignMessageCode.ERROR_MP_NOT_EXIST,
//                    defaultMessage = "KeyStore描述文件不存在或者下载失败。"
//            )
//        }
//        val decryptedMobileProvisionEncrypt = resp.body()!!.bytes()
//        val decryptedMobileProvisionDecrypt = EncryptUtil.decrypt(decryptedMobileProvisionEncrypt ,keyStoreAuthSecret)
//        mobileProvisionFile.writeBytes(decryptedMobileProvisionDecrypt)
//    }
//    val a = 1
// }