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

@ApiModel("GCloud-puffer-动态资源更新(IEG专用)", description = GcloudPufferElement.classType)
data class GcloudPufferElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "GCLOUD-创建APP原子",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("环境配置id", required = true)
    var configId: String,
    @ApiModelProperty("游戏ID", required = true)
    var gameId: String,
    @ApiModelProperty("accessId && accessKey 的ticket id", required = true)
    var ticketId: String,
    @ApiModelProperty("上传文件的accessId && accessKey 的ticket id", required = false)
    var fileTicketId: String?,
    @ApiModelProperty("渠道 ID", required = true)
    var productId: String,
    @ApiModelProperty("资源版本号，格式同 IPv4 点分十进制格式，如 3.3.3.3", required = true)
    var resourceVersion: String,
    @ApiModelProperty("资源名称", required = true)
    var resourceName: String,
    @ApiModelProperty("CDN 是否使用 HTTPS, 可选值 0/1", required = false)
    var https: String?,
    @ApiModelProperty("文件路径，支持正则表达式(不支持逗号分隔多个文件)", required = true)
    var filePath: String,
    @ApiModelProperty("文件来源（PIPELINE-流水线仓库、CUSTOMIZE-自定义仓库）", required = true)
    var fileSource: String,
    @ApiModelProperty("版本标签(0: 不可用，1：正式版本, 2：审核版本）", required = true)
    var versionType: String,
    @ApiModelProperty("版本描述", required = false)
    var versionDes: String?,
    @ApiModelProperty("自定义字符串", required = false)
    var customStr: String?
) : Element(name, id, status) {
    companion object {
        const val classType = "gcloudPuffer"
    }

    override fun getTaskAtom() = "gcloudPufferTaskAtom"

    override fun getClassType() = classType
}
