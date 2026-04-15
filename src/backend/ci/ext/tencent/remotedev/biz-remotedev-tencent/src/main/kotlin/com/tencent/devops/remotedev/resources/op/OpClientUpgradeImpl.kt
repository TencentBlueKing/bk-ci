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
                macosClientVersion = UpgradeVersionsData(
                    currentVersion = upgradeProps.getCurrentVersion(ClientUpgradeComp.CLIENT, ClientOS.MACOS),
                    maxCanUpgradeNumber = upgradeProps.getMaxNumb(ClientUpgradeComp.CLIENT, ClientOS.MACOS),
                    userVersion = upgradeProps.getUserVersion(ClientUpgradeComp.CLIENT, ClientOS.MACOS),
                    workspaceNameVersion = upgradeProps.getWorkspaceNameVersion(
                        ClientUpgradeComp.CLIENT,
                        ClientOS.MACOS
                    ),
                    projectVersion = upgradeProps.getProjectVersion(ClientUpgradeComp.CLIENT, ClientOS.MACOS)
                ),
                winClientVersion = UpgradeVersionsData(
                    currentVersion = upgradeProps.getCurrentVersion(ClientUpgradeComp.CLIENT, ClientOS.WINDOWS),
                    maxCanUpgradeNumber = upgradeProps.getMaxNumb(ClientUpgradeComp.CLIENT, ClientOS.WINDOWS),
                    userVersion = upgradeProps.getUserVersion(ClientUpgradeComp.CLIENT, ClientOS.WINDOWS),
                    workspaceNameVersion = upgradeProps.getWorkspaceNameVersion(
                        ClientUpgradeComp.CLIENT,
                        ClientOS.WINDOWS
                    ),
                    projectVersion = upgradeProps.getProjectVersion(ClientUpgradeComp.CLIENT, ClientOS.WINDOWS)
                ),
                macosStartVersion = UpgradeVersionsData(
                    currentVersion = upgradeProps.getCurrentVersion(ClientUpgradeComp.START, ClientOS.MACOS),
                    maxCanUpgradeNumber = upgradeProps.getMaxNumb(ClientUpgradeComp.START, ClientOS.MACOS),
                    userVersion = upgradeProps.getUserVersion(ClientUpgradeComp.START, ClientOS.MACOS),
                    workspaceNameVersion = upgradeProps.getWorkspaceNameVersion(
                        ClientUpgradeComp.START,
                        ClientOS.MACOS
                    ),
                    projectVersion = upgradeProps.getProjectVersion(ClientUpgradeComp.START, ClientOS.MACOS)
                ),
                winStartVersion = UpgradeVersionsData(
                    currentVersion = upgradeProps.getCurrentVersion(ClientUpgradeComp.START, ClientOS.WINDOWS),
                    maxCanUpgradeNumber = upgradeProps.getMaxNumb(ClientUpgradeComp.START, ClientOS.WINDOWS),
                    userVersion = upgradeProps.getUserVersion(ClientUpgradeComp.START, ClientOS.WINDOWS),
                    workspaceNameVersion = upgradeProps.getWorkspaceNameVersion(
                        ClientUpgradeComp.START,
                        ClientOS.WINDOWS
                    ),
                    projectVersion = upgradeProps.getProjectVersion(ClientUpgradeComp.START, ClientOS.WINDOWS)
                ),
                andrClientVersion = UpgradeVersionsData(
                    currentVersion = upgradeProps.getCurrentVersion(ClientUpgradeComp.CLIENT, ClientOS.ANDR),
                    maxCanUpgradeNumber = upgradeProps.getMaxNumb(ClientUpgradeComp.CLIENT, ClientOS.ANDR),
                    userVersion = upgradeProps.getUserVersion(ClientUpgradeComp.CLIENT, ClientOS.ANDR),
                    workspaceNameVersion = upgradeProps.getWorkspaceNameVersion(
                        ClientUpgradeComp.CLIENT,
                        ClientOS.ANDR
                    ),
                    projectVersion = upgradeProps.getProjectVersion(ClientUpgradeComp.CLIENT, ClientOS.ANDR)
                )
            )
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