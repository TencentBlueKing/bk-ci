package com.tencent.devops.process.pojo.trigger.artifact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 制品库 webhook 事件模型（基类）
 *
 * bkrepo webhook 消息体分四大类：项目事件 / 仓库事件 / 节点事件 / 包版本事件。
 * 本需求只关注「节点事件」与「包版本事件」两类：
 * - [NodeArtifactEvent]：节点体（二进制文件制品，含 `node` 字段）；
 * - [PackageVersionArtifactEvent]：包版本体（容器镜像等，含 `packageKey`/`packageVersion`）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class ArtifactEvent {
    @get:Schema(title = "事件类型（bkrepo 原始动作，如 NODE_CREATED / VERSION_CREATED）")
    abstract val eventType: String

    @get:Schema(title = "触发用户（bkrepo webhook payload 中的 user 对象，必传）")
    abstract val user: ArtifactEventUser

    /**
     * 获取 bkrepo 关联对象ID，用于推导事件源。
     *
     * 与 webhook 注册时的 associationType=PROJECT 对应，associationId 即 projectId：
     * - [NodeArtifactEvent]：取 `node.projectId:node.repoName`；
     * - [PackageVersionArtifactEvent]：取 `packageVersion.projectId:packageVersion.repoName`。
     */
    abstract fun getAssociationId(): String
}
