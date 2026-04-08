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
	"syscall"
)

const (
	modeService     = "SERVICE"
	modeUser        = "USER"
	modeDirect      = "DIRECT"
	installTypeFile = ".install_type"
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

func hasUserSystemd() bool {
	if isRoot() {
		return false
	}
	if _, err := exec.LookPath("systemctl"); err != nil {
		return false
	}
	out, err := exec.Command("systemctl", "--user", "is-system-running").CombinedOutput()
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

// ── install ──────────────────────────────────────────────────────────────

func handleInstall(workDir string, args []string) error {
	mode := ""
	if len(args) > 0 {
		mode = args[0]
	}

	installMode, err := resolveInstallMode(mode)
	if err != nil {
		return err
	}

	cleanupBeforeInstall(workDir, installMode)

	printDivider()
	printStep(msgf("Installing agent daemon service (Linux, mode: %s)", "安装 Agent 守护进程服务 (Linux, 模式: %s)", installMode))
	printDivider()

	printStep(msg("Step 1: preparing work directory ...", "步骤 1: 准备工作目录 ..."))
	prepareWorkDir(workDir)
	snapshotEnvFiles(workDir)

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	switch installMode {
	case modeService:
		printStep(msg("Step 2: registering systemd service ...", "步骤 2: 注册 systemd 服务 ..."))
		_ = uninstallSystemd(serviceName)
		cleanRcLocal(serviceName)
		if err := installSystemd(workDir, serviceName); err != nil {
			return err
		}
	case modeUser:
		printStep(msg("Step 2: registering user systemd service ...", "步骤 2: 注册用户级 systemd 服务 ..."))
		_ = uninstallUserSystemd(serviceName)
		if err := installUserSystemd(workDir, serviceName); err != nil {
			return err
		}
		ensureLinger()
	case modeDirect:
		printStep(msg("Step 2: starting daemon directly ...", "步骤 2: 直接启动守护进程 ..."))
		if err := startDirect(workDir); err != nil {
			return err
		}
	}

	writeInstallType(workDir, installMode)
	printStep(msg("Install complete", "安装完成"))
	return nil
}

// cleanupBeforeInstall stops and removes any previous installation before a fresh install.
// If .install_type exists, uninstalls that recorded mode.
// If .install_type is missing, uninstalls the target mode to clean up orphaned artifacts.
func cleanupBeforeInstall(workDir, targetMode string) {
	p := filepath.Join(workDir, installTypeFile)
	if _, err := os.Stat(p); err == nil {
		currentMode := readInstallMode(workDir)
		printStep(msgf("Cleaning up previous %s installation ...",
			"清理之前的 %s 安装 ...", currentMode))
		_ = handleUninstall(workDir)
		return
	}
	printStep(msgf("Cleaning up %s mode before install ...",
		"安装前清理 %s 模式 ...", targetMode))
	cleanupMode(workDir, targetMode)
}

// cleanupMode removes service artifacts for a specific mode without a full uninstall flow.
func cleanupMode(workDir, mode string) {
	serviceName, _ := getServiceName(workDir)
	if serviceName == "" {
		stopProcesses(workDir)
		return
	}
	switch mode {
	case modeService:
		_ = uninstallSystemd(serviceName)
		cleanRcLocal(serviceName)
	case modeUser:
		_ = uninstallUserSystemd(serviceName)
	}
	stopProcesses(workDir)
}

// resolveInstallMode determines the effective install mode from the positional argument.
func resolveInstallMode(flagMode string) (string, error) {
	if flagMode == "" {
		if isRoot() {
			if hasSystemd() {
				return modeService, nil
			}
			printWarn(msg(
				"No systemd detected (container?), falling back to direct start.",
				"未检测到 systemd (容器环境?), 回退为直接启动。"))
			return modeDirect, nil
		}
		return modeDirect, nil
	}

	switch strings.ToLower(flagMode) {
	case "service":
		if !isRoot() {
			return "", cliErrorf(
				"install service requires root privileges. Use: sudo devopsAgent install service",
				"install service 需要 root 权限。请使用: sudo devopsAgent install service")
		}
		if !hasSystemd() {
			return "", cliErrorf(
				"install service requires systemd, but systemd is not available.",
				"install service 需要 systemd, 但 systemd 不可用。")
		}
		return modeService, nil
	case "user":
		if isRoot() {
			return "", cliErrorf(
				"install user is for non-root users. Root should use: install service",
				"install user 仅用于非 root 用户。root 用户请使用: install service")
		}
		if !hasUserSystemd() {
			return "", cliErrorf(
				"install user requires user systemd session, but it is not available.\n"+
					"  Possible causes: systemd not running, or no user session bus.\n"+
					"  Check: systemctl --user is-system-running",
				"install user 需要用户级 systemd 会话, 但当前不可用。\n"+
					"  可能原因: systemd 未运行, 或没有用户会话总线。\n"+
					"  检查命令: systemctl --user is-system-running")
		}
		return modeUser, nil
	case "direct":
		return modeDirect, nil
	default:
		return "", cliErrorf(
			"unknown install mode: %s (valid: service, user, direct)",
			"未知安装模式: %s (可选: service, user, direct)", flagMode)
	}
}

// ── uninstall ────────────────────────────────────────────────────────────

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
		_ = uninstallUserSystemd(serviceName)
	}
	cleanRcLocal(serviceName)

	printStep(msg("Stopping processes ...", "停止进程 ..."))
	stopProcesses(workDir)

	os.Remove(filepath.Join(workDir, installTypeFile))
	printStep(msg("Uninstall complete", "卸载完成"))
	return nil
}

// ── start / stop (auto-detect mode via .install_type) ────────────────────

func handleStart(workDir string) error {
	snapshotEnvFiles(workDir)

	mode := readInstallMode(workDir)
	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	return startByMode(workDir, serviceName, mode)
}

func handleStop(workDir string) error {
	mode := readInstallMode(workDir)
	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	return stopByMode(workDir, serviceName, mode)
}

func startByMode(workDir, serviceName, mode string) error {
	switch mode {
	case modeService:
		if hasSystemdUnit(serviceName) {
			printStep(msgf("Starting service %s via systemctl ...", "通过 systemctl 启动服务 %s ...", serviceName))
			out, err := exec.Command("systemctl", "start", serviceName).CombinedOutput()
			if err != nil {
				return cliErrorf("systemctl start failed: %s (%v)", "systemctl start 失败: %s (%v)", strings.TrimSpace(string(out)), err)
			}
			printStep(msgf("Service %s started", "服务 %s 已启动", serviceName))
			return nil
		}
		return startDirect(workDir)
	case modeUser:
		if hasUserSystemdUnit(serviceName) {
			printStep(msgf("Starting service %s via systemctl --user ...", "通过 systemctl --user 启动服务 %s ...", serviceName))
			out, err := exec.Command("systemctl", "--user", "start", serviceName).CombinedOutput()
			if err != nil {
				return cliErrorf("systemctl --user start failed: %s (%v)", "systemctl --user start 失败: %s (%v)", strings.TrimSpace(string(out)), err)
			}
			printStep(msgf("Service %s started", "服务 %s 已启动", serviceName))
			return nil
		}
		return startDirect(workDir)
	default:
		return startDirect(workDir)
	}
}

func stopByMode(workDir, serviceName, mode string) error {
	switch mode {
	case modeService:
		if hasSystemdUnit(serviceName) {
			printStep(msgf("Stopping service %s via systemctl ...", "通过 systemctl 停止服务 %s ...", serviceName))
			out, err := exec.Command("systemctl", "stop", serviceName).CombinedOutput()
			if err != nil {
				printWarn(msgf("systemctl stop: %s", "systemctl stop: %s", strings.TrimSpace(string(out))))
			}
			printStep(msgf("Service %s stopped", "服务 %s 已停止", serviceName))
			return nil
		}
	case modeUser:
		if hasUserSystemdUnit(serviceName) {
			printStep(msgf("Stopping service %s via systemctl --user ...", "通过 systemctl --user 停止服务 %s ...", serviceName))
			out, err := exec.Command("systemctl", "--user", "stop", serviceName).CombinedOutput()
			if err != nil {
				printWarn(msgf("systemctl --user stop: %s", "systemctl --user stop: %s", strings.TrimSpace(string(out))))
			}
			printStep(msgf("Service %s stopped", "服务 %s 已停止", serviceName))
			return nil
		}
	}
	stopProcesses(workDir)
	printStep(msg("Agent stopped", "Agent 已停止"))
	return nil
}

// ── .install_type marker ─────────────────────────────────────────────────

func writeInstallType(workDir, mode string) {
	p := filepath.Join(workDir, installTypeFile)
	if err := os.WriteFile(p, []byte(mode), 0644); err != nil {
		printWarn(msgf("failed to write %s: %v", "写入 %s 失败: %v", installTypeFile, err))
		return
	}
	printStep(msgf("Install type set to %s", "安装类型已设置为 %s", mode))
}

func readInstallMode(workDir string) string {
	data, err := os.ReadFile(filepath.Join(workDir, installTypeFile))
	if err != nil {
		return modeDirect
	}
	m := strings.TrimSpace(string(data))
	switch strings.ToUpper(m) {
	case modeService:
		return modeService
	case modeUser:
		return modeUser
	}
	return modeDirect
}

// ── system-level systemd ─────────────────────────────────────────────────

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
		return cliErrorf("write systemd unit failed: %v", "写入 systemd 单元失败: %v", err)
	}
	printStep(msgf("Created systemd unit: %s", "已创建 systemd 单元: %s", unitPath))

	if err := exec.Command("systemctl", "daemon-reload").Run(); err != nil {
		return cliErrorf("systemctl daemon-reload failed: %v", "systemctl daemon-reload 失败: %v", err)
	}
	if err := exec.Command("systemctl", "enable", "--now", serviceName).Run(); err != nil {
		return cliErrorf("systemctl enable --now failed: %v", "systemctl enable --now 失败: %v", err)
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

// ── user-level systemd ───────────────────────────────────────────────────

func userSystemdUnitDir() string {
	home, _ := os.UserHomeDir()
	return filepath.Join(home, ".config", "systemd", "user")
}

func userSystemdUnitPath(serviceName string) string {
	return filepath.Join(userSystemdUnitDir(), serviceName+".service")
}

func hasUserSystemdUnit(serviceName string) bool {
	_, err := os.Stat(userSystemdUnitPath(serviceName))
	return err == nil
}

func installUserSystemd(workDir, serviceName string) error {
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

[Install]
WantedBy=default.target
`, serviceName, workDir, workDir, daemonPath, workDir)

	unitDir := userSystemdUnitDir()
	os.MkdirAll(unitDir, 0755)

	unitPath := userSystemdUnitPath(serviceName)
	if err := os.WriteFile(unitPath, []byte(unit), 0644); err != nil {
		return cliErrorf("write user systemd unit failed: %v", "写入用户级 systemd 单元失败: %v", err)
	}
	printStep(msgf("Created user systemd unit: %s", "已创建用户级 systemd 单元: %s", unitPath))

	if err := exec.Command("systemctl", "--user", "daemon-reload").Run(); err != nil {
		return cliErrorf("systemctl --user daemon-reload failed: %v", "systemctl --user daemon-reload 失败: %v", err)
	}
	if err := exec.Command("systemctl", "--user", "enable", "--now", serviceName).Run(); err != nil {
		return cliErrorf("systemctl --user enable --now failed: %v", "systemctl --user enable --now 失败: %v", err)
	}
	printStep(msgf("User service %s enabled and started", "用户级服务 %s 已启用并启动", serviceName))
	return nil
}

func uninstallUserSystemd(serviceName string) error {
	if !hasUserSystemdUnit(serviceName) {
		return nil
	}
	_ = exec.Command("systemctl", "--user", "stop", serviceName).Run()
	_ = exec.Command("systemctl", "--user", "disable", serviceName).Run()
	_ = exec.Command("systemctl", "--user", "reset-failed", serviceName).Run()
	os.Remove(userSystemdUnitPath(serviceName))
	_ = exec.Command("systemctl", "--user", "daemon-reload").Run()
	printStep(msgf("User systemd service %s removed", "已移除用户级 systemd 服务 %s", serviceName))
	return nil
}

// ── linger management ────────────────────────────────────────────────────

var lingerDir = "/var/lib/systemd/linger"

func hasLinger() bool {
	u, _ := user.Current()
	if u == nil {
		return false
	}
	_, err := os.Stat(filepath.Join(lingerDir, u.Username))
	return err == nil
}

func ensureLinger() {
	if hasLinger() {
		printStep(msg("Linger already enabled for current user.", "当前用户的 linger 已启用。"))
		return
	}

	u, _ := user.Current()
	username := "unknown"
	if u != nil {
		username = u.Username
	}

	out, err := exec.Command("loginctl", "enable-linger").CombinedOutput()
	if err == nil {
		printStep(msgf("Linger enabled for user %s (service survives logout).",
			"已为用户 %s 启用 linger (注销后服务仍运行)。", username))
		return
	}

	printWarn(msgf(
		"Failed to enable linger: %s\n"+
			"  Without linger, the service will stop when you log out.\n"+
			"  To enable manually, run: sudo loginctl enable-linger %s",
		"启用 linger 失败: %s\n"+
			"  未启用 linger 时, 注销后服务将停止。\n"+
			"  手动启用命令: sudo loginctl enable-linger %s",
		strings.TrimSpace(string(out)), username))
}

// ── misc helpers ─────────────────────────────────────────────────────────

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

func isProcessAlive(pid int) bool {
	return syscall.Kill(pid, 0) == nil
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
