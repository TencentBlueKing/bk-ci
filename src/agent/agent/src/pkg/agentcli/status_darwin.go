//go:build darwin
// +build darwin

package agentcli

import (
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"syscall"
)

func handleStatus(workDir string) error {
	printDivider()
	printStep(msg("BK-CI Agent Status", "BK-CI Agent 状态"))
	printDivider()

	serviceName, _ := getServiceName(workDir)
	statusLine(msg("Platform", "平台"), "macOS")
	statusLine(msg("Work directory", "工作目录"), workDir)
	statusLine(msg("Service name", "服务名"), serviceName)
	statusLine(msg("Current user", "当前用户"), currentUser())

	if isRoot() {
		statusLine(msg("Run mode", "运行模式"), msg("root (LaunchDaemons - system level)", "root (LaunchDaemons - 系统级)"))
	} else {
		statusLine(msg("Run mode", "运行模式"), msg("user (LaunchAgents - user level)", "普通用户 (LaunchAgents - 用户级)"))
	}

	if serviceName != "" {
		pp := plistPath(serviceName)
		if _, err := os.Stat(pp); err == nil {
			statusLine(msg("Plist", "Plist 文件"), pp+" ✓")
		} else {
			statusLine(msg("Plist", "Plist 文件"), msg("not registered", "未注册")+" ✗")
		}
	}

	daemonPid := readPid(filepath.Join(workDir, "runtime", "daemon.pid"))
	agentPid := readPid(filepath.Join(workDir, "runtime", "agent.pid"))
	statusLine(msg("Daemon PID", "守护进程 PID"), pidStatus(daemonPid))
	statusLine(msg("Agent PID", "Agent PID"), pidStatus(agentPid))

	checkPropertiesFile(workDir)

	statusLine("JDK 17", dirStatus(filepath.Join(workDir, "jdk17")))
	statusLine("JDK 8", dirStatus(filepath.Join(workDir, "jdk")))
	statusLine("worker-agent.jar", fileStatus(filepath.Join(workDir, "worker-agent.jar")))

	printHealthChecks(workDir)

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
	fmt.Printf("  %-24s %s\n", label+":", value)
}
