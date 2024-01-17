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
package com.tencent.devops.store.pojo.image.response

import com.tencent.devops.store.pojo.common.HonorInfo
import com.tencent.devops.store.pojo.common.index.StoreIndexInfo
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @Description
 * @Date 2019/9/17
 * @Version 1.0
 */
@Schema(title = "首页市场镜像")
data class MarketImageItem constructor(

    @Schema(title = "镜像Id", required = true)
    val id: String,

    @Schema(title = "镜像代码", required = true)
    val code: String,

    @Schema(title = "镜像名称", required = true)
    val name: String,

    @Schema(title = "研发来源")
    val rdType: String,

    @Schema(title = "镜像来源 BKDEVOPS:蓝盾，THIRD:第三方", required = true)
    val imageSourceType: String,

    @Schema(title = "镜像大小（MB字符串）", required = true)
    val imageSize: String,

    @Schema(title = "镜像大小数值（字节）", required = true)
    val imageSizeNum: Long,

    @Schema(title = "所属镜像分类代码", required = true)
    val classifyCode: String,

    @Schema(title = "镜像logo", required = false)
    val logoUrl: String? = null,

    @Schema(title = "版本号", required = true)
    val version: String,

    @Schema(title = "镜像简介", required = false)
    val summary: String? = null,

    @Schema(title = "星级评分", required = true)
    val score: Double,

    @Schema(title = "下载量", required = true)
    val downloads: Int,

    @Schema(title = "是否为公共镜像 true：是 false：否", required = true)
    val publicFlag: Boolean,

    @Schema(title = "是否可安装 true：可以 false：不可以", required = true)
    val flag: Boolean,

    @Schema(title = "是否推荐标识 true：推荐，false：不推荐", required = true)
    val recommendFlag: Boolean,

    @Schema(title = "发布者", required = false)
    val publisher: String? = null,

    @Schema(title = "发布时间", required = false)
    val pubTime: Long? = null,

    @Schema(title = "创建人", required = true)
    val creator: String,

    @Schema(title = "修改人", required = true)
    val modifier: String,

    @Schema(title = "创建时间", required = true)
    val createTime: Long,

    @Schema(title = "修改时间", required = true)
    val updateTime: Long,

    @Schema(title = "是否已安装", required = true)
    var installedFlag: Boolean?,

    @Schema(title = "荣誉信息", required = false)
    val honorInfos: List<HonorInfo>? = null,

    @Schema(title = "指标信息列表")
    val indexInfos: List<StoreIndexInfo>? = null,

    @Schema(title = "hotFlag")
    val hotFlag: Boolean? = null
) {
    constructor(instance: MarketImageItem) : this(
        instance.id,
        instance.code,
        instance.name,
        instance.rdType,
        instance.imageSourceType,
        instance.imageSize,
        instance.imageSizeNum,
        instance.classifyCode,
        instance.logoUrl,
        instance.version,
        instance.summary,
        instance.score,
        instance.downloads,
        instance.publicFlag,
        instance.flag,
        instance.recommendFlag,
        instance.publisher,
        instance.pubTime,
        instance.creator,
        instance.modifier,
        instance.createTime,
        instance.updateTime,
        instance.installedFlag,
        instance.honorInfos,
        instance.indexInfos,
        instance.hotFlag
    )
}
