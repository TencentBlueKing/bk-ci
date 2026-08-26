//go:build !windows && !loong64
// +build !windows,!loong64

package monitor

import (
	"os/exec"
	"syscall"
)

// setChildProcAttr 让 monitor 子进程运行在独立进程组（Setpgid），这样主进程
// 退出/取消时可以用负 pid 一次性 kill 整个进程组（含 gopsutil fork 出的
// netstat/lsof/ps 等孙进程），避免残留。
func setChildProcAttr(cmd *exec.Cmd) {
	if cmd.SysProcAttr == nil {
		cmd.SysProcAttr = &syscall.SysProcAttr{}
	}
	cmd.SysProcAttr.Setpgid = true
}

// killChild kill 子进程所在的整个进程组（负 pid）。SIGKILL 保证即使子进程
// 卡在不可中断的 syscall（darwin 休眠冻结）也尽量清理其可被杀死的孙进程。
func killChild(cmd *exec.Cmd) {
	if cmd.Process == nil {
		return
	}
	pid := cmd.Process.Pid
	// 负 pid = 整个进程组。Setpgid=true 时子进程 pgid == 其 pid。
	_ = syscall.Kill(-pid, syscall.SIGKILL)
	// 兜底：再单独 kill 一次子进程本身。
	_ = cmd.Process.Kill()
}
