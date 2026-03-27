package agentcli

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

// Run dispatches CLI subcommands. Called from agent main() before process lock.
func Run(workDir string, args []string) {
	initLang(workDir)

	if len(args) == 0 {
		printUsageLocalized()
		os.Exit(1)
	}

	var err error
	switch args[0] {
	case "-h", "--help", "help":
		printUsageLocalized()
		return
	case "version":
		fmt.Println(config.AgentVersion)
		return
	case "fullVersion":
		fmt.Println(config.AgentVersion)
		fmt.Println(config.GitCommit)
		fmt.Println(config.BuildTime)
		return
	case "debug":
		err = handleDebug(workDir, args[1:])
	case "install":
		err = handleInstall(workDir, args[1:])
	case "uninstall":
		err = handleUninstall(workDir)
	case "start":
		err = handleStart(workDir)
	case "stop":
		err = handleStop(workDir)
	case "repair":
		err = handleRepair(workDir)
	case "reinstall":
		err = handleReinstall(workDir, args[1:])
	case "status":
		err = handleStatus(workDir)
	case "configure-session":
		err = handleConfigureSession(workDir, args[1:])
	default:
		printErr(msgf("unknown command: %s", "未知命令: %s", args[0]))
		printUsageLocalized()
		os.Exit(1)
	}

	if err != nil {
		printErr(err.Error())
		os.Exit(1)
	}
}

// IsSubcommand returns true if the argument is a known CLI subcommand.
func IsSubcommand(arg string) bool {
	switch arg {
	case "install", "uninstall", "start", "stop", "repair", "reinstall", "status", "configure-session",
		"version", "fullVersion", "debug",
		"-h", "--help", "help":
		return true
	}
	return false
}

const debugFile = ".debug"

func handleDebug(workDir string, args []string) error {
	debugPath := filepath.Join(workDir, debugFile)

	if len(args) == 0 {
		_, err := os.Stat(debugPath)
		if err == nil {
			printStep(msg("Debug mode: ON (.debug file exists)", "调试模式: 开启 (.debug 文件存在)"))
		} else {
			printStep(msg("Debug mode: OFF", "调试模式: 关闭"))
		}
		return nil
	}

	switch args[0] {
	case "on", "enable":
		if err := os.WriteFile(debugPath, []byte("1"), 0644); err != nil {
			return err
		}
		printStep(msg(
			"Debug mode enabled. Restart agent to take effect.",
			"调试模式已开启。重启 Agent 后生效。"))
	case "off", "disable":
		os.Remove(debugPath)
		printStep(msg(
			"Debug mode disabled. Restart agent to take effect.",
			"调试模式已关闭。重启 Agent 后生效。"))
	default:
		return fmt.Errorf(msgf("unknown debug action: %s (use: on/off)",
			"未知调试操作: %s (可用: on/off)", args[0]))
	}
	return nil
}

// DebugFileExists checks if the .debug file exists in the given directory.
func DebugFileExists(workDir string) bool {
	_, err := os.Stat(filepath.Join(workDir, debugFile))
	return err == nil
}

// ── Output helpers ───────────────────────────────────────────────────────

func printStep(m string)                    { fmt.Printf("[BK-CI] %s\n", m) }
func printWarn(m string)                    { fmt.Printf("[BK-CI][WARN] %s\n", m) }
func printErr(m string)                     { fmt.Fprintf(os.Stderr, "[BK-CI][ERROR] %s\n", m) }
func printStepf(f string, a ...interface{}) { fmt.Printf("[BK-CI] "+f+"\n", a...) }

func printDivider() {
	printStep("============================================")
}

// ── Property / config helpers ────────────────────────────────────────────

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

// ── Process helpers ──────────────────────────────────────────────────────

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
		printStep(msgf("killed process PID %d", "已终止进程 PID %d", pid))
	}
}

func stopProcesses(workDir string) {
	killByPidFile(filepath.Join(workDir, "runtime", "daemon.pid"))
	killByPidFile(filepath.Join(workDir, "runtime", "agent.pid"))
}

// ── Work directory preparation ───────────────────────────────────────────

func prepareWorkDir(workDir string) {
	unzipIfNeeded(filepath.Join(workDir, "jdk17.zip"), filepath.Join(workDir, "jdk17"), false)
	unzipIfNeeded(filepath.Join(workDir, "jre.zip"), filepath.Join(workDir, "jdk"), false)
	os.MkdirAll(filepath.Join(workDir, "logs"), 0755)
	os.MkdirAll(filepath.Join(workDir, "workspace"), 0755)
}

func repairWorkDir(workDir string) {
	unzipIfNeeded(filepath.Join(workDir, "jdk17.zip"), filepath.Join(workDir, "jdk17"), true)
	unzipIfNeeded(filepath.Join(workDir, "jre.zip"), filepath.Join(workDir, "jdk"), true)
	os.MkdirAll(filepath.Join(workDir, "logs"), 0755)
	os.MkdirAll(filepath.Join(workDir, "workspace"), 0755)
}

func unzipIfNeeded(zipPath, destDir string, force bool) {
	base := filepath.Base(zipPath)
	if _, err := os.Stat(zipPath); os.IsNotExist(err) {
		printWarn(msgf("%s not found, skipped", "%s 未找到, 跳过", base))
		return
	}
	if _, err := os.Stat(destDir); err == nil {
		if !force {
			return
		}
		printStep(msgf("removing %s for re-extract", "删除 %s 以重新解压", filepath.Base(destDir)))
		os.RemoveAll(destDir)
	}
	printStep(msgf("extracting %s ...", "解压 %s ...", base))
	if err := unzipFile(zipPath, destDir); err != nil {
		printWarn(msgf("extract %s failed: %v", "解压 %s 失败: %v", base, err))
	}
}

func unzipFile(src, dest string) error {
	return platformUnzip(src, dest)
}

// ── Reinstall ────────────────────────────────────────────────────────────

func preserveSet() map[string]bool {
	m := map[string]bool{
		".agent.properties": true,
		".install_type":     true,
		".cert":             true,
		"workspace":         true,
	}
	m[agentBinary()] = true
	m[installScriptName()] = true
	return m
}

func installScriptName() string {
	if runtime.GOOS == "windows" {
		return "download_install.ps1"
	}
	return "install.sh"
}

func runInstallScript(workDir string) error {
	script := installScriptName()
	scriptPath := filepath.Join(workDir, script)
	if _, err := os.Stat(scriptPath); os.IsNotExist(err) {
		return fmt.Errorf(msgf(
			"install script %s not found, cannot re-download. Please re-download manually.",
			"安装脚本 %s 未找到, 无法重新下载。请手动重新下载安装。", script))
	}

	printStep(msgf("Running %s to download and install ...", "运行 %s 下载并安装 ...", script))

	var cmd *exec.Cmd
	if runtime.GOOS == "windows" {
		cmd = exec.Command("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", scriptPath)
	} else {
		os.Chmod(scriptPath, 0755)
		cmd = exec.Command("bash", scriptPath)
	}
	cmd.Dir = workDir
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

func handleReinstall(workDir string, args []string) error {
	skipConfirm := false
	for _, a := range args {
		if a == "-y" || a == "--yes" {
			skipConfirm = true
		}
	}

	scriptName := installScriptName()
	scriptPath := filepath.Join(workDir, scriptName)
	if _, err := os.Stat(scriptPath); os.IsNotExist(err) {
		return fmt.Errorf(msgf(
			"install script %s not found in %s. Cannot reinstall without it.",
			"安装脚本 %s 不在 %s 中, 无法执行重装。", scriptName, workDir))
	}

	printDivider()
	printStep(msg("Full Reinstall", "完全重装"))
	printDivider()
	fmt.Println()

	printStep(msg("This will:", "本操作将:"))
	printStep(msg(
		"  1. Uninstall the daemon service",
		"  1. 卸载守护进程服务"))
	printStep(msg(
		"  2. Delete all files EXCEPT identity and install script",
		"  2. 删除除身份文件和安装脚本外的所有内容"))
	printStep(msgf(
		"  3. Run %s to re-download and install from server",
		"  3. 运行 %s 从服务端重新下载并安装", scriptName))
	fmt.Println()
	printStep(msg("Files preserved:", "保留的文件:"))
	for name := range preserveSet() {
		printStep("    " + name)
	}
	fmt.Println()

	if !skipConfirm {
		fmt.Print(msg("Continue? (y/N): ", "是否继续? (y/N): "))
		var answer string
		fmt.Scanln(&answer)
		answer = strings.TrimSpace(strings.ToLower(answer))
		if answer != "y" && answer != "yes" {
			printStep(msg("Cancelled", "已取消"))
			return nil
		}
	}

	keep := preserveSet()

	// Step 1: stop (don't uninstall — preserve session config / .install_type on Windows;
	// the install script's "devopsAgent install" will re-register the service)
	printStep(msg("Step 1: stopping agent ...", "步骤 1: 停止 Agent ..."))
	_ = handleStop(workDir)

	// Step 2: clean
	printStep(msg("Step 2: cleaning files ...", "步骤 2: 清理文件 ..."))
	entries, err := os.ReadDir(workDir)
	if err != nil {
		return fmt.Errorf("read workdir: %w", err)
	}
	for _, entry := range entries {
		name := entry.Name()
		if keep[name] {
			continue
		}
		path := filepath.Join(workDir, name)
		if err := os.RemoveAll(path); err != nil {
			printWarn(msgf("  failed to remove %s: %v", "  删除 %s 失败: %v", name, err))
		} else {
			printStep(msgf("  deleted: %s", "  已删除: %s", name))
		}
	}

	// Step 3: re-download and install via script
	printStep(msgf("Step 3: re-downloading via %s ...", "步骤 3: 通过 %s 重新下载 ...", scriptName))
	if err := runInstallScript(workDir); err != nil {
		return fmt.Errorf(msgf(
			"install script failed: %v",
			"安装脚本执行失败: %v", err))
	}

	fmt.Println()
	printDivider()
	printStep(msg("Reinstall complete", "重装完成"))
	printDivider()
	return nil
}

// ── Repair ───────────────────────────────────────────────────────────────

func handleRepair(workDir string) error {
	printDivider()
	printStep(msg("Repairing agent files", "修复 Agent 文件"))
	printDivider()

	printStep(msg("Step 1: stopping agent ...", "步骤 1: 停止 Agent ..."))
	_ = handleStop(workDir)

	printStep(msg("Step 2: re-extracting dependencies ...", "步骤 2: 重新解压依赖 ..."))
	repairWorkDir(workDir)

	printStep(msg("Step 3: restarting agent ...", "步骤 3: 重启 Agent ..."))
	if err := handleStart(workDir); err != nil {
		return err
	}

	printStep(msg("Repair complete", "修复完成"))
	return nil
}
