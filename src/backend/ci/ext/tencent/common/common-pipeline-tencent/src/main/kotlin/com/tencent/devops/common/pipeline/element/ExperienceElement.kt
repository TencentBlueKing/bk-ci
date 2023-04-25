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

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验", description = ExperienceElement.classType)
data class ExperienceElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "转体验",
    @ApiModelProperty("id", required = false, hidden = true)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("路径", required = true)
    val path: String = "",
    @ApiModelProperty("是否自定义仓库", required = true)
    val customized: Boolean,
    @ApiModelProperty("时间类型(ABSOLUTE, RELATIVE)", required = true)
    val timeType: String = "ABSOLUTE",
    @ApiModelProperty("结束时间(s)或者结束天数(day)", required = true)
    val expireDate: Long,
    @ApiModelProperty("体验组", required = true)
    val experienceGroups: Set<String> = setOf(),
    @ApiModelProperty("内部名单", required = true)
    val innerUsers: Set<String> = setOf(),
    @ApiModelProperty("外部名单", required = true)
    val outerUsers: String = "",
    @ApiModelProperty("通知类型(RTX,WECHAT,EMAIL)", required = true)
    val notifyTypes: Set<String> = setOf(),
    @ApiModelProperty("是否开启企业微信群通知", required = true)
    val enableGroupId: Boolean? = true,
    @ApiModelProperty("企业微信群ID(逗号分隔)", required = true)
    val groupId: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "experience"
    }

    override fun getTaskAtom() = "experienceTaskAtom"

    override fun getClassType() = classType
}
