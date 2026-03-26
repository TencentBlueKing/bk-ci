//go:build linux
// +build linux

package agentcli

import (
	"fmt"
	"os"
	"os/exec"
	"os/user"
	"path/filepath"
	"strings"
)

func platformUnzip(src, dest string) error {
	cmd := exec.Command("unzip", "-q", "-o", src, "-d", dest)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

func hasSystemd() bool {
	if _, err := exec.LookPath("systemctl"); err != nil {
		return false
	}
	out, err := exec.Command("systemctl", "is-system-running").CombinedOutput()
	if err != nil {
		s := strings.TrimSpace(string(out))
		return s == "degraded" || s == "starting"
	}
	return true
}

func isRoot() bool {
	u, _ := user.Current()
	return u != nil && u.Uid == "0"
}

func handleInstall(workDir string) error {
	printDivider()
	printStep(msg("Installing agent daemon service (Linux)", "安装 Agent 守护进程服务 (Linux)"))
	printDivider()

	printStep(msg("Step 1: preparing work directory ...", "步骤 1: 准备工作目录 ..."))
	prepareWorkDir(workDir)

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	if isRoot() && hasSystemd() {
		printStep(msg("Step 2: registering systemd service ...", "步骤 2: 注册 systemd 服务 ..."))
		_ = uninstallSystemd(serviceName)
		cleanRcLocal(serviceName)
		if err := installSystemd(workDir, serviceName); err != nil {
			return err
		}
		printStep(msg("Install complete", "安装完成"))
		return nil
	}

	if isRoot() {
		printStep(msg(
			"Step 2: no systemd detected (container?), starting daemon directly ...",
			"步骤 2: 未检测到 systemd (容器环境?), 直接启动守护进程 ..."))
	} else {
		printStep(msg(
			"Step 2: non-root, starting daemon directly ...",
			"步骤 2: 非 root 用户, 直接启动守护进程 ..."))
	}
	if err := startDirect(workDir); err != nil {
		return err
	}
	printStep(msg("Install complete", "安装完成"))
	return nil
}

func handleUninstall(workDir string) error {
	printDivider()
	printStep(msg("Uninstalling agent daemon service (Linux)", "卸载 Agent 守护进程服务 (Linux)"))
	printDivider()

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	if hasSystemd() {
		printStep(msg("Removing systemd service ...", "移除 systemd 服务 ..."))
		_ = uninstallSystemd(serviceName)
	}
	cleanRcLocal(serviceName)

	printStep(msg("Stopping processes ...", "停止进程 ..."))
	stopProcesses(workDir)

	printStep(msg("Uninstall complete", "卸载完成"))
	return nil
}

func handleStart(workDir string) error {
	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	if hasSystemdUnit(serviceName) {
		printStep(msgf("Starting service %s via systemctl ...", "通过 systemctl 启动服务 %s ...", serviceName))
		out, err := exec.Command("systemctl", "start", serviceName).CombinedOutput()
		if err != nil {
			return fmt.Errorf("systemctl start: %s (%w)", strings.TrimSpace(string(out)), err)
		}
		printStep(msgf("Service %s started", "服务 %s 已启动", serviceName))
		return nil
	}

	return startDirect(workDir)
}

func handleStop(workDir string) error {
	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	if hasSystemdUnit(serviceName) {
		printStep(msgf("Stopping service %s via systemctl ...", "通过 systemctl 停止服务 %s ...", serviceName))
		out, err := exec.Command("systemctl", "stop", serviceName).CombinedOutput()
		if err != nil {
			printWarn(fmt.Sprintf("systemctl stop: %s", strings.TrimSpace(string(out))))
		}
		printStep(msgf("Service %s stopped", "服务 %s 已停止", serviceName))
		return nil
	}

	stopProcesses(workDir)
	printStep(msg("Agent stopped", "Agent 已停止"))
	return nil
}

// ── systemd ──────────────────────────────────────────────────────────────

func systemdUnitPath(serviceName string) string {
	return fmt.Sprintf("/etc/systemd/system/%s.service", serviceName)
}

func hasSystemdUnit(serviceName string) bool {
	_, err := os.Stat(systemdUnitPath(serviceName))
	return err == nil
}

func installSystemd(workDir, serviceName string) error {
	daemonPath := filepath.Join(workDir, daemonBinary())
	if _, err := os.Stat(daemonPath); os.IsNotExist(err) {
		return fmt.Errorf(msgf("daemon binary not found: %s", "守护进程二进制未找到: %s", daemonPath))
	}
	os.Chmod(daemonPath, 0755)
	os.Chmod(filepath.Join(workDir, agentBinary()), 0755)

	unit := fmt.Sprintf(`[Unit]
Description=BK-CI Agent Daemon (%s)
After=network.target

[Service]
Type=simple
ExecStartPre=/bin/rm -f %s/runtime/daemon.pid %s/runtime/agent.pid
ExecStart=%s
WorkingDirectory=%s
KillMode=none
PrivateTmp=false

[Install]
WantedBy=multi-user.target
`, serviceName, workDir, workDir, daemonPath, workDir)

	unitPath := systemdUnitPath(serviceName)
	if err := os.WriteFile(unitPath, []byte(unit), 0644); err != nil {
		return fmt.Errorf("write systemd unit: %w", err)
	}
	printStep(msgf("Created systemd unit: %s", "已创建 systemd 单元: %s", unitPath))

	if err := exec.Command("systemctl", "daemon-reload").Run(); err != nil {
		return fmt.Errorf("systemctl daemon-reload: %w", err)
	}
	if err := exec.Command("systemctl", "enable", "--now", serviceName).Run(); err != nil {
		return fmt.Errorf("systemctl enable --now: %w", err)
	}
	printStep(msgf("Service %s enabled and started", "服务 %s 已启用并启动", serviceName))
	return nil
}

func uninstallSystemd(serviceName string) error {
	if !hasSystemdUnit(serviceName) {
		return nil
	}
	_ = exec.Command("systemctl", "stop", serviceName).Run()
	_ = exec.Command("systemctl", "disable", serviceName).Run()
	_ = exec.Command("systemctl", "reset-failed", serviceName).Run()
	os.Remove(systemdUnitPath(serviceName))
	_ = exec.Command("systemctl", "daemon-reload").Run()
	printStep(msgf("Systemd service %s removed", "已移除 systemd 服务 %s", serviceName))
	return nil
}

func cleanRcLocal(serviceName string) {
	rcLocal := "/etc/rc.d/rc.local"
	data, err := os.ReadFile(rcLocal)
	if err != nil {
		return
	}
	var lines []string
	changed := false
	for _, line := range strings.Split(string(data), "\n") {
		if strings.Contains(line, serviceName) {
			changed = true
			continue
		}
		lines = append(lines, line)
	}
	if changed {
		os.WriteFile(rcLocal, []byte(strings.Join(lines, "\n")), 0755)
		printStep(msg("Cleaned rc.local entries", "已清理 rc.local 条目"))
	}
}

func startDirect(workDir string) error {
	daemonPath := filepath.Join(workDir, daemonBinary())
	os.Chmod(daemonPath, 0755)
	os.Chmod(filepath.Join(workDir, agentBinary()), 0755)

	cmd := exec.Command(daemonPath)
	cmd.Dir = workDir
	cmd.Stdout = nil
	cmd.Stderr = nil
	if err := cmd.Start(); err != nil {
		return fmt.Errorf(msgf("start daemon failed: %v", "启动守护进程失败: %v", err))
	}
	printStep(msgf("Daemon started, PID=%d", "守护进程已启动, PID=%d", cmd.Process.Pid))
	cmd.Process.Release()
	return nil
}
