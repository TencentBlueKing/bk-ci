package com.tencent.devops.process.pojo

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @Description
 * @Date 2019/9/19
 * @Version 1.0
 */
@ApiModel("带构建变量的历史构建模型")
data class BuildHistoryWithVars(
    @ApiModelProperty("构建ID", required = true)
    val id: String,
    @ApiModelProperty("启动用户", required = true)
    val userId: String,
    @ApiModelProperty("触发条件", required = true)
    val trigger: String,
    @ApiModelProperty("构建号", required = true)
    val buildNum: Int?,
    @ApiModelProperty("编排文件版本号", required = true)
    val pipelineVersion: Int,
    @ApiModelProperty("开始时间", required = true)
    val startTime: Long,
    @ApiModelProperty("结束时间", required = true)
    val endTime: Long?,
    @ApiModelProperty("状态", required = true)
    val status: String,
    @ApiModelProperty("结束原因", required = true)
    val deleteReason: String?,
    @ApiModelProperty("服务器当前时间戳", required = true)
    val currentTimestamp: Long,
    @ApiModelProperty("是否是手机启动", required = false)
    val isMobileStart: Boolean = false,
    @ApiModelProperty("原材料", required = false)
    val material: List<PipelineBuildMaterial>?,
    @ApiModelProperty("排队于", required = false)
    val queueTime: Long?,
    @ApiModelProperty("构件列表", required = false)
    val artifactList: List<FileInfo>?,
    @ApiModelProperty("备注", required = false)
    val remark: String?,
    @ApiModelProperty("总耗时(秒)", required = false)
    val totalTime: Long?,
    @ApiModelProperty("运行耗时(秒，不包括人工审核时间)", required = false)
    val executeTime: Long?,
    @ApiModelProperty("启动参数", required = false)
    val buildParameters: List<BuildParameters>?,
    @ApiModelProperty("WebHookType", required = false)
    val webHookType: String?,
    @ApiModelProperty("启动类型(新)", required = false)
    val startType: String?,
    @ApiModelProperty("推荐版本号", required = false)
    val recommendVersion: String?,
    @ApiModelProperty("构建变量集合", required = true)
    val variables: Map<String, String>
)