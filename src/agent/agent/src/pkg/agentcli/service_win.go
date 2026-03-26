//go:build windows
// +build windows

package agentcli

import (
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

func handleInstall(workDir string) error {
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
		return fmt.Errorf("sc.exe create: %s (%w)", string(out), err)
	}

	printStep(msg("Step 3: starting service ...", "步骤 3: 启动服务 ..."))
	if out, err := exec.Command("sc.exe", "start", serviceName).CombinedOutput(); err != nil {
		printWarn(fmt.Sprintf("sc.exe start: %s", string(out)))
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

	if serviceExists(serviceName) {
		printStep(msgf("Starting service %s ...", "启动服务 %s ...", serviceName))
		out, err := exec.Command("sc.exe", "start", serviceName).CombinedOutput()
		if err != nil {
			return fmt.Errorf("sc.exe start: %s (%w)", string(out), err)
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
