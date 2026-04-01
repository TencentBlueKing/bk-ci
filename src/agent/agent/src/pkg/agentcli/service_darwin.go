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
	"syscall"
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
	bootoutService(serviceName)
	cleanupLegacyPlist(serviceName)
	if err := writePlist(workDir, serviceName); err != nil {
		return err
	}

	printStep(msg("Step 3: starting service ...", "步骤 3: 启动服务 ..."))
	if err := bootstrapAndStart(serviceName); err != nil {
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
	bootoutService(serviceName)
	cleanupLegacyPlist(serviceName)
	removePlist(serviceName)

	printStep(msg("Stopping processes ...", "停止进程 ..."))
	stopProcesses(workDir)

	printStep(msg("Uninstall complete", "卸载完成"))
	return nil
}

func handleStart(workDir string, args []string) error {
	if hasLegacyFlag(args) {
		return handleStartLegacy(workDir)
	}

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	pp := plistPath(serviceName)
	if _, err := os.Stat(pp); err == nil {
		printStep(msgf("Starting %s via launchctl ...", "通过 launchctl 启动 %s ...", serviceName))

		// bootout first to clear stale state (e.g. process exited on its own),
		// then bootstrap + kickstart for a reliable fresh spawn.
		bootoutService(serviceName)
		if err := bootstrapAndStart(serviceName); err != nil {
			return err
		}
		printStep(msgf("Service %s started", "服务 %s 已启动", serviceName))
		return nil
	}

	return startDirect(workDir)
}

func handleStop(workDir string, args []string) error {
	if hasLegacyFlag(args) {
		return handleStopLegacy(workDir)
	}

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	bootoutService(serviceName)
	stopProcesses(workDir)
	printStep(msg("Agent stopped", "Agent 已停止"))
	return nil
}

// handleStartLegacy mimics the old start.sh process-start behavior:
// check if already running, then direct start (inheriting shell env).
// Does NOT prepare workdir — use "install" or "repair" for that.
func handleStartLegacy(workDir string) error {
	printStep(msg("Legacy mode (-o): direct start", "兼容模式 (-o): 直接启动"))

	pidFile := filepath.Join(workDir, "runtime", "daemon.pid")
	if pid := readPid(pidFile); pid > 0 && isProcessAlive(pid) {
		printStep(msgf("Daemon already running, PID=%d", "守护进程已在运行, PID=%d", pid))
		return nil
	}

	return startDirect(workDir)
}

// handleStopLegacy mimics the old stop.sh behavior: kill processes by PID
// file only, without any launchctl/service-manager involvement.
func handleStopLegacy(workDir string) error {
	printStep(msg("Legacy mode (-o): kill by PID", "兼容模式 (-o): 通过 PID 终止"))
	stopProcesses(workDir)
	printStep(msg("Agent stopped", "Agent 已停止"))
	return nil
}

// ── launchd (modern API: bootstrap/bootout/kickstart) ────────────────────

// launchdDomain returns the launchctl domain target.
// Uses "user/UID" (not "gui/UID") so the service works in headless / SSH
// scenarios, matching the approach taken by GitHub Actions Runner.
// For root, uses the "system" domain.
func launchdDomain() string {
	if isRoot() {
		return "system"
	}
	u, _ := user.Current()
	if u != nil {
		return "user/" + u.Uid
	}
	return "user/" + fmt.Sprint(os.Getuid())
}

// serviceTarget returns the launchctl service target "domain/serviceName".
func serviceTarget(serviceName string) string {
	return launchdDomain() + "/" + serviceName
}

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
		return cliErrorf("write plist failed: %v", "写入 plist 失败: %v", err)
	}

	level := msg("user-level (LaunchAgents)", "用户级 (LaunchAgents)")
	if isRoot() {
		level = msg("system-level (LaunchDaemons)", "系统级 (LaunchDaemons)")
	}
	printStep(msgf("Created %s (%s)", "已创建 %s (%s)", pp, level))
	return nil
}

// bootstrapAndStart registers the plist with launchd and force-starts it.
// Uses the modern launchctl API (macOS 10.10+): bootstrap + kickstart.
func bootstrapAndStart(serviceName string) error {
	pp := plistPath(serviceName)
	target := serviceTarget(serviceName)
	domain := launchdDomain()

	out, err := exec.Command("launchctl", "bootstrap", domain, pp).CombinedOutput()
	if err != nil {
		outStr := strings.TrimSpace(string(out))
		// 36: "Operation already in progress" — job already bootstrapped; safe to proceed with kickstart.
		if !strings.Contains(outStr, "36:") {
			return cliErrorf("launchctl bootstrap failed: %s (%v)", "launchctl bootstrap 失败: %s (%v)", outStr, err)
		}
	}

	out, err = exec.Command("launchctl", "kickstart", target).CombinedOutput()
	if err != nil {
		return cliErrorf("launchctl kickstart failed: %s (%v)", "launchctl kickstart 失败: %s (%v)",
			strings.TrimSpace(string(out)), err)
	}
	return nil
}

// bootoutService removes the service from launchd (stops it if running).
func bootoutService(serviceName string) {
	target := serviceTarget(serviceName)
	_ = exec.Command("launchctl", "bootout", target).Run()
}

// cleanupLegacyPlist removes a plist that might exist in the "other" location
// (e.g. LaunchDaemons vs LaunchAgents) from a previous installation.
func cleanupLegacyPlist(serviceName string) {
	other := otherPlistPath(serviceName)
	if _, err := os.Stat(other); err == nil {
		_ = exec.Command("launchctl", "bootout", serviceTarget(serviceName)).Run()
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
