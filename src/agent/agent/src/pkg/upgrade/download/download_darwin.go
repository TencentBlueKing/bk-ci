//go:build darwin
// +build darwin

package download

import (
	"path/filepath"
	"runtime"
	"strings"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

func getServerFileArch() string {
	var osArch string
	if runtime.GOARCH == "arm64" {
		osArch = "_macos_arm64"
	} else {
		osArch = "_macos"
	}
	return osArch
}

func DownloadUpgradeFile(saveDir string) (string, error) {
	return api.DownloadUpgradeFile(
		"upgrade/upgrader"+getServerFileArch(), saveDir+"/"+config.UpgraderFileClientLinux,
	)
}

func DownloadDaemonFile(saveDir string) (string, error) {
	return api.DownloadUpgradeFile(
		"upgrade/devopsDaemon"+getServerFileArch(), saveDir+"/"+config.DaemonFileClientLinux,
	)
}

func DownloadAgentFile(saveDir string) (string, error) {
	return api.DownloadUpgradeFile(
		"upgrade/devopsAgent"+getServerFileArch(), saveDir+"/"+config.AgentFileClientLinux,
	)
}

func DownloadJdkFile(saveDir string) (string, error) {
	return api.DownloadUpgradeFile(
		"jre/"+strings.TrimPrefix(getServerFileArch(), "_")+"/jdk17.zip", saveDir+"/"+config.Jdk17ClientFile,
	)
}

func DownloadDockerInitFile(saveDir string) (string, error) {
	return api.DownloadUpgradeFile(
		"script/macos/agent_docker_init.sh", filepath.Join(saveDir, config.DockerInitFile),
	)
}
