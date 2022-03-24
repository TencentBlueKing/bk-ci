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

import com.tencent.devops.common.pipeline.enums.TclsType
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@Deprecated("作废，由其他团队负责")
@ApiModel("TCLS-升级版本", description = TclsAddVersionElement.classType)
data class TclsAddVersionElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "TCLS-升级版本",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("TCLS业务ID", required = true)
    val tclsAppId: String?,
    @ApiModelProperty("是否属于MTCLS业务", required = false)
    val mtclsApp: TclsType?,
    @ApiModelProperty("业务ServiceID", required = false)
    val serviceId: String? = null,
    @ApiModelProperty("凭证ID", required = true)
    val ticketId: String = "",
    @ApiModelProperty("执行环境", required = true)
    val envId: String = "",
    @ApiModelProperty("当前版本号", required = true)
    val versionFrom: String = "",
    @ApiModelProperty("升级版本号", required = true)
    val versionTo: String = "",
    @ApiModelProperty("升级包描述", required = true)
    val desc: String = "",
    @ApiModelProperty("升级包名称", required = true)
    val pkgName: String = "",
    @ApiModelProperty("升级包下载URL", required = true)
    val httpUrl: String = "",
    @ApiModelProperty("升级包Hash值", required = true)
    val fileHash: String = "",
    @ApiModelProperty("升级包大小", required = true)
    val size: String = "",
    @ApiModelProperty("升级策略", required = true)
    val updateWay: String = "",
    @ApiModelProperty("完整性校验hash文件URL", required = false)
    val hashUrl: String = "",
    @ApiModelProperty("完整性校验hash文件MD5", required = false)
    val hashMd5: String = "",
    @ApiModelProperty("版本升级包自定义字段", required = false)
    val customStr: String = "",
    @ApiModelProperty("升级包类型(手游)", required = false)
    val updatePkgType: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "tclsAddVersion"
    }

    override fun getTaskAtom(): String {
        return "tclsAddVersionTaskAtom"
    }

    override fun getClassType() = classType
}
