package com.tencent.devops.remotedev.service.redis

object RedisKeys {
    const val REDIS_CALL_LIMIT_KEY_PREFIX = "remotedev:callLimit"

    // 基础配置
    const val CLIENT_VERSION_LIMIT = "remotedev:clientVersionLimit" // 客户端版本限制
    const val CLIENT_VERSION_WARNING = "remotedev:clientVersionWarning" // 客户端版本warning，会发邮件
    const val PIPELINE_CONFIG_INFO = "remotedev:assignWorkspace.pipelineinfo" // L盘挂载流水线
    const val PIPELINE_EXPORT_CONFIG_INFO = "remotedev:createExpSupport.pipelineinfo" // 专家协助流水线
    const val PIPELINE_QUERY_CGS_PWD = "remotedev:queryCgsPwd.pipelineinfo" // 查询cgs密码流水线
    const val REDIS_REMOTEDEV_PUBLIC_IPS = "remotedev:public:ips" // 云桌面公网ip，可能会动态变化所以放redis里

    // 选填项
    const val REDIS_1PASSWORD_EXPIRED_SECOND = "remotedev:1passwordExpiredSecond" // 一次性密钥过期时间，默认5秒
    const val REDIS_CLIENT_INSTALL_URL = "remotedev:clientInstallUrl" // 蓝盾客户端更新地址
    const val REDIS_CLIENT_VERSION_CHECK = "remotedev:clientVersionCheck" // 是否校验蓝盾客户端版本

    // 保存人脸识别错误的校验错误码，;进行分割
    const val REMOTEDEV_USER_FACE_RECOGNITION_ERROR_CODE_KEY = "remotedev:user_face_recognition:error_code"
    const val REMOTEDEV_WORKSPACE_USER_APPROVAL_EXPIRED_DAYS = "remotedev:worksapce.user.approval.expiredDays"

    const val REDIS_WORKING_ON_WEEKEND_DAY = "remotedev:holiday:workingDays" // 调休上班时间
    const val REDIS_HOLIDAY = "remotedev:holiday:holidays" // 休假时间
}
