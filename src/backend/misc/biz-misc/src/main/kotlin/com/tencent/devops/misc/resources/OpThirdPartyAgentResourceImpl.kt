package com.tencent.devops.misc.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.misc.api.OpThirdPartyAgentResource
import com.tencent.devops.misc.service.UpgradeService
import org.springframework.beans.factory.annotation.Autowired

/**
 * deng
 * 2018/5/9
 */
@RestResource
class OpThirdPartyAgentResourceImpl @Autowired constructor(
    private val upgradeService: UpgradeService
) : OpThirdPartyAgentResource {

    override fun setMaxParallelUpgradeCount(maxParallelUpgradeCount: Int): Result<Boolean> {
        upgradeService.setMaxParallelUpgradeCount(maxParallelUpgradeCount)
        return Result(true)
    }

    override fun getMaxParallelUpgradeCount(): Result<Int> {
        return Result(upgradeService.getMaxParallelUpgradeCount())
    }
}