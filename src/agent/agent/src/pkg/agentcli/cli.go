package agentcli

import (
	"flag"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"gopkg.in/ini.v1"
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
		err = handleVersion(args[1:])
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
	case "install", "uninstall", "start", "stop", "repair", "reinstall", "status",
		"version", "debug",
		"-h", "--help", "help":
		return true
	}
	return false
}

func handleVersion(args []string) error {
	fs := flag.NewFlagSet("version", flag.ContinueOnError)
	full := fs.Bool("f", false, "")
	if err := fs.Parse(args); err != nil {
		return err
	}
	fmt.Println(config.AgentVersion)
	if *full {
		fmt.Println(config.GitCommit)
		fmt.Println(config.BuildTime)
	}
	return nil
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
func cliErrorf(en, zh string, a ...interface{}) error {
	return fmt.Errorf(msgf(en, zh, a...))
}

func printDivider() {
	printStep("============================================")
}

type statusSummaryState struct {
	hasIssue bool
}

var currentStatusSummary *statusSummaryState

func beginStatusSummary() {
	currentStatusSummary = &statusSummaryState{}
}

func trackStatusLine(label, value string) {
	if currentStatusSummary == nil {
		return
	}
	if statusValueHasIssue(label, value) {
		currentStatusSummary.hasIssue = true
	}
}

func printStatusSummaryLine() {
	if currentStatusSummary == nil {
		return
	}
	summary := msg("Normal ✓", "正常 ✓")
	if currentStatusSummary.hasIssue {
		summary = msg("Abnormal ✗", "异常 ✗")
	}
	fmt.Printf("  %-24s %s\n", msg("Summary", "汇总")+":", summary)
}

func statusValueHasIssue(label, value string) bool {
	lowerValue := strings.ToLower(value)
	if strings.Contains(value, "✗") || strings.Contains(value, "⚠") || strings.Contains(lowerValue, "fail:") {
		return true
	}

	lowerLabel := strings.ToLower(label)
	isPidLabel := strings.Contains(lowerLabel, "daemon pid") || strings.Contains(lowerLabel, "agent pid") ||
		strings.Contains(label, "守护进程 PID") || strings.Contains(label, "Agent PID")
	if isPidLabel {
		return strings.Contains(lowerValue, "not running") || strings.Contains(value, "未运行") || strings.Contains(value, "已退出")
	}

	return false
}

func configureSessionSummaryLines(user string, autoLogon bool) []string {
	if autoLogon {
		return []string{
			msg("The agent is active in your current session NOW.", "Agent 已在当前桌面会话中生效。"),
			msgf("On future reboots Windows auto-logs in as %s.", "后续重启后 Windows 会自动登录为 %s。", user),
			msg("If the password changes, re-run with the new password.", "如果密码变更，请使用新密码重新执行命令。"),
		}
	}
	return []string{
		msg("The agent is active in your current session NOW.", "Agent 已在当前桌面会话中生效。"),
		msg("When no user is logged in, agent waits until a user logs in.", "当无人登录时，agent 会等待用户登录后再启动。"),
		msg("To auto-logon on reboot, add --auto-logon USER PASS.",
			"如需在重启/注销后自动登录，请添加 --auto-logon 用户名 密码。"),
	}
}

// ── Property / config helpers ────────────────────────────────────────────

func readProperty(workDir, key string) (string, error) {
	data, err := os.ReadFile(filepath.Join(workDir, ".agent.properties"))
	if err != nil {
		return "", cliErrorf("cannot read .agent.properties: %v", "无法读取 .agent.properties: %v", err)
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
	return "", cliErrorf("key %s not found in .agent.properties", "未在 .agent.properties 中找到键 %s", key)
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

// ── Properties file validation ───────────────────────────────────────────

// checkPropertiesFile validates .agent.properties, replicating the checks
// performed by the agent process in config.LoadAgentConfig().
func checkPropertiesFile(workDir string) {
	fmt.Println()
	printStep(msg("Configuration (.agent.properties)", "配置文件 (.agent.properties)"))
	printStep("--------------------------------------------")

	propPath := filepath.Join(workDir, ".agent.properties")
	status, conf := parsePropertiesFile(propPath)
	statusLine(msg("  File", "  文件"), status)
	if conf == nil {
		return
	}

	type rk struct {
		key, label string
		mask       bool
	}
	for _, k := range []rk{
		{"devops.project.id", "Project ID", false},
		{"devops.agent.id", "Agent ID", false},
		{"devops.agent.secret.key", "Secret Key", true},
		{"landun.gateway", "Gateway", false},
		{"landun.env", "Env Type", false},
	} {
		statusLine("  "+k.label, requiredKeyStatus(conf, k.key, k.mask))
	}

	statusLine(
		msg("  Parallel tasks", "  并行任务数"),
		intKeyStatus(conf, "devops.parallel.task.count", 0),
	)
}

// parsePropertiesFile checks existence and parses as INI (matching agent's ini.Load behavior).
func parsePropertiesFile(path string) (string, *ini.File) {
	info, err := os.Stat(path)
	if err != nil {
		return msg("MISSING", "缺失") + " ✗", nil
	}
	if info.IsDir() {
		return msg("ERROR: is a directory", "错误: 是目录") + " ✗", nil
	}
	conf, err := ini.Load(path)
	if err != nil {
		return fmt.Sprintf("%s: %v ✗", msg("PARSE ERROR", "解析失败"), err), nil
	}
	return fmt.Sprintf("%s ✓ (%d bytes)", msg("OK", "正常"), info.Size()), conf
}

// requiredKeyStatus checks that a config key is present and non-empty.
func requiredKeyStatus(conf *ini.File, key string, mask bool) string {
	val := strings.TrimSpace(conf.Section("").Key(key).String())
	if val == "" {
		return msg("missing or empty", "缺失或为空") + " ✗"
	}
	if mask {
		return msg("configured", "已配置") + " ✓"
	}
	return val + " ✓"
}

// intKeyStatus checks that a config key is a valid integer >= minVal.
func intKeyStatus(conf *ini.File, key string, minVal int) string {
	raw := conf.Section("").Key(key).String()
	if raw == "" {
		return msg("missing", "缺失") + " ✗"
	}
	v, err := conf.Section("").Key(key).Int()
	if err != nil {
		return fmt.Sprintf("%s: %q ✗", msg("invalid number", "无效数值"), raw)
	}
	if v < minVal {
		return fmt.Sprintf("%s: %d < %d ✗", msg("too small", "值过小"), v, minVal)
	}
	return fmt.Sprintf("%d ✓", v)
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
		"  1. Download agent.zip from server (verify before any changes)",
		"  1. 从服务端下载 agent.zip (先验证, 再变更)"))
	printStep(msg(
		"  2. Stop agent and clean files",
		"  2. 停止 Agent 并清理文件"))
	printStep(msg(
		"  3. Extract and install",
		"  3. 解压并安装"))
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

	// Step 1: download FIRST — verify before making any destructive changes
	// Check heartbeat before download (pid file still exists at this point)
	waitForHeartbeatExpiry(workDir)

	printStep(msg("Step 1: downloading agent.zip from server ...", "步骤 1: 从服务端下载 agent.zip ..."))
	zipPath := filepath.Join(workDir, "agent.zip")
	if err := downloadAgentZip(workDir, gateway, agentId, zipPath); err != nil {
		return err
	}

	// Step 2: stop + clean (only after download succeeded)
	printStep(msg("Step 2: stopping agent ...", "步骤 2: 停止 Agent ..."))
	_ = handleStop(workDir)

	printStep(msg("Step 3: cleaning files ...", "步骤 3: 清理文件 ..."))
	entries, err := os.ReadDir(workDir)
	if err != nil {
		return cliErrorf("read workdir failed: %v", "读取工作目录失败: %v", err)
	}
	for _, entry := range entries {
		name := entry.Name()
		if keep[name] || name == "agent.zip" {
			continue
		}
		path := filepath.Join(workDir, name)
		if err := os.RemoveAll(path); err != nil {
			printWarn(msgf("  failed to remove %s: %v", "  删除 %s 失败: %v", name, err))
		} else {
			printStep(msgf("  deleted: %s", "  已删除: %s", name))
		}
	}

	// Step 4: extract
	printStep(msg("Step 4: extracting agent.zip ...", "步骤 4: 解压 agent.zip ..."))
	if runtime.GOOS == "windows" {
		agentExe := filepath.Join(workDir, agentBinary())
		agentBak := agentExe + ".bak"
		os.Remove(agentBak)
		if err := os.Rename(agentExe, agentBak); err == nil {
			printStep(msg(
				"Renamed running binary to avoid file lock conflict",
				"已重命名运行中的二进制以避免文件锁冲突"))
		}
	}
	if err := unzipFile(zipPath, workDir); err != nil {
		return fmt.Errorf(msgf("extract agent.zip failed: %v", "解压 agent.zip 失败: %v", err))
	}
	if runtime.GOOS == "windows" {
		os.Remove(filepath.Join(workDir, agentBinary()+".bak"))
	}

	printStep(msg("Step 5: preparing work directory ...", "步骤 5: 准备工作目录 ..."))
	prepareWorkDir(workDir)
	if runtime.GOOS != "windows" {
		os.Chmod(filepath.Join(workDir, "devopsAgent"), 0755)
		os.Chmod(filepath.Join(workDir, "devopsDaemon"), 0755)
	}

	printStep(msg("Step 6: installing and starting service ...", "步骤 6: 安装并启动服务 ..."))
	mode := strings.ToLower(readInstallMode(workDir))
	if err := handleInstall(workDir, []string{"--mode", mode}); err != nil {
		return fmt.Errorf(msgf(
			"reinstall failed at install step: %v",
			"重装在安装步骤失败: %v", err))
	}

	fmt.Println()
	printDivider()
	printStep(msg("Reinstall complete", "重装完成"))
	printDivider()
	return nil
}

// waitForHeartbeatExpiry checks if the agent process was recently running.
// The backend blocks re-install for ~50s after the last heartbeat. If the agent PID
// file indicates a recently-alive process, we wait for the heartbeat to expire.
func waitForHeartbeatExpiry(workDir string) {
	pidFile := filepath.Join(workDir, "runtime", "agent.pid")
	data, err := os.ReadFile(pidFile)
	if err != nil {
		return
	}
	pid, err := strconv.Atoi(strings.TrimSpace(string(data)))
	if err != nil || pid <= 0 {
		return
	}

	// Check if the agent PID file was recently active (written within the last 60s),
	// which means the agent was sending heartbeats shortly before we stopped it.
	info, err := os.Stat(pidFile)
	if err != nil || time.Since(info.ModTime()) > 60*time.Second {
		return
	}

	const waitSec = 55
	printStep(msgf(
		"Agent was recently running (PID %d). Waiting %ds for backend heartbeat to expire ...",
		"Agent 近期有运行记录 (PID %d)。等待 %d 秒让后台心跳过期 ...", pid, waitSec))
	for i := waitSec; i > 0; i-- {
		fmt.Printf("\r[BK-CI]   %s  ", msgf("%ds remaining ...", "剩余 %d 秒 ...", i))
		time.Sleep(time.Second)
	}
	fmt.Println()
}

// agentArch returns the arch query parameter for the backend download API.
// The backend recognizes "arm64" and "mips64"; anything else (amd64, 386, loong64)
// is treated as the default x86_64 package.
func agentArch() string {
	switch runtime.GOARCH {
	case "arm64":
		return "arm64"
	case "mips64", "mips64le":
		return "mips64"
	default:
		return ""
	}
}

// downloadAgentZip downloads agent.zip from the BK-CI server using credentials from .agent.properties.
func downloadAgentZip(workDir, gateway, agentId, savePath string) error {
	if !strings.HasPrefix(gateway, "http") {
		gateway = "http://" + gateway
	}
	url := gateway + "/external/agents/" + agentId + "/agent"
	if arch := agentArch(); arch != "" {
		url += "?arch=" + arch
	}

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

	httpProxy, _ := readProperty(workDir, "HTTP_PROXY")
	httpsProxy, _ := readProperty(workDir, "HTTPS_PROXY")
	noProxy, _ := readProperty(workDir, "NO_PROXY")
	certPath := filepath.Join(workDir, ".cert")
	tlsConfig := loadCertIfExists(certPath)
	proxyFunc := buildProxyFunc(httpProxy, httpsProxy, noProxy)
	httpClient := &http.Client{
		Transport: &http.Transport{
			TLSClientConfig:       tlsConfig,
			Proxy:                 proxyFunc,
			ResponseHeaderTimeout: 60 * time.Second,
		},
	}

	resp, err := httpClient.Do(req)
	if err != nil {
		return fmt.Errorf(msgf("download failed: %v", "下载失败: %v", err))
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 400 {
		body, _ := io.ReadAll(io.LimitReader(resp.Body, 4096))
		return fmt.Errorf(msgf(
			"server returned HTTP %d: %s",
			"服务端返回 HTTP %d: %s", resp.StatusCode, strings.TrimSpace(string(body))))
	}

	tmpPath := savePath + ".tmp"
	f, err := os.Create(tmpPath)
	if err != nil {
		return fmt.Errorf(msgf("create temp file failed: %v", "创建临时文件失败: %v", err))
	}
	defer func() {
		f.Close()
		os.Remove(tmpPath)
	}()

	written, err := io.Copy(f, resp.Body)
	if err != nil {
		return fmt.Errorf(msgf("download interrupted: %v", "下载中断: %v", err))
	}
	f.Close()

	// Validate zip magic bytes (PK\x03\x04)
	zf, err := os.Open(tmpPath)
	if err != nil {
		return fmt.Errorf(msgf("open temp file failed: %v", "打开临时文件失败: %v", err))
	}
	var magic [4]byte
	_, err = io.ReadFull(zf, magic[:])
	zf.Close()
	if err != nil || magic[0] != 'P' || magic[1] != 'K' || magic[2] != 3 || magic[3] != 4 {
		snippet, _ := os.ReadFile(tmpPath)
		if len(snippet) > 500 {
			snippet = snippet[:500]
		}
		return fmt.Errorf(msgf(
			"server returned HTTP %d but response is not a valid zip file.\nResponse body: %s",
			"服务端返回 HTTP %d 但响应不是有效的 zip 文件。\n响应内容: %s",
			resp.StatusCode, strings.TrimSpace(string(snippet))))
	}

	if err := os.Rename(tmpPath, savePath); err != nil {
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
