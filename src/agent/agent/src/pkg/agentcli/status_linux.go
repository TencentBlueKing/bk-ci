//go:build linux
// +build linux

package agentcli

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"syscall"
)

func handleStatus(workDir string) error {
	beginStatusSummary()
	printDivider()
	printStep(msg("BK-CI Agent Status", "BK-CI Agent 状态"))
	printDivider()

	serviceName, _ := getServiceName(workDir)
	installMode := readInstallMode(workDir)

	statusLine(msg("Platform", "平台"), "Linux")
	statusLine(msg("Work directory", "工作目录"), workDir)
	statusLine(msg("Service name", "服务名"), serviceName)
	statusLine(msg("Current user", "当前用户"), currentUser())
	statusLine(msg("Install mode", "安装模式"), installMode)

	// Run mode
	switch installMode {
	case modeService:
		statusLine(msg("Run mode", "运行模式"), msg("root + systemd (service)", "root + systemd (系统服务)"))
	case modeUser:
		statusLine(msg("Run mode", "运行模式"), msg("user systemd (survives logout with linger)", "用户级 systemd (启用 linger 后注销仍运行)"))
	default:
		if isRoot() {
			statusLine(msg("Run mode", "运行模式"), msg("root + direct", "root + 直接启动"))
		} else {
			statusLine(msg("Run mode", "运行模式"), msg("non-root + direct", "非 root + 直接启动"))
		}
	}

	// Service status
	switch installMode {
	case modeService:
		if serviceName != "" && hasSystemdUnit(serviceName) {
			printSystemdStatus(serviceName, false)
		} else {
			statusLine(msg("Service state", "服务状态"), msg("not registered", "未注册"))
		}
	case modeUser:
		if serviceName != "" && hasUserSystemdUnit(serviceName) {
			printSystemdStatus(serviceName, true)
			if hasLinger() {
				statusLine("Linger", msg("enabled (survives logout) ✓", "已启用 (注销后仍运行) ✓"))
			} else {
				statusLine("Linger", msg("disabled (service stops on logout) ✗", "未启用 (注销后服务将停止) ✗"))
			}
		} else {
			statusLine(msg("Service state", "服务状态"), msg("not registered", "未注册"))
		}
	default:
		statusLine(msg("Service state", "服务状态"), msg("direct mode (no service)", "直接启动模式 (无服务)"))
	}

	// Process status
	daemonPid := readPid(filepath.Join(workDir, "runtime", "daemon.pid"))
	agentPid := readPid(filepath.Join(workDir, "runtime", "agent.pid"))
	statusLine(msg("Daemon PID", "守护进程 PID"), pidStatus(daemonPid))
	statusLine(msg("Agent PID", "Agent PID"), pidStatus(agentPid))

	checkPropertiesFile(workDir)

	// JDK
	statusLine("JDK 17", dirStatus(filepath.Join(workDir, "jdk17")))
	statusLine("JDK 8", dirStatus(filepath.Join(workDir, "jdk")))
	statusLine("worker-agent.jar", fileStatus(filepath.Join(workDir, "worker-agent.jar")))

	printHealthChecks(workDir)
	fmt.Println()
	printStatusSummaryLine()

	return nil
}

func printSystemdStatus(serviceName string, userMode bool) {
	args := []string{"is-active", serviceName}
	if userMode {
		args = []string{"--user", "is-active", serviceName}
	}
	out, _ := exec.Command("systemctl", args...).CombinedOutput()
	statusLine(msg("Service state", "服务状态"), strings.TrimSpace(string(out)))

	args = []string{"is-enabled", serviceName}
	if userMode {
		args = []string{"--user", "is-enabled", serviceName}
	}
	out, _ = exec.Command("systemctl", args...).CombinedOutput()
	label := msg("Auto start", "开机启动")
	if userMode {
		label = msg("Auto start", "自动启动")
	}
	statusLine(label, strings.TrimSpace(string(out)))

	args = []string{"show", "-p", "MainPID,ActiveEnterTimestamp", "--value", serviceName}
	if userMode {
		args = []string{"--user", "show", "-p", "MainPID,ActiveEnterTimestamp", "--value", serviceName}
	}
	out, _ = exec.Command("systemctl", args...).CombinedOutput()
	mainPID, startedAt := parseSystemdShow(string(out))
	if mainPID > 0 {
		statusLine(msg("Service PID", "服务主进程 PID"), fmt.Sprintf("%d", mainPID))
	}
	if startedAt != "" {
		statusLine(msg("Started at", "启动时间"), startedAt)
	}
}

// parseSystemdShow parses output of `systemctl show -p MainPID,ActiveEnterTimestamp --value`.
// The output has two lines: MainPID value, then ActiveEnterTimestamp value.
func parseSystemdShow(output string) (mainPID int, startedAt string) {
	lines := strings.Split(strings.TrimSpace(output), "\n")
	if len(lines) >= 1 {
		mainPID, _ = strconv.Atoi(strings.TrimSpace(lines[0]))
	}
	if len(lines) >= 2 {
		ts := strings.TrimSpace(lines[1])
		if ts != "" && ts != "n/a" {
			startedAt = ts
		}
	}
	return
}

func currentUser() string {
	if u := os.Getenv("USER"); u != "" {
		return u
	}
	return "unknown"
}

func readPid(path string) int {
	data, err := os.ReadFile(path)
	if err != nil {
		return 0
	}
	pid, _ := strconv.Atoi(strings.TrimSpace(string(data)))
	return pid
}

func pidStatus(pid int) string {
	if pid <= 0 {
		return msg("not running", "未运行")
	}
	if err := syscall.Kill(pid, 0); err != nil {
		return fmt.Sprintf("%d (%s)", pid, msg("not running", "已退出"))
	}
	return fmt.Sprintf("%d (%s)", pid, msg("running", "运行中"))
}

func dirStatus(path string) string {
	info, err := os.Stat(path)
	if err != nil {
		return msg("missing", "缺失") + " ✗"
	}
	if !info.IsDir() {
		return msg("not a directory", "非目录") + " ✗"
	}
	return msg("OK", "正常") + " ✓"
}

func fileStatus(path string) string {
	info, err := os.Stat(path)
	if err != nil {
		return msg("missing", "缺失") + " ✗"
	}
	size := info.Size()
	if size == 0 {
		return msg("empty", "空文件") + " ✗"
	}
	return fmt.Sprintf("%s ✓ (%.1f MB)", msg("OK", "正常"), float64(size)/1024/1024)
}

func statusLine(label, value string) {
	trackStatusLine(label, value)
	fmt.Printf("  %-24s %s\n", label+":", value)
}
