package com.tencent.devops.process.pojo.trigger.artifact

/**
 * 制品到达触发常量
 */
object ArtifactWebhookConstant {

    // bkrepo 内置仓库名
    const val REPO_PIPELINE = "pipeline"
    const val REPO_CUSTOM = "custom"
    const val REPO_IMAGE = "image"

    // 目录归档完成哨兵文件：bkrepo 目录逐个文件上传后，最后上传该文件表示目录归档完成
    const val SENTINEL_FILE = ".bkci_pipeline"

    // 制品节点元数据中记录生产流水线的 key（bkrepo 归档时写入）
    const val METADATA_PIPELINE_ID = "pipelineId"
    const val METADATA_BUILD_ID = "buildId"
    // 归档目录节点可能只写 bk_ci_bid，不写 buildId
    const val METADATA_BK_CI_BID = "bk_ci_bid"
    const val METADATA_BUILD_NO = "buildNo"
    const val METADATA_SIZE = "size"
    // 镜像 digest 在包版本元数据中的 key
    const val METADATA_DOCKER_MANIFEST_DIGEST = "docker.manifest.digest"

    // 制品库 packageKey 协议前缀，输出镜像名时去掉
    const val DOCKER_PACKAGE_KEY_PREFIX = "docker://"

    // 注册 webhook 时下发、bkrepo 回调时带回的密钥请求头，用于校验回调来源
    const val HEADER_BKREPO_WEBHOOK_SECRET = "X-BKREPO-WEBHOOK-SECRET"
}
