package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.pojo.userinfo.CheckType
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionData
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionNoPassType
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionResult
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckData
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckResult
import com.tencent.devops.remotedev.service.client.TaiClient
import com.tencent.devops.remotedev.service.client.TaiUserInfoRequest
import com.tencentcloudapi.common.Credential
import com.tencentcloudapi.common.exception.TencentCloudSDKException
import com.tencentcloudapi.iai.v20180301.IaiClient
import com.tencentcloudapi.iai.v20180301.models.CreatePersonRequest
import com.tencentcloudapi.iai.v20180301.models.GetPersonBaseInfoRequest
import com.tencentcloudapi.iai.v20180301.models.VerifyPersonRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class UserInfoCertService @Autowired constructor(
    private val taiClient: TaiClient
) {
    @Value("\${tcloud.apiSecretId:}")
    val secretId = ""

    @Value("\${tcloud.apiSecretKey:}")
    val secretKey = ""

    fun needRealNameCert(username: String): Boolean {
        val taiInfo = taiClient.taiUserInfo(TaiUserInfoRequest(setOf(username))).filter { it.username == username }
        // TODO: 校验是否进行了实名认证
        return false
    }

    /**
     * 人脸=no & moa=no 直接通过
     * 人脸=no & moa=yes moa 二次认证
     * 人脸=yes & moa=yes, 人脸=yes & moa=no 内部员工打开，CP人脸
     */
    fun multipleCert(data: UserInfoCheckData): UserInfoCheckResult {
        // TODO: 调用 wesec 判断这个实例使用什么方式认证
        val face: Boolean = true
        val moa: Boolean = false

        if (face) {
            // TODO: 判断是否是内部人员，目前先用@tai，未来确认下
            if (!data.username.endsWith("@tai")) {
                return UserInfoCheckResult(false, null)
            }
            return UserInfoCheckResult(true, CheckType.FACE_RECOGNITION)
        } else {
            if (moa) {
                return UserInfoCheckResult(true, CheckType.MOA_DOUBLE)
            }
            return UserInfoCheckResult(false, null)
        }
    }

    fun faceRecognition(data: FaceRecognitionData): FaceRecognitionResult {
        // TODO：获取人员信息需要创建人员ID和人员库，目前先按有人员库，人员 ID为固定的用户名
        val personRepo = ""
        val personId = ""
        // TODO: 创建需要地域，目前先试试 internal 地域
        val client = buildIaiClient()
        // TODO: 看看这里的人员ID要不要存一下，因为后面都是直接用人员ID做对比了
        val personInfo = try {
            client.GetPersonBaseInfo(GetPersonBaseInfoRequest().apply { this.personId = personId })
        } catch (e: TencentCloudSDKException) {
            // TODO: 如果没有找到用户 ID对应的用户会抛出异常和错误吗，需要测试下，先按没有找到算
            null
        }
        // TODO: 腾讯云的接口是否都要抓一下异常换成自己的，或者打个日志
        if (personInfo != null) {
            // TODO: 这里需要确认下校验精度等各种参数
            val verifyInfo = try {
                client.VerifyPerson(VerifyPersonRequest().apply {
                    this.personId = personId
                    this.image = data.base64FaceData
                })
            } catch (e: TencentCloudSDKException) {
                logger.warn("faceRecognition|VerifyPerson|$data|$e")
                return FaceRecognitionResult(false, FaceRecognitionNoPassType.NO_PASS, e.toString())
            }
            logger.info("faceRecognition|$data|$verifyInfo")
            return if (verifyInfo.isMatch) {
                FaceRecognitionResult.pass()
            } else {
                FaceRecognitionResult(false, FaceRecognitionNoPassType.NO_PASS, null)
            }
        }
        // TODO: 这里需要确认下校验精度等各种参数
        try {
            client.CreatePerson(CreatePersonRequest().apply {
                this.groupId = personRepo
                this.personName = data.username
                this.personId = personId
                this.image = data.base64FaceData
            })
        } catch (e: TencentCloudSDKException) {
            logger.warn("faceRecognition|CreatePerson|$data|$e")
            return FaceRecognitionResult(false, FaceRecognitionNoPassType.REUPLOAD_AVATAR, e.toString())
        }
        return FaceRecognitionResult.pass()
    }

    private fun buildIaiClient(): IaiClient {
        val cred = Credential(secretId, secretKey)
//        val profile = HttpProfile().apply {
//            this.endpoint = TCLOUD_DOMAIN
//        }
        return IaiClient(
            cred,
            IAI_TCLOUD_DOMAIN
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserInfoCertService::class.java)
        private const val IAI_TCLOUD_DOMAIN = "iai.internal.tencentcloudapi.com"
    }
}