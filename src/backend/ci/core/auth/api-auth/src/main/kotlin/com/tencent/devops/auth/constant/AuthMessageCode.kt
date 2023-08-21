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

package com.tencent.devops.auth.constant

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
object AuthMessageCode {
    const val UN_DEFAULT_GROUP_ERROR = "2121001" // 权限系统：非默认分组与默认分组重名
    const val DEFAULT_GROUP_ERROR = "2121002" // 权限系统：该分组为默认分组,不允许重复添加
    const val GRADE_CHECK_FAIL = "2121003" // 权限系统：无分级管理员权限,不允许进行该操作
    const val GROUP_ACTION_EMPTY = "2121004" // 权限系统：用户组未绑定动作
    const val GROUP_USER_ALREADY_EXIST = "2121005" // 权限系统：用户已在该用户组
    const val GROUP_NOT_BIND_PERSSION = "2121006" // 权限系统：自定义用户组未绑定权限
    const val GROUP_NOT_EXIST = "2121007" // 权限系统： 用户组不存在
    const val GROUP_EXIST = "2121008" // 权限系统： 用户组已存在
    const val LOGIN_TOKEN_VERIFY_FAILED = "2121009" // 权限系统: token验证失败
    const val LOGIN_USER_FREEZE = "2121010" // 账号冻结中
    const val LOGIN_USER_INFO_EXIST = "2121011" // 权限系统： 用户已存在，无需重复创建
    const val LOGIN_THIRD_CODE_INVALID = "2121012" // 权限系统： 第三方登录code校验失败
    const val MANAGER_PERMISSION_EXPIRE = "2121013" // 管理员权限过期
    const val APPROVAL_RECORD_NOT_EXIST = "2121014" // 审批记录不存在
    const val RELATED_RESOURCE_EMPTY = "2121015" // 权限系统：绑定系统资源为空
    const val RELATED_RESOURCE_CHECK_FAIL = "2121016" // 权限系统：iam回调关联资源不存在
    const val PATH_CHECK_FAIL = "2121017" // 权限系统：iam回调路径校验失败
    const val HOST_CHECKOU_FAIL = "2121018" // 权限系统：iam回调域名校验失败
    const val USER_NOT_EXIST = "2121019" // 权限系统: 用户中心非法用户/组织
    const val IAM_SYSTEM_ERROR = "2121020" // 权限系统：Iam权限中心异常。异常信息{0}
    const val CAN_NOT_FIND_RELATION = "2121021" // 权限系统：用户组无关联系统用户组
    const val DEFAULT_GROUP_UPDATE_NAME_ERROR = "2121022" // 权限系统：该分组为默认分组,不允许重命名
    const val STRATEGT_NAME_NOT_EXIST = "2121023" // 权限系统： 权限集合{0}不存在
    const val STRATEGT_NAME_EXIST = "2121024" // 权限系统： 权限集合名称重复
    const val MANAGER_GRANT_WHITELIST_USER_EXIST = "2121025" // 权限系统： 用户{0}不在白名单内,请先配置策略白名单
    const val MANAGER_WHITE_USER_EXIST = "2121026" // 权限系统： 白名单用户{0}已存在
    const val MANAGER_USER_EXIST = "2121027" // 权限系统： 用户已有该授权
    const val MANAGER_ORG_NOT_EXIST = "2121028" // 权限系统： 权限授权ID{0}不存在
    const val MANAGER_ORG_EXIST = "2121029" // 权限系统： 权限授权已存在
    const val MANAGER_ORG_CHECKOUT_FAIL = "2121030" // 权限系统： 权限授权校验失败
    const val STRATEGT_CHECKOUT_FAIL = "2121031" // 权限系统： 权限集合校验失败
    const val TOO_MANY_INFOS = "2121032" // 权限系统：搜索结果过多,请提供精准关键词
    const val KEYWORD_TOO_SHORT = "2121033" // 权限系统：搜索关键词长度必须大于2
    const val PARENT_TYPE_FAIL = "2121034" // 权限系统：父类资源必须为"项目"
    const val TOKEN_TICKET_FAIL = "2121035" // 权限系统：token校验失败

    const val RESOURCE_NOT_FOUND = "2121036" // 权限系统：资源{0}不存在
    const val DEFAULT_GROUP_CONFIG_NOT_FOUND = "2121037" // 权限系统：默认组配置不存在
    const val RESOURCE_ACTION_EMPTY = "2121038" // 权限系统：资源未绑定动作
    const val ACTION_NOT_EXIST = "2121039" // 权限系统：操作不存在
    const val RESOURCE_TYPE_NOT_FOUND = "2121040" // 权限系统：资源类型{0}不存在
    const val RESOURCE_TYPE_NOT_EMPTY = "2121041" // 权限系统：资源类型不能为空
    const val APPLY_TO_JOIN_GROUP_FAIL = "2121042" // 权限系统: 申请加入用户组失败
    const val GET_GROUP_PERMISSION_DETAIL_FAIL = "2121043" // 权限系统: 获取用户组{0}权限信息失败
    const val GET_IAM_GROUP_FAIL = "2121044" // 权限系统: 获取用户组失败
    const val GET_REDIRECT_INFORMATION_FAIL = "2121045" // 权限系统: 获取跳转链接失败
    const val ERROR_AUTH_NO_MANAGE_PERMISSION = "2121046" // 用户没有管理员权限
    const val ERROR_AUTH_GROUP_NOT_EXIST = "2121047" // 用户组{0}不存在
    const val ERROR_AUTH_RESOURCE_GROUP_CONFIG_NOT_EXIST = "2121048" // 用户组配置{0}不存在
    const val ERROR_ITSM_CALLBACK_APPLICATION_FAIL = "2121049" // 申请单{0}不存在
    const val ERROR_PROJECT_IN_MIGRATION = "2121050" // 项目迁移中
    const val ERROR_ITSM_APPLICATION_CANCEL_FAIL = "2121051" // ITSM单据取消失败
    const val ERROR_ITSM_VERIFY_TOKEN_FAIL = "2121052" // 检验ITSM的token失败
    const val AUTH_GROUP_MEMBER_EXPIRED_DESC = "2121053" // 已过期
    const val ERROR_PROJECT_PERMISSION_CLOSE_FAIL = "2121054" // 项目权限管理不能被关闭
    const val ERROR_DEFAULT_GROUP_DELETE_FAIL = "2121055" // 默认组不能被删除
    const val ERROR_DEFAULT_GROUP_RENAME_FAIL = "2121056" // 默认组不能重命名
    const val ERROR_GROUP_NAME_TO_LONG = "2121057" // 用户组名不能超过32个字符
    const val ERROR_GROUP_NAME_TO_SHORT = "2121058" // 用户组名不能不少于5个字符
    const val ERROR_PROJECT_NOT_UPGRADE = "2121059" // 权限系统: 项目尚未升级到新版权限系统
    const val MIGRATION_GROUP_DESCRIPTION = "2121060" // 用户组:{0},迁移于{1}
    const val ERROR_MIGRATE_AUTH_COMPARE_FAIL = "2121061" // 迁移项目{0}失败,权限策略对比失败
    const val ERROR_CREATOR_NOT_EXIST = "2121062" // 创建人离职
    const val ERROR_RESOURCE_CREATE_FAIL = "2121063" // 资源创建失败
}
