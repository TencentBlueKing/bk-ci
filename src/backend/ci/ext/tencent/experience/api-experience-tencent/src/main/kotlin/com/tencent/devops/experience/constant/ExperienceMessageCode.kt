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

package com.tencent.devops.experience.constant

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（除开0代表成功外，为了兼容历史接口成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表持续集成平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-持续集成环境 06：experience-版本体验 07：image-镜像 08：log-持续集成日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-持续集成支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2018-11-09
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object ExperienceMessageCode {

    const val EXP_GROUP_NOT_EXISTS = "2106001" // 体验：体验组({0})不存在
    const val EXP_GROUP_IS_EXISTS = "2106002" // 体验：体验组({0})已存在
    const val TOKEN_EXPIRED = "2106003" // 体验：Token过期
    const val TOKEN_NOT_EXISTS = "2106004" // 体验：Token不存在
    const val EXP_EXPIRE = "2106005" // 体验：体验已过期
    const val EXP_REMOVED = "2106006" // 体验：体验已下架
    const val EXP_FILE_NOT_FOUND = "2106007" // 体验：体验的文件不存在
    const val EXP_NOT_EXISTS = "2106008" // 体验：体验不存在
    const val OUTER_LOGIN_ERROR = "2106009" // 外部用户登录错误
    const val OUTER_ACCESS_FAILED = "2106010" // 外部用户访问失败
    const val EXPERIENCE_NEED_PERMISSION = "2106011" // 需要用户权限
    const val EXPERIENCE_NO_AVAILABLE = "2106012" // 该版本不可用，可能已被下架、已过期或被新版本覆盖，请刷新页面重试

    const val USER_NEED_EXP_X_PERMISSION = "2101009" // 体验: 用户没有体验的{0}权限
    const val USER_NEED_EXP_GROUP_X_PERMISSION = "2101010" // 体验：用户没有体验组的{0}权限
    const val EXP_META_DATA_PIPELINE_ID_NOT_EXISTS = "2101011" // 体验：体验未与流水线绑定
    const val USER_NOT_IN_EXP_GROUP = "2101012" // 体验：用户{0}不在体验用户名单中
    const val USER_NEED_CREATE_EXP_PERMISSION = "21010013" // 体验: 用户没有创建版本体验{0}权限
    const val USER_NEED_CREATE_EXP_GROUP_PERMISSION = "21010014" // 体验: 用户没有创建体验用户组{0}权限
}
