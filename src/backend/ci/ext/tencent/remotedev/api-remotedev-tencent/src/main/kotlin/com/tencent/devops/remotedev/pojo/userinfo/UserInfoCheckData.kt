package com.tencent.devops.remotedev.pojo.userinfo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "校验是否需要管控信息")
data class UserInfoCheckData(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "工作空间名称")
    val workspaceName: String,
    @get:Schema(title = "用户名称")
    val username: String
)

@Schema(title = "校验结果")
data class UserInfoCheckResult(
    @get:Schema(title = "是否需要管控校验")
    val needCheck: Boolean,
    @get:Schema(title = "管控校验的类型")
    val checkType: CheckType?
)

enum class CheckType {
    MOA_DOUBLE,
    FACE_RECOGNITION
}

@Schema(title = "人脸识别数据")
data class FaceRecognitionData(
    @get:Schema(title = "用户名称")
    val username: String,
    @get:Schema(title = "人脸的base64数据")
    val base64FaceData: String
) {
    // 打日志不打印 base64，太大了
    fun toLog(): String = username
}

@Schema(title = "人脸识别结果")
data class FaceRecognitionResult(
    @get:Schema(title = "验证是否通过")
    val pass: Boolean,
    @get:Schema(title = "未通过类型")
    val noPassType: FaceRecognitionNoPassType?,
    @get:Schema(title = "未通过时的错误信息")
    val errMsg: String?
) {
    companion object {
        fun pass() = FaceRecognitionResult(true, null, null)
    }
}

enum class FaceRecognitionNoPassType {
    REUPLOAD_AVATAR,
    NO_PASS
}