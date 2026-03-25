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

func handleInstall(workDir string) error {
	printStep("Installing agent daemon service (macOS)...")
	prepareWorkDir(workDir)

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	unloadPlist(serviceName)

	if err := writePlist(workDir, serviceName); err != nil {
		return err
	}

	return loadPlist(serviceName)
}

func handleUninstall(workDir string) error {
	printStep("Uninstalling agent daemon service (macOS)...")
	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	unloadPlist(serviceName)
	removePlist(serviceName)
	stopProcesses(workDir)
	printStep("uninstall complete")
	return nil
}

func handleStart(workDir string) error {
	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	plistPath := plistPath(serviceName)
	if _, err := os.Stat(plistPath); err == nil {
		printStepf("loading %s via launchctl", serviceName)
		out, err := exec.Command("launchctl", "load", "-w", plistPath).CombinedOutput()
		if err != nil {
			return fmt.Errorf("launchctl load: %s (%w)", strings.TrimSpace(string(out)), err)
		}
		printStepf("service %s started", serviceName)
		return nil
	}

	return startDirect(workDir)
}

func handleStop(workDir string) error {
	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	unloadPlist(serviceName)
	stopProcesses(workDir)
	printStep("agent stopped")
	return nil
}

// ── launchd ──────────────────────────────────────────────────────────────

// plistDir returns the appropriate plist directory:
//   - root: /Library/LaunchDaemons (system-level, runs at boot)
//   - user: ~/Library/LaunchAgents (user-level, runs at login)
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

func writePlist(workDir, serviceName string) error {
	daemonPath := filepath.Join(workDir, daemonBinary())
	os.Chmod(daemonPath, 0755)
	os.Chmod(filepath.Join(workDir, agentBinary()), 0755)

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
        <false/>
</dict>
</plist>
`, serviceName, daemonPath, workDir)

	dir := plistDir()
	os.MkdirAll(dir, 0755)

	pp := plistPath(serviceName)
	if err := os.WriteFile(pp, []byte(plist), 0644); err != nil {
		return fmt.Errorf("write plist: %w", err)
	}

	level := "user-level (LaunchAgents)"
	if isRoot() {
		level = "system-level (LaunchDaemons)"
	}
	printStepf("created %s (%s)", pp, level)
	return nil
}

func loadPlist(serviceName string) error {
	pp := plistPath(serviceName)
	out, err := exec.Command("launchctl", "load", "-w", pp).CombinedOutput()
	if err != nil {
		return fmt.Errorf("launchctl load: %s (%w)", strings.TrimSpace(string(out)), err)
	}
	printStepf("service %s loaded", serviceName)
	return nil
}

func unloadPlist(serviceName string) {
	pp := plistPath(serviceName)
	if _, err := os.Stat(pp); err == nil {
		_ = exec.Command("launchctl", "unload", pp).Run()
	}
	// Also try the other location for cleanup during migration
	other := otherPlistPath(serviceName)
	if _, err := os.Stat(other); err == nil {
		_ = exec.Command("launchctl", "unload", other).Run()
		os.Remove(other)
		printStepf("cleaned up legacy plist %s", other)
	}
}

func removePlist(serviceName string) {
	pp := plistPath(serviceName)
	if _, err := os.Stat(pp); err == nil {
		os.Remove(pp)
		printStepf("removed %s", pp)
	}
}

func otherPlistPath(serviceName string) string {
	if isRoot() {
		home, _ := os.UserHomeDir()
		return filepath.Join(home, "Library", "LaunchAgents", serviceName+".plist")
	}
	return filepath.Join("/Library/LaunchDaemons", serviceName+".plist")
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
		return fmt.Errorf("start daemon: %w", err)
	}
	printStepf("daemon started, pid=%d", cmd.Process.Pid)
	cmd.Process.Release()
	return nil
}
