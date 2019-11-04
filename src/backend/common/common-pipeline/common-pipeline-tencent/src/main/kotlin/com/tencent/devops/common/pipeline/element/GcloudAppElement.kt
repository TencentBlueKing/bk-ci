/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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

@ApiModel("GCloud-创建程序版本(IEG专用)", description = GcloudAppElement.classType)
data class GcloudAppElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "GCLOUD-创建APP原子",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("gcloud上传程序文件（newApp）、上传资源区分（newRes）", required = true)
    var type: String?,
    @ApiModelProperty("环境配置id", required = true)
    var configId: String = "",
    @ApiModelProperty("游戏ID", required = true)
    var gameId: String = "",
    @ApiModelProperty("accessId && accessKey 的ticket id", required = true)
    var ticketId: String = "",
    @ApiModelProperty("上传文件的accessId && accessKey 的ticket id", required = true)
    var fileTicketId: String?,
    @ApiModelProperty("渠道 ID", required = true)
    var productId: String = "",
    @ApiModelProperty("版本号，格式同 IPv4 点分十进制格式，如 3.3.3.3", required = true)
    var versionStr: String = "",
    @ApiModelProperty("需要对哪些历史版本进行差异更新，多个版本之间\n" +
            "用 | 分隔，例如 2.2.2.2|1.1.1.1, 默认不对任何历史\n" +
            "版本差异更新", required = false)
    var diffVersions: String?,
    @ApiModelProperty("进行差异更新时需指定，由 gcloud 提供", required = false)
    var regionId: String?,
    @ApiModelProperty("是否跳过上传，可选值为 0/1, 开启该选项之后该接\n" +
            "口退化成一个普通接口，即无需传递文件。由于 iOS\n" +
            "渠道 APP 包只能从 AppStore 下载，可以开启该选\n" +
            "项来省去文件上传。", required = false)
    var skipUpload: String?,
    @ApiModelProperty("文件的下载 HTTP 下载链接，如果指定了该参数，\n" +
            "则 gcloud 从该链接下载文件，以此代替业务上传。\n" +
            "由于该参数包含特殊字符，因此需 URL encode", required = false)
    var downloadLink: String?,
    @ApiModelProperty("CDN 是否使用 HTTPS, 可选值 0/1", required = false)
    var https: String?,
    @ApiModelProperty("文件路径，支持正则表达式(不支持逗号分隔多个文件)", required = true)
    var filePath: String = "",
    @ApiModelProperty("文件来源（PIPELINE-流水线仓库、CUSTOMIZE-自定义仓库）", required = true)
    var fileSource: String = "",
    @ApiModelProperty("版本标签(0: 测试版本， 1：审核版本）", required = false)
    var versionType: String?,
    @ApiModelProperty("普通用户可用", required = false)
    var normalUserCanUse: String?,
    @ApiModelProperty("灰度用户可用", required = false)
    var grayUserCanUse: String?,
    @ApiModelProperty("灰度规则 ID, 灰度用户可用时必须指定", required = false)
    var grayRuleId: String?,
    @ApiModelProperty("版本描述", required = false)
    var versionDes: String?,
    @ApiModelProperty("自定义字符串", required = false)
    var customStr: String?,
    @ApiModelProperty("对应的程序版本号（只在资源上传需要）", required = false)
    var appVersionStr: String?,
    @ApiModelProperty("自动diff版本数", required = false)
    var preVersionCount: String?
) : Element(name, id, status) {
    companion object {
        const val classType = "gcloudNewApp"
    }

    override fun getTaskAtom() = "gcloudNewAppTaskAtom"

    override fun getClassType() = classType
}
