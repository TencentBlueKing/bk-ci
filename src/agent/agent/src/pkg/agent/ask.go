package agent

import (
	"runtime"

	"github.com/TencentBlueKing/bk-ci/agent/src/third_components"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	exitcode "github.com/TencentBlueKing/bk-ci/agent/src/pkg/exiterror"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/job"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/upgrade"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
)

func genHeartInfoAndUpgrade(
	upgradeEnable bool,
	exitError *exitcode.ExitErrorType,
) (api.AgentHeartbeatInfo, *api.UpgradeInfo) {
	var taskList []api.ThirdPartyTaskInfo
	for _, info := range job.GBuildManager.GetInstances() {
		taskList = append(taskList, api.ThirdPartyTaskInfo{
			ProjectId: info.ProjectId,
			BuildId:   info.BuildId,
			VmSeqId:   info.VmSeqId,
			Workspace: info.Workspace,
		})
	}

	if err := third_components.Jdk.Jdk17.SyncJdkVersion(); err != nil {
		logs.Error("ask sync jdkVersion error", err)
	}
	if err := upgrade.SyncDockerInitFileMd5(); err != nil {
		logs.Error("ask sync docker file md5 error", err)
	}
	jdkVersion := third_components.Jdk.Jdk17.GetVersion()
	dockerInitFile := api.DockerInitFileInfo{
		FileMd5:     upgrade.DockerFileMd5.Md5,
		NeedUpgrade: upgrade.DockerFileMd5.NeedUpgrade,
	}

	var upg *api.UpgradeInfo = nil
	if upgradeEnable {
		upg = &api.UpgradeInfo{
			WorkerVersion:      third_components.Worker.GetVersion(),
			GoAgentVersion:     config.AgentVersion,
			JdkVersion:         jdkVersion,
			DockerInitFileInfo: dockerInitFile,
		}
	}

	return api.AgentHeartbeatInfo{
		MasterVersion:     config.AgentVersion,
		SlaveVersion:      third_components.Worker.GetVersion(),
		HostName:          config.GAgentEnv.HostName,
		AgentIp:           config.GAgentEnv.GetAgentIp(),
		ParallelTaskCount: config.GAgentConfig.ParallelTaskCount,
		AgentInstallPath:  systemutil.GetExecutableDir(),
		StartedUser:       systemutil.GetCurrentUser().Username,
		TaskList:          taskList,
		Props: api.AgentPropsInfo{
			Arch:              runtime.GOARCH,
			JdkVersion:        jdkVersion,
			DockerInitFileMd5: dockerInitFile,
			OsVersion:         config.GAgentEnv.OsVersion,
		},
		DockerParallelTaskCount: config.GAgentConfig.DockerParallelTaskCount,
		DockerTaskList:          job.GBuildDockerManager.GetInstances(),
		ErrorExitData:           exitError,
	}, upg
}

func genAskEnable() api.AskEnable {
	return api.AskEnable{
		Build:       checkBuildType(),
		Upgrade:     checkUpgrade(),
		DockerDebug: checkDockerDebug(),
		Pipeline:    config.GAgentConfig.EnablePipeline,
	}
}

func checkBuildType() api.BuildJobType {
	dockerCanRun, normalCanRun := job.CheckParallelTaskCount()
	if !dockerCanRun && !normalCanRun {
		return api.NoneBuildType
	}
	if dockerCanRun && normalCanRun {
		return api.AllBuildType
	} else if normalCanRun {
		return api.BinaryBuildType
	} else {
		return api.DockerBuildType
	}
}

func checkUpgrade() bool {
	// debug模式下关闭升级，方便调试问题
	if config.IsDebug {
		logs.Debug("debug no upgrade")
		return false
	}
	if job.CheckRunningJob() {
		return false
	}
	return true
}

func checkDockerDebug() bool {
	if systemutil.IsLinux() && config.GAgentConfig.EnableDockerBuild {
		return true
	}
	return false
}
