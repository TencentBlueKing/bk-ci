/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.pipeline.element

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.pipeline.element.bcs.BcsNamespaceVar
import com.tencent.devops.common.pipeline.element.bcs.KeyValue
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("BCS容器部署-研发、测试", description = BcsContainerOpElement.classType)
data class BcsContainerOpElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "执行job作业",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("关联CC业务Id", required = true)
    val ccAppId: String = "",
    @ApiModelProperty("操作类型(包括：create,recreate,scale,rollingupdate,delete)", required = true)
    val opType: String = "",
        // 其他操作参数
    @ApiModelProperty("对象类型(包含k8s: DaemonSet/Job/Deployment/StatefulSet/,mesos: Application/Deployment)", required = false)
    val category: String?,
    @ApiModelProperty("超时时间(minutes)", required = false)
    val timeout: Int = 8,

        // 创建类参数
    @ApiModelProperty("集群ID", required = false)
    val clusterId: String?,
    @ApiModelProperty("模板ID", required = false)
    val musterId: String?,
    @ApiModelProperty("版本ID", required = false)
    val versionId: String?,
    @ApiModelProperty("展示的版本ID", required = false)
    val showVersionId: String?,
    @ApiModelProperty("展示版本名称", required = false)
    val showVersionName: String?,
    @ApiModelProperty("模板详细信息", required = false)
    val instanceEntity: String?,
    @ApiModelProperty("模板详细信息", required = false)
    val namespaceVar: List<BcsNamespaceVar>?,

    @ApiModelProperty("应用实例名称", required = false)
    val bcsAppInstName: String?,
    @ApiModelProperty("应用实例ID", required = false)
    val bcsInstNum: Int?,
    @ApiModelProperty("应用实例名称", required = false)
    val instVersionId: String?,
    @ApiModelProperty("应用实例ID", required = false)
    val bcsAppInstId: String?,

        // 公共参数
    @ApiModelProperty("命名空间以及变量", required = false)
    val instVar: List<KeyValue>?

) : Element(name, id, status) {
    companion object {
        const val classType = "bcsContainerOp"
    }

    override fun getTaskAtom(): String = "bcsContainerOpAtom"

    override fun getClassType() = classType

    private fun getObjectStr(namespaceVar1: List<Any>?): String {
        if (null == namespaceVar1) {
            return ""
        }
        return ObjectMapper().writeValueAsString(namespaceVar1)
    }
}

// fun main(array: Array<String>) {
//    val distributionElement = BcsContainerOpElement("myworld", null,
//             "stop", "/data", "opType","cate",7,null,null,null,
//            null,"ve",null, listOf(),"app"
//    ,3,"5","fff", listOf())
//
//    println(distributionElement.genTaskParams())
//    println(JsonUtil.toMutableMap(distributionElement))
//
//    val vmBuildContainer = VMBuildContainer("vv", listOf(), "stop", 0, 0, 0, VMBaseOS.LINUX
//            , setOf("ff"), 12, 33, null, null, "vv",
//            "tpagenEvnId", "dir", null, null
//    )
//
//    println(vmBuildContainer.genTaskParams())
//    println(JsonUtil.toMap(vmBuildContainer))
//
// }
