/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.store.pojo.common.StoreInfoQuery
import com.tencent.devops.store.pojo.common.deploy.UserComponentDeployInfo

/**
 * 组件部署信息查询服务：面向客户端拉取用户有权限安装的应用及其部署信息。
 */
interface StoreComponentDeployService {

    /**
     * 获取用户可拉取的组件部署信息列表。
     * 聚合返回：用户配置的应用名、版本列表(标注最新版本)、应用安装路径(组件级共享，可空)、
     * 各版本的安装方式与安装参数(跟随版本)。下载链接由客户端按需调用
     * UserArchiveComponentPkgResource#getComponentPkgDownloadUrl 获取。
     *
     * 可见范围仅与用户组织架构 + 组件可见范围有关，不受项目/实例已安装情况影响；
     * 列表 = 用户可见的已发布组件 ∪ 当前调试用例(projectCode/instanceId)下的纯调试组件，
     * 按 sortType 指定字段统一排序(缺省更新时间倒序)。
     */
    fun getUserComponentDeployInfos(
        userId: String,
        storeInfoQuery: StoreInfoQuery
    ): Page<UserComponentDeployInfo>
}
