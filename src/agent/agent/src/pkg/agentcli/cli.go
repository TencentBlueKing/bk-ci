package agentcli

import (
	"fmt"
	"io"
	"net/http"
	"os"
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
		".debug":            true,
		"workspace":         true,
	}
	m[agentBinary()] = true
	return m
}

func handleReinstall(workDir string, args []string) error {
	skipConfirm := false
	for _, a := range args {
		if a == "-y" || a == "--yes" {
			skipConfirm = true
		}
	}

	gateway, err := readProperty(workDir, "landun.gateway")
	if err != nil {
		return fmt.Errorf(msgf(
			"cannot read gateway from .agent.properties: %v",
			"无法从 .agent.properties 读取网关地址: %v", err))
	}
	agentId, err := readProperty(workDir, "devops.agent.id")
	if err != nil {
		return fmt.Errorf(msgf(
			"cannot read agent ID from .agent.properties: %v",
			"无法从 .agent.properties 读取 Agent ID: %v", err))
	}

	printDivider()
	printStep(msg("Full Reinstall", "完全重装"))
	printDivider()
	fmt.Println()

	printStep(msg("This will:", "本操作将:"))
	printStep(msg(
		"  1. Stop agent",
		"  1. 停止 Agent"))
	printStep(msg(
		"  2. Delete all files EXCEPT identity files",
		"  2. 删除除身份文件外的所有内容"))
	printStep(msg(
		"  3. Download agent.zip from server and install",
		"  3. 从服务端下载 agent.zip 并安装"))
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

	printStep(msg("Step 1: stopping agent ...", "步骤 1: 停止 Agent ..."))
	_ = handleStop(workDir)

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

	printStep(msg("Step 3: downloading agent.zip from server ...", "步骤 3: 从服务端下载 agent.zip ..."))
	zipPath := filepath.Join(workDir, "agent.zip")
	if err := downloadAgentZip(workDir, gateway, agentId, zipPath); err != nil {
		return err
	}

	printStep(msg("Step 4: extracting agent.zip ...", "步骤 4: 解压 agent.zip ..."))
	if runtime.GOOS == "windows" {
		printStep(msg(
			"NOTE: devopsAgent.exe is locked while running, skip overwrite is expected.",
			"提示: devopsAgent.exe 运行中被锁定, 解压时跳过覆盖属正常现象。"))
	}
	if err := unzipFile(zipPath, workDir); err != nil {
		return fmt.Errorf(msgf("extract agent.zip failed: %v", "解压 agent.zip 失败: %v", err))
	}

	printStep(msg("Step 5: preparing work directory ...", "步骤 5: 准备工作目录 ..."))
	prepareWorkDir(workDir)
	if runtime.GOOS != "windows" {
		os.Chmod(filepath.Join(workDir, "devopsAgent"), 0755)
		os.Chmod(filepath.Join(workDir, "devopsDaemon"), 0755)
	}

	printStep(msg("Step 6: installing service ...", "步骤 6: 安装服务 ..."))
	if err := handleInstall(workDir, []string{}); err != nil {
		return err
	}

	fmt.Println()
	printDivider()
	printStep(msg("Reinstall complete", "重装完成"))
	printDivider()
	return nil
}

// downloadAgentZip downloads agent.zip from the BK-CI server using credentials from .agent.properties.
func downloadAgentZip(workDir, gateway, agentId, savePath string) error {
	if !strings.HasPrefix(gateway, "http") {
		gateway = "http://" + gateway
	}
	url := gateway + "/external/agents/" + agentId + "/install"

	projectId, _ := readProperty(workDir, "devops.project.id")
	secretKey, _ := readProperty(workDir, "devops.agent.secret.key")

	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return fmt.Errorf(msgf("create request failed: %v", "创建请求失败: %v", err))
	}
	req.Header.Set("X-DEVOPS-PROJECT-ID", projectId)
	req.Header.Set("X-DEVOPS-AGENT-ID", agentId)
	req.Header.Set("X-DEVOPS-AGENT-SECRET-KEY", secretKey)

	printStep(msgf("  GET %s", "  GET %s", url))

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return fmt.Errorf(msgf("download failed: %v", "下载失败: %v", err))
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 400 {
		body, _ := io.ReadAll(resp.Body)
		return fmt.Errorf(msgf(
			"server returned HTTP %d: %s",
			"服务端返回 HTTP %d: %s", resp.StatusCode, strings.TrimSpace(string(body))))
	}

	out, err := os.Create(savePath)
	if err != nil {
		return fmt.Errorf(msgf("create file failed: %v", "创建文件失败: %v", err))
	}
	defer out.Close()

	written, err := io.Copy(out, resp.Body)
	if err != nil {
		return fmt.Errorf(msgf("save file failed: %v", "保存文件失败: %v", err))
	}

	printStep(msgf("  downloaded %.1f MB", "  已下载 %.1f MB", float64(written)/1024/1024))
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
