package com.tencent.devops.common.api.enum

import org.junit.Assert
import org.junit.Test

/**
 *
 * @version 1.0
 */
class SystemModuleEnumTest {
    @Test
    fun getSystemModule() {
        Assert.assertEquals(SystemModuleEnum.COMMON.name, SystemModuleEnum.getSystemModule("00"))
        Assert.assertEquals(SystemModuleEnum.TASK.name, SystemModuleEnum.getSystemModule("01"))
        Assert.assertEquals(SystemModuleEnum.RULE.name, SystemModuleEnum.getSystemModule("02"))
    }
}
