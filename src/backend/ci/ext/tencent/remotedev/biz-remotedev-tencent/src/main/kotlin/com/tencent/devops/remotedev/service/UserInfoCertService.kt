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
import com.tencentcloudapi.common.profile.ClientProfile
import com.tencentcloudapi.common.profile.HttpProfile
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

    @Value("\${tcloud.personGroupId}")
    val personGroupId = ""

    fun needRealNameCert(username: String): Boolean {
        val taiInfo = taiClient.taiUserInfo(TaiUserInfoRequest(setOf(username))).filter { it.username == username }
        // TODO: 校验是否进行了实名认证，接口暂无等待接口
        return false
    }

    /**
     * 人脸=no & moa=no 直接通过
     * 人脸=no & moa=yes moa 二次认证
     * 人脸=yes & moa=yes, 人脸=yes & moa=no 内部员工打开，CP人脸
     */
    fun multipleCert(data: UserInfoCheckData): UserInfoCheckResult {
        // TODO: 调用 wesec 判断这个实例使用什么方式认证，接口暂无等待接口
        val face: Boolean = true
        val moa: Boolean = false

        if (face) {
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
        val client = buildIaiClient()
        val personInfo = try {
            client.GetPersonBaseInfo(GetPersonBaseInfoRequest().apply { this.personId = data.username })
        } catch (e: TencentCloudSDKException) {
            // TODO: 如果没有找到用户 ID对应的用户会抛出异常和错误吗，需要测试下，先按没有找到算
            null
        }
        if (personInfo != null) {
            val verifyInfo = try {
                client.VerifyPerson(VerifyPersonRequest().apply {
                    this.personId = data.username
                    this.image = data.base64FaceData
                    this.qualityControl = 3
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
        try {
            client.CreatePerson(CreatePersonRequest().apply {
                this.groupId = personGroupId
                this.personName = data.username
                this.personId = data.username
                this.image = data.base64FaceData
                this.qualityControl = 3
            })
        } catch (e: TencentCloudSDKException) {
            logger.warn("faceRecognition|CreatePerson|$data|$e")
            return FaceRecognitionResult(false, FaceRecognitionNoPassType.REUPLOAD_AVATAR, e.toString())
        }
        return FaceRecognitionResult.pass()
    }

    private fun buildIaiClient(): IaiClient {
        val cred = Credential(secretId, secretKey)
        val profile = HttpProfile().apply {
            this.endpoint = IAI_TCLOUD_DOMAIN
        }
        return IaiClient(
            cred,
            IAI_TCLOUD_REGION,
            ClientProfile().apply { this.httpProfile = profile }
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserInfoCertService::class.java)
        private const val IAI_TCLOUD_REGION = "iai.tencentcloudapi.com"
        private const val IAI_TCLOUD_DOMAIN = "iai.internal.tencentcloudapi.com"
    }
}