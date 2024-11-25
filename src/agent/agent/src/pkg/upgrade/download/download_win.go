//go:build windows
// +build windows

package download

import (
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/pkg/errors"
)

func DownloadUpgradeFile(saveDir string) (string, error) {
	return api.DownloadUpgradeFile(
		"upgrade/upgrader.exe", saveDir+"/"+config.UpgraderFileClientWindows,
	)
}

func DownloadDaemonFile(saveDir string) (string, error) {
	return api.DownloadUpgradeFile(
		"upgrade/devopsDaemon.exe", saveDir+"/"+config.DaemonFileClientWindows,
	)
}

func DownloadAgentFile(saveDir string) (string, error) {
	return api.DownloadUpgradeFile(
		"upgrade/devopsAgent.exe", saveDir+"/"+config.AgentFileClientWindows,
	)
}

func DownloadJdkFile(saveDir string) (string, error) {
	return api.DownloadUpgradeFile(
		"jre/windows/jdk17.zip", saveDir+"/"+config.Jdk17ClientFile,
	)
}

func DownloadDockerInitFile(saveDir string) (string, error) {
	return "", errors.New("not support windows use docker agent")
}
