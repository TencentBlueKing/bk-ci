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

package com.tencent.devops.store.pojo

import com.tencent.devops.store.pojo.enums.DescInputTypeEnum
import com.tencent.devops.store.pojo.enums.ServiceTypeEnum
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

data class ExtServiceFeatureUpdateInfo(
    @ApiModelProperty("扩展服务类型：0：官方自研，1：第三方", required = true)
    val serviceTypeEnum: ServiceTypeEnum? = ServiceTypeEnum.SELF_DEVELOPED,
    @ApiModelProperty("是否为公共扩展服务， TRUE：是 FALSE：不是  ")
    val publicFlag: Boolean? = null,
    @ApiModelProperty("是否推荐， TRUE：是 FALSE：不是 ")
    val recommentFlag: Boolean? = null,
    @ApiModelProperty("是否官方认证， TRUE：是 FALSE：不是  ")
    val certificationFlag: Boolean? = null,
    @ApiModelProperty("权重（数值越大代表权重越高）")
    val weight: Int? = null,
    @ApiModelProperty("扩展服务可见范围 0：私有 10：登录用户开源")
    val visibilityLevel: Int? = null,
    @ApiModelProperty("描述录入类型")
    val descInputType: DescInputTypeEnum? = null,
    @ApiModelProperty("代码库hashId")
    val repositoryHashId: String? = null,
    @ApiModelProperty("代码库地址")
    val codeSrc: String? = null,
    @ApiModelProperty("删除标签")
    val deleteFlag: Boolean? = null,
    @ApiModelProperty("是否停掉灰度环境应用， TRUE：是 FALSE：否")
    val killGrayAppFlag: Boolean? = null,
    @ApiModelProperty("停掉灰度环境应用标记时间")
    val killGrayAppMarkTime: LocalDateTime? = null,
    @ApiModelProperty("添加用户")
    val creatorUser: String? = null,
    @ApiModelProperty("修改用户")
    val modifierUser: String? = null
)
