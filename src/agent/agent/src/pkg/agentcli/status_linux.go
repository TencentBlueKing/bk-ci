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
	statusLine(msg("Platform", "平台"), "Linux")
	statusLine(msg("Work directory", "工作目录"), workDir)
	statusLine(msg("Service name", "服务名"), serviceName)
	statusLine(msg("Current user", "当前用户"), currentUser())

	// Run mode
	if isRoot() {
		if hasSystemd() {
			statusLine(msg("Run mode", "运行模式"), msg("root + systemd (service)", "root + systemd (系统服务)"))
		} else {
			statusLine(msg("Run mode", "运行模式"), msg("root + direct (no systemd, e.g. container)", "root + 直接启动 (无 systemd, 如容器环境)"))
		}
	} else {
		statusLine(msg("Run mode", "运行模式"), msg("non-root + direct", "非 root + 直接启动"))
	}

	// Service status
	if serviceName != "" && hasSystemdUnit(serviceName) {
		out, _ := exec.Command("systemctl", "is-active", serviceName).CombinedOutput()
		state := strings.TrimSpace(string(out))
		statusLine(msg("Service state", "服务状态"), state)

		out, _ = exec.Command("systemctl", "is-enabled", serviceName).CombinedOutput()
		enabled := strings.TrimSpace(string(out))
		statusLine(msg("Auto start", "开机启动"), enabled)
	} else {
		statusLine(msg("Service state", "服务状态"), msg("not registered", "未注册"))
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
