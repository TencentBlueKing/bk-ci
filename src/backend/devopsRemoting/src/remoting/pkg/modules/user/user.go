package user

import (
	"os/exec"
	"syscall"
)

func RunAsDevopsRemotingUser(cmd *exec.Cmd) *exec.Cmd {
	if cmd.SysProcAttr == nil {
		cmd.SysProcAttr = &syscall.SysProcAttr{}
	}
	if cmd.SysProcAttr.Credential == nil {
		cmd.SysProcAttr.Credential = &syscall.Credential{}
	}
	// TODO: 暂时使用root
	// cmd.SysProcAttr.Credential.Uid = devopsRemotingUID
	// cmd.SysProcAttr.Credential.Gid = devopsRemotingGID
	return cmd
}
