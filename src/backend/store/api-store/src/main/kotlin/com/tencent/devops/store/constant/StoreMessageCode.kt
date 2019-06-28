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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.constant

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表持续集成平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-持续集成环境 06：experience-版本体验 07：image-镜像 08：log-持续集成日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-持续集成支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-应用商店）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2018-12-21
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object StoreMessageCode {
    const val USER_QUERY_ATOM_PERMISSION_IS_INVALID = "2120001" // 您没有查询该插件的权限
    const val USER_QUERY_PROJECT_PERMISSION_IS_INVALID = "2120002" // 您没有查询该项目的权限
    const val USER_CREATE_REPOSITORY_FAIL = "2120003" // 创建代码库失败，请稍后再试
    const val USER_INSTALL_ATOM_CODE_IS_INVALID = "2120004" // 安装插件失败
    const val USER_REPOSITORY_PULL_TASK_JSON_FILE_FAIL = "2120005" // 拉取插件json配置文件失败,请确认文件是否正确上传至代码库
    const val USER_REPOSITORY_TASK_JSON_FIELD_IS_NULL = "2120006" // 插件json配置文件{0}节点不能为空
    const val USER_REPOSITORY_TASK_JSON_FIELD_IS_INVALID = "2120007" // 插件json配置文件{0}字段与系统录入不一致
    const val USER_ATOM_RELEASE_STEPS_ERROR = "2120008" // 插件发布流程中状态变更顺序不正确
    const val USER_ATOM_VERSION_IS_NOT_FINISH = "2120009" // 插件{0}的{1}版本发布未结束，请稍后再试
    const val USER_ATOM_VERSION_IS_INVALID = "2120010" // 插件升级的版本号{0}错误，应为{1}
    const val USER_ATOM_LOGO_SIZE_IS_INVALID = "2120011" // 插件logo的尺寸必须为{0}x{1}
    const val USER_ATOM_LOGO_TYPE_IS_NOT_SUPPORT = "2120012" // {0}类型logo文件不支持上传，您可以上传{1}类型logo文件
    const val UPLOAD_LOGO_IS_TOO_LARGE = "2120013" // 上传的logo文件不能超过{0}
    const val USER_ATOM_CONF_INVALID = "2120014" // 插件配置文件{0}格式不正确，请检查
    const val USER_ATOM_VISIBLE_DEPT_IS_INVALID = "2120015" // 你不在{0}插件的可见范围之内，如需调整插件的可见范围请联系插件的发布者
    const val USER_COMPONENT_ADMIN_COUNT_ERROR = "2120016" // 管理员个数不能少于1个

    const val USER_TEMPLATE_VERSION_IS_NOT_FINISH = "2120201" // {0}模板的{1}版本发布未结束，请稍后再试
    const val USER_TEMPLATE_RELEASE_STEPS_ERROR = "2120202" // 模板发布流程中状态变更顺序不正确
    const val USER_TEMPLATE_ATOM_VISIBLE_DEPT_IS_INVALID = "2120203" // 模板的可见范围不在{0}插件的可见范围之内，如需调整插件的可见范围请联系插件的发布者
    const val USER_TEMPLATE_ATOM_NOT_INSTALLED = "2120204" // 模版下的插件{0}尚未安装，请先安装后再使用
    const val USER_TEMPLATE_RELEASED = "2120205" // 模版{0}已发布到商店，请先下架再删除
    const val USER_TEMPLATE_USED = "2120206" // 模版{0}已安装到其他项目下使用，请勿移除

    const val USER_SENSITIVE_CONF_EXIST = "2120401" // 字段名{0}已存在

    const val USER_PRAISE_IS_INVALID = "2120901" // 你已点赞过
    const val USER_PROJECT_IS_NOT_ALLOW_INSTALL = "2120902" // 你没有权限将组件安装到项目：{0}
    const val USER_COMMENT_IS_INVALID = "2120903" // 你已评论过，无法继续添加评论，但可以对评论进行修改
    const val USER_CLASSIFY_IS_NOT_ALLOW_DELETE = "2120904" // 该分类下还有项目正在使用的组件，不允许直接删除
    const val USER_UPLOAD_PACKAGE_INVALID = "2120905" // 请确认上传的包是否正确
}
