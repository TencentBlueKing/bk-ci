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
	"time"
)

func platformUnzip(src, dest string) error {
	cmd := exec.Command("powershell", "-NoProfile", "-Command",
		fmt.Sprintf("Expand-Archive -Path '%s' -DestinationPath '%s' -Force", src, dest))
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

func handleInstall(workDir string, args []string) error {
	fs := flag.NewFlagSet("install", flag.ContinueOnError)
	mode := fs.String("mode", "service", "")
	user := fs.String("user", "", "")
	pass := fs.String("password", "", "")
	autoLogon := fs.Bool("auto-logon", false, "")
	if err := fs.Parse(args); err != nil {
		return err
	}

	cleanupBeforeInstallWin(workDir)

	switch strings.ToLower(*mode) {
	case "service":
		return installService(workDir)
	case "session":
		if *autoLogon && *user == "" {
			return fmt.Errorf(msg("--auto-logon requires --user and --password",
				"--auto-logon 需要同时指定 --user 和 --password"))
		}
		if *user != "" && *pass == "" {
			return fmt.Errorf(msg("--password is required when --user is specified",
				"指定 --user 时必须提供 --password"))
		}
		return enableSession(workDir, *user, *pass, *autoLogon)
	case "task":
		printWarn(msg(
			"[DEPRECATED] Task mode is deprecated. Consider using '--mode session' for desktop access.",
			"[已废弃] 计划任务模式已废弃, 建议使用 '--mode session' 获取桌面访问能力。"))
		return installTask(workDir)
	default:
		return fmt.Errorf(msgf("unknown install mode: %s (valid: service, session, task)",
			"未知安装模式: %s (可选: service, session, task)", *mode))
	}
}

func isProcessAlive(pid int) bool {
	proc, err := os.FindProcess(pid)
	if err != nil {
		return false
	}
	proc.Release()
	return true
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
	case "TASK":
		return "TASK"
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

	writeInstallType(workDir, "TASK")

	printStep(msg("Step 3: starting scheduled task ...", "步骤 3: 启动计划任务 ..."))
	out, err = exec.Command("schtasks", "/run", "/tn", serviceName).CombinedOutput()
	if err != nil {
		printWarn(msgf("schtasks run: %s", "schtasks run: %s", strings.TrimSpace(string(out))))
	} else {
		printStep(msgf("Scheduled task %s started", "计划任务 %s 已启动", serviceName))
	}

	printStep(msg("Install complete", "安装完成"))
	printWarn(msg(
		"Task mode is deprecated. Use 'devopsAgent install --mode session' for desktop access.",
		"计划任务模式已废弃。请使用 'devopsAgent install --mode session' 获取桌面访问能力。"))
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
	os.Remove(filepath.Join(workDir, "devopsctl.vbs"))

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

	if serviceExists(serviceName) {
		printStep(msgf("Starting service %s ...", "启动服务 %s ...", serviceName))
		out, err := exec.Command("sc.exe", "start", serviceName).CombinedOutput()
		if err != nil {
			return cliErrorf("sc.exe start failed: %s (%v)", "sc.exe start 失败: %s (%v)", string(out), err)
		}
		printStep(msgf("Service %s started", "服务 %s 已启动", serviceName))
		return nil
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
	stopProcesses(workDir)
	printStep(msg("Agent stopped", "Agent 已停止"))
	return nil
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
	err := exec.Command("schtasks", "/query", "/tn", serviceName).Run()
	if err == nil {
		printStep(msgf("Removing legacy scheduled task: %s", "移除旧版计划任务: %s", serviceName))
		_ = exec.Command("schtasks", "/delete", "/tn", serviceName, "/f").Run()
	}
}
