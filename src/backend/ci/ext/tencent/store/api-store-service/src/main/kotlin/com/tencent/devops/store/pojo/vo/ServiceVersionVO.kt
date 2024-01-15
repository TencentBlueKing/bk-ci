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

package com.tencent.devops.store.pojo.vo

import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.StoreMediaInfo
import com.tencent.devops.store.pojo.common.StoreUserCommentInfo
import com.tencent.devops.store.pojo.enums.ServiceTypeEnum
import io.swagger.v3.oas.annotations.media.Schema

data class ServiceVersionVO(
    @Schema(description = "扩展服务ID")
    val serviceId: String,
    @Schema(description = "扩展服务标识")
    val serviceCode: String,
    @Schema(description = "扩展服务名称")
    val serviceName: String,
    @Schema(description = "logo地址")
    val logoUrl: String?,
    @Schema(description = "扩展服务简介")
    val summary: String?,
    @Schema(description = "扩展服务描述")
    val description: String?,
    @Schema(description = "版本号")
    val version: String?,
    @Schema(description = "扩展服务状态，INIT：初始化|COMMITTING：提交中|BUILDING：构建中|BUILD_FAIL：构建失败|TESTING：测试中|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGING：下架中|UNDERCARRIAGED：已下架", required = true)
    val serviceStatus: String,
    @Schema(description = "开发语言")
    val language: String?,
    @Schema(description = "代码库链接")
    val codeSrc: String?,
    @Schema(description = "发布者")
    val publisher: String?,
    @Schema(description = "创建人")
    val creator: String,
    @Schema(description = "修改人")
    val modifier: String,
    @Schema(description = "创建时间")
    val createTime: String,
    @Schema(description = "修改时间")
    val updateTime: String,
    @Schema(description = "是否为默认扩展服务（默认扩展服务默认所有项目可见）true：默认扩展服务 false：普通扩展服务")
    val defaultFlag: Boolean?,
    @Schema(description = "是否可安装标识")
    val flag: Boolean?,
    @Schema(description = "扩展服务代码库授权者")
    val repositoryAuthorizer: String?,
    @Schema(description = "扩展服务的调试项目")
    val projectCode: String?,
    @Schema(description = "用户评论信息")
    val userCommentInfo: StoreUserCommentInfo,
    @Schema(description = "项目可视范围,PRIVATE:私有 LOGIN_PUBLIC:登录用户开源")
    val visibilityLevel: String?,
//    @Schema(description = "扩展服务代码库不开源原因")
//    val privateReason: String?,
    @Schema(description = "扩展服务类型：0：官方自研，1：第三方", required = true)
    val serviceType: Int ? = ServiceTypeEnum.SELF_DEVELOPED.type,
    @Schema(description = "描述录入类型")
    val descInputType: String,
    @Schema(description = "是否推荐标识 true：推荐，false：不推荐")
    val recommendFlag: Boolean? = false,
    @Schema(description = "是否公共 true：推荐，false：不推荐")
    val publicFlag: Boolean? = true,
    @Schema(description = "是否官方认证 true：推荐，false：不推荐")
    val certificationFlag: Boolean? = false,
    @Schema(description = "权重")
    val weight: Int? = 1,
    @Schema(description = "扩展点列表")
    val extensionItemList: List<String>,
    @Schema(description = "媒体信息")
    val mediaList: List<StoreMediaInfo>?,
    @Schema(description = "标签")
    val labelList: List<Label>,
    @Schema(description = "标签Id")
    val labelIdList: List<String>,
    @Schema(description = "扩展点名称")
    val itemName: String,
    @Schema(description = "扩展点所属蓝盾服务Id串")
    val bkServiceId: Set<Long>,
    @Schema(description = "发布类型，0：新上架 1：非兼容性升级 2：兼容性功能更新 3：兼容性问题修正 ")
    val releaseType: String,
    @Schema(description = "版本日志内容", required = true)
    val versionContent: String,
    @Schema(description = "是否可编辑")
    val editFlag: Boolean? = null
)
