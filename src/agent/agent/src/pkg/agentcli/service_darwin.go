//go:build darwin
// +build darwin

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

func isRoot() bool {
	u, _ := user.Current()
	return u != nil && u.Uid == "0"
}

func handleInstall(workDir string, _ []string) error {
	printDivider()
	printStep(msg("Installing agent daemon service (macOS)", "安装 Agent 守护进程服务 (macOS)"))
	printDivider()

	printStep(msg("Step 1: preparing work directory ...", "步骤 1: 准备工作目录 ..."))
	prepareWorkDir(workDir)

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	printStep(msg("Step 2: registering launchd service ...", "步骤 2: 注册 launchd 服务 ..."))
	unloadPlist(serviceName)
	if err := writePlist(workDir, serviceName); err != nil {
		return err
	}

	printStep(msg("Step 3: starting service ...", "步骤 3: 启动服务 ..."))
	if err := loadPlist(serviceName); err != nil {
		return err
	}

	printStep(msg("Install complete", "安装完成"))
	return nil
}

func handleUninstall(workDir string) error {
	printDivider()
	printStep(msg("Uninstalling agent daemon service (macOS)", "卸载 Agent 守护进程服务 (macOS)"))
	printDivider()

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	printStep(msg("Removing launchd service ...", "移除 launchd 服务 ..."))
	unloadPlist(serviceName)
	removePlist(serviceName)

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

	pp := plistPath(serviceName)
	if _, err := os.Stat(pp); err == nil {
		printStep(msgf("Loading %s via launchctl ...", "通过 launchctl 加载 %s ...", serviceName))
		out, err := exec.Command("launchctl", "load", "-w", pp).CombinedOutput()
		if err != nil {
			return fmt.Errorf("launchctl load: %s (%w)", strings.TrimSpace(string(out)), err)
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

	unloadPlist(serviceName)
	stopProcesses(workDir)
	printStep(msg("Agent stopped", "Agent 已停止"))
	return nil
}

// ── launchd ──────────────────────────────────────────────────────────────

func plistDir() string {
	if isRoot() {
		return "/Library/LaunchDaemons"
	}
	home, _ := os.UserHomeDir()
	return filepath.Join(home, "Library", "LaunchAgents")
}

func plistPath(serviceName string) string {
	return filepath.Join(plistDir(), serviceName+".plist")
}

func writePlist(workDir, serviceName string) error {
	daemonPath := filepath.Join(workDir, daemonBinary())
	os.Chmod(daemonPath, 0755)
	os.Chmod(filepath.Join(workDir, agentBinary()), 0755)

	plist := fmt.Sprintf(`<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple Computer//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
        <string>%s</string>
    <key>Program</key>
        <string>%s</string>
    <key>RunAtLoad</key>
        <true/>
    <key>WorkingDirectory</key>
        <string>%s</string>
    <key>KeepAlive</key>
        <false/>
</dict>
</plist>
`, serviceName, daemonPath, workDir)

	dir := plistDir()
	os.MkdirAll(dir, 0755)

	pp := plistPath(serviceName)
	if err := os.WriteFile(pp, []byte(plist), 0644); err != nil {
		return fmt.Errorf("write plist: %w", err)
	}

	level := msg("user-level (LaunchAgents)", "用户级 (LaunchAgents)")
	if isRoot() {
		level = msg("system-level (LaunchDaemons)", "系统级 (LaunchDaemons)")
	}
	printStep(msgf("Created %s (%s)", "已创建 %s (%s)", pp, level))
	return nil
}

func loadPlist(serviceName string) error {
	pp := plistPath(serviceName)
	out, err := exec.Command("launchctl", "load", "-w", pp).CombinedOutput()
	if err != nil {
		return fmt.Errorf("launchctl load: %s (%w)", strings.TrimSpace(string(out)), err)
	}
	printStep(msgf("Service %s loaded", "服务 %s 已加载", serviceName))
	return nil
}

func unloadPlist(serviceName string) {
	pp := plistPath(serviceName)
	if _, err := os.Stat(pp); err == nil {
		_ = exec.Command("launchctl", "unload", pp).Run()
	}
	other := otherPlistPath(serviceName)
	if _, err := os.Stat(other); err == nil {
		_ = exec.Command("launchctl", "unload", other).Run()
		os.Remove(other)
		printStep(msgf("Cleaned up legacy plist %s", "已清理旧 plist %s", other))
	}
}

func removePlist(serviceName string) {
	pp := plistPath(serviceName)
	if _, err := os.Stat(pp); err == nil {
		os.Remove(pp)
		printStep(msgf("Removed %s", "已移除 %s", pp))
	}
}

func otherPlistPath(serviceName string) string {
	if isRoot() {
		home, _ := os.UserHomeDir()
		return filepath.Join(home, "Library", "LaunchAgents", serviceName+".plist")
	}
	return filepath.Join("/Library/LaunchDaemons", serviceName+".plist")
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
