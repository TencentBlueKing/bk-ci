//go:build windows
// +build windows

package monitor

import (
	"os/exec"
	"syscall"
)

// setChildProcAttr 在 windows 上隐藏 monitor 子进程的控制台窗口，避免
// service / 后台运行时弹出黑框。CREATE_NEW_PROCESS_GROUP 让子进程独立
// 进程组，便于信号隔离。
func setChildProcAttr(cmd *exec.Cmd) {
	if cmd.SysProcAttr == nil {
		cmd.SysProcAttr = &syscall.SysProcAttr{}
	}
	cmd.SysProcAttr.HideWindow = true
	cmd.SysProcAttr.CreationFlags |= syscall.CREATE_NEW_PROCESS_GROUP
}

// killChild 在 windows 上直接 kill 子进程。windows 无进程组负 pid 语义，
// 依赖 Process.Kill（TerminateProcess）。gopsutil 在 windows 走 PDH 不 fork
// 外部命令，孙进程残留风险低。
func killChild(cmd *exec.Cmd) {
	if cmd.Process == nil {
		return
	}
	_ = cmd.Process.Kill()
}
