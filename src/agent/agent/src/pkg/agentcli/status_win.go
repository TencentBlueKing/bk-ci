//go:build windows
// +build windows

package agentcli

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"unsafe"

	"golang.org/x/sys/windows"
	"golang.org/x/sys/windows/registry"
)

func handleStatus(workDir string) error {
	printDivider()
	printStep(msg("BK-CI Agent Status", "BK-CI Agent 状态"))
	printDivider()

	serviceName, _ := getServiceName(workDir)
	statusLine(msg("Platform", "平台"), "Windows")
	statusLine(msg("Work directory", "工作目录"), workDir)
	statusLine(msg("Service name", "服务名"), serviceName)
	statusLine(msg("Current user", "当前用户"), os.Getenv("USERNAME"))

	// Install type
	installType := readInstallTypeFile(workDir)
	runMode := detectRunMode(serviceName, installType)
	statusLine(msg("Install type", "安装类型"), installType)
	statusLine(msg("Run mode", "运行模式"), runMode)

	// Service status
	if serviceName != "" && serviceExists(serviceName) {
		if serviceRunning(serviceName) {
			statusLine(msg("Service state", "服务状态"), msg("running", "运行中")+" ✓")
		} else {
			statusLine(msg("Service state", "服务状态"), msg("stopped", "已停止")+" ✗")
		}
	} else {
		statusLine(msg("Service state", "服务状态"), msg("not registered", "未注册"))
	}

	// Scheduled task (legacy)
	hasSchtask := false
	if serviceName != "" {
		err := exec.Command("schtasks", "/query", "/tn", serviceName).Run()
		hasSchtask = err == nil
	}
	if hasSchtask {
		statusLine(msg("Scheduled task", "计划任务"), msg("exists (legacy)", "存在 (旧版)")+" ⚠")
	}

	// Process status
	daemonPid := readPid(filepath.Join(workDir, "runtime", "daemon.pid"))
	agentPid := readPid(filepath.Join(workDir, "runtime", "agent.pid"))
	statusLine(msg("Daemon PID", "守护进程 PID"), pidStatus(daemonPid))
	statusLine(msg("Agent PID", "Agent PID"), pidStatus(agentPid))

	// Session mode details
	fmt.Println()
	printStep(msg("Session Mode Details", "会话模式详情"))
	printStep("--------------------------------------------")

	hasUser := checkLsaSecretExists(lsaKeyUser)
	hasPass := checkLsaSecretExists(lsaKeyPassword)
	autoLogon := checkAutoLogonEnabled()
	autoLogonUser := readAutoLogonUser()

	if hasUser {
		userName := readLsaSecretSafe(lsaKeyUser)
		statusLine(msg("Session credentials", "会话凭据"), msgf("stored (user: %s)", "已存储 (用户: %s)", userName)+" ✓")
	} else {
		statusLine(msg("Session credentials", "会话凭据"), msg("not configured", "未配置"))
	}
	if hasPass {
		statusLine(msg("Session password", "会话密码"), msg("stored in LSA Secret", "已加密存储于 LSA Secret")+" ✓")
	} else {
		statusLine(msg("Session password", "会话密码"), msg("not configured", "未配置"))
	}

	if autoLogon {
		statusLine(msg("Windows auto-logon", "Windows 自动登录"),
			msgf("enabled (user: %s)", "已启用 (用户: %s)", autoLogonUser)+" ✓")
	} else {
		statusLine(msg("Windows auto-logon", "Windows 自动登录"), msg("disabled", "未启用"))
	}

	hasAutoLogonPass := checkLsaSecretExists("DefaultPassword")
	if hasAutoLogonPass {
		statusLine(msg("Auto-logon password", "自动登录密码"), msg("stored in LSA Secret", "已加密存储于 LSA Secret")+" ✓")
	} else if autoLogon {
		statusLine(msg("Auto-logon password", "自动登录密码"), msg("missing!", "缺失!")+" ✗")
	}

	// Dependencies
	fmt.Println()
	printStep(msg("Dependencies", "依赖组件"))
	printStep("--------------------------------------------")
	statusLine("JDK 17", dirStatus(filepath.Join(workDir, "jdk17")))
	statusLine("JDK 8", dirStatus(filepath.Join(workDir, "jdk")))
	statusLine("worker-agent.jar", fileStatus(filepath.Join(workDir, "worker-agent.jar")))

	printHealthChecks(workDir)

	return nil
}

func readInstallTypeFile(workDir string) string {
	data, err := os.ReadFile(filepath.Join(workDir, ".install_type"))
	if err != nil {
		return "SERVICE (" + msg("default, no .install_type file", "默认, 无 .install_type 文件") + ")"
	}
	return strings.TrimSpace(string(data))
}

func detectRunMode(serviceName, installType string) string {
	it := strings.ToUpper(strings.TrimSpace(installType))

	hasSvc := serviceName != "" && serviceExists(serviceName)
	hasSchtask := false
	if serviceName != "" {
		err := exec.Command("schtasks", "/query", "/tn", serviceName).Run()
		hasSchtask = err == nil
	}

	if strings.Contains(it, "SESSION") && hasSvc {
		return msg("SERVICE + SESSION (desktop session access)", "服务 + 会话模式 (桌面会话访问)")
	}
	if hasSvc {
		return msg("SERVICE (Windows service)", "服务模式 (Windows 服务)")
	}
	if hasSchtask {
		return msg("TASK (scheduled task, legacy)", "计划任务模式 (旧版)")
	}
	return msg("MANUAL (not installed as service)", "手动模式 (未注册为服务)")
}

func checkAutoLogonEnabled() bool {
	k, err := registry.OpenKey(
		registry.LOCAL_MACHINE,
		`SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon`,
		registry.QUERY_VALUE,
	)
	if err != nil {
		return false
	}
	defer k.Close()
	val, _, err := k.GetStringValue("AutoAdminLogon")
	return err == nil && val == "1"
}

func readAutoLogonUser() string {
	k, err := registry.OpenKey(
		registry.LOCAL_MACHINE,
		`SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon`,
		registry.QUERY_VALUE,
	)
	if err != nil {
		return ""
	}
	defer k.Close()

	domain, _, _ := k.GetStringValue("DefaultDomainName")
	user, _, _ := k.GetStringValue("DefaultUserName")
	if domain != "" && domain != "." {
		return domain + "\\" + user
	}
	return user
}

func checkLsaSecretExists(key string) bool {
	handle, err := lsaOpenPolicy(policyGetPrivateInfo)
	if err != nil {
		return false
	}
	defer procLsaClose.Call(handle)

	k := toLsaString(key)
	var privateData uintptr
	ret, _, _ := procLsaRetrievePrivateData.Call(
		handle,
		uintptr(unsafe.Pointer(&k)),
		uintptr(unsafe.Pointer(&privateData)),
	)
	if ret != 0 || privateData == 0 {
		return false
	}
	procLsaFreeMemory.Call(privateData)
	return true
}

func readLsaSecretSafe(key string) string {
	handle, err := lsaOpenPolicy(policyGetPrivateInfo)
	if err != nil {
		return ""
	}
	defer procLsaClose.Call(handle)

	k := toLsaString(key)
	var privateData *lsaUnicodeString
	ret, _, _ := procLsaRetrievePrivateData.Call(
		handle,
		uintptr(unsafe.Pointer(&k)),
		uintptr(unsafe.Pointer(&privateData)),
	)
	if ret != 0 || privateData == nil || privateData.Buffer == nil {
		return ""
	}
	defer procLsaFreeMemory.Call(uintptr(unsafe.Pointer(privateData)))
	return windows.UTF16PtrToString(privateData.Buffer)
}

// Process and file status helpers

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
	h, err := windows.OpenProcess(windows.PROCESS_QUERY_LIMITED_INFORMATION, false, uint32(pid))
	if err != nil {
		return fmt.Sprintf("%d (%s)", pid, msg("not running", "已退出"))
	}
	windows.CloseHandle(h)
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
	if info.Size() == 0 {
		return msg("empty", "空文件") + " ✗"
	}
	return fmt.Sprintf("%s ✓ (%.1f MB)", msg("OK", "正常"), float64(info.Size())/1024/1024)
}

func statusLine(label, value string) {
	fmt.Printf("  %-24s %s\n", label+":", value)
}
