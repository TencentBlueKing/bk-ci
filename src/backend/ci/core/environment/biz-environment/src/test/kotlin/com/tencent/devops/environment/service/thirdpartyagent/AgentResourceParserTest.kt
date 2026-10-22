package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.environment.pojo.AgentResourceSnapshot
import com.tencent.devops.environment.pojo.ResourceRemaining
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AgentResourceParserTest {
    private val now = 1_800_000_000_000L
    private fun metric(name: String, fields: String, tags: String = "{}", time: Long = now / 1000) =
        """{"name":"$name","timestamp":$time,"fields":$fields,"tags":$tags}"""
    private fun parse(vararg data: String, path: String? = "/data/agent") =
        AgentResourceParser.parse("""{"metrics":[${data.joinToString(",")}]}""", path, now)

    @Test
    fun `whole CPU available memory and containing disk are selected`() {
        val data = parse(
            metric("cpu_detail", """{"idle":20}""", """{"cpu":"cpu-total"}"""),
            metric("cpu_detail", """{"idle":99}""", """{"cpu":"cpu0"}"""),
            metric("mem", """{"available":40,"total":100,"free":1}"""),
            metric("disk", """{"total":1000,"free":900}""", """{"path":"/"}"""),
            metric("disk", """{"total":100,"free":8}""", """{"path":"/data"}"""),
            metric("disk", """{"total":999999,"free":999999}""", """{"path":"/other"}""")
        )
        assertEquals(20.0, data.cpu)
        assertEquals(40.0, data.memory)
        assertEquals(8.0, data.disk)
    }

    @Test
    fun `unknown disk path is not replaced by largest disk and free memory is not available memory`() {
        val data = parse(metric("mem", """{"free":80,"total":100}"""),
            metric("disk", """{"free":80,"total":100}""", """{"path":"/"}"""), path = null)
        assertNull(data.memory)
        assertNull(data.disk)
    }

    @Test
    fun `expiry boundary future timestamps and partial metrics`() {
        assertEquals(50.0, parse(metric("mem", """{"available_percent":50}""", time = now / 1000 - 180)).memory)
        assertNull(parse(metric("mem", """{"available_percent":50}""", time = now / 1000 - 181)).memory)
        assertNull(parse(metric("mem", """{"available_percent":50}""", time = now / 1000 + 1)).memory)
        assertNull(parse(metric("mem", """{"available_percent":101}""")).memory)
        assertNull(AgentResourceSnapshot.fresh(Double.NaN, now, now))
    }

    @Test
    fun `single metric and legacy core format are accepted`() {
        val result = AgentResourceParser.parse(
            metric("cpu", """{"usage_idle":30}""", """{"cpu":"cpu-total"}"""), null, now
        )
        assertEquals(30.0, result.cpu)
        assertNull(result.memory)
    }

    @Test
    fun `strict integer threshold rejects decimals strings endpoints and empty selection`() {
        val mapper = JsonUtil.getObjectMapper()
        for (value in listOf("0", "100", "1.5", "\"20\"", "-1")) {
            assertThrows(Exception::class.java) {
                mapper.readValue("""{"cpu":$value}""", ResourceRemaining::class.java)
            }
        }
        assertEquals(20, mapper.readValue("""{"cpu":20}""", ResourceRemaining::class.java).cpu)
        assertThrows(Exception::class.java) { ResourceRemaining().validate() }
    }
}
