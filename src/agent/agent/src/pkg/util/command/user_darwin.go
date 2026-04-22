//go:build darwin
// +build darwin

package command

import (
	"os/exec"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

func SetUser(_ *exec.Cmd, runUser string) error {
	if runUser != "" {
		logs.Info("set user(darwin): ignored, user switching is Linux-only")
	}
	return nil
}
