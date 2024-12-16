package com.tencent.devops.remotedev.pojo.userinfo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "校验是否需要管控信息")
data class UserInfoCheckData(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "工作空间名称")
    val workspaceName: String
)

@Schema(title = "校验结果")
data class UserInfoCheckResult(
    @get:Schema(title = "MOA管控检验")
    val moa: UserInfoMoaCheckConfig,
    @JsonProperty("face_recognition")
    @get:Schema(title = "人脸识别管控检验")
    val faceRecognition: FaceRecognition
) {
    companion object {
        fun noCheck(): UserInfoCheckResult = UserInfoCheckResult(
            UserInfoMoaCheckConfig(false),
            FaceRecognition(0, "", false)
        )
    }
}

data class UserInfoMoaCheckConfig(
    @get:Schema(title = "功能开关")
    val switch: Boolean
)

data class FaceRecognition(
    @get:Schema(title = "频率的值")
    val frequency: Int,
    @JsonProperty("frequency_unit")
    @get:Schema(title = "频率的单位：H 表示小时")
    val frequencyUnit: String,
    @get:Schema(title = "功能开关")
    val switch: Boolean
)

@Schema(title = "人脸识别数据")
data class FaceRecognitionData(
    @get:Schema(title = "用户名称")
    val username: String,
    @get:Schema(title = "人脸的base64数据")
    val base64FaceData: String
)

@Schema(title = "人脸识别结果")
data class FaceRecognitionResult(
    // 误识率千分之一对应分数为40分，误识率万分之一对应分数为50分，误识率十万分之一对应分数为60分。 一般超过50分则可认定为同一人
    @get:Schema(title = "误识率十万分之一对应分数为60分。 一般超过50分则可认定为同一人")
    val score: Float,
    // true：通过人脸验证；false：不通过。依据分数（score字段）判断是否通过人脸对比验证，是否为同一人判断，固定阈值分数为60分，若想更灵活地调整阈值可取score参数返回进行判断
    @get:Schema(title = "依据分数（score字段）判断是否通过人脸对比验证，是否为同一人判断，固定阈值分数为60分")
    @JsonProperty("face_matched")
    val faceMatched: Boolean
) {
    companion object {
        fun noCheck(): FaceRecognitionResult = FaceRecognitionResult(100f, true)
    }
}

@Schema(title = "校验权限中心权限信息")
data class UserInfoAuthCheck(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "用户ID")
    val userId: String
)