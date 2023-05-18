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

package com.tencent.devops.ticket.constant

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-环境 06：experience-版本体验 07：image-镜像 08：log-日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21： auth-权限 22:sign-签名服务 23:metrics-度量服务 24：external-外部
 *    25：prebuild-预建 26: dispatcher-kubernetes 27：buildless 28: lambda 29: stream  30: worker 31: dispatcher-docker
 *    32: remotedev）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）remotedev
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2023-3-20
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object TicketMessageCode {
    const val CERT_ID_TOO_LONG = "2118001" // 证书名字不能超过128个字符

    const val CREDENTIAL_NAME_ILLEGAL = "2118002" // 凭证名称必须是汉字、英文字母、数字、连字符(-)、下划线(_)或英文句号(.)
    const val CREDENTIAL_ID_ILLEGAL = "2118003" // 凭证标识必须是英文字母、数字或下划线(_)
    const val CREDENTIAL_NOT_FOUND = "2118004" // 凭证{0}不存在
    const val CREDENTIAL_FORMAT_INVALID = "2118005" // 凭证格式不正确
    const val CREDENTIAL_NAME_TOO_LONG = "2118006" // 凭证名字超过32个字符
    const val CREDENTIAL_ID_TOO_LONG = "2118007" // 凭证ID超过32个字符
    const val CREDENTIAL_EXIST = "2118008" // 凭证{0}已存在
    const val CERT_NOT_FOUND = "2118009" // 证书{0}不存在
    const val CERT_FILE_TYPE_ERROR = "2118010" // 证书文件必须是{0}}文件
    const val DESCRIPTION_FILE_TYPE_ERROR = "2118011" // 描述文件必须是{0}文件
    const val CERT_FILE_MUST_BE = "2118012" // 证书文件必须是{0}文件

    const val KEY_FILE_MUST_BE = "2118013" // 密钥文件必须是{0}文件
    const val INVALID_CERT_ID = "2118014" // 无效的证书ID
    const val FILE_SIZE_CANT_EXCEED = "2118015" // {0}文件大小不能超过{1}
    const val NAME_SIZE_CANT_EXCEED = "2118016" // {0}名称不能超过{1}
    const val ILLEGAL_FILE = "2118017" // 不合法的{0}文件
    const val CERTIFICATE_PASSWORD_WRONG = "2118018" // 证书密码错误
    const val CERTIFICATE_ALIAS_OR_PASSWORD_WRONG = "2118019" // 证书别名或者别名密码错误
    const val CERT_USED_BY_OTHERS = "2118020" // 证书{0}已被他人使用
    const val NAME_ALREADY_EXISTS = "2118021" // 名称{0}已存在
    const val CERT_ALREADY_EXISTS = "2118022" // 证书{0}已存在
    const val NAME_NO_EXISTS = "2118023" // 名称{0}不存在
    const val USER_NO_ENGINEERING_CERT_OPERATE_PERMISSIONS = "2118024" // 用户({0})在工程({1})下没有证书{2}的{3}权限
    const val USER_NO_ENGINEERING_CREDENTIAL_OPERATE_PERMISSIONS = "2118025" // 用户({0})在工程({1})下没有凭据{2}的{3}权限

    const val BK_NO_CREDENTIAL = "bkNoCredential" // 无凭证
    const val BK_NO_CERT = "bkNoCert" // 无证书
}
