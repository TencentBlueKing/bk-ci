package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpClientUpgrade
import com.tencent.devops.remotedev.pojo.ClientUpgradeOpType
import com.tencent.devops.remotedev.pojo.ClientUpgradeType
import com.tencent.devops.remotedev.service.clientupgrade.UpgradeProps
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpClientUpgradeImpl @Autowired constructor(
    private val upgradeProps: UpgradeProps
) : OpClientUpgrade {
    override fun setParallelUpgradeCount(parallelUpgradeCount: Int): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun setCurrentVersion(type: ClientUpgradeType, version: String): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun setMaxNumber(type: ClientUpgradeType, maxNumber: Int): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun setUserVersion(
        type: ClientUpgradeType,
        opType: ClientUpgradeOpType,
        data: Map<String, String>
    ): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun setProjectVersion(
        type: ClientUpgradeType,
        opType: ClientUpgradeOpType,
        data: Map<String, String>
    ): Result<Boolean> {
        TODO("Not yet implemented")
    }
}