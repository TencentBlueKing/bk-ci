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

package com.tencent.devops.sign.api.constant

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（除开0代表成功外，为了兼容历史接口成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表持续集成平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-持续集成环境 06：experience-版本体验 07：image-镜像 08：log-持续集成日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-持续集成支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店  21： auth-权限 22:sign-签名服务）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2018-11-09
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object SignMessageCode {

    // sign服务模块业务错误
    const val ERROR_PARSE_SIGN_INFO_HEADER = "2122001" // 解析签名信息头部失败
    const val ERROR_CHECK_SIGN_INFO_HEADER = "2122002" // 验证签名信息头部失败
    const val ERROR_COPY_FILE = "2122003" // 复制并计算MD5失败
    const val ERROR_SIGN_IPA_ILLEGAL = "2122004" // IPA包解析失败
    const val ERROR_SIGN_INFO_ILLEGAL = "2122005" // 缺少拓展的签名信息
    const val ERROR_SIGN_IPA = "2122006" // IPA包签名失败
    const val ERROR_ARCHIVE_SIGNED_IPA = "2122007" // 归档IPA包失败
    const val ERROR_MP_NOT_EXIST = "2122008" // 描述文件不存在
    const val ERROR_ENCODE_SIGN_INFO = "2122009" // 编码签名信息头部失败
    const val ERROR_PARS_INFO_PLIST = "2122010" // 解析Info.plist失败
    const val ERROR_RESIGN_TASK_NOT_EXIST = "2122011" // 签名任务不存在
    const val ERROR_CREATE_DOWNLOAD_URL = "2122012" // 创建下载连接失败
    const val ERROR_INFO_PLIST_NOT_EXIST = "2122013" // ipa文件解压并检查签名信息失败
    const val ERROR_WILDCARD_MP_NOT_EXIST = "2122014" // 通配符描述文件不存在
    const val ERROR_INSERT_KEYCHAIN_GROUPS = "2122015" // entitlement插入keychain失败
    const val ERROR_NOT_AUTH_UPLOAD = "2122016" // 无发起iOS重签名权限
    const val ERROR_UPLOAD_TOKEN_INVALID = "2122017" // 上传IPA使用的token不存在
    const val ERROR_UPLOAD_TOKEN_EXPIRED = "2122018" // 上传IPA使用的token已过期
    const val ERROR_MP_PARSE_ERROR = "2122019" // 描述文件不存在
}
