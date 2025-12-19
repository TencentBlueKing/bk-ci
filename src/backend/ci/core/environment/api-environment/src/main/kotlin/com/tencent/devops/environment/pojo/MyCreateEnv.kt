package com.tencent.devops.environment.pojo

/**
 * 创作环境相关内置环境
 */

// 我的创作环境
object MyCreateEnv {
    const val ENV_ID = -1L
    const val ENV_NAME_KEY = "myCreateEnv"
}

// 所有创作节点环境
object AllCreateNodeEnv {
    const val ENV_ID = -2L
    const val ENV_NAME_KEY = "allNodeCreateEnv"
}