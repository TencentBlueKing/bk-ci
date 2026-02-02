package com.tencent.devops.common.api.util

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.PathNotFoundException
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import com.jayway.jsonpath.spi.mapper.MappingProvider
import java.util.EnumSet

object JsonPathUtil {

    init {
        Configuration.setDefaults(JacksonConfiguration())
    }

    class JacksonConfiguration : Configuration.Defaults {
        private val jsonProvider = JacksonJsonProvider()
        private val mappingProvider = JacksonMappingProvider()

        override fun jsonProvider(): JsonProvider {
            return jsonProvider
        }

        override fun mappingProvider(): MappingProvider {
            return mappingProvider
        }

        override fun options(): MutableSet<Option> {
            return EnumSet.noneOf(Option::class.java)
        }
    }

    /**
     * 基本读取方法 - 读取JsonPath路径的值
     * @param json JSON字符串
     * @param path JsonPath路径表达式
     * @return 路径对应的值，如果路径不存在返回null
     */
    fun <T> read(json: String, path: String): T? {
        return try {
            JsonPath.read(json, path)
        } catch (e: PathNotFoundException) {
            null
        }
    }

    /**
     * 安全读取方法 - 读取JsonPath路径的值，如果不存在返回默认值
     * @param document 解析后的JSON文档
     * @param path JsonPath路径表达式
     * @return 路径对应的值或默认值
     */
    fun <T> read(document: DocumentContext, path: String): T? {
        return try {
            document.read(path)
        } catch (e: PathNotFoundException) {
            null
        }
    }

    /**
     * 安全读取方法 - 读取JsonPath路径的值，如果不存在返回默认值
     * @param json JSON字符串
     * @param path JsonPath路径表达式
     * @param defaultValue 默认值
     * @return 路径对应的值或默认值
     */
    fun <T> read(json: String, path: String, defaultValue: T): T {
        return try {
            JsonPath.read(json, path)
        } catch (e: PathNotFoundException) {
            defaultValue
        }
    }

    /**
     * 安全读取方法 - 读取JsonPath路径的值，如果不存在返回默认值
     * @param document 解析后的JSON文档
     * @param path JsonPath路径表达式
     * @param defaultValue 默认值
     * @return 路径对应的值或默认值
     */
    fun <T> read(document: DocumentContext, path: String, defaultValue: T): T {
        return try {
            document.read(path)
        } catch (e: PathNotFoundException) {
            defaultValue
        }
    }

    /**
     * 批量读取多个路径的值
     * @param json JSON字符串
     * @param paths 多个JsonPath路径表达式
     * @return 包含所有路径值的Map，键为路径，值为对应的结果
     */
    fun readMultiple(json: String, paths: List<String>): Map<String, Any?> {
        val document = JsonPath.parse(json)
        return paths.associateWith { path ->
            try {
                document.read(path)
            } catch (e: PathNotFoundException) {
                null
            }
        }
    }

    /**
     * 检查路径是否存在
     * @param json JSON字符串
     * @param path JsonPath路径表达式
     * @return 路径是否存在
     */
    fun pathExists(json: String, path: String): Boolean {
        return try {
            JsonPath.read<Any>(json, path)
            true
        } catch (e: PathNotFoundException) {
            false
        }
    }

    /**
     * 获取DocumentContext，用于多次读取同一JSON文档
     * @param json JSON字符串
     * @return DocumentContext对象
     */
    fun parse(json: String): DocumentContext {
        return JsonPath.parse(json)
    }
}
