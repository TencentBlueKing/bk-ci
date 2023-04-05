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

package com.tencent.devops.common.api.constant

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-环境 06：experience-版本体验 07：image-镜像 08：log-日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21： auth-权限 22:sign-签名服务 23:metrics-度量服务 24：external-外部
 *    25：prebuild-预建 26: stream 27：buildless 28: lambda 29: dispatcher-kubernetes  30: worker 31: dispatcher-docker）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2023-3-20
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object CommonMessageCode {
    const val MSG_CODE_ROLE_PREFIX = "MSG_CODE_ROLE_PREFIX_"// 角色国际化前缀
    const val MSG_CODE_PERMISSION_PREFIX = "MSG_CODE_PERMISSION_PREFIX_"// 操作权限国际化前缀
    const val SUCCESS = "0"// 成功
    const val OAUTH_DENERD = 418 // 自定义状态码, 未进行oauth认证
    const val SYSTEM_ERROR = "2100001"// 系统内部繁忙，请稍后再试
    const val PARAMETER_IS_NULL = "2100002"// 参数{0}不能为空
    const val PARAMETER_IS_EXIST = "2100003"// 参数值{0}已经存在系统，请换一个再试
    const val PARAMETER_IS_INVALID = "2100004"// 参数值{0}为非法数据
    const val OAUTH_TOKEN_IS_INVALID = "2100005"// 无效的token，请先oauth认证
    const val PERMISSION_DENIED = "2100006"// 无权限{0}
    const val ERROR_SERVICE_NO_FOUND = "2100007"// "找不到任何有效的{0}服务提供者"
    const val ERROR_SERVICE_INVOKE_FAILURE = "2100008"// "服务调用失败：{0},uniqueId={1}"
    const val ERROR_INVALID_CONFIG = "2100009"// "配置不可用：{0},uniqueId={1}"
    const val ERROR_REST_EXCEPTION_COMMON_TIP = "2100010"// 接口访问出现异常，请联系助手或稍后再重试
    const val ERROR_CLIENT_REST_ERROR = "2100011"// 用户请求不合法，参数或方法错误，请咨询助手
    const val ERROR_PROJECT_FEATURE_NOT_ACTIVED = "2100012"// 项目[{0}]未开通该功能
    const val ERROR_INVALID_PARAM_ = "2100013"// 无效参数: {0}
    const val ERROR_NEED_PARAM_ = "2100014"// 缺少参数: {0}
    const val PARAMETER_VALIDATE_ERROR = "2100015"// {0}参数校验错误: {1}
    const val ERROR_SERVICE_NO_AUTH = "2100016"// 无访问服务的权限
    const val ERROR_QUERY_NUM_TOO_BIG = "2100017"// 查询的数量超过系统规定的值：{0}，请调整查询条件或咨询助手
    const val ERROR_QUERY_TIME_RANGE_TOO_LARGE = "2100018"// 查询的时间范围跨度最大，最长时间范围跨度不能超过{0}天
    const val ERROR_HTTP_RESPONSE_BODY_TOO_LARGE = "2100019"// http请求返回体太大
    const val PERMISSION_DENIED_FOR_APP = "2100020"// APP的无权限{0}
    const val ERROR_SENSITIVE_API_NO_AUTH = "2100021"// 无敏感API访问权限
    const val PARAMETER_LENGTH_TOO_LONG = "2100022"// 参数长度不能超过{0}个字符
    const val PARAMETER_LENGTH_TOO_SHORT = "2100023"// 参数长度不能小于{0}个字符
    const val PARAMETER_ILLEGAL_ERROR = "2100024"// {0}参数非法错误: {1}
    const val PARAMETER_EXPIRED_ERROR = "2100025"// {0}token过期错误: {1}
    const val PARAMETER_SECRET_ERROR = "2100026"// {0}密钥配置错误: {1}
    const val PARAMETER_IS_EMPTY = "2100027"// 参数不能为空
    const val ERROR_QUERY_TIME_RANGE_ERROR = "2100028"// 查询的时间范围跨度错误


    const val USER_NOT_PERMISSIONS_OPERATE_PIPELINE = "2100029"// 用户({0})无权限在工程({1})下{2}流水线({3})
    const val USER_NOT_HAVE_PROJECT_PERMISSIONS = "2100030"// 用户 {0}无项目{1}权限
    const val UNABLE_GET_PIPELINE_JOB_STATUS = "2100031"// 无法获取流水线JOB状态，构建停止
    const val JOB_BUILD_STOPS = "2100032"// 流水线JOB已经不再运行，构建停止
    const val PIPELINE_NAME_OCCUPIED = "2100033"// 流水线名称已被他人使用
    const val INTERNAL_DEPENDENCY_SERVICE_EXCEPTION = "2100034"// 内部依赖服务异常

}
