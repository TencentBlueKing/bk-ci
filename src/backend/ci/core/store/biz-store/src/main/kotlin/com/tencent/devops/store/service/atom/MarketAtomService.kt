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

package com.tencent.devops.store.service.atom

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.store.pojo.atom.AtomDevLanguage
import com.tencent.devops.store.pojo.atom.AtomOutput
import com.tencent.devops.store.pojo.atom.AtomPostReqItem
import com.tencent.devops.store.pojo.atom.AtomPostResp
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.AtomVersionListItem
import com.tencent.devops.store.pojo.atom.GetRelyAtom
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import com.tencent.devops.store.pojo.atom.MarketAtomResp
import com.tencent.devops.store.pojo.atom.MarketMainItem
import com.tencent.devops.store.pojo.atom.MyAtomResp
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import com.tencent.devops.store.pojo.common.StoreErrorCodeInfo
import com.tencent.devops.store.pojo.common.StoreShowVersionInfo

@Suppress("ALL")
interface MarketAtomService {

    /**
     * 插件市场首页
     */
    fun mainPageList(
        userId: String,
        page: Int?,
        pageSize: Int?,
        urlProtocolTrim: Boolean = false
    ): Result<List<MarketMainItem>>

    /**
     * 插件市场，查询插件列表
     */
    fun list(
        userId: String,
        keyword: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: AtomTypeEnum?,
        yamlFlag: Boolean?,
        recommendFlag: Boolean?,
        qualityFlag: Boolean?,
        sortType: MarketAtomSortTypeEnum?,
        page: Int?,
        pageSize: Int?,
        urlProtocolTrim: Boolean = false
    ): MarketAtomResp

    /**
     * 根据用户和插件名称获取插件信息
     */
    fun getMyAtoms(
        accessToken: String,
        userId: String,
        atomName: String?,
        page: Int,
        pageSize: Int
    ): Result<MyAtomResp?>

    /**
     * 根据插件版本ID获取版本基本信息、发布信息
     */
    fun getAtomById(atomId: String, userId: String): Result<AtomVersion?>

    /**
     * 根据插件标识获取插件最新、正式版本息
     */
    fun getAtomByCode(userId: String, atomCode: String): Result<AtomVersion?>

    /**
     * 根据标识获取最新版本信息（若最新版本为测试中，取最新版本，否则取最新正式版本）
     */
    fun getNewestAtomByCode(userId: String, atomCode: String): Result<AtomVersion?>

    /**
     * 安装插件到项目
     */
    fun installAtom(
        accessToken: String,
        userId: String,
        channelCode: ChannelCode,
        installAtomReq: InstallAtomReq
    ): Result<Boolean>

    /**
     * 设置插件构建状态
     */
    fun setAtomBuildStatusByAtomCode(
        atomCode: String,
        version: String,
        userId: String,
        atomStatus: AtomStatusEnum,
        msg: String?
    ): Result<Boolean>

    /**
     * 根据插件标识获取插件版本列表
     */
    fun getAtomVersionsByCode(
        userId: String,
        atomCode: String,
        page: Int,
        pageSize: Int
    ): Result<Page<AtomVersionListItem>>

    /**
     * 获取插件开发支持的语言
     */
    fun listLanguage(): Result<List<AtomDevLanguage?>>

    /**
     * 删除插件
     */
    fun deleteAtom(userId: String, atomCode: String): Result<Boolean>

    /**
     * 生成插件yml文件
     */
    fun generateCiYaml(
        atomCode: String?,
        os: String? = null,
        classType: String? = null,
        defaultShowFlag: Boolean? = true
    ): String

    /**
     * 生成插件yml 2.0文件
     */
    fun generateCiV2Yaml(
        atomCode: String,
        os: String? = null,
        classType: String? = null,
        defaultShowFlag: Boolean? = true
    ): String

    /**
     * 获取插件output参数列表
     */
    fun getAtomOutput(
        atomCode: String
    ): List<AtomOutput>

    /**
     * 获得插件依赖关系
     */
    fun getAtomsRely(getRelyAtom: GetRelyAtom): Map<String, Map<String, Any>>

    /**
     * 查找带post属性的插件
     */
    fun getPostAtoms(projectCode: String, atomItems: Set<AtomPostReqItem>): Result<AtomPostResp>

    /**
     * 根据插件标识获取插件回显版本信息
     */
    fun getAtomShowVersionInfo(userId: String, atomCode: String): Result<StoreShowVersionInfo>

    /**
     * 更新插件自定义错误码信息
     */
    fun updateAtomErrorCodeInfo(
        userId: String,
        projectCode: String,
        storeErrorCodeInfo: StoreErrorCodeInfo
    ): Result<Boolean>
}
