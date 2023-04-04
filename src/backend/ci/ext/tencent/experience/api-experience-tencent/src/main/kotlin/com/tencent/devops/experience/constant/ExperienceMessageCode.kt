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

    const val RECORD_COULD_NOT_FOUND = "2101013" //找不到该记录
    const val USER_NOT_PERMISSION = "2101014" //用户没有权限
    const val GRANT_EXPERIENCE_PERMISSION = "2101015" //请联系产品负责人：\n{0} 授予体验权限。
    const val NO_PERMISSION_QUERY_EXPERIENCE = "2101016" //没有查询该体验的权限
    const val LOGIN_IP_FREQUENTLY = "2101017" //登录IP频繁,请稍后重试
    const val LOGIN_ACCOUNT_FREQUENT = "2101018" //登录账号频繁,请稍后重试
    const val ACCOUNT_HAS_BEEN_BLOCKED = "2101019" //账号已被封禁
    const val LOGIN_EXPIRED = "2101020" //登录过期,请重新登录
    const val ACCOUNT_INFORMATION_ABNORMAL = "2101021" //账号信息异常,请重新登录
    const val UNABLE_GET_IP = "2101022" //无法获取IP , 请联系相关人员排查
    const val METADATA_NOT_EXIST = "2101023" //元数据{0}不存在
    const val EXPERIENCE_NOT_EXIST = "2101024" //体验({0})不存在
    const val FILE_NOT_EXIST = "2101025" //文件({0})不存在

}
