package com.tencent.devops.store.pojo.common

import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import com.tencent.devops.store.pojo.common.label.Label
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 服务范围详情（用于接口返回报文展示）
 * 与 ServiceScopeConfig（输入模型）不同，本类包含分类名称和标签对象列表等可直接展示的信息。
 */
@Schema(title = "服务范围详情")
data class ServiceScopeDetail(
    @get:Schema(title = "服务范围", required = true)
    val serviceScope: ServiceScopeEnum,

    @get:Schema(title = "所属插件分类代码", required = true)
    val classifyCode: String,

    @get:Schema(title = "所属插件分类名称", required = true)
    val classifyName: String,

    @get:Schema(title = "Job类型配置列表（包含 jobType 及其操作系统信息）", required = false)
    val jobTypeConfigs: List<JobTypeConfig>? = null,

    @get:Schema(title = "适用Job类型列表", required = false)
    @Deprecated("使用 jobTypeConfigs 替代")
    val jobTypes: List<JobTypeEnum>? = null,

    @get:Schema(title = "插件标签列表", required = false)
    val labelList: List<Label>? = null
)
