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

package com.tencent.devops.scm.constant

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-环境 06：experience-版本体验 07：image-镜像 08：log-日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21： auth-权限 22:sign-签名服务 23:metrics-度量服务 24：external-外部
 *    25：prebuild-预建 26:dispatcher-kubernetes 27：buildless 28: lambda 29: stream  30: worker 31: dispatcher-docker）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2023-3-20
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object ScmMessageCode {
    const val ERROR_GIT_BAD_REQUEST = "2116001" //参数错误，或是参数格式错误
    const val ERROR_GIT_UNAUTHORIZED = "2116002" //认证失败
    const val ERROR_GIT_FORBIDDEN = "2116003" //账号并没有该操作的权限
    const val ERROR_GIT_NOT_FOUND = "2116004" //资源不存在，也可能是账号没有该项目的权限（为防止黑客撞库获取库列表）
    const val ERROR_GIT_METHOD_NOT_ALLOWED = "2116005" //没有该接口
    const val ERROR_GIT_CONFLICT = "2116006" //已经有一个同名项目存在了
    const val ERROR_GIT_UNPROCESSABLE = "2116007" //操作不能进行
    const val ERROR_GIT_SERVER_ERROR = "2116008" //服务器出错
    const val GIT_TOKEN_EMPTY = "2116009" //Git Token为空
    const val INCORRECT_GIT_TOKEN = "2116010" //Git Token不正确

    const val BK_FILE_CANNOT_EXCEED = "bkFileCannotExceed" //请求文件不能超过1M
    const val BK_LOCAL_WAREHOUSE_CREATION_FAILED = "bkLocalWarehouseCreationFailed" //工程({0})本地仓库创建失败
    const val BK_TRIGGER_METHOD = "bkTriggerMethod" //触发方式
    const val BK_QUALITY_RED_LINE = "bkQualityRedLine" //质量红线
    const val BK_QUALITY_RED_LINE_OUTPUT = "bkQualityRedLineOutput" //质量红线产出插件
    const val BK_RESULT = "bkResult" //结果
    const val BK_EXPECT = "bkExpect" //预期
}
