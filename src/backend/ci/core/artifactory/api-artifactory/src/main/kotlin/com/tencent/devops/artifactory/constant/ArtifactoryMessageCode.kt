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

package com.tencent.devops.artifactory.constant

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（除开0代表成功外，为了兼容历史接口成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表持续集成平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-持续集成环境 06：experience-版本体验 07：image-镜像 08：log-持续集成日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-持续集成支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21： auth-权限）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2018-12-21
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object ArtifactoryMessageCode {
    const val UPLOAD_FILE_TYPE_IS_NOT_SUPPORT = "2102001" // {0}类型文件不支持上传，您可以上传{1}类型文件
    const val UPLOAD_FILE_IS_TOO_LARGE = "2102002" // 上传的文件不能超过{0}
    const val FILE_SIZE_EXCEEDS_LIMIT = "2102003" // 文件大小不能超过{0}
    const val INVALID_CUSTOM_ARTIFACTORY_PATH = "2102004" // 非法自定义仓库路径
    const val SHORT_URL_EXPIRED = "2102005" // 短链接已过期
    const val USER_PROJECT_DOWNLOAD_PERMISSION_FORBIDDEN = "2102006" // 用户{0}没有项目{1}下载权限
    const val USER_PIPELINE_DOWNLOAD_PERMISSION_FORBIDDEN = "2102007" // 用户{0}在项目{1}下没有流水线{2}下载构件权限
    const val LAST_MODIFY_USER_PROJECT_DOWNLOAD_PERMISSION_FORBIDDEN = "2102008" // 流水线最后修改人{0}没有项目{1}下载权限
    const val LAST_MODIFY_USER_PIPELINE_DOWNLOAD_PERMISSION_FORBIDDEN = "2102009" // 流水线最后修改人{0}在项目{1}下没有流水线{2}下载构件权限
    const val USER_PIPELINE_SHARE_PERMISSION_FORBIDDEN = "2102010" // 用户{0}在项目{1}下没有流水线{2}分享构件权限

    const val GRANT_DOWNLOAD_PERMISSION = "2102011" //请联系流水线负责人授予下载构件权限
    const val GRANT_PIPELINE_PERMISSION = "2102012"//访问件构请联系流水线负责人：\n{0} 授予流水线权限。
    const val METADATA_NOT_EXIST_DOWNLOAD_FILE_BY_SHARING = "2102013"//元数据({0})不存在，请通过共享下载文件
    const val NO_EXPERIENCE_PERMISSION = "2102014"//您没有该体验的权限
    const val FILE_NOT_EXIST = "2102015"//文件{0}不存在
    const val DESTINATION_PATH_SHOULD_BE_FOLDER = "2102016"//目标路径应为文件夹
    const val CANNOT_COPY_TO_CURRENT_DIRECTORY = "2102017"//不能在拷贝到当前目录
    const val CANNOT_MOVE_TO_CURRENT_DIRECTORY = "2102018"//不能移动到当前目录
    const val CANNOT_MOVE_PARENT_DIRECTORY_TO_SUBDIRECTORY = "2102019"//不能将父目录移动到子目录
    const val METADATA_NOT_EXIST = "2102020"//元数据({0})不存在
    const val BUILD_NOT_EXIST = "2102021"//构建不存在({0})
    const val USER_NO_PIPELINE_PERMISSION_UNDER_PROJECT = "2102022"//用户({0})在工程({1})下没有流水线{2}权限


}
