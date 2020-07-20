package com.tencent.devops.sign.api.pojo

/**
 * 事件动作
 * @version 1.0
 */
enum class SignArchiveType {
    PIPELINE, // 流水线仓库
    CUSTOM, // 自定义仓库
    OTHERS // 其他
    ;

    companion object {
        fun isInner(signArchiveType: SignArchiveType) = PIPELINE == signArchiveType || CUSTOM == signArchiveType
    }
}