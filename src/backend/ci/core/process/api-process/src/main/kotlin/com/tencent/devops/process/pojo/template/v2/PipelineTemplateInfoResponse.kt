package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.common.pipeline.template.UpgradeStrategyEnum
import com.tencent.devops.process.pojo.PipelinePermissions
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.pojo.template.TemplateType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板基础信息")
data class PipelineTemplateInfoResponse(
    /*基本信息相关*/
    @get:Schema(title = "模板ID", required = true)
    val id: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "模板名称", required = true)
    val name: String,
    @get:Schema(title = "简介", required = true)
    val desc: String?,
    @get:Schema(title = "公共/约束/自定义模式", required = true)
    val mode: TemplateType,
    @get:Schema(title = "发布策略-研发商店", required = true)
    val publishStrategy: UpgradeStrategyEnum? = null,
    @get:Schema(title = "应用范畴", required = true)
    val category: String? = null,
    @get:Schema(title = "模板类型", required = true)
    val type: PipelineTemplateType,
    @get:Schema(title = "logo地址", required = false)
    val logoUrl: String? = null,
    @get:Schema(title = "是否开启PAC", required = true)
    val enablePac: Boolean,
    @get:Schema(title = "是否已经上架研发商店", required = true)
    val storeFlag: Boolean,
    @get:Schema(title = "发布标识", required = false)
    val publishFlag: Boolean? = false,
    @get:Schema(title = "父模板ID", required = false)
    val srcTemplateId: String? = null,
    @get:Schema(title = "父模板项目ID", required = false)
    val srcTemplateProjectId: String? = null,
    @get:Schema(title = "能否调试", required = true)
    val canDebug: Boolean? = null,
    @get:Schema(title = "调试流水线数", required = true)
    val debugPipelineCount: Int? = 0,
    @get:Schema(title = "实例流水线数", required = true)
    val instancePipelineCount: Int? = 0,
    @get:Schema(title = "创建人", required = true)
    val creator: String,
    @get:Schema(title = "更新人", required = false)
    val updater: String? = null,
    @get:Schema(title = "创建时间", required = false)
    val createTime: Long? = null,
    @get:Schema(title = "更新时间", required = false)
    val updateTime: Long? = null,
    /*权限相关*/
    @get:Schema(title = "权限", required = false)
    val permissions: PipelinePermissions? = null,
    /*版本状态相关*/
    @get:Schema(title = "是否可以发布")
    val canRelease: Boolean,
    @get:Schema(title = "用于前端交互的版本号")
    val version: Long?,
    @get:Schema(title = "用于前端交互的版本名称")
    val versionName: String?,
    @get:Schema(title = "草稿的基准版本（存在草稿才有值）", required = false)
    val baseVersion: Long?,
    @get:Schema(title = "草稿的基准版本的状态（存在草稿才有值）", required = false)
    val baseVersionStatus: VersionStatus?,
    @get:Schema(title = "基准版本的版本名称")
    val baseVersionName: String?,
    @get:Schema(title = "最新的发布版本，如果为空则说明没有过发布版本")
    val releaseVersion: Long?,
    @get:Schema(title = "最新的发布版本名称，如果为空则说明没有过发布版本")
    val releaseVersionName: String?,
    @get:Schema(title = "最新流水线版本状态（如有任何发布版本则为发布版本）", required = false)
    var latestVersionStatus: VersionStatus? = VersionStatus.RELEASED,
    @get:Schema(title = "PAC配置", required = false)
    val pipelineAsCodeSettings: PipelineAsCodeSettings?,
    @get:Schema(title = "流水线YAML信息", required = false)
    val yamlInfo: PipelineYamlVo?,
    @get:Schema(title = "yaml文件在默认分支是否存在", required = false)
    var yamlExist: Boolean? = false,
    @get:Schema(title = "流水线模版研发商店相关", required = false)
    val pipelineTemplateMarketRelatedInfo: PipelineTemplateMarketRelatedInfo?
)
