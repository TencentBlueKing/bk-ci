package com.tencent.devops.metrics.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.util.ReflectionTestUtils

/**
 * MetricsQueryService 单元测试
 * 主要测试 promql 安全校验相关方法
 */
class MetricsQueryServiceTest {

    private lateinit var metricsQueryService: MetricsQueryService

    @BeforeEach
    fun setUp() {
        metricsQueryService = MetricsQueryService(ObjectMapper())
        // 设置测试用的配置值
        ReflectionTestUtils.setField(metricsQueryService, "monitorTable", "bk_monitor_table")
        ReflectionTestUtils.setField(metricsQueryService, "monitorTableAgent", "bk_monitor_agent_table")
        ReflectionTestUtils.setField(metricsQueryService, "monitorBizId", "100")
    }

    // ==================== validateNoMaliciousPatterns 测试 ====================

    @Test
    fun `test validateNoMaliciousPatterns should pass for valid promql`() {
        val validPromqls = listOf(
            // 正常的table占位符
            """sum(count_over_time({{table}}:dtEventTimestamp{dimensions__bk_46__projectId="{{projectId}}"}[10800s]))""",
            // 正常的table_agent占位符
            """count((100 - min by (hostIp, hostName) ({{table_agent}}:cpu_detail:idle{projectId="{{projectId}}"})) > 80)""",
            // 包含unless运算符（已从危险模式中移除）
            """rate({{table}}:requests[5m]) unless rate({{table}}:errors[5m])""",
            """count((100 - min by (hostIp, hostName) ({{table_agent}}:cpu_detail:idle{projectId=\"{{projectId}}\"})) > 80)""",
            // 复杂查询
            """histogram_quantile(0.95, sum(rate({{table}}:http_request_duration_seconds_bucket{projectId="{{projectId}}"}[5m])) by (le))"""
        )

        validPromqls.forEach { promql ->
            Assertions.assertDoesNotThrow {
                metricsQueryService.validateNoMaliciousPatterns(promql)
            }
        }
    }

    @Test
    fun `test validateNoMaliciousPatterns should throw exception when contains or operator`() {
        val promql = """{{table}}:metric{label="value"} or {{table}}:other_metric"""

        val exception = assertThrows<IllegalArgumentException> {
            metricsQueryService.validateNoMaliciousPatterns(promql)
        }

        Assertions.assertEquals("promql包含非法字符或模式", exception.message)
    }

    @Test
    fun `test validateNoMaliciousPatterns should throw exception when contains and operator`() {
        val promql = """{{table}}:metric{label="value"} and {{table}}:other_metric"""

        val exception = assertThrows<IllegalArgumentException> {
            metricsQueryService.validateNoMaliciousPatterns(promql)
        }

        Assertions.assertEquals("promql包含非法字符或模式", exception.message)
    }

    @Test
    fun `test validateNoMaliciousPatterns should throw exception when contains comment symbol`() {
        val promql = """{{table}}:metric{label="value"} # this is a comment"""

        val exception = assertThrows<IllegalArgumentException> {
            metricsQueryService.validateNoMaliciousPatterns(promql)
        }

        Assertions.assertEquals("promql包含非法字符或模式", exception.message)
    }

    @Test
    fun `test validateNoMaliciousPatterns should throw exception when contains semicolon`() {
        val promql = """{{table}}:metric{label="value"}; drop table users"""

        val exception = assertThrows<IllegalArgumentException> {
            metricsQueryService.validateNoMaliciousPatterns(promql)
        }

        Assertions.assertEquals("promql包含非法字符或模式", exception.message)
    }

    @Test
    fun `test validateNoMaliciousPatterns should throw exception when contains command injection pattern`() {
        val promqlWithDollar = """{{table}}:metric{label="${'$'}{malicious}"}"""

        val exception1 = assertThrows<IllegalArgumentException> {
            metricsQueryService.validateNoMaliciousPatterns(promqlWithDollar)
        }

        Assertions.assertEquals("promql包含非法字符或模式", exception1.message)

        val promqlWithBacktick = """{{table}}:metric{label=`malicious`}"""

        val exception2 = assertThrows<IllegalArgumentException> {
            metricsQueryService.validateNoMaliciousPatterns(promqlWithBacktick)
        }

        Assertions.assertEquals("promql包含非法字符或模式", exception2.message)
    }

    @Test
    fun `test validateNoMaliciousPatterns should throw exception when directly access table name`() {
        val promql = """malicious_table:cpu_detail:idle{projectId="test"}"""

        val exception = assertThrows<IllegalArgumentException> {
            metricsQueryService.validateNoMaliciousPatterns(promql)
        }
    }

    @Test
    fun `test validateNoMaliciousPatterns should throw exception when contains invalid placeholder`() {
        val promql = """{{table}}:metric{label="{{maliciousPlaceholder}}"}"""

        val exception = assertThrows<IllegalArgumentException> {
            metricsQueryService.validateNoMaliciousPatterns(promql)
        }

        Assertions.assertEquals("promql包含非法占位符: {{maliciousPlaceholder}}", exception.message)
    }

    @Test
    fun `test validateNoMaliciousPatterns should pass when only contains allowed placeholders`() {
        val promql = """{{table}}:metric{projectId="{{projectId}}"}"""

        Assertions.assertDoesNotThrow {
            metricsQueryService.validateNoMaliciousPatterns(promql)
        }
    }

    @Test
    fun `test validateNoMaliciousPatterns should pass when contains table_agent placeholder`() {
        val promql = """{{table_agent}}:cpu_detail:idle{projectId="{{projectId}}"}"""

        Assertions.assertDoesNotThrow {
            metricsQueryService.validateNoMaliciousPatterns(promql)
        }
    }

    // ==================== validateReplacedPromql 测试 ====================

    @Test
    fun `test validateReplacedPromql should pass for valid replaced promql`() {
        val replacedPromql =
            """sum(count_over_time(bk_monitor_table:dtEventTimestamp{dimensions__bk_46__projectId="test-project"}[10800s]))"""
        val projectId = "test-project"

        Assertions.assertDoesNotThrow {
            metricsQueryService.validateReplacedPromql(replacedPromql, projectId)
        }
    }

    @Test
    fun `test validateReplacedPromql should throw exception when not contains configured table name`() {
        val replacedPromql =
            """sum(count_over_time(wrong_table:dtEventTimestamp{dimensions__bk_46__projectId="test-project"}[10800s]))"""
        val projectId = "test-project"

        val exception = assertThrows<IllegalArgumentException> {
            metricsQueryService.validateReplacedPromql(replacedPromql, projectId)
        }

        Assertions.assertEquals("promql替换失败", exception.message)
    }

    @Test
    fun `test validateReplacedPromql should throw exception when not contains project id`() {
        val replacedPromql =
            """sum(count_over_time(bk_monitor_table:dtEventTimestamp{dimensions__bk_46__projectId="other-project"}[10800s]))"""
        val projectId = "test-project"

        val exception = assertThrows<IllegalArgumentException> {
            metricsQueryService.validateReplacedPromql(replacedPromql, projectId)
        }

        Assertions.assertEquals("promql必须包含{{projectId}}占位符", exception.message)
    }

    @Test
    fun `test validateReplacedPromql should throw exception when contains multiple different project ids`() {
        val replacedPromql =
            """sum(count_over_time(bk_monitor_table:dtEventTimestamp{dimensions__bk_46__projectId="project1"}[10800s])) + sum(count_over_time(bk_monitor_table:dtEventTimestamp{dimensions__bk_46__projectId="project2"}[10800s]))"""
        val projectId = "project1"

        val exception = assertThrows<IllegalArgumentException> {
            metricsQueryService.validateReplacedPromql(replacedPromql, projectId)
        }

        Assertions.assertEquals("不允许跨项目查询", exception.message)
    }

    @Test
    fun `test validateReplacedPromql should throw exception when project id not match`() {
        val replacedPromql =
            """sum(count_over_time(bk_monitor_table:dtEventTimestamp{dimensions__bk_46__projectId="wrong-project"}[10800s]))"""
        val projectId = "test-project"

        val exception = assertThrows<IllegalArgumentException> {
            metricsQueryService.validateReplacedPromql(replacedPromql, projectId)
        }

        Assertions.assertEquals("promql必须包含{{projectId}}占位符", exception.message)
    }

    @Test
    fun `test validateReplacedPromql should throw exception when still contains placeholder`() {
        val replacedPromql =
            """sum(count_over_time(bk_monitor_table:dtEventTimestamp{dimensions__bk_46__projectId="{{projectId}}"}[10800s]))"""
        val projectId = "test-project"

        val exception = assertThrows<IllegalArgumentException> {
            metricsQueryService.validateReplacedPromql(replacedPromql, projectId)
        }
    }

    @Test
    fun `test validateReplacedPromql should pass when contains multiple same project ids`() {
        val replacedPromql =
            """sum(count_over_time(bk_monitor_table:dtEventTimestamp{dimensions__bk_46__projectId="test-project"}[10800s])) + sum(count_over_time(bk_monitor_table:dtEventTimestamp{dimensions__bk_46__projectId="test-project"}[10800s]))"""
        val projectId = "test-project"

        Assertions.assertDoesNotThrow {
            metricsQueryService.validateReplacedPromql(replacedPromql, projectId)
        }
    }

    @Test
    fun `test validateReplacedPromql should throw exception when not contains projectId dimension`() {
        val replacedPromql =
            """sum(count_over_time(bk_monitor_table:dtEventTimestamp{other_dimension="value"}[10800s]))"""
        val projectId = "test-project"

        val exception = assertThrows<IllegalArgumentException> {
            metricsQueryService.validateReplacedPromql(replacedPromql, projectId)
        }

        Assertions.assertEquals("promql必须包含{{projectId}}占位符", exception.message)
    }

    @Test
    fun `test validateReplacedPromql should pass when using table_agent table name`() {
        val replacedPromql =
            """count((100 - min by (hostIp, hostName) (bk_monitor_agent_table:cpu_detail:idle{projectId="test-project"})) > 80)"""
        val projectId = "test-project"

        // 需要临时修改monitorTable为agent表名
        val originalTable = ReflectionTestUtils.getField(metricsQueryService, "monitorTable")
        ReflectionTestUtils.setField(metricsQueryService, "monitorTable", "bk_monitor_agent_table")

        try {
            Assertions.assertDoesNotThrow {
                metricsQueryService.validateReplacedPromql(replacedPromql, projectId)
            }
        } finally {
            // 恢复原值
            ReflectionTestUtils.setField(metricsQueryService, "monitorTable", originalTable)
        }
    }
}
