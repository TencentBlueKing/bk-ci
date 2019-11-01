package com.tencent.devops.common.pipeline.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("分发至海外蓝鲸", description = JobCloudsFastPushElement.classType)
data class JobCloudsFastPushElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "混合云版-作业平台-构件分发",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("源类型(CUSTOMIZE, PIPELINE,REMOTE)", required = false)
    val srcType: String = "",
    @ApiModelProperty("源路径", required = false)
    var srcPath: String = "",
    @ApiModelProperty("源节点id", required = false)
    var srcNodeId: String = "",
    @ApiModelProperty("源服务器账户", required = false)
    var srcAccount: String = "",
    @ApiModelProperty("上传最长时间（单位：分钟）,默认10小时", required = false)
    var maxRunningMins: Int = 600,
    @ApiModelProperty("文件上传的目标路径", required = true)
    var targetPath: String = "",
    @ApiModelProperty("目标业务ID", required = true)
    var targetAppId: Int,
    @ApiModelProperty("openState的值", required = true)
    var openState: String = "",
    @ApiModelProperty("目标服务器-指定IP(格式:云区域ID:IP1)，多个ip之间用逗号分隔。示例:1:10.0.0.1,1:10.0.0.2", required = true)
    var ipList: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "jobCloudsFastPush"
    }

    override fun getTaskAtom() = "jobCloudsFastPushTaskAtom"

    override fun getClassType() = classType
}
