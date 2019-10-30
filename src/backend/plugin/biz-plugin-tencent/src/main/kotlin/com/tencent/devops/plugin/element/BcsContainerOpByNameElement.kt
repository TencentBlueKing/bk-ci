package com.tencent.devops.plugin.element

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.pipeline.pojo.bcs.BcsNamespaceVar
import com.tencent.devops.common.pipeline.pojo.bcs.KeyValue
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("BCS容器部署-研发、测试（2.0）", description = BcsContainerOpByNameElement.classType)
data class BcsContainerOpByNameElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "执行job作业",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("关联CC业务Id", required = true)
    val ccAppId: String = "",
    @ApiModelProperty("操作类型(包括：create,recreate,scale,rollingupdate,delete,signal,command)", required = true)
    val opType: String = "",
        // 其他操作参数
    @ApiModelProperty("对象类型(包含k8s: DaemonSet/Job/Deployment/StatefulSet/,mesos: Application/Deployment)", required = false)
    val category: String?,
    @ApiModelProperty("超时时间(minutes),默认8分钟", required = false)
    val timeout: Int = 8,

        // 创建类参数
    @ApiModelProperty("集群ID", required = false)
    val clusterId: String?,
    @ApiModelProperty("模板集名称", required = false)
    val musterName: String?,
    @ApiModelProperty("模板名称", required = false)
    val templateName: String?,
    @ApiModelProperty("模板集版本名称", required = false)
    val versionName: String?,

    @ApiModelProperty("模板集展示版本名称", required = false)
    val showVersionName: String?,
    @ApiModelProperty("命名空间变量", required = false)
    val namespaceVar: List<BcsNamespaceVar>?,

    @ApiModelProperty("应用实例名称", required = false)
    val bcsAppInstName: String?,
    @ApiModelProperty("命名空间", required = false)
    val namespace: String?,
    @ApiModelProperty("应用实例个数", required = false)
    val bcsInstNum: String?,
    @ApiModelProperty("应用实例版本名称", required = false)
    val instVersionName: String?,

        // 信号参数
    @ApiModelProperty("进程名称", required = false)
    val processName: String?,
    @ApiModelProperty("信号(整数)", required = false)
    val signal: String?,

    // 命令参数
    @ApiModelProperty("环境变量", required = false)
    val env: List<KeyValue>?,
    @ApiModelProperty("命令， 例如 ps", required = false)
    val command: String?,
    @ApiModelProperty("命令参数， 例如 -a", required = false)
    val commandParam: String?,
    @ApiModelProperty("用户，默认为root", required = false)
    val username: String?,
    @ApiModelProperty("工作目录", required = false)
    val workDir: String?,
    @ApiModelProperty("特权，默认是false", required = false)
    val privileged: Boolean = false,
    @ApiModelProperty("任务信息保存时间, 默认为 24607 m", required = false)
    val reserveTime: String?,

        // 公共参数
    @ApiModelProperty("命名空间以及变量", required = false)
    val instVar: List<KeyValue>?

) : Element(name, id, status) {
    companion object {
        const val classType = "bcsContainerOpByName"
    }

    override fun getTaskAtom(): String = "bcsContainerOpByNameAtom"

    override fun getClassType() = classType

    private fun getObjectStr(namespaceVar1: List<Any>?): String {
        if (null == namespaceVar1) {
            return ""
        }
        return ObjectMapper().writeValueAsString(namespaceVar1)
    }
}
