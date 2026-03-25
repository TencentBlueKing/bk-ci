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
	printStep("Installing agent daemon service (Windows)...")
	prepareWorkDir(workDir)

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	cleanupLegacySchtasks(serviceName)

	if serviceExists(serviceName) {
		stopService(serviceName)
		deleteService(serviceName)
		time.Sleep(2 * time.Second)
	}

	daemonPath := filepath.Join(workDir, daemonBinary())
	if _, err := os.Stat(daemonPath); os.IsNotExist(err) {
		return fmt.Errorf("daemon binary not found: %s", daemonPath)
	}

	binPath := fmt.Sprintf(`"%s"`, daemonPath)
	printStepf("creating service %s", serviceName)
	if out, err := exec.Command("sc.exe", "create", serviceName, "binPath=", binPath, "start=", "auto").CombinedOutput(); err != nil {
		return fmt.Errorf("sc.exe create: %s (%w)", string(out), err)
	}

	printStepf("starting service %s", serviceName)
	if out, err := exec.Command("sc.exe", "start", serviceName).CombinedOutput(); err != nil {
		printWarn(fmt.Sprintf("sc.exe start: %s", string(out)))
	}

	time.Sleep(2 * time.Second)
	if serviceRunning(serviceName) {
		printStepf("service %s is running", serviceName)
	} else {
		printWarn(fmt.Sprintf("service %s may not have started, check: sc.exe query %s", serviceName, serviceName))
	}
	return nil
}

func handleUninstall(workDir string) error {
	printStep("Uninstalling agent daemon service (Windows)...")
	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	removeSessionSecrets()
	removeAutoLogon()

	if serviceExists(serviceName) {
		stopService(serviceName)
		deleteService(serviceName)
	}

	cleanupLegacySchtasks(serviceName)
	stopProcesses(workDir)

	printStep("uninstall complete")
	return nil
}

func handleStart(workDir string) error {
	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	if serviceExists(serviceName) {
		printStepf("starting service %s", serviceName)
		out, err := exec.Command("sc.exe", "start", serviceName).CombinedOutput()
		if err != nil {
			return fmt.Errorf("sc.exe start: %s (%w)", string(out), err)
		}
		printStepf("service %s started", serviceName)
		return nil
	}

	printWarn("service not found, cannot start")
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
	printStep("agent stopped")
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
	printStepf("stopping service %s", name)
	_ = exec.Command("sc.exe", "stop", name).Run()
	for i := 0; i < 20; i++ {
		time.Sleep(time.Second)
		if !serviceRunning(name) {
			return
		}
	}
	printWarn("service stop timed out")
}

func deleteService(name string) {
	printStepf("deleting service %s", name)
	_ = exec.Command("sc.exe", "delete", name).Run()
}

func cleanupLegacySchtasks(serviceName string) {
	err := exec.Command("schtasks", "/query", "/tn", serviceName).Run()
	if err == nil {
		printStepf("removing legacy scheduled task: %s", serviceName)
		_ = exec.Command("schtasks", "/delete", "/tn", serviceName, "/f").Run()
	}
}
