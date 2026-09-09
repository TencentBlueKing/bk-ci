package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpClientUpgrade
import com.tencent.devops.remotedev.pojo.ClientUpgradeComp
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientOS
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientUpgradeOpType
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientUpgradeVersions
import com.tencent.devops.remotedev.pojo.clientupgrade.UpgradeVersionsData
import com.tencent.devops.remotedev.service.clientupgrade.ClientChannelUpgradeService
import com.tencent.devops.remotedev.service.clientupgrade.UpgradeProps
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpClientUpgradeImpl @Autowired constructor(
    private val upgradeProps: UpgradeProps,
    private val clientChannelUpgradeService: ClientChannelUpgradeService
) : OpClientUpgrade {
    override fun getVersions(): Result<ClientUpgradeVersions> {
        return Result(
            ClientUpgradeVersions(
                parallelUpgradeCount = upgradeProps.getMaxParallelUpgradeCount(),
                macosClientVersion = upgradeVersionsData(ClientUpgradeComp.CLIENT, ClientOS.MACOS),
                winClientVersion = upgradeVersionsData(ClientUpgradeComp.CLIENT, ClientOS.WINDOWS),
                macosStartVersion = upgradeVersionsData(ClientUpgradeComp.START, ClientOS.MACOS),
                winStartVersion = upgradeVersionsData(ClientUpgradeComp.START, ClientOS.WINDOWS),
                andrClientVersion = upgradeVersionsData(ClientUpgradeComp.CLIENT, ClientOS.ANDR),
                iosAppVersion = upgradeVersionsData(ClientUpgradeComp.APP, ClientOS.IOS),
                androidAppVersion = upgradeVersionsData(ClientUpgradeComp.APP, ClientOS.ANDROID),
                ohosAppVersion = upgradeVersionsData(ClientUpgradeComp.APP, ClientOS.OHOS)
            )
        )
    }

    private fun upgradeVersionsData(
        type: ClientUpgradeComp,
        os: ClientOS
    ): UpgradeVersionsData {
        return UpgradeVersionsData(
            currentVersion = upgradeProps.getCurrentVersion(type, os),
            maxCanUpgradeNumber = upgradeProps.getMaxNumb(type, os),
            userVersion = upgradeProps.getUserVersion(type, os),
            workspaceNameVersion = upgradeProps.getWorkspaceNameVersion(type, os),
            projectVersion = upgradeProps.getProjectVersion(type, os)
        )
    }

    override fun setParallelUpgradeCount(parallelUpgradeCount: Int): Result<Boolean> {
        upgradeProps.setMaxParallelUpgradeCount(parallelUpgradeCount)
        return Result(true)
    }

    override fun setCurrentVersion(type: ClientUpgradeComp, os: ClientOS, version: String): Result<Boolean> {
        upgradeProps.setCurrentVersion(type, os, version)
        return Result(true)
    }

    override fun setMaxNumber(type: ClientUpgradeComp, os: ClientOS, maxNumber: Int): Result<Boolean> {
        upgradeProps.setMaxNumb(type, os, maxNumber)
        return Result(true)
    }

    override fun setUserVersion(
        type: ClientUpgradeComp,
        os: ClientOS,
        opType: ClientUpgradeOpType,
        data: Map<String, String>
    ): Result<Boolean> {
        upgradeProps.setUserVersion(type, os, data, opType)
        return Result(true)
    }

    override fun setWorkspaceNameVersion(
        type: ClientUpgradeComp,
        os: ClientOS,
        opType: ClientUpgradeOpType,
        data: Map<String, String>
    ): Result<Boolean> {
        upgradeProps.setWorkspaceNameVersion(type, os, data, opType)
        return Result(true)
    }

    override fun setProjectVersion(
        type: ClientUpgradeComp,
        os: ClientOS,
        opType: ClientUpgradeOpType,
        data: Map<String, String>
    ): Result<Boolean> {
        upgradeProps.setProjectVersion(type, os, data, opType)
        return Result(true)
    }

    override fun setChannelVersion(version: String): Result<Boolean> {
        clientChannelUpgradeService.setChannelVersion(version)
        return Result(true)
    }
}