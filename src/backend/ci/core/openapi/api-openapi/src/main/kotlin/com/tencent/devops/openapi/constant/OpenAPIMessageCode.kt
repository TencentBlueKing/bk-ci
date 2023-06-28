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

package com.tencent.devops.openapi.constant

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
object OpenAPIMessageCode {
    const val ERROR_OPENAPI_APIGW_PUBFILE_NOT_SETTLE = "2112001" // OpenAPI：API Gateway公钥文件未设置
    const val ERROR_OPENAPI_APIGW_PUBFILE_NOT_EXIST = "2112002" // OpenAPI：API Gateway公钥文件不存在，路径：{0}
    const val ERROR_OPENAPI_APIGW_PUBFILE_READ_ERROR = "2112003" // OpenAPI：API Gateway公钥文件读取错误，路径：{0}
    const val ERROR_OPENAPI_APIGW_PUBFILE_CONTENT_EMPTY = "2112004" // OpenAPI：API Gateway公钥文件内容为空，路径：{0}
    const val ERROR_OPENAPI_JWT_PARSE_FAIL = "2112005" // OpenAPI：JWT解析失败
    const val ERROR_OPENAPI_INNER_SERVICE_FAIL = "2112006" // OpenAPI：内部服务调用失败：{0}
    const val USER_CHECK_FAIL = "2112007" // OpenAPI：用户校验失败
    const val ERROR_USER_EXIST = "2112008" // OpenAPI：重复添加
    const val PARAM_VERIFY_FAIL = "2112009" // 参数校验失败:{0}
    const val ILLEGAL_USER = "2112010" // 非法用户
    const val ERROR_NODE_NOT_EXISTS = "2112011" // 环境管理：[{0}] 节点不存在
    const val BK_REQUEST_METHOD = "bkRequestMethod" // 请求方法/请求路径
    const val BK_RESOURCE_DESCRIPTION = "bkResourceDescription" // 资源描述
    const val BK_INPUT_PARAMETER_DESCRIPTION = "bkInputParameterDescription" // 输入参数说明
    const val BK_PATH_PARAMETER = "bkPathParameter" // Path参数
    const val BK_HEADER_PARAMETER = "bkHeaderParameter" // Header参数
    const val BK_BODY_PARAMETER = "bkBodyParameter" // Body参数
    const val BK_RESPONSE_PARAMETER = "bkResponseParameter" // 响应参数
    const val BK_QUERY_PARAMETER = "bkQueryParameter" // Query参数
    const val BK_PARAM_NAME = "bkParamName" // 参数名称
    const val BK_PARAM_TYPE = "bkParamType" // 参数类型
    const val BK_PARAM_ILLUSTRATE = "bkParamIllustrate" // 参数说明
    const val BK_HAVE_TO = "bkHaveTo" // 必须
    const val BK_ILLUSTRATE = "bkIllustrate" // 说明
    const val BK_DEFAULT_VALUE = "bkDefaultValue" // 默认值
    const val BK_APPLICATION_STATE_REQUIRED = "bkApplicationStateRequired" // 应用态必填、用户态不填
    const val BK_USER_NAME = "bkUserName" // 用户名
    const val BK_HTTP_CODE = "bkHttpCode" // HTTP代码
    const val BK_REQUEST_SAMPLE = "bkRequestSample" // 请求样例
    const val BK_ALL_MODEL_DATA = "bkAllModelData" // 相关模型数据
    const val BK_YES = "bkYes" // 是
    const val BK_NO = "bkNo" // 否
    const val BK_DISCRIMINATOR_ILLUSTRATE = "bkDiscriminatorIllustrate" // 用于指定实现某一多态类, 可选{0},具体实现见下方
    const val BK_MUST_BE = "bkMustBe" // 必须是[{0}]
    const val BK_POLYMORPHIC_CLASS_IMPLEMENTATION = "bkPolymorphicClassImplementation" // 多态类实现
    // *多态基类 <{0}> 的实现处, 其中当字段 {1} = [{2}] 时指定为该类实现*
    const val BK_POLYMORPHISM_MODEL_ILLUSTRATE = "bkPolymorphismModelIllustrate"
    const val BK_RETURNS_THE_SAMPLE = "bkReturnsTheSample" // 返回样例
    // **注意: 确保 header 中存在 Content-Type: application/json ,否则请求返回415错误码**
    const val BK_ERROR_PROMPT = "bkErrorPrompt"
    const val BK_CURL_PROMPT = "bkCurlPrompt" // '[请替换为上方API地址栏请求地址]{0}'
    const val BK_PAYLOAD_REQUEST_SAMPLE = "bkPayloadRequestSample" // < {0} >, 那么请求应该为:
    const val BK_THE_FIELD_IS_READ_ONLY = "bkTheFieldIsReadOnly" // 该字段只读
    const val BK_OBJECT_PROPERTY_ILLUSTRATE = "bkObjectPropertyIllustrate" // Any 任意类型，参照实际请求或返回
    const val BK_NO_SUCH_PARAMETER = "bkNoSuchParameter" // 无此参数
}
