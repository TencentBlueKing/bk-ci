//go:build windows
// +build windows

package agentcli

import (
	"flag"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"syscall"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/utils/fileutil"
)

const (
	modeTaskUpper = "TASK"
)

func platformUnzip(src, dest string) error {
	// 优先使用 Go 原生 archive/zip 解压，避免通过 PowerShell/外部命令
	// 传递用户可控路径导致的命令注入风险，同时做 ZipSlip 防护。
	if err := fileutil.Unzip(src, dest); err == nil {
		return nil
	} else {
		printWarn(msgf("native unzip failed: %v, trying unzip.exe fallback ...",
			"原生解压失败: %v, 尝试 unzip.exe 兜底 ...", err))
	}

	// 兜底：查找 agent 二进制同目录下的 unzip.exe。
	// 注意：src/dest 作为 exec.Command 的独立参数传入，不参与命令行解析，
	// 与 PowerShell -Command 字符串拼接不同，这里不存在注入风险。
	exePath, _ := os.Executable()
	unzipExe := filepath.Join(filepath.Dir(exePath), "unzip.exe")
	if _, err := os.Stat(unzipExe); err != nil {
		return fmt.Errorf(msgf(
			"native unzip failed and unzip.exe not found in %s",
			"原生解压失败且 %s 下未找到 unzip.exe",
			filepath.Dir(exePath)))
	}

	cmd := exec.Command(unzipExe, "-o", src, "-d", dest)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	if err := cmd.Run(); err != nil {
		return fmt.Errorf(msgf(
			"unzip.exe also failed: %v",
			"unzip.exe 也失败了: %v", err))
	}
	return nil
}

func handleInstall(workDir string, args []string) error {
	mode := modeServiceLower
	if len(args) > 0 {
		mode = strings.ToLower(args[0])
		args = args[1:]
	}

	cleanupBeforeInstallWin(workDir)

	switch mode {
	case modeServiceLower:
		return installService(workDir)
	case "session":
		var user, pass string
		var autoLogon bool
		fs := flag.NewFlagSet("session", flag.ContinueOnError)
		fs.BoolVar(&autoLogon, "auto-logon", false, "")
		if err := fs.Parse(args); err != nil {
			return err
		}
		if autoLogon {
			remaining := fs.Args()
			if len(remaining) < 2 {
				return fmt.Errorf(msg("--auto-logon requires USER and PASSWORD, e.g.: install session --auto-logon admin P@ssw0rd",
					"--auto-logon 需要跟用户名和密码, 例如: install session --auto-logon admin P@ssw0rd"))
			}
			user, pass = remaining[0], remaining[1]
		}
		return enableSession(workDir, user, pass, autoLogon)
	case "task":
		printWarn(msg(
			"[DEPRECATED] Task mode is deprecated. Consider using 'install session' for desktop access.",
			"[已废弃] 计划任务模式已废弃, 建议使用 'install session' 获取桌面访问能力。"))
		return installTask(workDir)
	default:
		return fmt.Errorf(msgf("unknown install mode: %s (valid: service, session, task)",
			"未知安装模式: %s (可选: service, session, task)", mode))
	}
}

func isProcessAlive(pid int) bool {
	const processQueryLimitedInformation = 0x1000
	const stillActive = 259

	h, err := syscall.OpenProcess(processQueryLimitedInformation, false, uint32(pid))
	if err != nil {
		return false
	}
	defer syscall.CloseHandle(h)

	var exitCode uint32
	if err := syscall.GetExitCodeProcess(h, &exitCode); err != nil {
		return false
	}
	return exitCode == stillActive
}

func readInstallMode(workDir string) string {
	data, err := os.ReadFile(filepath.Join(workDir, ".install_type"))
	if err != nil {
		return "SERVICE"
	}
	m := strings.TrimSpace(string(data))
	switch strings.ToUpper(m) {
	case "SESSION":
		return "SESSION"
	case modeTaskUpper:
		return modeTaskUpper
	}
	return "SERVICE"
}

func cleanupBeforeInstallWin(workDir string) {
	if _, err := os.Stat(filepath.Join(workDir, ".install_type")); err == nil {
		currentMode := readInstallTypeFile(workDir)
		printStep(msgf("Cleaning up previous %s installation ...",
			"清理之前的 %s 安装 ...", currentMode))
		_ = handleUninstall(workDir)
		return
	}
	printStep(msg("Cleaning up before install ...", "安装前清理 ..."))
	serviceName, _ := getServiceName(workDir)
	if serviceName != "" {
		if serviceExists(serviceName) {
			stopService(serviceName)
			deleteService(serviceName)
		}
		cleanupLegacySchtasks(serviceName)
	}
	stopProcesses(workDir)
}

func installService(workDir string) error {
	printDivider()
	printStep(msg("Installing agent daemon service (Windows)", "安装 Agent 守护进程服务 (Windows)"))
	printDivider()

	printStep(msg("Step 1: preparing work directory ...", "步骤 1: 准备工作目录 ..."))
	prepareWorkDir(workDir)

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	printStep(msg("Step 2: registering Windows service ...", "步骤 2: 注册 Windows 服务 ..."))
	cleanupLegacySchtasks(serviceName)

	if serviceExists(serviceName) {
		stopService(serviceName)
		deleteService(serviceName)
		time.Sleep(2 * time.Second)
	}

	daemonPath := filepath.Join(workDir, daemonBinary())
	if _, err := os.Stat(daemonPath); os.IsNotExist(err) {
		return fmt.Errorf(msgf("daemon binary not found: %s", "守护进程二进制未找到: %s", daemonPath))
	}

	binPath := fmt.Sprintf(`"%s"`, daemonPath)
	printStep(msgf("Creating service %s ...", "创建服务 %s ...", serviceName))
	if out, err := exec.Command("sc.exe", "create", serviceName, "binPath=", binPath, "start=", "auto").CombinedOutput(); err != nil {
		return cliErrorf("sc.exe create failed: %s (%v)", "sc.exe create 失败: %s (%v)", string(out), err)
	}

	configureSCMRecovery(serviceName)

	writeInstallType(workDir, "SERVICE")

	printStep(msg("Step 3: starting service ...", "步骤 3: 启动服务 ..."))
	if out, err := exec.Command("sc.exe", "start", serviceName).CombinedOutput(); err != nil {
		printWarn(msgf("sc.exe start: %s", "sc.exe start: %s", string(out)))
	}

	time.Sleep(2 * time.Second)
	if serviceRunning(serviceName) {
		printStep(msgf("Service %s is running", "服务 %s 已启动", serviceName))
	} else {
		printWarn(msgf("Service %s may not have started, check: sc.exe query %s",
			"服务 %s 可能未启动, 请检查: sc.exe query %s", serviceName, serviceName))
	}
	printStep(msg("Install complete", "安装完成"))
	return nil
}

func installTask(workDir string) error {
	printDivider()
	printStep(msg("Installing agent as scheduled task (Windows) [DEPRECATED]",
		"以计划任务方式安装 Agent (Windows) [已废弃]"))
	printDivider()

	printStep(msg("Step 1: preparing work directory ...", "步骤 1: 准备工作目录 ..."))
	prepareWorkDir(workDir)

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	printStep(msg("Step 2: creating scheduled task ...", "步骤 2: 创建计划任务 ..."))

	if serviceExists(serviceName) {
		stopService(serviceName)
		deleteService(serviceName)
		time.Sleep(2 * time.Second)
	}
	cleanupLegacySchtasks(serviceName)

	daemonPath := filepath.Join(workDir, daemonBinary())
	if _, err := os.Stat(daemonPath); os.IsNotExist(err) {
		return fmt.Errorf(msgf("daemon binary not found: %s", "守护进程二进制未找到: %s", daemonPath))
	}

	vbsPath := filepath.Join(workDir, "devopsctl.vbs")
	vbsContent := fmt.Sprintf("Set ws = CreateObject(\"Wscript.Shell\")\nws.run \"\"\"%s\"\"\",0", daemonPath)
	if err := os.WriteFile(vbsPath, []byte(vbsContent), 0644); err != nil {
		return fmt.Errorf(msgf("write devopsctl.vbs failed: %v", "写入 devopsctl.vbs 失败: %v", err))
	}
	printStep(msgf("Created %s", "已创建 %s", vbsPath))

	tr := fmt.Sprintf(`wscript.exe "%s"`, vbsPath)
	out, err := exec.Command("schtasks", "/create", "/tn", serviceName,
		"/tr", tr, "/sc", "onlogon", "/rl", "highest", "/f").CombinedOutput()
	if err != nil {
		return cliErrorf("schtasks create failed: %s (%v)", "schtasks create 失败: %s (%v)", strings.TrimSpace(string(out)), err)
	}
	printStep(msgf("Scheduled task %s created (trigger: on logon)",
		"计划任务 %s 已创建 (触发: 用户登录时)", serviceName))

	writeInstallType(workDir, modeTaskUpper)

	printStep(msg("Step 3: starting scheduled task ...", "步骤 3: 启动计划任务 ..."))
	out, err = exec.Command("schtasks", "/run", "/tn", serviceName).CombinedOutput()
	if err != nil {
		printWarn(msgf("schtasks run: %s", "schtasks run: %s", strings.TrimSpace(string(out))))
	} else {
		printStep(msgf("Scheduled task %s started", "计划任务 %s 已启动", serviceName))
	}

	printStep(msg("Install complete", "安装完成"))
	printWarn(msg(
		"Task mode is deprecated. Use 'devopsAgent install session' for desktop access.",
		"计划任务模式已废弃。请使用 'devopsAgent install session' 获取桌面访问能力。"))
	return nil
}

func handleUninstall(workDir string) error {
	printDivider()
	printStep(msg("Uninstalling agent daemon service (Windows)", "卸载 Agent 守护进程服务 (Windows)"))
	printDivider()

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	printStep(msg("Removing session configuration ...", "移除会话配置 ..."))
	removeSessionSecrets()
	removeAutoLogon()
	os.Remove(filepath.Join(workDir, ".install_type"))

	if serviceExists(serviceName) {
		printStep(msg("Removing Windows service ...", "移除 Windows 服务 ..."))
		stopService(serviceName)
		deleteService(serviceName)
	}

	cleanupLegacySchtasks(serviceName)

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

	mode := readInstallMode(workDir)

	switch mode {
	case modeTaskUpper:
		// task 模式优先用 schtasks，降级到 service
		if schtasksExists(serviceName) {
			printStep(msgf("Starting scheduled task %s ...", "启动计划任务 %s ...", serviceName))
			if err := startSchtasks(serviceName); err != nil {
				return err
			}
			printStep(msgf("Scheduled task %s started", "计划任务 %s 已启动", serviceName))
			return nil
		}
		if serviceExists(serviceName) {
			printStep(msgf("Scheduled task not found, starting service %s ...",
				"计划任务未找到, 启动服务 %s ...", serviceName))
			out, err := exec.Command("sc.exe", "start", serviceName).CombinedOutput()
			if err != nil {
				return cliErrorf("sc.exe start failed: %s (%v)", "sc.exe start 失败: %s (%v)", string(out), err)
			}
			printStep(msgf("Service %s started", "服务 %s 已启动", serviceName))
			return nil
		}

	default: // SERVICE, SESSION
		// service/session 模式优先用 sc.exe，降级到 schtasks
		if serviceExists(serviceName) {
			printStep(msgf("Starting service %s ...", "启动服务 %s ...", serviceName))
			out, err := exec.Command("sc.exe", "start", serviceName).CombinedOutput()
			if err != nil {
				return cliErrorf("sc.exe start failed: %s (%v)", "sc.exe start 失败: %s (%v)", string(out), err)
			}
			printStep(msgf("Service %s started", "服务 %s 已启动", serviceName))
			return nil
		}
		if schtasksExists(serviceName) {
			printStep(msgf("Service not found, starting scheduled task %s ...",
				"服务未找到, 启动计划任务 %s ...", serviceName))
			if err := startSchtasks(serviceName); err != nil {
				return err
			}
			printStep(msgf("Scheduled task %s started", "计划任务 %s 已启动", serviceName))
			return nil
		}
	}

	printWarn(msg("service not found, cannot start", "服务未注册, 无法启动"))
	return nil
}

func handleStop(workDir string) error {
	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	if serviceExists(serviceName) {
		stopService(serviceName)
	}
	// 结束计划任务正在运行的实例（task 模式）
	if schtasksExists(serviceName) {
		_ = exec.Command("schtasks", "/end", "/tn", serviceName).Run()
	}
	stopProcesses(workDir)
	printStep(msg("Agent stopped", "Agent 已停止"))
	return nil
}

// configureSCMRecovery sets SCM failure recovery options so that the service
// automatically restarts when the daemon exits unexpectedly (e.g., after a
// daemon binary upgrade). Delays: 5s / 10s / 30s for 1st / 2nd / 3rd failure.
// Inspired by GitHub Actions Runner's approach to self-updating service hosts.
func configureSCMRecovery(serviceName string) {
	if out, err := exec.Command("sc.exe", "failure", serviceName,
		"reset=", "86400",
		"actions=", "restart/5000/restart/10000/restart/30000",
	).CombinedOutput(); err != nil {
		printWarn(msgf("sc.exe failure config warning: %s (%v)",
			"sc.exe failure 配置警告: %s (%v)", strings.TrimSpace(string(out)), err))
	}
}

// ── sc.exe helpers ───────────────────────────────────────────────────────

func serviceExists(name string) bool {
	err := exec.Command("sc.exe", "query", name).Run()
	return err == nil
}

func serviceRunning(name string) bool {
	out, err := exec.Command("sc.exe", "query", name).CombinedOutput()
	if err != nil {
		return false
	}
	return strings.Contains(string(out), "RUNNING")
}

func stopService(name string) {
	printStep(msgf("Stopping service %s ...", "停止服务 %s ...", name))
	_ = exec.Command("sc.exe", "stop", name).Run()
	for i := 0; i < 20; i++ {
		time.Sleep(time.Second)
		if !serviceRunning(name) {
			return
		}
	}
	printWarn(msg("service stop timed out", "停止服务超时"))
}

func deleteService(name string) {
	printStep(msgf("Deleting service %s ...", "删除服务 %s ...", name))
	_ = exec.Command("sc.exe", "delete", name).Run()
}

func cleanupLegacySchtasks(serviceName string) {
	if schtasksExists(serviceName) {
		printStep(msgf("Removing legacy scheduled task: %s", "移除旧版计划任务: %s", serviceName))
		_ = exec.Command("schtasks", "/delete", "/tn", serviceName, "/f").Run()
	}
}

// ── schtasks helpers ────────────────────────────────────────────────────

func schtasksExists(name string) bool {
	err := exec.Command("schtasks", "/query", "/tn", name).Run()
	return err == nil
}

func startSchtasks(serviceName string) error {
	out, err := exec.Command("schtasks", "/run", "/tn", serviceName).CombinedOutput()
	if err != nil {
		return cliErrorf("schtasks run failed: %s (%v)", "schtasks run 失败: %s (%v)", strings.TrimSpace(string(out)), err)
	}
	return nil
}
