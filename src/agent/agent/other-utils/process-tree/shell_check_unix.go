//go:build linux || darwin

package main

import (
	"bufio"
	"bytes"
	"errors"
	"flag"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"sort"
	"strings"
	"syscall"
	"time"
)

const defaultShellCheckTimeout = 8 * time.Second

type shellCheckOptions struct {
	ShellPath string
	WorkDir   string
	Timeout   time.Duration
	KeepFiles bool
	Verbose   bool
}

type shellCheckFinding struct {
	Path    string
	Line    int
	Reason  string
	Content string
}

type shellCheckResult struct {
	RequestedShell   string
	ResolvedShell    string
	WorkDir          string
	TempDir          string
	PrepareScript    string
	StartScript      string
	ProbeLogFile     string
	MarkerFile       string
	ExecLine         string
	Stdout           string
	Stderr           string
	ProbeLog         string
	Findings         []shellCheckFinding
	MarkerCreated    bool
	TimedOut         bool
	ExitCode         int
	RunErr           error
	Status           string
	Summary          string
	Recommendation   string
	ScannedInitFiles []string
}

type suspiciousRule struct {
	pattern *regexp.Regexp
	reason  string
}

var suspiciousRules = []suspiciousRule{
	{pattern: regexp.MustCompile(`^\s*exec\b`), reason: "启动文件中包含 exec，可能替换掉真正要执行的脚本"},
	{pattern: regexp.MustCompile(`^\s*(exit|logout)\b`), reason: "启动文件中包含 exit/logout，可能让 shell 提前结束"},
	{pattern: regexp.MustCompile(`^\s*read\b`), reason: "启动文件中包含 read，非交互场景可能等待输入或直接失败"},
	{pattern: regexp.MustCompile(`^\s*stty\b`), reason: "启动文件中包含 stty，非 TTY 环境可能报错或中断启动"},
	{pattern: regexp.MustCompile(`\b(tmux|screen|zellij)\b`), reason: "启动文件中可能切到交互会话管理器，影响非交互启动"},
}

func runShellCheckCommand(args []string) int {
	fs := flag.NewFlagSet("shell-check", flag.ContinueOnError)
	fs.SetOutput(os.Stdout)

	shellPath := fs.String("shell", os.Getenv("SHELL"), "要检测的 shell 路径，默认取 $SHELL，为空时回退到 /bin/bash")
	workDir := fs.String("workdir", "", "执行探测脚本时使用的工作目录，默认当前目录")
	timeout := fs.Duration("timeout", defaultShellCheckTimeout, "探测超时时间，例如 8s、15s")
	keepFiles := fs.Bool("keep", false, "保留临时脚本和探测结果文件，便于二次排查")
	verbose := fs.Bool("verbose", false, "输出脚本内容和完整 stdout/stderr")
	showHelp := fs.Bool("h", false, "显示帮助")
	showHelpLong := fs.Bool("help", false, "显示帮助")
	fs.Usage = func() { printShellCheckUsage(fs) }

	if err := fs.Parse(args); err != nil {
		return 2
	}
	if *showHelp || *showHelpLong {
		printShellCheckUsage(fs)
		return 0
	}

	result, err := diagnoseShellStartup(shellCheckOptions{
		ShellPath: *shellPath,
		WorkDir:   *workDir,
		Timeout:   *timeout,
		KeepFiles: *keepFiles,
		Verbose:   *verbose,
	})
	if err != nil {
		fmt.Fprintf(os.Stderr, "shell-check 执行失败: %v\n", err)
		return 1
	}

	printShellCheckResult(result, *verbose)
	if result.Status == "FAIL" {
		return 1
	}
	return 0
}

func printShellCheckUsage(fs *flag.FlagSet) {
	exe := filepath.Base(os.Args[0])
	fmt.Printf("%s shell-check [OPTIONS]\n\n", exe)
	fmt.Println("复现 agent 当前 Unix worker 的启动方式：")
	fmt.Println("  1. 生成真正的 start 脚本")
	fmt.Println("  2. 再生成一层 prepare 脚本")
	fmt.Println("  3. 通过 login shell 执行 prepare -> start")
	fmt.Println("用来检测 rc/profile 中的 exec zsh、exit、read、交互命令等是否会吞掉真正脚本。")
	fmt.Println()
	fmt.Println("OPTIONS:")
	fs.PrintDefaults()
	fmt.Println()
	fmt.Println("EXAMPLES:")
	fmt.Printf("  %s shell-check\n", exe)
	fmt.Printf("  %s shell-check -shell /bin/bash -timeout 10s\n", exe)
	fmt.Printf("  %s shell-check -shell /bin/zsh -verbose -keep\n", exe)
}

func diagnoseShellStartup(opts shellCheckOptions) (*shellCheckResult, error) {
	resolvedShell, err := resolveShellPath(opts.ShellPath)
	if err != nil {
		return nil, err
	}

	workDir := opts.WorkDir
	if strings.TrimSpace(workDir) == "" {
		workDir, err = os.Getwd()
		if err != nil {
			return nil, err
		}
	}
	if opts.Timeout <= 0 {
		opts.Timeout = defaultShellCheckTimeout
	}

	tempDir, err := os.MkdirTemp("", "agent-util-shell-check-")
	if err != nil {
		return nil, err
	}
	if !opts.KeepFiles {
		defer os.RemoveAll(tempDir)
	}

	result := &shellCheckResult{
		RequestedShell: opts.ShellPath,
		ResolvedShell:  resolvedShell,
		WorkDir:        workDir,
		TempDir:        tempDir,
	}

	startScript := filepath.Join(tempDir, "agent_start_probe.sh")
	prepareScript := filepath.Join(tempDir, "agent_prepare_probe.sh")
	probeLogFile := filepath.Join(tempDir, "probe.log")
	markerFile := filepath.Join(tempDir, "probe.ok")

	result.StartScript = startScript
	result.PrepareScript = prepareScript
	result.ProbeLogFile = probeLogFile
	result.MarkerFile = markerFile
	result.ScannedInitFiles = shellInitFiles(resolvedShell)

	startContent := strings.Join(buildStartProbeScript(resolvedShell, probeLogFile, markerFile), "\n") + "\n"
	prepareLines := buildPrepareScriptLines(resolvedShell, startScript)
	prepareContent := strings.Join(prepareLines, "\n") + "\n"
	result.ExecLine = prepareLines[len(prepareLines)-1]

	if err := os.WriteFile(startScript, []byte(startContent), 0o755); err != nil {
		return nil, err
	}
	if err := os.WriteFile(prepareScript, []byte(prepareContent), 0o755); err != nil {
		return nil, err
	}
	if err := os.Chmod(startScript, 0o755); err != nil {
		return nil, err
	}
	if err := os.Chmod(prepareScript, 0o755); err != nil {
		return nil, err
	}

	result.Findings = scanInitFiles(result.ScannedInitFiles)

	stdout, stderr, runErr, timedOut, exitCode := runProbeScript(prepareScript, workDir, opts.Timeout)
	result.Stdout = stdout
	result.Stderr = stderr
	result.RunErr = runErr
	result.TimedOut = timedOut
	result.ExitCode = exitCode

	if _, err := os.Stat(markerFile); err == nil {
		result.MarkerCreated = true
	}
	if data, err := os.ReadFile(probeLogFile); err == nil {
		result.ProbeLog = string(data)
	}

	classifyShellCheckResult(result)
	return result, nil
}

func resolveShellPath(shellPath string) (string, error) {
	shellPath = strings.TrimSpace(shellPath)
	if shellPath == "" {
		shellPath = strings.TrimSpace(os.Getenv("SHELL"))
	}
	if shellPath == "" {
		shellPath = "/bin/bash"
	}
	if filepath.IsAbs(shellPath) {
		if info, err := os.Stat(shellPath); err != nil {
			return "", err
		} else if info.IsDir() {
			return "", fmt.Errorf("shell 路径指向目录: %s", shellPath)
		}
		return shellPath, nil
	}
	resolved, err := exec.LookPath(shellPath)
	if err != nil {
		return "", err
	}
	return resolved, nil
}

func buildStartProbeScript(shellPath, probeLogFile, markerFile string) []string {
	logPath := shellQuote(probeLogFile)
	markerPath := shellQuote(markerFile)
	return []string{
		"#!" + shellPath,
		"echo probe_script_reached > " + logPath,
		"echo shell_argv0:$0 >> " + logPath,
		"pwd >> " + logPath,
		"touch " + markerPath,
		"exit 0",
	}
}

func buildPrepareScriptLines(shellPath, startScript string) []string {
	quotedStartScript := shellQuote(startScript)
	if isTcshLike(shellPath) {
		return []string{
			"#!" + shellPath,
			"exec " + shellPath + " " + quotedStartScript + " -l",
		}
	}
	return []string{
		"#!" + shellPath,
		"exec " + shellPath + " -l " + quotedStartScript,
	}
}

func isTcshLike(shellPath string) bool {
	base := strings.ToLower(filepath.Base(shellPath))
	return base == "tcsh" || base == "csh"
}

func runProbeScript(prepareScript, workDir string, timeout time.Duration) (string, string, error, bool, int) {
	devNull, err := os.Open(os.DevNull)
	if err != nil {
		return "", "", err, false, -1
	}
	defer devNull.Close()

	cmd := exec.Command(prepareScript)
	cmd.Dir = workDir
	cmd.Env = os.Environ()
	cmd.Stdin = devNull
	cmd.SysProcAttr = &syscall.SysProcAttr{Setpgid: true}

	var stdout, stderr bytes.Buffer
	cmd.Stdout = &stdout
	cmd.Stderr = &stderr

	if err := cmd.Start(); err != nil {
		return stdout.String(), stderr.String(), err, false, -1
	}

	waitCh := make(chan error, 1)
	go func() {
		waitCh <- cmd.Wait()
	}()

	select {
	case err := <-waitCh:
		return stdout.String(), stderr.String(), err, false, exitCodeOf(err)
	case <-time.After(timeout):
		if cmd.Process != nil {
			_ = syscall.Kill(-cmd.Process.Pid, syscall.SIGKILL)
		}
		err := <-waitCh
		if err == nil {
			err = fmt.Errorf("shell probe timeout after %s", timeout)
		}
		return stdout.String(), stderr.String(), err, true, exitCodeOf(err)
	}
}

func exitCodeOf(err error) int {
	if err == nil {
		return 0
	}
	var exitErr *exec.ExitError
	if errors.As(err, &exitErr) {
		return exitErr.ExitCode()
	}
	return -1
}

func shellInitFiles(shellPath string) []string {
	home, _ := os.UserHomeDir()
	base := strings.ToLower(filepath.Base(shellPath))
	files := []string{}
	appendIf := func(paths ...string) {
		for _, path := range paths {
			if strings.TrimSpace(path) == "" {
				continue
			}
			files = append(files, path)
		}
	}

	switch base {
	case "bash":
		appendIf("/etc/profile", "/etc/bash.bashrc", filepath.Join(home, ".bash_profile"), filepath.Join(home, ".bash_login"), filepath.Join(home, ".profile"), filepath.Join(home, ".bashrc"))
	case "zsh":
		appendIf("/etc/zprofile", "/etc/zsh/zprofile", "/etc/zlogin", "/etc/zsh/zlogin", filepath.Join(home, ".zprofile"), filepath.Join(home, ".zlogin"), filepath.Join(home, ".zshrc"))
	case "tcsh", "csh":
		appendIf("/etc/csh.cshrc", "/etc/csh.login", filepath.Join(home, ".tcshrc"), filepath.Join(home, ".cshrc"), filepath.Join(home, ".login"))
	default:
		appendIf("/etc/profile", filepath.Join(home, ".profile"))
	}

	seen := make(map[string]struct{}, len(files))
	uniq := make([]string, 0, len(files))
	for _, file := range files {
		if _, ok := seen[file]; ok {
			continue
		}
		seen[file] = struct{}{}
		uniq = append(uniq, file)
	}
	return uniq
}

func scanInitFiles(paths []string) []shellCheckFinding {
	findings := make([]shellCheckFinding, 0)
	for _, path := range paths {
		file, err := os.Open(path)
		if err != nil {
			continue
		}
		scanner := bufio.NewScanner(file)
		lineNo := 0
		for scanner.Scan() {
			lineNo++
			text := strings.TrimSpace(scanner.Text())
			if text == "" || strings.HasPrefix(text, "#") {
				continue
			}
			for _, rule := range suspiciousRules {
				if rule.pattern.MatchString(text) {
					findings = append(findings, shellCheckFinding{
						Path:    path,
						Line:    lineNo,
						Reason:  rule.reason,
						Content: text,
					})
					break
				}
			}
		}
		_ = file.Close()
	}
	sort.Slice(findings, func(i, j int) bool {
		if findings[i].Path == findings[j].Path {
			return findings[i].Line < findings[j].Line
		}
		return findings[i].Path < findings[j].Path
	})
	return findings
}

func classifyShellCheckResult(result *shellCheckResult) {
	switch {
	case result.TimedOut:
		result.Status = "FAIL"
		result.Summary = "login shell 启动超时，通常说明启动文件执行了交互命令或长期阻塞命令"
		result.Recommendation = "检查 profile/rc 中是否有 read、tmux、screen、长驻命令；把交互逻辑包在仅交互 shell 才执行的判断里"
	case result.MarkerCreated && result.ExitCode == 0:
		if len(result.Findings) > 0 {
			result.Status = "WARN"
			result.Summary = "本次探测成功执行到了目标脚本，但初始化文件里存在可疑语句，后续仍可能受环境差异影响"
			result.Recommendation = "重点检查可疑 rc/profile 语句，尤其是 exec/exit；如无必要，避免在 login shell 初始化阶段切换 shell"
		} else {
			result.Status = "PASS"
			result.Summary = "探测成功：两层脚本 + login shell 能正常执行到目标脚本"
			result.Recommendation = "当前 shell 启动链路没有复现问题；若线上仍偶发失败，可用 -keep -verbose 保留现场进一步比对"
		}
	case result.MarkerCreated:
		result.Status = "WARN"
		result.Summary = "目标脚本已经执行到，但 shell 最终以非 0 状态退出"
		result.Recommendation = "检查 shell 启动文件是否在脚本执行后又触发了 exit/错误返回；同时查看 stdout/stderr 细节"
	default:
		result.Status = "FAIL"
		if result.ExitCode == 0 {
			result.Summary = "shell 进程已退出，但目标脚本没有真正执行；很像被 rc/profile 中的 exec 切走了"
		} else {
			result.Summary = "目标脚本未执行成功；login shell 在进入真实脚本前就失败了"
		}
		result.Recommendation = "重点排查 rc/profile 中的 exec zsh、exit、logout、read 等语句，并优先把交互逻辑放进交互 shell 判断分支"
	}
}

func printShellCheckResult(result *shellCheckResult, verbose bool) {
	fmt.Println("========================================")
	fmt.Println("  Shell Startup Check")
	fmt.Println("========================================")
	fmt.Printf("状态: %s\n", result.Status)
	fmt.Printf("结论: %s\n", result.Summary)
	fmt.Println()
	fmt.Printf("请求 shell: %s\n", blankAs(result.RequestedShell, "<empty, fallback to default>"))
	fmt.Printf("解析 shell: %s\n", result.ResolvedShell)
	fmt.Printf("工作目录: %s\n", result.WorkDir)
	fmt.Printf("临时目录: %s\n", result.TempDir)
	fmt.Printf("prepare exec: %s\n", result.ExecLine)
	fmt.Printf("退出码: %d\n", result.ExitCode)
	fmt.Printf("是否超时: %t\n", result.TimedOut)
	fmt.Printf("是否执行到目标脚本: %t\n", result.MarkerCreated)
	fmt.Println()

	fmt.Println("扫描的初始化文件:")
	for _, path := range result.ScannedInitFiles {
		fmt.Printf("  - %s\n", path)
	}
	fmt.Println()

	if len(result.Findings) > 0 {
		fmt.Println("发现可疑初始化语句:")
		for _, finding := range result.Findings {
			fmt.Printf("  - %s:%d\n    原因: %s\n    内容: %s\n", finding.Path, finding.Line, finding.Reason, finding.Content)
		}
		fmt.Println()
	} else {
		fmt.Println("未在常见初始化文件里发现明显的高风险语句。")
		fmt.Println()
	}

	if strings.TrimSpace(result.ProbeLog) != "" {
		fmt.Println("目标脚本探测日志:")
		for _, line := range strings.Split(strings.TrimSpace(result.ProbeLog), "\n") {
			fmt.Printf("  %s\n", line)
		}
		fmt.Println()
	}

	if verbose {
		fmt.Println("stdout:")
		fmt.Println(indentMultiline(blankAs(strings.TrimSpace(result.Stdout), "<empty>"), "  "))
		fmt.Println()
		fmt.Println("stderr:")
		fmt.Println(indentMultiline(blankAs(strings.TrimSpace(result.Stderr), "<empty>"), "  "))
		fmt.Println()
		fmt.Printf("start script: %s\n", result.StartScript)
		fmt.Printf("prepare script: %s\n", result.PrepareScript)
		fmt.Printf("probe log file: %s\n", result.ProbeLogFile)
		fmt.Printf("marker file: %s\n", result.MarkerFile)
		fmt.Println()
	}

	fmt.Printf("建议: %s\n", result.Recommendation)
}

func shellQuote(value string) string {
	return "'" + strings.ReplaceAll(value, "'", `'"'"'`) + "'"
}

func blankAs(value, fallback string) string {
	if strings.TrimSpace(value) == "" {
		return fallback
	}
	return value
}

func indentMultiline(value, prefix string) string {
	lines := strings.Split(value, "\n")
	for i, line := range lines {
		lines[i] = prefix + line
	}
	return strings.Join(lines, "\n")
}
