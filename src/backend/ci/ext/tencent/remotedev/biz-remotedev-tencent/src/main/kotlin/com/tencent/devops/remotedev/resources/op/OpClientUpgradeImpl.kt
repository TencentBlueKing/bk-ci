package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpClientUpgrade
import com.tencent.devops.remotedev.pojo.ClientUpgradeComp
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientUpgradeOpType
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientUpgradeVersions
import com.tencent.devops.remotedev.pojo.clientupgrade.UpgradeVersionsData
import com.tencent.devops.remotedev.service.clientupgrade.UpgradeProps
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpClientUpgradeImpl @Autowired constructor(
    private val upgradeProps: UpgradeProps
) : OpClientUpgrade {
    override fun getVersions(): Result<ClientUpgradeVersions> {
        return Result(
            ClientUpgradeVersions(
                parallelUpgradeCount = upgradeProps.getMaxParallelUpgradeCount(),
                macosClientVersion = UpgradeVersionsData(
                    currentVersion = upgradeProps.getCurrentVersion(ClientUpgradeComp.CLIENT, OS.MACOS),
                    maxCanUpgradeNumber = upgradeProps.getMaxNumb(ClientUpgradeComp.CLIENT, OS.MACOS),
                    userVersion = upgradeProps.getUserVersion(ClientUpgradeComp.CLIENT, OS.MACOS),
                    workspaceNameVersion = upgradeProps.getWorkspaceNameVersion(ClientUpgradeComp.CLIENT, OS.MACOS),
                    projectVersion = upgradeProps.getProjectVersion(ClientUpgradeComp.CLIENT, OS.MACOS)
                ),
                winClientVersion = UpgradeVersionsData(
                    currentVersion = upgradeProps.getCurrentVersion(ClientUpgradeComp.CLIENT, OS.WINDOWS),
                    maxCanUpgradeNumber = upgradeProps.getMaxNumb(ClientUpgradeComp.CLIENT, OS.WINDOWS),
                    userVersion = upgradeProps.getUserVersion(ClientUpgradeComp.CLIENT, OS.WINDOWS),
                    workspaceNameVersion = upgradeProps.getWorkspaceNameVersion(ClientUpgradeComp.CLIENT, OS.WINDOWS),
                    projectVersion = upgradeProps.getProjectVersion(ClientUpgradeComp.CLIENT, OS.WINDOWS)
                ),
                macosStartVersion = UpgradeVersionsData(
                    currentVersion = upgradeProps.getCurrentVersion(ClientUpgradeComp.START, OS.MACOS),
                    maxCanUpgradeNumber = upgradeProps.getMaxNumb(ClientUpgradeComp.START, OS.MACOS),
                    userVersion = upgradeProps.getUserVersion(ClientUpgradeComp.START, OS.MACOS),
                    workspaceNameVersion = upgradeProps.getWorkspaceNameVersion(ClientUpgradeComp.START, OS.MACOS),
                    projectVersion = upgradeProps.getProjectVersion(ClientUpgradeComp.START, OS.MACOS)
                ),
                winStartVersion = UpgradeVersionsData(
                    currentVersion = upgradeProps.getCurrentVersion(ClientUpgradeComp.START, OS.WINDOWS),
                    maxCanUpgradeNumber = upgradeProps.getMaxNumb(ClientUpgradeComp.START, OS.WINDOWS),
                    userVersion = upgradeProps.getUserVersion(ClientUpgradeComp.START, OS.WINDOWS),
                    workspaceNameVersion = upgradeProps.getWorkspaceNameVersion(ClientUpgradeComp.START, OS.WINDOWS),
                    projectVersion = upgradeProps.getProjectVersion(ClientUpgradeComp.START, OS.WINDOWS)
                )
            )
        )
    }

    override fun setParallelUpgradeCount(parallelUpgradeCount: Int): Result<Boolean> {
        upgradeProps.setMaxParallelUpgradeCount(parallelUpgradeCount)
        return Result(true)
    }

    override fun setCurrentVersion(type: ClientUpgradeComp, os: OS, version: String): Result<Boolean> {
        upgradeProps.setCurrentVersion(type, os, version)
        return Result(true)
    }

    override fun setMaxNumber(type: ClientUpgradeComp, os: OS, maxNumber: Int): Result<Boolean> {
        upgradeProps.setMaxNumb(type, os, maxNumber)
        return Result(true)
    }

    override fun setUserVersion(
        type: ClientUpgradeComp,
        os: OS,
        opType: ClientUpgradeOpType,
        data: Map<String, String>
    ): Result<Boolean> {
        upgradeProps.setUserVersion(type, os, data, opType)
        return Result(true)
    }

    override fun setWorkspaceNameVersion(
        type: ClientUpgradeComp,
        os: OS,
        opType: ClientUpgradeOpType,
        data: Map<String, String>
    ): Result<Boolean> {
        upgradeProps.setWorkspaceNameVersion(type, os, data, opType)
        return Result(true)
    }

    override fun setProjectVersion(
        type: ClientUpgradeComp,
        os: OS,
        opType: ClientUpgradeOpType,
        data: Map<String, String>
    ): Result<Boolean> {
        upgradeProps.setProjectVersion(type, os, data, opType)
        return Result(true)
    }
}