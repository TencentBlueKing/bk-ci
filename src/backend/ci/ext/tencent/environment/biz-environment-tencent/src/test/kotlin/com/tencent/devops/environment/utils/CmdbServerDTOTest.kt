package com.tencent.devops.environment.utils

import com.tencent.devops.environment.pojo.cmdb.common.CmdbServerDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CmdbServerDTOTest {
    @Test
    fun testGetBakOperatorStrAndOsNameLessThanMaxLength() {
        // 1.被截断
        var cmdbServerDTO = CmdbServerDTO(
            serverId = 1L,
            ip = "127.0.0.1",
            operator = "user1",
            bakOperatorList = listOf(
                "user123456", "user123456", "user123456", "user123456", "user123456", "user123456",
                "user123456", "user123456", "user123456", "user123456", "user123456", "user123456",
                "user123456", "user123456", "user123456", "user123456", "user123456", "user123456",
                "user123456", "user123456", "user123456", "user123456", "user123456", "user123456",
                "user123456", "user456789"
            ),
            lanIpList = emptyList(),
            deptId = null,
            hostName = null,
            osName = "01234567890123456789012345678901234567890123456789012345678901234567890123456789" +
                "01234567890123456789012345678901234567890123456789"
        )
        assertThat(cmdbServerDTO.getBakOperatorStrLessThanMaxLength().length).isEqualTo(252)
        assertThat(cmdbServerDTO.getBakOperatorStrLessThanMaxLength()).endsWith("user123456")
        assertThat(cmdbServerDTO.getOsNameLessThanMaxLength()!!.length).isEqualTo(128)
        assertThat(cmdbServerDTO.getOsNameLessThanMaxLength()).endsWith("01234567")
        // 2.不被截断
        cmdbServerDTO = CmdbServerDTO(
            serverId = 1L,
            ip = "127.0.0.1",
            operator = "user1",
            bakOperatorList = listOf(
                "user123456", "user123456", "user123456", "user123456", "user123456", "user123456",
                "user123456", "user123456", "user123456", "user123456", "user123456", "user123456",
                "user123456", "user123456", "user123456", "user123456", "user123456", "user456789"
            ),
            lanIpList = emptyList(),
            deptId = null,
            hostName = null,
            osName = "osName-1"
        )
        assertThat(cmdbServerDTO.getBakOperatorStrLessThanMaxLength()).endsWith("user456789")
        assertThat(cmdbServerDTO.getOsNameLessThanMaxLength()).isEqualTo("osName-1")
        // 3.边界值
        cmdbServerDTO = CmdbServerDTO(
            serverId = 1L,
            ip = "127.0.0.1",
            operator = "user1",
            bakOperatorList = null,
            lanIpList = emptyList(),
            deptId = null,
            hostName = null,
            osName = null
        )
        assertThat(cmdbServerDTO.getBakOperatorStrLessThanMaxLength().length).isEqualTo(0)
        assertThat(cmdbServerDTO.getOsNameLessThanMaxLength()).isNull()
        cmdbServerDTO = CmdbServerDTO(
            serverId = 1L,
            ip = "127.0.0.1",
            operator = "user1",
            bakOperatorList = listOf(),
            lanIpList = emptyList(),
            deptId = null,
            hostName = null,
            osName = ""
        )
        assertThat(cmdbServerDTO.getBakOperatorStrLessThanMaxLength().length).isEqualTo(0)
        assertThat(cmdbServerDTO.getOsNameLessThanMaxLength()).isEqualTo("")
    }
}
