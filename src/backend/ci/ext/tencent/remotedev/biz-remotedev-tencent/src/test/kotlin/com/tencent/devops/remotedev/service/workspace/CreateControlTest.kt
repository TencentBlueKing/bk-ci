package com.tencent.devops.remotedev.service.workspace

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.remotedev.ResourceVmRespData
import com.tencent.devops.remotedev.utils.CommonUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CreateControlTest {

    companion object {
        const val sumResourceVmFreeJson = "[\n" +
            "    {\n" +
            "        \"zoneId\": \"CQ1\",\n" +
            "        \"machineResources\": [\n" +
            "            {\n" +
            "                \"cap\": 216,\n" +
            "                \"used\": 70,\n" +
            "                \"free\": 146,\n" +
            "                \"machineType\": \"M\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"machineType\": \"M+\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"cap\": 200,\n" +
            "                \"used\": 46,\n" +
            "                \"free\": 154,\n" +
            "                \"machineType\": \"S\"\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "   {\n" +
            "        \"zoneId\": \"CQ2\",\n" +
            "        \"machineResources\": [\n" +
            "            {\n" +
            "                \"cap\": 216,\n" +
            "                \"used\": 70,\n" +
            "                \"free\": 155,\n" +
            "                \"machineType\": \"M\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"machineType\": \"M+\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"cap\": 200,\n" +
            "                \"used\": 46,\n" +
            "                \"free\": 154,\n" +
            "                \"machineType\": \"S\"\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "   {\n" +
            "        \"zoneId\": \"CQ20\",\n" +
            "        \"machineResources\": [\n" +
            "            {\n" +
            "                \"cap\": 216,\n" +
            "                \"used\": 70,\n" +
            "                \"free\": 155,\n" +
            "                \"machineType\": \"M\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"machineType\": \"M+\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"cap\": 200,\n" +
            "                \"used\": 46,\n" +
            "                \"free\": 154,\n" +
            "                \"machineType\": \"S\"\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "    {\n" +
            "        \"zoneId\": \"GZ2\",\n" +
            "        \"machineResources\": [\n" +
            "            {\n" +
            "                \"cap\": 128,\n" +
            "                \"used\": 43,\n" +
            "                \"free\": 85,\n" +
            "                \"machineType\": \"M\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"cap\": 232,\n" +
            "                \"used\": 229,\n" +
            "                \"free\": 3,\n" +
            "                \"machineType\": \"M+\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"cap\": 200,\n" +
            "                \"used\": 195,\n" +
            "                \"free\": 5,\n" +
            "                \"machineType\": \"S\"\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "    {\n" +
            "        \"zoneId\": \"NJ1\",\n" +
            "        \"machineResources\": [\n" +
            "            {\n" +
            "                \"machineType\": \"M\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"machineType\": \"M+\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"cap\": 88,\n" +
            "                \"used\": 12,\n" +
            "                \"free\": 76,\n" +
            "                \"machineType\": \"S\"\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "    {\n" +
            "        \"zoneId\": \"SH1\",\n" +
            "        \"machineResources\": [\n" +
            "            {\n" +
            "                \"machineType\": \"M\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"cap\": 52,\n" +
            "                \"used\": 24,\n" +
            "                \"free\": 28,\n" +
            "                \"machineType\": \"M+\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"machineType\": \"S\"\n" +
            "            }\n" +
            "        ]\n" +
            "    },\n" +
            "    {\n" +
            "        \"zoneId\": \"SZ3\",\n" +
            "        \"machineResources\": [\n" +
            "            {\n" +
            "                \"machineType\": \"M\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"machineType\": \"M+\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"machineType\": \"S\"\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "]"
    }

    val spec = lazy { listOf("CQ20") }

    @Test
    fun sumResourceVmFree() {
        val res = JsonUtil.getObjectMapper()
            .readValue(sumResourceVmFreeJson, object : TypeReference<List<ResourceVmRespData>>() {})
        Assertions.assertEquals(
            CommonUtil.parseResourceVmRespData(
                res,
                WindowsResourceZoneConfig(
                    id = null,
                    available = null,
                    zone = "重启",
                    zoneShortName = "CQ",
                    description = "",
                    type = WindowsResourceZoneConfigType.DEFAULT
                ),
                spec,
                "M"
            ).values.sum(), 146 + 155
        )
    }

    @Test
    fun sumResourceVmFree2() {
        val res = JsonUtil.getObjectMapper()
            .readValue(sumResourceVmFreeJson, object : TypeReference<List<ResourceVmRespData>>() {})
        Assertions.assertEquals(
            CommonUtil.parseResourceVmRespData(
                res,
                WindowsResourceZoneConfig(
                    id = null,
                    available = null,
                    zone = "csig专区",
                    zoneShortName = "CQ20",
                    description = "",
                    type = WindowsResourceZoneConfigType.CSIG_USE
                ),
                spec,
                "M"
            ).values.sum(), 155
        )
    }
}
