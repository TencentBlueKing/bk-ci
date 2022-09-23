//go:build !windows && !darwin
// +build !windows,!darwin

package download

import (
	"github.com/Tencent/bk-ci/src/agent/src/pkg/api"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/config"
	"runtime"
	"strings"
)

func getServerFileArch() string {
	var osArch string
	if runtime.GOARCH == "arm64" {
		osArch = "_linux_arm64"
	} else if runtime.GOARCH == "mips64" {
		osArch = "_linux_mips64"
	} else {
		osArch = "_linux"
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
