package com.tencent.bk.codecc.task.pojo

import com.tencent.devops.common.constant.ComConstants
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线触发请求体")
data class TriggerPipelineReq(
    @ApiModelProperty("仓库路径")
    val gitUrl: String?,
    @ApiModelProperty("分支")
    val branch: String?,
    @ApiModelProperty("工蜂项目id")
    val gongfengProjectId: Int?,
    @ApiModelProperty("逻辑仓标识")
    val logicRepo: String? = null,
    @ApiModelProperty("仓库类型")
    val repoType: String?,
    @ApiModelProperty("是否显示告警")
    val defectDisplay: Boolean?,
    @ApiModelProperty("是否校验工蜂项目")
    val checkGongfengProject: Boolean? = false,
    @ApiModelProperty("是否采用yml文件进行流水线编排")
    val useYml: Boolean? = false,
    @ApiModelProperty("指定开源扫描规则集模型")
    val checkerSetRange: List<OpenSourceCheckerSetModel>?,
    @ApiModelProperty("指定扫描集群")
    val codeccDispatchRoute: ComConstants.CodeCCDispatchRoute?,
    @ApiModelProperty("codecc流水线模型")
    val codeCCPipelineReq: CodeCCPipelineReq
)
