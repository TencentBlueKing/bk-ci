package com.tencent.devops.metrics.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MetricsQueryService @Autowired constructor(
    private val objectMapper: ObjectMapper
) {
    @Value("\${metrics.monitor.url:}")
    val monitorUrl: String = ""

    @Value("\${metrics.monitor.appCode:}")
    val monitorAppCode: String = ""

    @Value("\${metrics.monitor.appSecret:}")
    val monitorAppSecret: String = ""

    @Value("\${metrics.monitor.bizId:}")
    val monitorBizId: String = ""

    @Value("\${metrics.monitor.table:}")
    val monitorTable: String = ""

    @Value("\${metrics.monitor.tableAgent:}")
    val monitorTableAgent: String = ""

    companion object {
        private val logger = LoggerFactory.getLogger(MetricsQueryService::class.java)
    }

    /**
     * 查询指标数据
     * @param projectId 项目ID
     * @param requestParams 请求参数（不包含bk_biz_id）
     * @return 监控平台返回的数据
     */
    fun queryMetrics(projectId: String, requestParams: Map<String, Any>): Map<String, Any> {
        if (monitorUrl.isEmpty()) {
            return emptyMap()
        }

        // 获取promql并进行校验和替换
        val promql = requestParams["promql"] as? String
            ?: throw IllegalArgumentException("promql参数不能为空")

        // 安全校验：检测promql必须包含{{table}}或{{table_agent}}占位符
        if (!promql.contains("{{table}}") && !promql.contains("{{table_agent}}")) {
            throw IllegalArgumentException("promql必须包含{{table}}或{{table_agent}}占位符")
        }

        // 安全校验：检测是否包含危险字符和模式
        validateNoMaliciousPatterns(promql)

        // 替换占位符
        val replacedPromql = replacePromqlPlaceholders(promql, projectId)

        // 安全校验：替换后再次检查是否包含非法内容
        validateReplacedPromql(replacedPromql, projectId)

        // 构建完整的请求参数，添加bk_biz_id和替换后的promql
        val fullParams = requestParams.toMutableMap()
        fullParams["bk_biz_id"] = monitorBizId
        fullParams["promql"] = replacedPromql

        // 构建请求体
        val requestBody = JsonUtil.toJson(fullParams, false)

        // 构建认证头
        val authHeader = JsonUtil.toJson(
            mapOf(
                "bk_app_code" to monitorAppCode,
                "bk_app_secret" to monitorAppSecret
            ),
            false
        )

        logger.info("Query metrics with params: $requestBody")

        // 发送HTTP请求
        val request = Request.Builder()
            .url(monitorUrl)
            .header("X-Bkapi-Authorization", authHeader)
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                logger.error("Query metrics failed: code=${response.code}, body=$responseBody")
                throw RuntimeException("查询指标数据失败: ${response.code}")
            }

            logger.info("Query metrics success: $responseBody")

            // 解析响应
            return try {
                objectMapper.readValue(responseBody, Map::class.java) as Map<String, Any>
            } catch (e: Exception) {
                logger.error("Parse response failed", e)
                throw RuntimeException("解析响应数据失败: ${e.message}")
            }
        }
    }

    /**
     * 替换promql中的占位符
     * @param promql 原始promql
     * @param projectId 项目ID
     * @return 替换后的promql
     */
    private fun replacePromqlPlaceholders(promql: String, projectId: String): String {
        var result = promql

        // 替换{{table}}占位符
        result = result.replace("{{table}}", monitorTable)

        // 替换{{table_agent}}占位符
        result = result.replace("{{table_agent}}", monitorTableAgent)

        // 替换{{projectId}}占位符
        result = result.replace("{{projectId}}", projectId)

        logger.info("Replace promql placeholders: original=$promql, replaced=$result")

        return result
    }

    /**
     * 校验是否包含危险字符和模式（防止注入攻击）
     * @param promql 原始promql
     */
    fun validateNoMaliciousPatterns(promql: String) {
        // 危险模式列表
        val dangerousPatterns = listOf(
            // 防止逻辑运算符绕过
            Regex("""\s+or\s+""", RegexOption.IGNORE_CASE),
            Regex("""\s+and\s+""", RegexOption.IGNORE_CASE),
//            Regex("""\s+unless\s+""", RegexOption.IGNORE_CASE),
            // 防止注释注入
            Regex("""#.*"""),
            // 防止多语句
            Regex(""";"""),
            // 防止命令注入
            Regex("""\$\{.*\}"""),
            Regex("""`.*`""")
        )

        for (pattern in dangerousPatterns) {
            if (pattern.containsMatchIn(promql)) {
                logger.warn("检测到危险模式，可能存在注入攻击: pattern=$pattern, promql=$promql")
                throw IllegalArgumentException("promql包含非法字符或模式")
            }
        }

        // 防止访问其他表（不通过占位符）：使用正则检测第一个冒号前必须紧跟table占位符
        // 匹配模式：查找第一个冒号，检查其前面是否紧跟{{table}}或{{table_agent}}
        val tableAccessPattern = Regex("""^[^:]*\{\{(table|table_agent)\}\}:""")
        if (promql.contains(':') && !tableAccessPattern.containsMatchIn(promql)) {
            logger.warn("检测到非法表名访问，第一个冒号前未紧跟table占位符: promql=$promql")
            throw IllegalArgumentException("promql必须通过{{table}}:或{{table_agent}}:格式访问表")
        }

        // 检查是否只包含允许的占位符
        val allowedPlaceholders = setOf("{{table}}", "{{table_agent}}", "{{projectId}}")
        val placeholderPattern = Regex("""\{\{[^}]+\}\}""")
        val foundPlaceholders = placeholderPattern.findAll(promql).map { it.value }.toSet()
        val invalidPlaceholders = foundPlaceholders - allowedPlaceholders

        if (invalidPlaceholders.isNotEmpty()) {
            logger.warn("检测到非法占位符: $invalidPlaceholders, promql=$promql")
            throw IllegalArgumentException("promql包含非法占位符: ${invalidPlaceholders.joinToString()}")
        }
    }

    /**
     * 校验替换后的promql是否合法（防止替换后注入）
     * @param replacedPromql 替换后的promql
     * @param projectId 项目ID
     */
    fun validateReplacedPromql(replacedPromql: String, projectId: String) {
        // 确保替换后的promql包含配置的表名（至少包含一个）
        if (!replacedPromql.contains(monitorTable) && !replacedPromql.contains(monitorTableAgent)) {
            logger.error(
                "替换后的promql不包含配置的表名: table=${monitorTable}, tableAgent=${monitorTableAgent}, " +
                    "promql=$replacedPromql"
            )
            throw IllegalArgumentException("promql替换失败")
        }

        // 确保替换后的promql包含当前项目ID（防止跨项目查询）
        if (!replacedPromql.contains(projectId)) {
            logger.warn("替换后的promql不包含项目ID，可能存在跨项目查询风险: projectId=$projectId, promql=$replacedPromql")
            throw IllegalArgumentException("promql必须包含{{projectId}}占位符")
        }

        // 检查是否存在多个不同的项目ID（防止跨项目查询）
        val projectIdPattern = Regex("""dimensions__bk_46__projectId\s*=\s*["']([^"']+)["']""")
        val foundProjectIds = projectIdPattern.findAll(replacedPromql)
            .map { it.groupValues[1] }
            .toSet()

        if (foundProjectIds.size > 1 || (foundProjectIds.isNotEmpty() && !foundProjectIds.contains(projectId))) {
            logger.warn(
                "检测到跨项目查询攻击: expectedProjectId=$projectId, foundProjectIds=$foundProjectIds, " +
                    "promql=$replacedPromql"
            )
            throw IllegalArgumentException("不允许跨项目查询")
        }

        // 检查是否仍然包含占位符（防止替换不完整）
        if (replacedPromql.contains("{{") || replacedPromql.contains("}}")) {
            logger.error("替换后的promql仍包含占位符: $replacedPromql")
            throw IllegalArgumentException("promql包含未识别的占位符")
        }
    }
}
