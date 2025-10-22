/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.store.pojo.common.honor.HonorInfo
import com.tencent.devops.store.pojo.common.label.Label
import com.tencent.devops.store.pojo.common.index.StoreIndexInfo
import com.tencent.devops.store.pojo.common.comment.StoreUserCommentInfo
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

/**
 * @Description
 * @Date 2019/9/17
 * @Version 1.0
 */
@Schema(title = "镜像详情")
data class ImageDetail(

    @get:Schema(title = "镜像ID", required = true)
    val imageId: String,

    @get:Schema(title = "镜像ID（兼容多种解析方式）", required = true)
    val id: String,

    @get:Schema(title = "镜像代码", required = true)
    val imageCode: String,

    @get:Schema(title = "镜像代码（兼容多种解析方式）", required = true)
    val code: String,

    @get:Schema(title = "镜像名称", required = true)
    val imageName: String,

    @get:Schema(title = "镜像名称（兼容多种解析方式）", required = true)
    val name: String,

    @get:Schema(title = "研发来源")
    val rdType: String,

    @get:Schema(title = "权重")
    val weight: Int?,

    @get:Schema(title = "镜像适用的Agent类型")
    var agentTypeScope: List<ImageAgentTypeEnum>,

    @get:Schema(title = "镜像logo", required = true)
    val logoUrl: String,

    @get:Schema(title = "镜像图标", required = true)
    val icon: String,

    @get:Schema(title = "镜像简介", required = true)
    val summary: String,

    @get:Schema(title = "镜像说明文档链接", required = false)
    val docsLink: String?,

    @get:Schema(title = "镜像调试项目Code", required = true)
    val projectCode: String,

    @get:Schema(title = "星级评分", required = true)
    val score: Double,

    @get:Schema(title = "下载量", required = true)
    val downloads: Int,

    @get:Schema(title = "所属镜像分类ID", required = true)
    val classifyId: String,

    @get:Schema(title = "所属镜像分类代码", required = true)
    val classifyCode: String,

    @get:Schema(title = "所属镜像分类名称", required = true)
    val classifyName: String,

    @get:Schema(title = "镜像来源 BKDEVOPS:蓝盾，THIRD:第三方", required = true)
    val imageSourceType: String,

    @get:Schema(title = "镜像仓库Url", required = true)
    val imageRepoUrl: String,

    @get:Schema(title = "镜像仓库名称", required = true)
    val imageRepoName: String,

    @get:Schema(title = "凭证Id", required = true)
    val ticketId: String,

    @get:Schema(title = "镜像tag", required = true)
    val imageTag: String,

    @get:Schema(title = "镜像大小（MB字符串）", required = true)
    val imageSize: String,

    @get:Schema(title = "镜像大小数值（字节）", required = true)
    val imageSizeNum: Long,

    @get:Schema(title =
        "镜像状态，INIT：初始化|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGED：已下架",
        required = true
    )
    val imageStatus: String,

    @get:Schema(title = "镜像描述", required = true)
    val description: String,

    @get:Schema(title = "dockerFile类型", required = true)
    val dockerFileType: String,

    @get:Schema(title = "dockerFile内容", required = true)
    val dockerFileContent: String,

    @get:Schema(title = "Label数组", required = true)
    val labelList: List<Label>,

    @get:Schema(title = "范畴code", required = true)
    val category: String,

    @get:Schema(title = "范畴名称", required = true)
    val categoryName: String,

    @get:Schema(title = "是否为最新版本镜像 true：最新 false：非最新", required = true)
    val latestFlag: Boolean,

    @get:Schema(title = "发布者", required = true)
    val publisher: String,

    @get:Schema(title = "发布时间", required = false)
    val pubTime: Long? = null,

    @get:Schema(title = "是否为公共镜像 true：是 false：否", required = true)
    val publicFlag: Boolean,

    @get:Schema(title = "是否可安装 true：可以 false：不可以", required = true)
    val flag: Boolean,

    @get:Schema(title = "是否有处于上架状态的版本 true：可以 false：不可以", required = true)
    val releaseFlag: Boolean,

    @get:Schema(title = "是否推荐 true：推荐 false：不推荐", required = true)
    val recommendFlag: Boolean,

    @get:Schema(title = "是否官方认证 true：是 false：否", required = true)
    val certificationFlag: Boolean,

    @get:Schema(title = "CommentInfo数组", required = true)
    val userCommentInfo: StoreUserCommentInfo,

    @get:Schema(title = "版本号", required = true)
    val version: String,

    @get:Schema(title =
        "发布类型，NEW：新上架 INCOMPATIBILITY_UPGRADE：非兼容性升级 COMPATIBILITY_UPGRADE：兼容性功能更新 COMPATIBILITY_FIX：兼容性问题修正",
        required = true
    )
    val releaseType: String,

    @get:Schema(title = "版本日志内容", required = true)
    val versionContent: String,

    @get:Schema(title = "创建人", required = true)
    val creator: String?,

    @get:Schema(title = "修改人", required = true)
    val modifier: String?,

    @get:Schema(title = "创建时间", required = true)
    val createTime: Long,

    @get:Schema(title = "修改时间", required = true)
    val updateTime: Long,

    @get:Schema(title = "是否已安装", required = true)
    var installedFlag: Boolean? = null,

    @get:Schema(title = "是否可编辑", required = false)
    val editFlag: Boolean? = null,

    @get:Schema(title = "荣誉信息", required = false)
    val honorInfos: List<HonorInfo>? = null,

    @get:Schema(title = "指标信息", required = false)
    val indexInfos: List<StoreIndexInfo>? = null
)
