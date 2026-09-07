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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.store.api.atom.ServiceMarketAtomEnvResource
import com.tencent.devops.store.pojo.atom.AtomProp
import com.tencent.devops.store.pojo.common.version.StoreVersion
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 为插件属性补充 [AtomProp.versionOsMap]，供流水线与模板的插件属性接口共用，避免各接口的判定口径分叉。
 */
@Service
class AtomPropVersionOsService @Autowired constructor(
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AtomPropVersionOsService::class.java)
    }

    /**
     * 按编排中各插件的版本补充其适用的运行环境操作系统。
     *
     * 必须保留版本维度：同一份编排里同一插件可以被多个 element 引用到不同版本，各版本声明的适用操作系统
     * 可能不同，按插件标识聚合无论取并集还是交集都会与保存校验的逐 element 判定产生偏差。
     *
     * @param atomVersions 编排中的市场插件版本，由调用方按自身编排结构解析
     * @param channelCode 编排所属渠道，决定读插件哪个 jobType 的声明，须由调用方按其编排的归属解析后传入。
     *                    流水线要取流水线自身记录的渠道而非请求渠道：请求渠道来自 X-DEVOPS-CHANNEL 请求头，
     *                    缺失时会缺省为 BS，前端就会拿到 AGENT 口径的适用范围，与保存校验的口径不一致，
     *                    出现「前端提示适配、保存却被拦下」这种最需要避免的情况
     */
    fun fillVersionOsMap(
        projectId: String,
        atomVersions: Set<StoreVersion>,
        atomPropResult: Result<Map<String, AtomProp>?>,
        channelCode: ChannelCode
    ): Result<Map<String, AtomProp>?> {
        val atomProps = atomPropResult.data
        if (atomProps.isNullOrEmpty() || atomVersions.isEmpty()) {
            return atomPropResult
        }
        // StoreVersion 含 storeName，同一插件版本被多个不同名 element 引用时会产生重复项，
        // 而按插件版本查询运行时信息与名称无关，去重可避免下游对同一 key 做重复的缓存查询
        val distinctAtomVersions = atomVersions.distinctBy { "${it.storeCode}:${it.version}" }.toSet()
        val versionOsMap = buildVersionOsMap(
            projectId = projectId,
            atomVersions = distinctAtomVersions,
            channelCode = channelCode
        )
        if (versionOsMap.isEmpty()) {
            return atomPropResult
        }
        return Result(
            atomProps.mapValues { (atomCode, atomProp) ->
                versionOsMap[atomCode]?.let { atomProp.copy(versionOsMap = it) } ?: atomProp
            }
        )
    }

    /**
     * 数据来源沿用保存校验所用的 batchGetAtomRunInfos，由它统一完成浮动版本解析与调试项目取值，
     * 前端与后端因此看到同一份声明，不会出现前端提示适配而保存被拦截。
     *
     * @return 外层 key 为插件标识，内层 key 为编排中该插件的版本号
     */
    private fun buildVersionOsMap(
        projectId: String,
        atomVersions: Set<StoreVersion>,
        channelCode: ChannelCode
    ): Map<String, Map<String, List<String>>> {
        // 适用操作系统属于展示增强信息，插件在项目下不可用等异常时降级为不返回，不影响编排本身的展示
        val atomRunInfoMap = try {
            client.get(ServiceMarketAtomEnvResource::class).batchGetAtomRunInfos(
                projectCode = projectId,
                atomVersions = atomVersions
            ).data
        } catch (ignored: Throwable) {
            logger.warn("Failed to batch get atom run infos|$projectId", ignored)
            null
        } ?: return emptyMap()
        val osJobTypeName = AtomUtils.resolveOsJobType(channelCode).name
        val versionOsMap = mutableMapOf<String, MutableMap<String, List<String>>>()
        atomVersions.forEach { atomVersion ->
            val atomCode = atomVersion.storeCode
            val version = atomVersion.version
            // key 与 batchGetAtomRunInfos 的返回一致，为请求时的版本号而非解析后的具体版本
            val atomRunInfo = atomRunInfoMap["$atomCode:$version"] ?: return@forEach
            // 插件未声明该 jobType 的适用范围时为空列表，与保存校验的放行逻辑保持一致
            versionOsMap.getOrPut(atomCode) { mutableMapOf() }[version] =
                atomRunInfo.osMap?.get(osJobTypeName) ?: emptyList()
        }
        return versionOsMap
    }
}
