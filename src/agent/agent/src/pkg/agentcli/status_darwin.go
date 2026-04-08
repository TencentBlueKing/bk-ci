//go:build darwin
// +build darwin

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

type launchdStatus struct {
	loaded   bool
	running  bool
	pid      int
	lastExit int
	hasExit  bool
}

// parseLaunchdList parses the output of `launchctl list <label>`.
// Example output:
//
//	{
//	    "LimitLoadToSessionType" = "Background";
//	    "Label" = "devops_agent_xxx";
//	    "PID" = 1234;
//	    "LastExitStatus" = 256;
//	};
func parseLaunchdList(output string) launchdStatus {
	s := launchdStatus{loaded: true}
	for _, line := range strings.Split(output, "\n") {
		line = strings.TrimSpace(line)
		line = strings.TrimRight(line, ";")
		parts := strings.SplitN(line, "=", 2)
		if len(parts) != 2 {
			continue
		}
		key := strings.Trim(strings.TrimSpace(parts[0]), "\"")
		val := strings.Trim(strings.TrimSpace(parts[1]), "\"")
		switch key {
		case "PID":
			if pid, err := strconv.Atoi(val); err == nil && pid > 0 {
				s.pid = pid
				s.running = true
			}
		case "LastExitStatus":
			if code, err := strconv.Atoi(val); err == nil {
				s.lastExit = code
				s.hasExit = true
			}
		}
	}
	return s
}

func queryLaunchdStatus(serviceName string) launchdStatus {
	out, err := exec.Command("launchctl", "list", serviceName).CombinedOutput()
	if err != nil {
		return launchdStatus{loaded: false}
	}
	return parseLaunchdList(string(out))
}

func formatLaunchdStatus(s launchdStatus, plistExists bool) string {
	if !plistExists {
		return msg("not registered (no plist)", "未注册 (无 plist 文件)") + " ✗"
	}
	if !s.loaded {
		return msg("not loaded (plist exists but service not bootstrapped)",
			"未加载 (plist 存在但服务未注册到 launchd)") + " ✗"
	}
	if s.running {
		return fmt.Sprintf("%s (PID %d) ✓",
			msg("loaded, running", "已加载, 运行中"), s.pid)
	}
	if s.hasExit {
		return fmt.Sprintf("%s (%s: %d) ✗",
			msg("loaded, not running", "已加载, 未运行"),
			msg("last exit", "上次退出码"), s.lastExit)
	}
	return msg("loaded, not running", "已加载, 未运行") + " ✗"
}

func handleStatus(workDir string) error {
	beginStatusSummary()
	printDivider()
	printStep(msg("BK-CI Agent Status", "BK-CI Agent 状态"))
	printDivider()

	serviceName, _ := getServiceName(workDir)
	installMode := readInstallMode(workDir)

	statusLine(msg("Platform", "平台"), "macOS")
	statusLine(msg("Work directory", "工作目录"), workDir)
	statusLine(msg("Service name", "服务名"), serviceName)
	statusLine(msg("Current user", "当前用户"), currentUser())
	statusLine(msg("Install mode", "安装模式"), installMode)

	domain := launchdDomain(installMode)
	statusLine(msg("Run mode", "运行模式"), msgf("LaunchAgents (domain: %s)", "LaunchAgents (域: %s)", domain))

	if serviceName != "" {
		pp := plistPath(serviceName)
		_, plistErr := os.Stat(pp)
		plistExists := plistErr == nil

		if plistExists {
			statusLine(msg("Plist", "Plist 文件"), pp+" ✓")
		} else {
			statusLine(msg("Plist", "Plist 文件"), msg("not found", "未找到")+" ✗")
		}

		ls := queryLaunchdStatus(serviceName)
		statusLine(msg("launchd state", "launchd 状态"), formatLaunchdStatus(ls, plistExists))
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
