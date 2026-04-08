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

const (
	modeLogin       = "LOGIN"
	modeBackground  = "BACKGROUND"
	installTypeFile = ".install_type"
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

// ── install / uninstall ──────────────────────────────────────────────────

func handleInstall(workDir string, args []string) error {
	mode := "login"
	if len(args) > 0 {
		mode = args[0]
	}

	installMode := strings.ToUpper(mode)
	switch installMode {
	case modeLogin, modeBackground:
	default:
		return cliErrorf("unknown install mode: %s (valid: login, background)",
			"未知安装模式: %s (可选: login, background)", mode)
	}

	if installMode == modeBackground && !hasModernLaunchctl() {
		printWarn(msg(
			"Background mode requires macOS 10.10+ (launchctl bootstrap). Falling back to login mode.",
			"Background 模式需要 macOS 10.10+ (launchctl bootstrap)。回退为 login 模式。"))
		installMode = modeLogin
	}

	cleanupBeforeInstall(workDir, installMode)

	printDivider()
	printStep(msgf("Installing agent daemon service (macOS, mode: %s)", "安装 Agent 守护进程服务 (macOS, 模式: %s)", installMode))
	printDivider()

	printStep(msg("Step 1: preparing work directory ...", "步骤 1: 准备工作目录 ..."))
	prepareWorkDir(workDir)
	snapshotEnvFiles(workDir)

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	printStep(msg("Step 2: registering launchd service ...", "步骤 2: 注册 launchd 服务 ..."))
	bootoutService(serviceName, modeLogin)
	bootoutService(serviceName, modeBackground)
	cleanupLegacyPlist(serviceName)
	if err := writePlist(workDir, serviceName, installMode); err != nil {
		return err
	}
	writeInstallType(workDir, installMode)

	printStep(msg("Step 3: starting service ...", "步骤 3: 启动服务 ..."))
	if err := startByMode(workDir, serviceName, installMode); err != nil {
		return err
	}

	printStep(msg("Install complete", "安装完成"))

	if installMode == modeLogin {
		printStep("")
		printWarn(msg(
			"LOGIN mode requires a user to be logged in. To auto-recover after reboot,\n"+
				"  enable auto-login in System Settings > Users & Groups.",
			"LOGIN 模式需要用户已登录桌面。为确保重启后自动恢复，\n"+
				"  建议在 系统设置 > 用户与群组  中开启自动以此身份登录。"))
	}

	return nil
}

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
	serviceName, _ := getServiceName(workDir)
	if serviceName != "" {
		bootoutService(serviceName, modeLogin)
		bootoutService(serviceName, modeBackground)
		cleanupLegacyPlist(serviceName)
		removePlist(serviceName)
	}
	stopProcesses(workDir)
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
	bootoutService(serviceName, modeLogin)
	bootoutService(serviceName, modeBackground)
	cleanupLegacyPlist(serviceName)
	removePlist(serviceName)

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
	pp := plistPath(serviceName)
	if _, err := os.Stat(pp); err != nil {
		printWarn(msg(
			"plist not found, falling back to direct start (run install first for launchd management)",
			"plist 未找到, 回退为直接启动 (请先运行 install 以启用 launchd 管理)"))
		return startDirect(workDir)
	}
	printStep(msgf("Starting %s via launchctl ...", "通过 launchctl 启动 %s ...", serviceName))
	bootoutService(serviceName, mode)

	if hasModernLaunchctl() {
		if err := bootstrapAndStart(serviceName, mode); err != nil {
			return err
		}
	} else {
		if err := launchctlLoad(serviceName); err != nil {
			return err
		}
	}

	printStep(msgf("Service %s started", "服务 %s 已启动", serviceName))
	return nil
}

func stopByMode(workDir, serviceName, mode string) error {
	bootoutService(serviceName, mode)
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
		return modeLogin
	}
	m := strings.TrimSpace(string(data))
	if strings.EqualFold(m, modeBackground) {
		return modeBackground
	}
	return modeLogin
}

// ── launchctl capability detection ───────────────────────────────────────

// cachedModernLaunchctl caches the result of probing for bootstrap support.
// -1 = not probed yet, 0 = legacy, 1 = modern.
var cachedModernLaunchctl int = -1

// hasModernLaunchctl probes whether launchctl supports the modern
// bootstrap/bootout/kickstart API (macOS 10.10+). On older systems these
// subcommands are unrecognized and we fall back to load/unload.
// The result is cached after the first call.
func hasModernLaunchctl() bool {
	if cachedModernLaunchctl >= 0 {
		return cachedModernLaunchctl == 1
	}
	result := probeBootstrapSupport()
	if result {
		cachedModernLaunchctl = 1
	} else {
		cachedModernLaunchctl = 0
	}
	return result
}

// probeBootstrapSupport runs `launchctl bootstrap` with no arguments.
// Modern launchctl recognizes the subcommand and prints usage; legacy
// launchctl prints "unrecognized subcommand" or "unknown subcommand".
func probeBootstrapSupport() bool {
	out, _ := exec.Command("launchctl", "bootstrap").CombinedOutput()
	s := strings.ToLower(string(out))
	return !strings.Contains(s, "unrecognized") && !strings.Contains(s, "unknown")
}

// ── launchd (modern API: bootstrap/bootout/kickstart) ────────────────────

// launchdDomain returns the launchctl domain target for the given mode.
// LOGIN uses "gui/UID" (requires logged-in GUI session).
// BACKGROUND uses "user/UID" (works headless/SSH, matching GitHub Actions Runner).
// Root always uses "system".
func launchdDomain(mode string) string {
	if isRoot() {
		return "system"
	}
	uid := currentUID()
	if mode == modeBackground {
		return "user/" + uid
	}
	return "gui/" + uid
}

func currentUID() string {
	u, _ := user.Current()
	if u != nil {
		return u.Uid
	}
	return fmt.Sprint(os.Getuid())
}

func serviceTarget(serviceName, mode string) string {
	return launchdDomain(mode) + "/" + serviceName
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

func writePlist(workDir, serviceName, mode string) error {
	daemonPath := filepath.Join(workDir, daemonBinary())
	os.Chmod(daemonPath, 0755)
	os.Chmod(filepath.Join(workDir, agentBinary()), 0755)

	sessionTypeBlock := ""
	if mode == modeBackground {
		sessionTypeBlock = `
    <key>LimitLoadToSessionType</key>
        <string>Background</string>`
	}

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
        <false/>%s
</dict>
</plist>
`, serviceName, daemonPath, workDir, sessionTypeBlock)

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
	printStep(msgf("Created %s (%s, mode: %s)", "已创建 %s (%s, 模式: %s)", pp, level, mode))
	return nil
}

// bootstrapAndStart registers the plist with launchd and force-starts it.
// LOGIN uses gui/UID domain, BACKGROUND uses user/UID domain, Root uses system.
func bootstrapAndStart(serviceName, mode string) error {
	pp := plistPath(serviceName)
	target := serviceTarget(serviceName, mode)
	domain := launchdDomain(mode)

	out, err := exec.Command("launchctl", "bootstrap", domain, pp).CombinedOutput()
	if err != nil {
		outStr := strings.TrimSpace(string(out))
		// 36: "Operation already in progress" — job already bootstrapped.
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

// bootoutService removes the service from launchd for the given domain mode.
func bootoutService(serviceName, mode string) {
	if hasModernLaunchctl() {
		target := serviceTarget(serviceName, mode)
		_ = exec.Command("launchctl", "bootout", target).Run()
	} else {
		launchctlUnload(serviceName)
	}
}

// ── launchd (legacy API: load/unload, macOS < 10.10) ─────────────────────

func launchctlLoad(serviceName string) error {
	pp := plistPath(serviceName)
	out, err := exec.Command("launchctl", "load", "-w", pp).CombinedOutput()
	if err != nil {
		return cliErrorf("launchctl load failed: %s (%v)", "launchctl load 失败: %s (%v)",
			strings.TrimSpace(string(out)), err)
	}
	return nil
}

func launchctlUnload(serviceName string) {
	pp := plistPath(serviceName)
	_ = exec.Command("launchctl", "unload", pp).Run()
}

func cleanupLegacyPlist(serviceName string) {
	other := otherPlistPath(serviceName)
	if _, err := os.Stat(other); err == nil {
		bootoutService(serviceName, modeLogin)
		bootoutService(serviceName, modeBackground)
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
