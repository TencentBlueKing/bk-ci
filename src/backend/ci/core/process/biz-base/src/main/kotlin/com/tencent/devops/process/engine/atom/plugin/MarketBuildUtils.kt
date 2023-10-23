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

package com.tencent.devops.process.engine.atom.plugin

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.atom.AtomChangeEventParam
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.pipeline.enums.AtomChangeAction
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.pojo.atom.PipelineAtom
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.ws.rs.HttpMethod

object MarketBuildUtils {
    private const val INPUT_PARAM = "input"
    // 插件删除
    private const val BK_ATOM_DEL_HOOK_URL = "bk_atom_del_hook_url"
    private const val BK_ATOM_DEL_HOOK_URL_METHOD = "bk_atom_del_hook_url_method"
    private const val BK_ATOM_DEL_HOOK_URL_BODY = "bk_atom_del_hook_url_body"
    // 插件添加
    private const val BK_ATOM_CREATE_HOOK_URL = "bk_atom_add_hook_url"
    private const val BK_ATOM_CREATE_HOOK_URL_METHOD = "bk_atom_add_hook_url_method"
    private const val BK_ATOM_CREATE_HOOK_URL_BODY = "bk_atom_add_hook_url_body"
    // 插件修改
    private const val BK_ATOM_UPDATE_HOOK_URL = "bk_atom_update_hook_url"
    private const val BK_ATOM_UPDATE_HOOK_URL_METHOD = "bk_atom_update_hook_url_method"
    private const val BK_ATOM_UPDATE_HOOK_URL_BODY = "bk_atom_update_hook_url_body"

    private const val PROJECT_ID = "projectId"
    private const val PIPELINE_ID = "pipelineId"
    private const val USER_ID = "userId"

    private val logger = LoggerFactory.getLogger(MarketBuildUtils::class.java)

    private val atomCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, PipelineAtom?>(object : CacheLoader<String, PipelineAtom?>() {
            override fun load(atomCodeAndVersion: String): PipelineAtom? {
                val client = SpringContextUtil.getBean(Client::class.java)
                val arr = atomCodeAndVersion.split("|")
                val atomCode = arr[0]
                val atomVersion = arr[1]
                val atom = client.get(ServiceAtomResource::class).getAtomVersionInfo(atomCode, atomVersion).data
                logger.info("get atom version info for : $atomCode, $atomVersion, $atom")
                return atom
            }
        })

    @Suppress("ALL")
    private val marketBuildExecutorService = ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors(),
        Runtime.getRuntime().availableProcessors(),
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(16000)
    )

    fun changeAction(
        inputMap: Map<String, Any>,
        atomCode: String,
        atomVersion: String,
        param: AtomChangeEventParam,
        action: AtomChangeAction
    ) {
        marketBuildExecutorService.execute {
            logger.info("start to do [$action]: $atomCode, $atomVersion, $param")
            val bkAtomHookUrl = inputMap.getOrDefault(
                getHookUrl(action),
                getDefaultHookUrl(
                    atomCode = atomCode,
                    atomVersion = atomVersion,
                    channelCode = param.channelCode,
                    hookUrlParamKey = getHookUrl(action)
                )
            ) as String

            if (bkAtomHookUrl.isBlank()) {
                logger.info("bk atom hook url is blank: $atomCode, $atomVersion")
                return@execute
            }
            // hook方法
            val bkAtomHookUrlMethod = inputMap.getOrDefault(
                key = getHookMethod(action),
                defaultValue = getDefaultHookMethod(
                    atomCode = atomCode,
                    atomVersion = atomVersion,
                    hookUrlMethodParamKey = getHookMethod(action)
                )
            ) as String
            // 请求体
            val bkAtomHookBody = inputMap.getOrDefault(
                getHookBody(action),
                getDefaultHookBody(
                    atomCode = atomCode,
                    atomVersion = atomVersion,
                    hookUrlMethodBodyKey = getHookBody(action)
                )
            ) as String

            doHttp(bkAtomHookUrl, bkAtomHookUrlMethod, bkAtomHookBody, param, inputMap)
        }
    }

    private fun doHttp(
        bkAtomHookUrl: String,
        bkAtomHookUrlMethod: String,
        bkAtomHookBody: String,
        param: AtomChangeEventParam,
        inputMap: Map<String, Any>
    ) {
        val url = resolveParam(bkAtomHookUrl, param, inputMap)
        var request = Request.Builder()
            .url(url)

        logger.info("start to market build atom http: $url, $bkAtomHookUrlMethod, $bkAtomHookBody")
        // 没有配置hookBody时，增加默认接口入参
        val requestBody = if (bkAtomHookBody.isBlank()) {
            logger.info("the atom hook body config is empty|atomParam[$param]")
            JsonUtil.toJson(param, false)
        } else {
            resolveParam(bkAtomHookBody, param, inputMap)
        }
        when (bkAtomHookUrlMethod) {
            HttpMethod.GET -> {
                request = request.get()
            }

            HttpMethod.POST -> {
                request = request.post(RequestBody.create(OkhttpUtils.jsonMediaType, requestBody))
            }

            HttpMethod.PUT -> {
                request = request.put(RequestBody.create(OkhttpUtils.jsonMediaType, requestBody))
            }

            HttpMethod.DELETE -> {
                request = request.delete()
            }
        }

        OkhttpUtils.doHttp(request.build()).use { response ->
            val body = response.body!!.string()
            logger.info("before delete execute result: $body")
        }
    }

    @Suppress("ALL")
    private fun getDefaultHookUrl(
        atomCode: String,
        atomVersion: String,
        channelCode: ChannelCode,
        hookUrlParamKey: String
    ): String {
        if (channelCode != ChannelCode.BS) return ""
        val inputMap = atomCache.get("$atomCode|$atomVersion")?.props?.get(INPUT_PARAM)
        if (inputMap == null || inputMap !is Map<*, *>) {
            return ""
        }
        val bkAtomHookUrlItem = inputMap[hookUrlParamKey]
        if (bkAtomHookUrlItem != null && bkAtomHookUrlItem is Map<*, *>) {
            return bkAtomHookUrlItem["default"]?.toString() ?: ""
        }
        return ""
    }

    private fun getDefaultHookMethod(
        atomCode: String,
        atomVersion: String,
        hookUrlMethodParamKey: String
    ): String {
        val inputMap = atomCache.get("$atomCode|$atomVersion")?.props?.get(INPUT_PARAM)
        if (inputMap == null || inputMap !is Map<*, *>) {
            return ""
        }
        val bkAtomHookUrlItem = inputMap[hookUrlMethodParamKey]
        if (bkAtomHookUrlItem != null && bkAtomHookUrlItem is Map<*, *>) {
            return bkAtomHookUrlItem["default"]?.toString() ?: "POST"
        }
        return "POST"
    }

    private fun getDefaultHookBody(atomCode: String, atomVersion: String, hookUrlMethodBodyKey: String): String {
        val inputMap = atomCache.get("$atomCode|$atomVersion")?.props?.get(INPUT_PARAM)
        if (inputMap == null || inputMap !is Map<*, *>) {
            return ""
        }
        val bkAtomHookUrlItem = inputMap[hookUrlMethodBodyKey]
        if (bkAtomHookUrlItem != null && bkAtomHookUrlItem is Map<*, *>) {
            return bkAtomHookUrlItem["default"]?.toString() ?: ""
        }
        return ""
    }

    private fun resolveParam(str: String, param: AtomChangeEventParam, inputMap: Map<String, Any>): String {
        var result = str.replace("{$PROJECT_ID}", param.projectId)
            .replace("{$PIPELINE_ID}", param.pipelineId)
            .replace("{$USER_ID}", param.userId)
            .replace("%7B$PROJECT_ID%7D", param.projectId)
            .replace("%7B$PIPELINE_ID%7D", param.pipelineId)
            .replace("%7B$USER_ID%7D", param.userId)

        inputMap.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
                .replace("%7B$key%7D", value.toString())
        }

        // 没有变量值的变量默认置空
        return result.replace(Regex("\\{.*}"), "")
            .replace(Regex("%7B.*%7D"), "")
    }
    private fun getHookUrl(pluginChangeAction: AtomChangeAction) = when (pluginChangeAction) {
        AtomChangeAction.DELETE -> BK_ATOM_DEL_HOOK_URL
        AtomChangeAction.UPDATE -> BK_ATOM_UPDATE_HOOK_URL
        AtomChangeAction.CREATE -> BK_ATOM_CREATE_HOOK_URL
    }

    private fun getHookMethod(pluginChangeAction: AtomChangeAction) = when (pluginChangeAction) {
        AtomChangeAction.DELETE -> BK_ATOM_DEL_HOOK_URL_METHOD
        AtomChangeAction.UPDATE -> BK_ATOM_UPDATE_HOOK_URL_METHOD
        AtomChangeAction.CREATE -> BK_ATOM_CREATE_HOOK_URL_METHOD
    }

    private fun getHookBody(pluginChangeAction: AtomChangeAction) = when (pluginChangeAction) {
        AtomChangeAction.DELETE -> BK_ATOM_DEL_HOOK_URL_BODY
        AtomChangeAction.UPDATE -> BK_ATOM_UPDATE_HOOK_URL_BODY
        AtomChangeAction.CREATE -> BK_ATOM_CREATE_HOOK_URL_BODY
    }
}
