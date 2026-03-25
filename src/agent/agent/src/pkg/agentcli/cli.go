package agentcli

import (
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
)

// Run dispatches CLI subcommands. Called from agent main() before process lock.
func Run(workDir string, args []string) {
	if len(args) == 0 {
		PrintUsage()
		os.Exit(1)
	}

	var err error
	switch args[0] {
	case "install":
		err = handleInstall(workDir)
	case "uninstall":
		err = handleUninstall(workDir)
	case "start":
		err = handleStart(workDir)
	case "stop":
		err = handleStop(workDir)
	case "repair":
		err = handleRepair(workDir)
	case "configure-session":
		err = handleConfigureSession(workDir, args[1:])
	default:
		fmt.Fprintf(os.Stderr, "unknown command: %s\n", args[0])
		PrintUsage()
		os.Exit(1)
	}

	if err != nil {
		fmt.Fprintf(os.Stderr, "[BK-CI][ERROR] %s\n", err)
		os.Exit(1)
	}
}

// IsSubcommand returns true if the argument is a known CLI subcommand.
func IsSubcommand(arg string) bool {
	switch arg {
	case "install", "uninstall", "start", "stop", "repair", "configure-session":
		return true
	}
	return false
}

func PrintUsage() {
	fmt.Print(`Usage: devopsAgent <command> [options]

Service management:
  install              Install and start agent daemon service
  uninstall            Stop and remove agent daemon service
  start                Start agent daemon
  stop                 Stop agent daemon

Maintenance:
  repair               Stop agent, re-extract JDK/dependencies, restart

Session mode (Windows only):
  configure-session    Configure desktop session access
    --user USER        Windows logon account (optional)
    --password PASS    Password (required with --user)
    --auto-logon       Enable Windows auto-logon on reboot
    --disable          Revert to plain service mode

Other:
  version              Print version
  fullVersion          Print full version info
  (no command)         Run agent (normal mode)
`)
}

// ── Helpers ──────────────────────────────────────────────────────────────

func printStep(msg string)                  { fmt.Printf("[BK-CI] %s\n", msg) }
func printWarn(msg string)                  { fmt.Printf("[BK-CI][WARN] %s\n", msg) }
func printStepf(f string, a ...interface{}) { fmt.Printf("[BK-CI] "+f+"\n", a...) }

func readProperty(workDir, key string) (string, error) {
	data, err := os.ReadFile(filepath.Join(workDir, ".agent.properties"))
	if err != nil {
		return "", fmt.Errorf("cannot read .agent.properties: %w", err)
	}
	prefix := key + "="
	for _, line := range strings.Split(string(data), "\n") {
		line = strings.TrimSpace(line)
		if strings.HasPrefix(line, "#") {
			continue
		}
		if strings.HasPrefix(line, prefix) {
			return strings.TrimPrefix(line, prefix), nil
		}
	}
	return "", fmt.Errorf("key %s not found in .agent.properties", key)
}

func getServiceName(workDir string) (string, error) {
	id, err := readProperty(workDir, "devops.agent.id")
	if err != nil {
		return "", err
	}
	return "devops_agent_" + id, nil
}

func daemonBinary() string {
	if runtime.GOOS == "windows" {
		return "devopsDaemon.exe"
	}
	return "devopsDaemon"
}

func agentBinary() string {
	if runtime.GOOS == "windows" {
		return "devopsAgent.exe"
	}
	return "devopsAgent"
}

// killByPidFile reads a PID from file and kills the process if alive.
func killByPidFile(pidFile string) {
	data, err := os.ReadFile(pidFile)
	if err != nil {
		return
	}
	pid, err := strconv.Atoi(strings.TrimSpace(string(data)))
	if err != nil || pid <= 0 {
		return
	}
	proc, err := os.FindProcess(pid)
	if err != nil {
		return
	}
	if err := proc.Kill(); err == nil {
		printStepf("killed process PID %d", pid)
	}
}

// stopProcesses kills daemon and agent by PID files.
func stopProcesses(workDir string) {
	killByPidFile(filepath.Join(workDir, "runtime", "daemon.pid"))
	killByPidFile(filepath.Join(workDir, "runtime", "agent.pid"))
}

// prepareWorkDir unzips JDK (if zip exists and dir doesn't) and creates standard directories.
func prepareWorkDir(workDir string) {
	unzipIfNeeded(filepath.Join(workDir, "jdk17.zip"), filepath.Join(workDir, "jdk17"), false)
	unzipIfNeeded(filepath.Join(workDir, "jre.zip"), filepath.Join(workDir, "jdk"), false)

	os.MkdirAll(filepath.Join(workDir, "logs"), 0755)
	os.MkdirAll(filepath.Join(workDir, "workspace"), 0755)
}

// repairWorkDir force re-extracts JDK from zip regardless of whether the dir exists.
func repairWorkDir(workDir string) {
	unzipIfNeeded(filepath.Join(workDir, "jdk17.zip"), filepath.Join(workDir, "jdk17"), true)
	unzipIfNeeded(filepath.Join(workDir, "jre.zip"), filepath.Join(workDir, "jdk"), true)

	os.MkdirAll(filepath.Join(workDir, "logs"), 0755)
	os.MkdirAll(filepath.Join(workDir, "workspace"), 0755)
}

func unzipIfNeeded(zipPath, destDir string, force bool) {
	if _, err := os.Stat(zipPath); os.IsNotExist(err) {
		printWarn(fmt.Sprintf("%s not found, skipped", filepath.Base(zipPath)))
		return
	}
	if _, err := os.Stat(destDir); err == nil {
		if !force {
			return
		}
		printStepf("removing %s for re-extract", filepath.Base(destDir))
		os.RemoveAll(destDir)
	}
	printStepf("unzipping %s", filepath.Base(zipPath))
	if err := unzipFile(zipPath, destDir); err != nil {
		printWarn(fmt.Sprintf("unzip %s failed: %v", filepath.Base(zipPath), err))
	}
}

// handleRepair stops the agent, re-extracts JDK/dependencies from zip, then restarts.
func handleRepair(workDir string) error {
	printStep("Repairing agent files...")

	printStep("stopping agent...")
	_ = handleStop(workDir)

	repairWorkDir(workDir)

	printStep("restarting agent...")
	if err := handleStart(workDir); err != nil {
		return err
	}

	printStep("repair complete")
	return nil
}

func unzipFile(src, dest string) error {
	return platformUnzip(src, dest)
}
