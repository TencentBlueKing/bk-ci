//go:build darwin
// +build darwin

package download

import (
	"runtime"
	"strings"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/pkg/errors"
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
	return "", errors.New("not support macos use docker agent")
}
