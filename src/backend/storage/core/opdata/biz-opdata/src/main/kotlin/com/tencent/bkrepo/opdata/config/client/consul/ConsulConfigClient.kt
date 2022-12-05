/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.opdata.config.client.consul

import com.ecwid.consul.v1.ConsulClient
import com.ecwid.consul.v1.QueryParams
import com.ecwid.consul.v1.kv.model.PutParams
import com.tencent.bkrepo.common.api.constant.StringPool.SLASH
import com.tencent.bkrepo.common.api.exception.BadRequestException
import com.tencent.bkrepo.common.api.exception.SystemErrorException
import com.tencent.bkrepo.opdata.config.client.ConfigClient
import com.tencent.bkrepo.opdata.message.OpDataMessageCode
import com.tencent.bkrepo.opdata.pojo.config.ConfigItem
import org.slf4j.LoggerFactory
import org.springframework.cloud.consul.config.ConsulConfigProperties
import org.yaml.snakeyaml.Yaml

class ConsulConfigClient(
    private val client: ConsulClient,
    private val properties: ConsulConfigProperties
) : ConfigClient {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun put(key: String, value: Any?, appName: String, targetProfile: String) {
        put(listOf(ConfigItem(key, value)), appName, targetProfile)
    }

    override fun put(values: List<ConfigItem>, appName: String, targetProfile: String) {
        // 当前仅支持
        if (properties.format != ConsulConfigProperties.Format.YAML) {
            logger.error("consul config format: ${properties.format} not support")
            throw SystemErrorException()
        }

        // 读取配置index，用于更新时执行cas校验，避免覆盖其他更新
        val consulConfigKey = getConsulConfigKey(appName, targetProfile)
        val getConfigResponse = client.getKVValue(consulConfigKey, this.properties.aclToken, QueryParams.DEFAULT)

        // 解析YAML
        val putParams = PutParams()
        val yaml = Yaml()
        val config = if (getConfigResponse.value == null) {
            putParams.cas = 0
            HashMap<String, Any?>()
        } else {
            putParams.cas = getConfigResponse.consulIndex
            yaml.load(getConfigResponse.value.decodedValue)
        }

        // 更新YAML中对应的配置项
        values.forEach { (key, value) ->
            putVal(config, key, value)
        }

        client.setKVValue(consulConfigKey, yaml.dumpAsMap(config), properties.aclToken, putParams)
    }

    private fun putVal(map: MutableMap<String, Any?>, key: String, value: Any?) {
        val keys = key.split(".")
        var currentMap = map
        for (i in keys.indices) {
            if (i == keys.size - 1) {
                currentMap[keys[i]] = value
            } else {
                val currentKey = keys[i]
                val currentVal = currentMap.getOrPut(currentKey) { HashMap<String, Any?>(1) }
                if (currentVal != null && currentVal !is Map<*, *>) {
                    throw BadRequestException(OpDataMessageCode.ConfigValueTypeInvalid)
                }
                currentMap = currentVal as MutableMap<String, Any?>
            }
        }
    }

    /**
     * 获取Consul配置的key
     */
    private fun getConsulConfigKey(appName: String, targetProfile: String): String {
        val context = getContext(appName, targetProfile)
        return "$context${properties.dataKey}"
    }

    /**
     * 获取consul配置路径
     */
    private fun getContext(appName: String, targetProfile: String): String {
        val prefix = getPrefix()
        val suffix = SLASH
        val contextBuilder = StringBuilder()
        if (appName.isEmpty()) {
            contextBuilder.append("$prefix${properties.defaultContext}")
        } else {
            contextBuilder.append("$prefix$appName")
        }

        if (targetProfile.isNotEmpty()) {
            contextBuilder.append("${properties.profileSeparator}$targetProfile")
        }

        contextBuilder.append(suffix)
        return contextBuilder.toString()
    }

    /**
     * 获取配置名前缀，所有微服务consul配置都相同，可以直接从consul properties中取
     */
    private fun getPrefix() = if (properties.prefix.isNullOrEmpty()) {
        ""
    } else {
        "${properties.prefix}$SLASH"
    }
}
