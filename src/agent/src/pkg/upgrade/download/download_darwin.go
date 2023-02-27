//go:build darwin
// +build darwin

package download

import (
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/pkg/errors"
	"runtime"
	"strings"
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
		"jre/"+strings.TrimPrefix(getServerFileArch(), "_")+"/jre.zip", saveDir+"/"+config.JdkClientFile,
	)
}

func DownloadDockerInitFile(saveDir string) (string, error) {
	return "", errors.New("not support macos use docker agent")
}
