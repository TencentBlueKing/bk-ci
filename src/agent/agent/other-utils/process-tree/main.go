//go:build windows

package main

import (
	"bufio"
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"sync"
	"syscall"
	"time"
	"unsafe"

	"github.com/yusufpapurcu/wmi"
	"golang.org/x/sys/windows"
)

// Win32_Process WMI struct
type Win32Process struct {
	ProcessId       uint32
	ParentProcessId uint32
	Name            string
	ExecutablePath  *string
	CommandLine     *string
	CreationDate    *string
	HandleCount     uint32
	ThreadCount     uint32
}

// Win32_Thread WMI struct
type Win32Thread struct {
	ProcessHandle    string
	Handle           string
	ThreadState      uint32
	ThreadWaitReason uint32
}

// PipeHolder represents a process holding a pipe handle related to the target
type PipeHolder struct {
	PID         uint32
	ProcessName string
	HandleValue uint16
	ObjectAddr  uintptr
	PipeName    string
	CMD         string
}

// ProcessDiag holds diagnosis info for one process
type ProcessDiag struct {
	Process      Win32Process
	RunningTime  time.Duration
	Threads      []Win32Thread
	WaitingCount int
	PipeHandles  int
	IsBlocked    bool
	BlockReason  string
	JstackOutput string // jstack output for java processes
}

// TargetReport holds all collected data for one target process
type TargetReport struct {
	Target       Win32Process
	ParentChain  []Win32Process
	ChildTree    []*ProcessNode
	DiagResults  []ProcessDiag
	Children     []Win32Process // direct children
	JstackOutput string         // jstack output for java target process
	PipeHolders  []PipeHolder   // processes holding pipe handles related to target
}

// ProcessNode is a tree node for child processes
type ProcessNode struct {
	Process  Win32Process
	Diag     *ProcessDiag
	Children []*ProcessNode
}

// ANSI color codes
const (
	colorReset   = "\033[0m"
	colorRed     = "\033[31m"
	colorGreen   = "\033[32m"
	colorYellow  = "\033[33m"
	colorMagenta = "\033[35m"
	colorCyan    = "\033[36m"
	colorWhite   = "\033[37m"
	colorGray    = "\033[90m"
)

var threadWaitReasonNames = map[uint32]string{
	0: "Executive", 1: "FreePage", 2: "PageIn", 3: "PoolAllocation",
	4: "DelayExecution", 5: "Suspended", 6: "UserRequest", 7: "WrExecutive",
	8: "WrFreePage", 9: "WrPageIn", 10: "WrPoolAllocation", 11: "WrDelayExecution",
	12: "WrSuspended", 13: "WrUserRequest", 14: "WrEventPair", 15: "WrQueue",
	16: "WrLpcReceive", 17: "WrLpcReply", 18: "WrVirtualMemory", 19: "WrPageOut",
	20: "WrRendezvous", 21: "WrKeyedEvent", 22: "WrTerminated", 23: "WrProcessInSwap",
	24: "WrCpuRateControl", 25: "WrCalloutStack", 26: "WrKernel", 27: "WrResource",
	28: "WrPushLock", 29: "WrMutex", 30: "WrQuantumEnd", 31: "WrDispatchInt",
	32: "WrPreempted", 33: "WrYieldExecution", 34: "WrFastMutex", 35: "WrGuardedMutex",
	36: "WrRundown",
}

// Windows API constants
const (
	PROCESS_DUP_HANDLE          = 0x0040
	PROCESS_QUERY_INFORMATION   = 0x0400
	STATUS_INFO_LENGTH_MISMATCH = 0xC0000004
	SystemHandleInformation     = 16
	ObjectNameInformation       = 1
	ObjectTypeInformation       = 2
)

type systemHandleEntryInfo struct {
	UniqueProcessId       uint16
	CreatorBackTraceIndex uint16
	ObjectTypeIndex       uint8
	HandleAttributes      uint8
	HandleValue           uint16
	Object                uintptr
	GrantedAccess         uint32
}

const version = "1.3.0"

func printUsage() {
	fmt.Println("agent-util v" + version)
	fmt.Println()
	fmt.Println("Subcommands:")
	fmt.Println("  tree         Windows 进程树 / 阻塞诊断")
	fmt.Println("  shell-check  Unix login shell 启动链路检测（当前平台仅提示不支持）")
	fmt.Println()
	fmt.Println("USAGE:")
	fmt.Println("  agent-util.exe tree [OPTIONS]")
	fmt.Println("  agent-util.exe shell-check [OPTIONS]")
	fmt.Println("  agent-util.exe [OPTIONS]   # 兼容旧用法，等价于 tree")
	fmt.Println()
	fmt.Println("TREE OPTIONS:")
	fmt.Println("  -buildid <string>   Build ID used to match process command line (required)")
	fmt.Println("  -name <string>      Target process name to search for (default: java.exe)")
	fmt.Println("  -i                  Launch interactive mode (prompts for buildId and options)")
	fmt.Println("  -diag               Enable thread-level blocking diagnosis (slower)")
	fmt.Println("  -pipe               Enable pipe handle scanning per process (slowest)")
	fmt.Println("  -html               Only generate HTML report, skip console output")
	fmt.Println("  -h                  Show this help message and exit")
	fmt.Println("  -v                  Show version and exit")
	fmt.Println()
	fmt.Println("EXAMPLES:")
	fmt.Println("  agent-util.exe tree -buildid b-ec8dfe3da2174a219d04907dd791479e")
	fmt.Println("  agent-util.exe tree -buildid b-ec8dfe3da2174a219d04907dd791479e -diag -pipe")
	fmt.Println("  agent-util.exe tree -buildid b-ec8dfe3da2174a219d04907dd791479e -name node.exe")
	fmt.Println("  agent-util.exe tree -buildid b-ec8dfe3da2174a219d04907dd791479e -html")
	fmt.Println("  agent-util.exe tree -i")
	fmt.Println()
	fmt.Println("NOTE:")
	fmt.Println("  tree 子命令需要管理员权限，并会在需要时自动提权。")
}

func main() {
	enableVirtualTerminal()

	args := os.Args[1:]
	if len(args) > 0 {
		switch args[0] {
		case "tree":
			os.Args = append([]string{os.Args[0]}, args[1:]...)
		case "shell-check":
			fmt.Println("shell-check 子命令仅支持 Linux/macOS 平台")
			return
		}
	}

	buildId := flag.String("buildid", "", "Build ID (used to match process command line)")
	processName := flag.String("name", "java.exe", "Process name (default: java.exe)")
	interactive := flag.Bool("i", false, "Interactive mode")
	diagnose := flag.Bool("diag", false, "Enable thread-level blocking diagnosis (slow, default: false)")
	pipeScan := flag.Bool("pipe", false, "Enable Pipe handle scanning (slowest, default: false)")
	htmlOnly := flag.Bool("html", false, "Only generate HTML report, skip console output")
	showVersion := flag.Bool("v", false, "Show version and exit")
	showHelp := flag.Bool("h", false, "Show help and exit")
	flag.Usage = printUsage
	flag.Parse()

	if *showHelp {
		printUsage()
		return
	}
	if *showVersion {
		fmt.Println("agent-util v" + version)
		return
	}

	if !isAdmin() {
		fmt.Println("Requesting administrator privileges...")
		runAsAdmin()
		return
	}

	if *interactive || *buildId == "" {
		interactiveMode(buildId, processName)
	}

	if *buildId == "" {
		printUsage()
		waitExit()
		return
	}

	queryAndReport(*buildId, *processName, *diagnose, *pipeScan, *htmlOnly)
	waitExit()
}

func interactiveMode(buildId *string, processName *string) {
	reader := bufio.NewReader(os.Stdin)
	fmt.Println("========================================")
	fmt.Println("  agent-util tree diagnosis")
	fmt.Println("========================================")
	fmt.Println()

	if *buildId == "" {
		fmt.Print("Enter BuildId: ")
		input, _ := reader.ReadString('\n')
		input = strings.TrimSpace(input)
		if input == "" {
			printColored(colorRed, "Error: BuildId cannot be empty!")
			waitExit()
			os.Exit(1)
		}
		*buildId = input
	}

	if *processName == "java.exe" {
		fmt.Print("Enter process name (press Enter for default java.exe): ")
		input, _ := reader.ReadString('\n')
		input = strings.TrimSpace(input)
		if input != "" {
			*processName = input
		}
	}
	fmt.Println()
}

func queryAndReport(buildId, processName string, diagnose, pipeScan, htmlOnly bool) {
	startTime := time.Now()
	printColored(colorCyan, fmt.Sprintf("Querying process: Name='%s', CommandLine contains '%s'", processName, buildId))
	if !diagnose {
		printColored(colorGray, "  (thread diagnosis OFF, use -diag to enable)")
	}
	if !pipeScan {
		printColored(colorGray, "  (pipe handle scan OFF, use -pipe to enable)")
	}

	query := fmt.Sprintf("SELECT ProcessId, ParentProcessId, Name, ExecutablePath, CommandLine, CreationDate, HandleCount, ThreadCount FROM Win32_Process WHERE Name='%s'", processName)
	var processes []Win32Process
	if err := wmi.Query(query, &processes); err != nil {
		printColored(colorRed, fmt.Sprintf("WMI query failed: %v", err))
		return
	}

	var targets []Win32Process
	for i := range processes {
		if processes[i].CommandLine != nil && strings.Contains(*processes[i].CommandLine, buildId) {
			targets = append(targets, processes[i])
		}
	}

	if len(targets) == 0 {
		printColored(colorRed, "No matching process found!")
		printColored(colorYellow, "Please check the process name and command line arguments.")
		return
	}

	printColored(colorGreen, fmt.Sprintf("Found %d matching process(es). Collecting data concurrently...", len(targets)))
	fmt.Println()

	// Collect all reports concurrently
	reports := make([]TargetReport, len(targets))
	var wg sync.WaitGroup

	for idx, target := range targets {
		wg.Add(1)
		go func(i int, t Win32Process) {
			defer wg.Done()
			reports[i] = collectReport(t, diagnose, pipeScan)
		}(idx, target)
	}

	wg.Wait()
	elapsed := time.Since(startTime)
	printColored(colorGreen, fmt.Sprintf("Data collection completed in %s", formatDuration(elapsed)))
	fmt.Println()

	// Console summary (brief)
	if !htmlOnly {
		for idx, r := range reports {
			if len(reports) > 1 {
				printColored(colorMagenta, fmt.Sprintf("==================== Process %d/%d ====================", idx+1, len(reports)))
			}
			printConsoleSummary(r, diagnose)
			fmt.Println()
		}
	}

	// Generate HTML report
	htmlPath := generateHTMLReport(reports, buildId, processName, diagnose, elapsed)
	if htmlPath != "" {
		printColored(colorGreen, fmt.Sprintf("HTML report generated: %s", htmlPath))
		printColored(colorCyan, "Opening in browser...")
		openBrowser(htmlPath)
	}
}

// collectReport gathers all data for one target process (parent chain, child tree, diagnostics)
func collectReport(target Win32Process, diagnose, pipeScan bool) TargetReport {
	report := TargetReport{Target: target}

	var wg sync.WaitGroup

	// Collect parent chain
	wg.Add(1)
	go func() {
		defer wg.Done()
		report.ParentChain = getParentChain(target.ProcessId)
	}()

	// Collect child tree (without inline diagnosis — we do it separately to avoid duplication)
	wg.Add(1)
	go func() {
		defer wg.Done()
		report.ChildTree = buildChildTree(target.ProcessId, false, false)
		report.Children = getChildren(target.ProcessId)
	}()

	// Always run jstack for java processes (independent of -diag flag)
	if strings.EqualFold(target.Name, "java.exe") && target.ExecutablePath != nil {
		wg.Add(1)
		go func() {
			defer wg.Done()
			report.JstackOutput = runJstack(*target.ExecutablePath, target.ProcessId)
		}()
	}

	// Always scan for pipe holders (find grandchild processes holding inherited pipe handles)
	wg.Add(1)
	go func() {
		defer wg.Done()
		report.PipeHolders = findPipeHolders(target.ProcessId)
	}()

	wg.Wait()

	// If diagnosis requested, collect all processes from tree and diagnose concurrently (once only)
	if diagnose || pipeScan {
		allProcs := collectProcessTreeFromNodes(target, report.ChildTree)
		diagResults := make([]ProcessDiag, len(allProcs))
		var diagWg sync.WaitGroup
		for i, p := range allProcs {
			diagWg.Add(1)
			go func(idx int, proc Win32Process) {
				defer diagWg.Done()
				diagResults[idx] = diagnoseProcess(proc, diagnose, pipeScan)
			}(i, p)
		}
		diagWg.Wait()
		report.DiagResults = diagResults

		// Attach diag results back to tree nodes
		diagMap := make(map[uint32]*ProcessDiag)
		for i := range diagResults {
			diagMap[diagResults[i].Process.ProcessId] = &diagResults[i]
		}
		attachDiagToTree(report.ChildTree, diagMap)
	}

	return report
}

// collectProcessTreeFromNodes builds the flat process list from the already-built tree (no extra WMI calls)
func collectProcessTreeFromNodes(target Win32Process, nodes []*ProcessNode) []Win32Process {
	result := []Win32Process{target}
	var walk func(ns []*ProcessNode)
	walk = func(ns []*ProcessNode) {
		for _, n := range ns {
			result = append(result, n.Process)
			walk(n.Children)
		}
	}
	walk(nodes)
	return result
}

// attachDiagToTree sets the Diag field on tree nodes from the diagMap
func attachDiagToTree(nodes []*ProcessNode, diagMap map[uint32]*ProcessDiag) {
	for _, n := range nodes {
		if d, ok := diagMap[n.Process.ProcessId]; ok {
			n.Diag = d
		}
		attachDiagToTree(n.Children, diagMap)
	}
}

func buildChildTree(pid uint32, diagnose, pipeScan bool) []*ProcessNode {
	children := getChildren(pid)
	var nodes []*ProcessNode
	for _, child := range children {
		node := &ProcessNode{Process: child}
		if diagnose {
			d := diagnoseProcess(child, diagnose, pipeScan)
			node.Diag = &d
		}
		node.Children = buildChildTree(child.ProcessId, diagnose, pipeScan)
		nodes = append(nodes, node)
	}
	return nodes
}

func printConsoleSummary(r TargetReport, diagnose bool) {
	t := r.Target
	printColored(colorWhite, fmt.Sprintf("  PID: %d | Name: %s", t.ProcessId, t.Name))
	printColored(colorGray, fmt.Sprintf("  CMD: %s", truncStr(ptrStr(t.CommandLine), 120)))
	if t.CreationDate != nil {
		ct := parseWmiDate(*t.CreationDate)
		if !ct.IsZero() {
			printColored(colorWhite, fmt.Sprintf("  Created: %s (running %s)", ct.Format("2006-01-02 15:04:05"), formatDuration(time.Since(ct))))
		}
	}
	printColored(colorWhite, fmt.Sprintf("  Handles: %d | Threads: %d | Children: %d",
		t.HandleCount, t.ThreadCount, len(r.Children)))

	// Pipe holders info (always shown)
	if len(r.PipeHolders) > 0 {
		holderPIDs := make(map[uint32]string)
		for _, ph := range r.PipeHolders {
			holderPIDs[ph.PID] = ph.ProcessName
		}
		printColored(colorRed, fmt.Sprintf("  [!] 发现 %d 个进程持有与目标共享的管道句柄:", len(holderPIDs)))
		for pid, name := range holderPIDs {
			printColored(colorYellow, fmt.Sprintf("      PID %d (%s) - 可使用 taskkill /PID %d /F 终止", pid, name, pid))
		}
	}

	if diagnose && len(r.DiagResults) > 0 {
		hasPipe := false
		hasBlocked := false
		longRunning := 0
		for _, d := range r.DiagResults {
			if d.PipeHandles > 0 {
				hasPipe = true
			}
			if d.IsBlocked {
				hasBlocked = true
			}
			if d.RunningTime > 30*time.Minute {
				longRunning++
			}
		}
		fmt.Println()
		if hasPipe {
			printColored(colorRed, "  [!] Pipe handles detected - possible output stream blocking")
		}
		if hasBlocked {
			printColored(colorRed, "  [!] Blocked processes detected (all threads waiting)")
		}
		if longRunning > 0 {
			printColored(colorYellow, fmt.Sprintf("  [!] %d long-running process(es) (>30min)", longRunning))
		}
		if !hasPipe && !hasBlocked && longRunning == 0 {
			printColored(colorGreen, "  [OK] No obvious blocking issues")
		}
		printColored(colorGray, "  -> See HTML report for full details")
	}
}

// JSON types for HTML report
type JSONDiag struct {
	RunningTime    string         `json:"runningTime"`
	TotalThreads   int            `json:"totalThreads"`
	WaitingThreads int            `json:"waitingThreads"`
	PipeHandles    int            `json:"pipeHandles"`
	IsBlocked      bool           `json:"isBlocked"`
	BlockReason    string         `json:"blockReason"`
	WaitReasons    map[string]int `json:"waitReasons,omitempty"`
	JstackOutput   string         `json:"jstackOutput,omitempty"`
}

type JSONProcessNode struct {
	PID         uint32            `json:"pid"`
	PPID        uint32            `json:"ppid"`
	Name        string            `json:"name"`
	Path        string            `json:"path"`
	CMD         string            `json:"cmd"`
	Handles     uint32            `json:"handles"`
	Threads     uint32            `json:"threads"`
	CreatedAt   string            `json:"createdAt"`
	RunningTime string            `json:"runningTime"`
	Children    []JSONProcessNode `json:"children,omitempty"`
	Diag        *JSONDiag         `json:"diag,omitempty"`
}

type JSONPipeHolder struct {
	PID         uint32 `json:"pid"`
	ProcessName string `json:"processName"`
	HandleValue uint16 `json:"handleValue"`
	PipeName    string `json:"pipeName"`
	CMD         string `json:"cmd"`
}

type JSONReport struct {
	PID            uint32            `json:"pid"`
	Name           string            `json:"name"`
	CMD            string            `json:"cmd"`
	CreatedAt      string            `json:"createdAt"`
	RunningTime    string            `json:"runningTime"`
	Handles        uint32            `json:"handles"`
	Threads        uint32            `json:"threads"`
	ParentChain    []JSONProcessNode `json:"parentChain"`
	ChildTree      []JSONProcessNode `json:"childTree"`
	Diag           []JSONDiag        `json:"diag,omitempty"`
	Issues         []string          `json:"issues"`
	DirectChildren int               `json:"directChildren"`
	JstackOutput   string            `json:"jstackOutput,omitempty"`
	PipeHolders    []JSONPipeHolder  `json:"pipeHolders,omitempty"`
}

// ==================== HTML Report Generation ====================

func generateHTMLReport(reports []TargetReport, buildId, processName string, diagnose bool, elapsed time.Duration) string {
	exePath, _ := os.Executable()
	dir := filepath.Dir(exePath)
	timestamp := time.Now().Format("20060102_150405")
	htmlPath := filepath.Join(dir, fmt.Sprintf("process-report_%s.html", timestamp))

	var convertNode func(n *ProcessNode) JSONProcessNode
	convertNode = func(n *ProcessNode) JSONProcessNode {
		jn := JSONProcessNode{
			PID:     n.Process.ProcessId,
			PPID:    n.Process.ParentProcessId,
			Name:    n.Process.Name,
			Path:    ptrStr(n.Process.ExecutablePath),
			CMD:     ptrStr(n.Process.CommandLine),
			Handles: n.Process.HandleCount,
			Threads: n.Process.ThreadCount,
		}
		if n.Process.CreationDate != nil {
			t := parseWmiDate(*n.Process.CreationDate)
			if !t.IsZero() {
				jn.CreatedAt = t.Format("2006-01-02 15:04:05")
				jn.RunningTime = formatDuration(time.Since(t))
			}
		}
		if n.Diag != nil {
			jd := &JSONDiag{
				RunningTime:    formatDuration(n.Diag.RunningTime),
				TotalThreads:   len(n.Diag.Threads),
				WaitingThreads: n.Diag.WaitingCount,
				PipeHandles:    n.Diag.PipeHandles,
				IsBlocked:      n.Diag.IsBlocked,
				BlockReason:    n.Diag.BlockReason,
				JstackOutput:   n.Diag.JstackOutput,
			}
			jn.Diag = jd
		}
		for _, child := range n.Children {
			jn.Children = append(jn.Children, convertNode(child))
		}
		return jn
	}

	var jsonReports []JSONReport
	for _, r := range reports {
		jr := JSONReport{
			PID:            r.Target.ProcessId,
			Name:           r.Target.Name,
			CMD:            ptrStr(r.Target.CommandLine),
			Handles:        r.Target.HandleCount,
			Threads:        r.Target.ThreadCount,
			DirectChildren: len(r.Children),
		}
		if r.Target.CreationDate != nil {
			t := parseWmiDate(*r.Target.CreationDate)
			if !t.IsZero() {
				jr.CreatedAt = t.Format("2006-01-02 15:04:05")
				jr.RunningTime = formatDuration(time.Since(t))
			}
		}
		for _, p := range r.ParentChain {
			jp := JSONProcessNode{
				PID:     p.ProcessId,
				PPID:    p.ParentProcessId,
				Name:    p.Name,
				Path:    ptrStr(p.ExecutablePath),
				CMD:     ptrStr(p.CommandLine),
				Handles: p.HandleCount,
				Threads: p.ThreadCount,
			}
			if p.CreationDate != nil {
				t := parseWmiDate(*p.CreationDate)
				if !t.IsZero() {
					jp.CreatedAt = t.Format("2006-01-02 15:04:05")
					jp.RunningTime = formatDuration(time.Since(t))
				}
			}
			jr.ParentChain = append(jr.ParentChain, jp)
		}
		for _, n := range r.ChildTree {
			jr.ChildTree = append(jr.ChildTree, convertNode(n))
		}

		// Diagnostics
		if diagnose {
			for _, d := range r.DiagResults {
				jd := JSONDiag{
					RunningTime:    formatDuration(d.RunningTime),
					TotalThreads:   len(d.Threads),
					WaitingThreads: d.WaitingCount,
					PipeHandles:    d.PipeHandles,
					IsBlocked:      d.IsBlocked,
					BlockReason:    d.BlockReason,
					WaitReasons:    make(map[string]int),
					JstackOutput:   d.JstackOutput,
				}
				for _, t := range d.Threads {
					if t.ThreadState == 5 {
						reason := getWaitReasonName(t.ThreadWaitReason)
						jd.WaitReasons[reason]++
					}
				}
				jr.Diag = append(jr.Diag, jd)
			}

			// Summarize issues
			for _, d := range r.DiagResults {
				if d.PipeHandles > 0 {
					jr.Issues = append(jr.Issues, fmt.Sprintf("PID %d (%s) 持有 %d 个 Pipe 句柄 - 可能存在输出流阻塞",
						d.Process.ProcessId, d.Process.Name, d.PipeHandles))
				}
				if d.IsBlocked {
					jr.Issues = append(jr.Issues, fmt.Sprintf("PID %d (%s) 已完全阻塞 (%s)",
						d.Process.ProcessId, d.Process.Name, d.BlockReason))
				}
				if d.RunningTime > 2*time.Hour {
					jr.Issues = append(jr.Issues, fmt.Sprintf("PID %d (%s) 已运行 %s (超过2小时)",
						d.Process.ProcessId, d.Process.Name, formatDuration(d.RunningTime)))
				}
			}
			if len(r.Children) > 0 {
				jr.Issues = append(jr.Issues, fmt.Sprintf("目标进程仍有 %d 个子进程在运行", len(r.Children)))
			}
		}

		// jstack output (always collected for java processes, independent of -diag)
		jr.JstackOutput = r.JstackOutput

		// Pipe holders (processes sharing pipe handles with target)
		for _, ph := range r.PipeHolders {
			jr.PipeHolders = append(jr.PipeHolders, JSONPipeHolder{
				PID:         ph.PID,
				ProcessName: ph.ProcessName,
				HandleValue: ph.HandleValue,
				PipeName:    ph.PipeName,
				CMD:         ph.CMD,
			})
		}
		if len(r.PipeHolders) > 0 {
			// Group by PID for issue summary
			holderPIDs := make(map[uint32]string)
			for _, ph := range r.PipeHolders {
				holderPIDs[ph.PID] = ph.ProcessName
			}
			for pid, name := range holderPIDs {
				jr.Issues = append(jr.Issues, fmt.Sprintf("PID %d (%s) 持有与目标进程共享的管道句柄 - 可能阻止管道关闭", pid, name))
			}
		}

		jsonReports = append(jsonReports, jr)
	}

	jsonData, _ := json.Marshal(jsonReports)

	html := `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>进程阻塞诊断报告</title>
<style>
* { margin:0; padding:0; box-sizing:border-box; }
body { font-family: 'Microsoft YaHei', 'Segoe UI', system-ui, -apple-system, sans-serif; background:#0d1117; color:#c9d1d9; padding:20px; }
.header { background:linear-gradient(135deg,#161b22,#1c2333); border:1px solid #30363d; border-radius:12px; padding:24px; margin-bottom:20px; }
.header h1 { font-size:22px; color:#58a6ff; margin-bottom:8px; }
.header .meta { color:#8b949e; font-size:13px; }
.header .meta span { margin-right:16px; }
.tabs { display:flex; gap:4px; margin-bottom:16px; flex-wrap:wrap; }
.tab { padding:8px 18px; border-radius:8px 8px 0 0; cursor:pointer; background:#161b22; border:1px solid #30363d; border-bottom:none; color:#8b949e; font-size:14px; transition:all .2s; }
.tab:hover { color:#c9d1d9; background:#1c2333; }
.tab.active { color:#58a6ff; background:#0d1117; border-color:#58a6ff; font-weight:600; }
.tab .badge { background:#da3633; color:#fff; border-radius:10px; padding:1px 7px; font-size:11px; margin-left:6px; }
.tab .badge.ok { background:#238636; }
.panel { display:none; background:#0d1117; border:1px solid #30363d; border-radius:0 12px 12px 12px; padding:20px; }
.panel.active { display:block; }
.section { margin-bottom:20px; }
.section-title { font-size:15px; font-weight:600; color:#58a6ff; margin-bottom:10px; display:flex; align-items:center; gap:8px; cursor:pointer; user-select:none; }
.section-title:before { content:'▸'; transition:transform .2s; display:inline-block; }
.section-title.open:before { transform:rotate(90deg); }
.section-body { display:none; padding-left:12px; }
.section-body.open { display:block; }
.process-info { background:#161b22; border:1px solid #30363d; border-radius:8px; padding:14px; margin-bottom:8px; }
.process-info .pid { color:#f0883e; font-weight:700; font-size:14px; }
.process-info .name { color:#7ee787; font-weight:600; }
.process-info .target-badge { background:#f0883e; color:#000; padding:1px 8px; border-radius:4px; font-size:11px; font-weight:700; margin-left:8px; }
.process-info .detail { color:#8b949e; font-size:12px; margin-top:4px; word-break:break-all; }
.process-info .detail b { color:#c9d1d9; font-weight:500; }
.tree { padding-left:20px; border-left:2px solid #30363d; margin-left:10px; }
.tree .tree-node { position:relative; padding:4px 0; }
.tree .tree-node:before { content:''; position:absolute; left:-20px; top:16px; width:18px; height:0; border-top:2px solid #30363d; }
.issues-list { list-style:none; }
.issues-list li { padding:10px 14px; margin-bottom:6px; border-radius:6px; font-size:13px; }
.issues-list li.error { background:rgba(218,54,51,.12); border-left:3px solid #da3633; color:#f85149; }
.issues-list li.warning { background:rgba(210,153,34,.12); border-left:3px solid #d29922; color:#e3b341; }
.issues-list li.ok { background:rgba(35,134,54,.12); border-left:3px solid #238636; color:#7ee787; }
.diag-table { width:100%; border-collapse:collapse; font-size:13px; }
.diag-table th { text-align:left; padding:8px 10px; background:#161b22; color:#8b949e; border-bottom:1px solid #30363d; font-weight:500; }
.diag-table td { padding:8px 10px; border-bottom:1px solid #21262d; }
.diag-table tr:hover td { background:#161b22; }
.status-dot { display:inline-block; width:8px; height:8px; border-radius:50%; margin-right:6px; }
.status-dot.red { background:#da3633; }
.status-dot.yellow { background:#d29922; }
.status-dot.green { background:#238636; }
.wait-reasons { display:flex; gap:6px; flex-wrap:wrap; margin-top:4px; }
.wait-reasons .chip { background:#21262d; color:#8b949e; padding:2px 8px; border-radius:4px; font-size:11px; }
.summary-cards { display:grid; grid-template-columns:repeat(auto-fit, minmax(160px,1fr)); gap:10px; margin-bottom:16px; }
.card { background:#161b22; border:1px solid #30363d; border-radius:8px; padding:14px; text-align:center; }
.card .value { font-size:26px; font-weight:700; }
.card .label { font-size:12px; color:#8b949e; margin-top:4px; }
.card .value.red { color:#f85149; }
.card .value.yellow { color:#e3b341; }
.card .value.green { color:#7ee787; }
.card .value.blue { color:#58a6ff; }
</style>
</head>
<body>
<div class="header">
  <h1>进程阻塞诊断报告</h1>
  <div class="meta">
    <span>构建ID: <b>` + escapeHTML(buildId) + `</b></span>
    <span>进程名: <b>` + escapeHTML(processName) + `</b></span>
    <span>生成时间: <b>` + time.Now().Format("2006-01-02 15:04:05") + `</b></span>
    <span>扫描耗时: <b>` + formatDuration(elapsed) + `</b></span>
    <span>匹配进程: <b>` + fmt.Sprintf("%d", len(reports)) + ` 个</b></span>
  </div>
</div>
<div id="app"></div>
<script>
const DATA = ` + string(jsonData) + `;
const diagnose = ` + fmt.Sprintf("%v", diagnose) + `;

function escapeHTML(s) {
  const d = document.createElement('div');
  d.textContent = s;
  return d.innerHTML;
}

function renderTree(nodes, targetPid) {
  if (!nodes || nodes.length === 0) return '<div style="color:#8b949e;padding:8px">(无子进程)</div>';
  let html = '<div class="tree">';
  for (const n of nodes) {
    const isTarget = n.pid === targetPid;
    html += '<div class="tree-node">';
    html += '<div class="process-info">';
    html += '<span class="pid">PID: ' + n.pid + '</span> ';
    html += '<span class="name">' + escapeHTML(n.name) + '</span>';
    if (isTarget) html += '<span class="target-badge">目标</span>';
    html += '<div class="detail"><b>路径:</b> ' + escapeHTML(n.path) + '</div>';
    html += '<div class="detail"><b>命令行:</b> ' + escapeHTML(n.cmd) + '</div>';
    html += '<div class="detail"><b>句柄数:</b> ' + n.handles + ' | <b>线程数:</b> ' + n.threads;
    if (n.runningTime) html += ' | <b>运行时长:</b> ' + n.runningTime;
    html += '</div>';
    if (n.diag) {
      let statusDot = 'green';
      if (n.diag.isBlocked) statusDot = 'red';
      else if (n.diag.pipeHandles > 0 || n.diag.waitingThreads > n.diag.totalThreads/2) statusDot = 'yellow';
      html += '<div class="detail"><span class="status-dot ' + statusDot + '"></span>';
      html += '<b>等待中:</b> ' + n.diag.waitingThreads + '/' + n.diag.totalThreads + ' 个线程';
      if (n.diag.pipeHandles > 0) html += ' | <b>管道句柄:</b> ' + n.diag.pipeHandles;
      if (n.diag.isBlocked) html += ' | <b style="color:#f85149">已阻塞</b>';
      html += '</div>';
    }
    html += '</div>';
    if (n.children && n.children.length > 0) {
      html += renderTree(n.children, targetPid);
    }
    html += '</div>';
  }
  html += '</div>';
  return html;
}

function render() {
  const app = document.getElementById('app');
  if (DATA.length === 0) { app.innerHTML = '<p>暂无数据</p>'; return; }

  let tabsHtml = '<div class="tabs">';
  DATA.forEach((r, i) => {
    const issueCount = r.issues ? r.issues.length : 0;
    const badge = issueCount > 0
      ? '<span class="badge">' + issueCount + '</span>'
      : '<span class="badge ok">正常</span>';
    tabsHtml += '<div class="tab' + (i===0?' active':'') + '" onclick="switchTab(' + i + ')" id="tab-' + i + '">';
    tabsHtml += 'PID ' + r.pid + ' (' + escapeHTML(r.name) + ')' + badge + '</div>';
  });
  tabsHtml += '</div>';

  let panelsHtml = '';
  DATA.forEach((r, i) => {
    panelsHtml += '<div class="panel' + (i===0?' active':'') + '" id="panel-' + i + '">';

    const totalProcs = r.diag ? r.diag.length : 0;
    const blockedCount = r.diag ? r.diag.filter(d=>d.isBlocked).length : 0;
    const pipeCount = r.diag ? r.diag.reduce((s,d)=>s+d.pipeHandles, 0) : 0;
    const pipeHolderCount = r.pipeHolders ? Object.keys(r.pipeHolders.reduce((m,h)=>{m[h.pid]=1;return m},{})).length : 0;
    panelsHtml += '<div class="summary-cards">';
    panelsHtml += '<div class="card"><div class="value blue">' + r.pid + '</div><div class="label">目标 PID</div></div>';
    panelsHtml += '<div class="card"><div class="value blue">' + r.directChildren + '</div><div class="label">直接子进程</div></div>';
    panelsHtml += '<div class="card"><div class="value blue">' + totalProcs + '</div><div class="label">进程总数</div></div>';
    panelsHtml += '<div class="card"><div class="value ' + (blockedCount>0?'red':'green') + '">' + blockedCount + '</div><div class="label">已阻塞</div></div>';
    panelsHtml += '<div class="card"><div class="value ' + (pipeCount>0?'yellow':'green') + '">' + pipeCount + '</div><div class="label">管道句柄</div></div>';
    panelsHtml += '<div class="card"><div class="value ' + (pipeHolderCount>0?'red':'green') + '">' + pipeHolderCount + '</div><div class="label">管道继承者</div></div>';
    panelsHtml += '<div class="card"><div class="value">' + (r.runningTime||'N/A') + '</div><div class="label">运行时长</div></div>';
    panelsHtml += '</div>';

    if (diagnose) {
      panelsHtml += '<div class="section">';
      panelsHtml += '<div class="section-title open" onclick="toggleSection(this)">诊断问题</div>';
      panelsHtml += '<div class="section-body open">';
      if (r.issues && r.issues.length > 0) {
        panelsHtml += '<ul class="issues-list">';
        r.issues.forEach(issue => {
          const cls = issue.includes('Pipe') || issue.includes('阻塞') ? 'error' : 'warning';
          panelsHtml += '<li class="' + cls + '">' + escapeHTML(issue) + '</li>';
        });
        panelsHtml += '</ul>';
      } else {
        panelsHtml += '<ul class="issues-list"><li class="ok">未检测到明显的阻塞问题</li></ul>';
      }
      panelsHtml += '</div></div>';
    }

    panelsHtml += '<div class="section">';
    panelsHtml += '<div class="section-title open" onclick="toggleSection(this)">目标进程</div>';
    panelsHtml += '<div class="section-body open">';
    panelsHtml += '<div class="process-info">';
    panelsHtml += '<span class="pid">PID: ' + r.pid + '</span> <span class="name">' + escapeHTML(r.name) + '</span><span class="target-badge">目标</span>';
    panelsHtml += '<div class="detail"><b>命令行:</b> ' + escapeHTML(r.cmd) + '</div>';
    panelsHtml += '<div class="detail"><b>创建时间:</b> ' + (r.createdAt||'N/A') + ' | <b>运行时长:</b> ' + (r.runningTime||'N/A') + ' | <b>句柄数:</b> ' + r.handles + ' | <b>线程数:</b> ' + r.threads + '</div>';
    panelsHtml += '</div></div></div>';

    panelsHtml += '<div class="section">';
    panelsHtml += '<div class="section-title" onclick="toggleSection(this)">父进程链 (' + (r.parentChain?r.parentChain.length:0) + ')</div>';
    panelsHtml += '<div class="section-body">';
    if (r.parentChain) {
      r.parentChain.forEach(p => {
        const isT = p.pid === r.pid;
        panelsHtml += '<div class="process-info" style="margin-left:' + (r.parentChain.indexOf(p)*16) + 'px">';
        panelsHtml += '<span class="pid">PID: ' + p.pid + '</span> <span class="name">' + escapeHTML(p.name) + '</span>';
        if (isT) panelsHtml += '<span class="target-badge">目标</span>';
        panelsHtml += '<div class="detail"><b>命令行:</b> ' + escapeHTML(p.cmd) + '</div>';
        panelsHtml += '</div>';
      });
    }
    panelsHtml += '</div></div>';

    panelsHtml += '<div class="section">';
    panelsHtml += '<div class="section-title open" onclick="toggleSection(this)">子进程树</div>';
    panelsHtml += '<div class="section-body open">';
    panelsHtml += renderTree(r.childTree, r.pid);
    panelsHtml += '</div></div>';

    // Pipe holders section (always shown if pipe holders found)
    if (r.pipeHolders && r.pipeHolders.length > 0) {
      panelsHtml += '<div class="section">';
      panelsHtml += '<div class="section-title open" onclick="toggleSection(this)"><span style="color:#f85149">⚠</span> 管道继承者分析 (' + r.pipeHolders.length + ' 个句柄)</div>';
      panelsHtml += '<div class="section-body open">';

      // Explanation
      panelsHtml += '<div style="background:rgba(218,54,51,.08);border:1px solid rgba(218,54,51,.3);border-radius:8px;padding:12px 16px;margin-bottom:12px;font-size:13px;color:#f0883e;line-height:1.6">';
      panelsHtml += '<b>发现其他进程持有与目标进程共享的管道句柄。</b><br>';
      panelsHtml += '这通常发生在构建脚本启动了后台进程（守护进程、nohup 等），这些进程继承了父进程的 stdout/stderr 管道句柄。';
      panelsHtml += '即使原始子进程已退出，只要这些"孙进程"还持有管道写端，commons-exec 的 StreamPumper 就无法读到 EOF，导致主进程阻塞。<br><br>';
      panelsHtml += '<b>解决方案：</b><br>';
      panelsHtml += '• <b>立即解除</b>：使用 <code style="background:#21262d;padding:2px 6px;border-radius:3px">handle.exe -c &lt;handleValue&gt; -p &lt;pid&gt; -y</code> (SysInternals) 关闭特定句柄<br>';
      panelsHtml += '• <b>终止进程</b>：使用 <code style="background:#21262d;padding:2px 6px;border-radius:3px">taskkill /PID &lt;pid&gt; /F</code> 终止持有句柄的进程<br>';
      panelsHtml += '• <b>根本修复</b>：修改构建脚本，在启动后台进程时关闭继承的文件描述符或重定向 stdout/stderr';
      panelsHtml += '</div>';

      // Group by PID
      const holdersByPid = {};
      r.pipeHolders.forEach(h => {
        if (!holdersByPid[h.pid]) holdersByPid[h.pid] = { name: h.processName, cmd: h.cmd, handles: [] };
        holdersByPid[h.pid].handles.push(h);
      });

      panelsHtml += '<table class="diag-table"><thead><tr><th>PID</th><th>进程名</th><th>句柄数</th><th>管道名称</th><th>命令行</th><th>操作</th></tr></thead><tbody>';
      Object.entries(holdersByPid).forEach(([pid, info]) => {
        const pipeNames = [...new Set(info.handles.map(h => h.pipeName))].join(', ');
        const handleVals = info.handles.map(h => '0x' + h.handleValue.toString(16).toUpperCase()).join(', ');
        panelsHtml += '<tr>';
        panelsHtml += '<td style="color:#f0883e;font-weight:600">' + pid + '</td>';
        panelsHtml += '<td style="color:#7ee787">' + escapeHTML(info.name) + '</td>';
        panelsHtml += '<td>' + info.handles.length + ' (' + handleVals + ')</td>';
        panelsHtml += '<td style="font-size:11px;word-break:break-all;max-width:300px">' + escapeHTML(pipeNames) + '</td>';
        panelsHtml += '<td style="font-size:11px;max-width:400px;word-break:break-all">' + escapeHTML(info.cmd.length > 200 ? info.cmd.substring(0,200) + '...' : info.cmd) + '</td>';
        panelsHtml += '<td style="white-space:nowrap">';
        info.handles.forEach(h => {
          panelsHtml += '<code style="background:#21262d;padding:2px 4px;border-radius:3px;font-size:10px;display:block;margin-bottom:2px;color:#f0883e;cursor:pointer" title="复制关闭句柄命令" onclick="navigator.clipboard.writeText(\'handle.exe -c 0x' + h.handleValue.toString(16).toUpperCase() + ' -p ' + pid + ' -y\')">handle -c 0x' + h.handleValue.toString(16).toUpperCase() + '</code>';
        });
        panelsHtml += '<code style="background:#21262d;padding:2px 4px;border-radius:3px;font-size:10px;display:block;color:#f85149;cursor:pointer" title="复制终止进程命令" onclick="navigator.clipboard.writeText(\'taskkill /PID ' + pid + ' /F\')">taskkill /PID ' + pid + '</code>';
        panelsHtml += '</td></tr>';
      });
      panelsHtml += '</tbody></table>';

      panelsHtml += '</div></div>';
    }

    if (diagnose && r.diag && r.diag.length > 0) {
      panelsHtml += '<div class="section">';
      panelsHtml += '<div class="section-title" onclick="toggleSection(this)">诊断详情 (' + r.diag.length + ' 个进程)</div>';
      panelsHtml += '<div class="section-body">';
      panelsHtml += '<table class="diag-table"><thead><tr><th>PID</th><th>名称</th><th>运行时长</th><th>线程数</th><th>等待中</th><th>管道</th><th>状态</th><th>等待原因</th></tr></thead><tbody>';
      r.diag.forEach((d, di) => {
        const proc = di === 0 ? r : null;
        let statusDot = 'green', statusText = '正常';
        if (d.isBlocked) { statusDot = 'red'; statusText = d.blockReason; }
        else if (d.pipeHandles > 0) { statusDot = 'yellow'; statusText = '持有管道句柄'; }
        else if (d.waitingThreads > d.totalThreads/2) { statusDot = 'yellow'; statusText = '多数线程等待'; }
        let reasons = '';
        if (d.waitReasons) {
          Object.entries(d.waitReasons).forEach(([k,v]) => { reasons += '<span class="chip">' + k + ' x' + v + '</span>'; });
        }
        panelsHtml += '<tr><td style="color:#f0883e;font-weight:600">' + (proc ? r.pid : '') + '</td>';
        panelsHtml += '<td>' + (proc ? escapeHTML(r.name) : '') + '</td>';
        panelsHtml += '<td>' + d.runningTime + '</td>';
        panelsHtml += '<td>' + d.totalThreads + '</td>';
        panelsHtml += '<td>' + d.waitingThreads + '/' + d.totalThreads + '</td>';
        panelsHtml += '<td>' + d.pipeHandles + '</td>';
        panelsHtml += '<td><span class="status-dot ' + statusDot + '"></span>' + statusText + '</td>';
        panelsHtml += '<td><div class="wait-reasons">' + reasons + '</div></td></tr>';
      });
      panelsHtml += '</tbody></table></div></div>';
    }

    // jstack output section (always shown for java processes)
    if (r.jstackOutput && r.jstackOutput.length > 0 && !r.jstackOutput.startsWith('[jstack not found')) {
      panelsHtml += '<div class="section">';
      panelsHtml += '<div class="section-title open" onclick="toggleSection(this)">Java 线程堆栈 (jstack)</div>';
      panelsHtml += '<div class="section-body open">';

      // Parse jstack output for summary
      const jstackLines = r.jstackOutput.split('\n');
      const threadBlocks = [];
      let currentBlock = null;
      for (const line of jstackLines) {
        if (line.startsWith('"')) {
          if (currentBlock) threadBlocks.push(currentBlock);
          currentBlock = { name: line, lines: [line], state: '' };
        } else if (currentBlock) {
          currentBlock.lines.push(line);
          if (line.trim().startsWith('java.lang.Thread.State:')) {
            currentBlock.state = line.trim().replace('java.lang.Thread.State: ', '');
          }
        }
      }
      if (currentBlock) threadBlocks.push(currentBlock);

      // Thread state summary
      const stateCounts = {};
      threadBlocks.forEach(b => {
        const s = b.state || 'UNKNOWN';
        stateCounts[s] = (stateCounts[s] || 0) + 1;
      });

      panelsHtml += '<div style="margin-bottom:12px">';
      panelsHtml += '<div style="font-size:13px;color:#8b949e;margin-bottom:8px">线程状态统计 (共 ' + threadBlocks.length + ' 个线程)</div>';
      panelsHtml += '<div class="wait-reasons">';
      const stateColors = {'RUNNABLE':'#7ee787','WAITING':'#e3b341','TIMED_WAITING':'#f0883e','BLOCKED':'#f85149','NEW':'#58a6ff','TERMINATED':'#8b949e'};
      Object.entries(stateCounts).sort((a,b)=>b[1]-a[1]).forEach(([state,count]) => {
        const color = stateColors[state] || '#8b949e';
        panelsHtml += '<span class="chip" style="border-left:3px solid ' + color + ';padding-left:6px">' + state + ' x' + count + '</span>';
      });
      panelsHtml += '</div></div>';

      // Search box
      panelsHtml += '<div style="margin-bottom:8px"><input type="text" id="jstack-filter-' + i + '" placeholder="搜索线程名称/堆栈..." oninput="filterJstack(' + i + ')" style="width:100%;padding:6px 10px;background:#161b22;border:1px solid #30363d;border-radius:6px;color:#c9d1d9;font-size:13px;outline:none;" /></div>';

      // Thread state filter buttons
      panelsHtml += '<div style="margin-bottom:10px;display:flex;gap:4px;flex-wrap:wrap">';
      panelsHtml += '<button class="jstack-state-btn" onclick="filterJstackByState(' + i + ',\'\')" style="padding:3px 10px;background:#21262d;border:1px solid #30363d;border-radius:4px;color:#c9d1d9;cursor:pointer;font-size:12px">全部</button>';
      Object.keys(stateCounts).forEach(state => {
        const color = stateColors[state] || '#8b949e';
        panelsHtml += '<button class="jstack-state-btn" onclick="filterJstackByState(' + i + ',\'' + state + '\')" style="padding:3px 10px;background:#21262d;border:1px solid #30363d;border-radius:4px;color:' + color + ';cursor:pointer;font-size:12px">' + state + '</button>';
      });
      panelsHtml += '</div>';

      // Thread blocks (collapsible)
      panelsHtml += '<div id="jstack-threads-' + i + '">';
      threadBlocks.forEach((block, bi) => {
        const stateColor = stateColors[block.state] || '#8b949e';
        const threadName = block.name.replace(/"/g, '').split(' ')[0];
        panelsHtml += '<div class="jstack-thread-block" data-state="' + escapeHTML(block.state) + '" style="margin-bottom:4px">';
        panelsHtml += '<div onclick="toggleJstackThread(this)" style="cursor:pointer;padding:6px 10px;background:#161b22;border:1px solid #21262d;border-left:3px solid ' + stateColor + ';border-radius:4px;font-size:12px;font-family:monospace;color:#c9d1d9;display:flex;align-items:center;gap:8px">';
        panelsHtml += '<span style="color:#8b949e;transition:transform .2s;display:inline-block" class="jstack-arrow">▸</span>';
        panelsHtml += '<span>' + escapeHTML(block.name.length > 120 ? block.name.substring(0,120) + '...' : block.name) + '</span>';
        if (block.state) panelsHtml += '<span style="margin-left:auto;color:' + stateColor + ';font-size:11px;white-space:nowrap">' + block.state + '</span>';
        panelsHtml += '</div>';
        panelsHtml += '<pre class="jstack-thread-detail" style="display:none;margin:0;padding:8px 10px 8px 16px;background:#0d1117;border:1px solid #21262d;border-top:none;border-radius:0 0 4px 4px;font-size:11px;line-height:1.5;color:#8b949e;overflow-x:auto;white-space:pre-wrap;word-break:break-all">';
        panelsHtml += escapeHTML(block.lines.join('\n'));
        panelsHtml += '</pre></div>';
      });
      panelsHtml += '</div>';

      // Full raw output (collapsed)
      panelsHtml += '<div style="margin-top:12px">';
      panelsHtml += '<div class="section-title" onclick="toggleSection(this)" style="font-size:13px;color:#8b949e">完整 jstack 原始输出</div>';
      panelsHtml += '<div class="section-body"><pre style="background:#161b22;border:1px solid #30363d;border-radius:6px;padding:12px;font-size:11px;line-height:1.5;color:#8b949e;overflow-x:auto;max-height:600px;overflow-y:auto;white-space:pre-wrap;word-break:break-all">' + escapeHTML(r.jstackOutput) + '</pre></div>';
      panelsHtml += '</div>';

      panelsHtml += '</div></div>';
    } else if (r.jstackOutput && r.jstackOutput.startsWith('[jstack')) {
      panelsHtml += '<div class="section">';
      panelsHtml += '<div class="section-title" onclick="toggleSection(this)">Java 线程堆栈 (jstack)</div>';
      panelsHtml += '<div class="section-body"><div style="color:#e3b341;padding:8px">' + escapeHTML(r.jstackOutput) + '</div></div></div>';
    }

    panelsHtml += '</div>';
  });

  app.innerHTML = tabsHtml + panelsHtml;
}

function toggleJstackThread(el) {
  const detail = el.nextElementSibling;
  const arrow = el.querySelector('.jstack-arrow');
  if (detail.style.display === 'none') {
    detail.style.display = 'block';
    arrow.style.transform = 'rotate(90deg)';
  } else {
    detail.style.display = 'none';
    arrow.style.transform = 'rotate(0deg)';
  }
}

function filterJstack(panelIdx) {
  const input = document.getElementById('jstack-filter-' + panelIdx);
  const keyword = input.value.toLowerCase();
  const container = document.getElementById('jstack-threads-' + panelIdx);
  const blocks = container.querySelectorAll('.jstack-thread-block');
  blocks.forEach(b => {
    const text = b.textContent.toLowerCase();
    b.style.display = text.includes(keyword) ? '' : 'none';
  });
}

function filterJstackByState(panelIdx, state) {
  const container = document.getElementById('jstack-threads-' + panelIdx);
  const blocks = container.querySelectorAll('.jstack-thread-block');
  blocks.forEach(b => {
    if (!state || b.dataset.state === state) {
      b.style.display = '';
    } else {
      b.style.display = 'none';
    }
  });
  // Also clear search input
  const input = document.getElementById('jstack-filter-' + panelIdx);
  if (input) input.value = '';
}

function switchTab(idx) {
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
  document.getElementById('tab-' + idx).classList.add('active');
  document.getElementById('panel-' + idx).classList.add('active');
}

function toggleSection(el) {
  el.classList.toggle('open');
  const body = el.nextElementSibling;
  body.classList.toggle('open');
}

render();
</script>
</body>
</html>`

	if err := os.WriteFile(htmlPath, []byte(html), 0644); err != nil {
		printColored(colorRed, fmt.Sprintf("Failed to write HTML report: %v", err))
		return ""
	}
	return htmlPath
}

func escapeHTML(s string) string {
	s = strings.ReplaceAll(s, "&", "&amp;")
	s = strings.ReplaceAll(s, "<", "&lt;")
	s = strings.ReplaceAll(s, ">", "&gt;")
	s = strings.ReplaceAll(s, "\"", "&quot;")
	return s
}

func openBrowser(path string) {
	exec.Command("rundll32", "url.dll,FileProtocolHandler", path).Start()
}

// ==================== Diagnostics ====================

func diagnoseProcess(p Win32Process, threadDiag, pipeScan bool) ProcessDiag {
	diag := ProcessDiag{Process: p}

	if p.CreationDate != nil {
		t := parseWmiDate(*p.CreationDate)
		if !t.IsZero() {
			diag.RunningTime = time.Since(t)
		}
	}

	// Thread diagnosis (WMI query per process — moderately slow)
	if threadDiag {
		diag.Threads = getThreads(p.ProcessId)
		for _, t := range diag.Threads {
			if t.ThreadState == 5 {
				diag.WaitingCount++
			}
		}
		if diag.WaitingCount == len(diag.Threads) && len(diag.Threads) > 0 {
			diag.IsBlocked = true
			diag.BlockReason = "所有线程均在等待"
		}
	}

	// Pipe handle scan (NtQuerySystemInformation — very slow)
	if pipeScan {
		pipeResult := countPipeHandles(p.ProcessId)
		if pipeResult < 0 {
			pipeResult = 0
		}
		diag.PipeHandles = pipeResult
	}

	// jstack for java processes: use jstack from the same directory as the java executable
	if strings.EqualFold(p.Name, "java.exe") && p.ExecutablePath != nil {
		diag.JstackOutput = runJstack(*p.ExecutablePath, p.ProcessId)
	}

	return diag
}

// runJstack runs jstack from the same directory as the java executable and returns the output
func runJstack(javaPath string, pid uint32) string {
	javaDir := filepath.Dir(javaPath)
	jstackPath := filepath.Join(javaDir, "jstack.exe")

	// Check if jstack exists
	if _, err := os.Stat(jstackPath); err != nil {
		return fmt.Sprintf("[jstack not found at %s]", jstackPath)
	}

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	cmd := exec.CommandContext(ctx, jstackPath, fmt.Sprintf("%d", pid))
	cmd.Env = append(os.Environ(), "JAVA_HOME="+filepath.Dir(javaDir))

	output, err := cmd.CombinedOutput()
	if err != nil {
		if ctx.Err() == context.DeadlineExceeded {
			return fmt.Sprintf("[jstack timeout after 30s for PID %d]", pid)
		}
		return fmt.Sprintf("[jstack error: %v]\n%s", err, string(output))
	}
	return string(output)
}

// findPipeHolders scans all system handles to find processes that share Pipe handles with the target process.
// This identifies "grandchild" processes that inherited pipe handles from the target's child processes.
func findPipeHolders(targetPID uint32) []PipeHolder {
	ntdll := syscall.NewLazyDLL("ntdll.dll")
	ntQuerySystemInformation := ntdll.NewProc("NtQuerySystemInformation")
	ntQueryObject := ntdll.NewProc("NtQueryObject")

	// Step 1: Get all system handles
	bufSize := uint32(1024 * 1024 * 4)
	buf := make([]byte, bufSize)
	var returnLength uint32
	for {
		ret, _, _ := ntQuerySystemInformation.Call(
			uintptr(SystemHandleInformation),
			uintptr(unsafe.Pointer(&buf[0])),
			uintptr(bufSize),
			uintptr(unsafe.Pointer(&returnLength)),
		)
		if ret == STATUS_INFO_LENGTH_MISMATCH {
			bufSize *= 2
			if bufSize > 512*1024*1024 {
				return nil
			}
			buf = make([]byte, bufSize)
			continue
		}
		if ret != 0 {
			return nil
		}
		break
	}

	handleCount := *(*uint32)(unsafe.Pointer(&buf[0]))
	if handleCount == 0 {
		return nil
	}

	entrySize := unsafe.Sizeof(systemHandleEntryInfo{})
	headerSize := uintptr(unsafe.Sizeof(uintptr(0)))
	if headerSize < 4 {
		headerSize = 4
	}
	base := uintptr(unsafe.Pointer(&buf[0])) + headerSize
	bufEnd := uintptr(unsafe.Pointer(&buf[0])) + uintptr(len(buf))

	// Step 2: Find all pipe handles belonging to the target process, collect their kernel object addresses
	targetPipeObjects := make(map[uintptr]string) // objectAddr -> pipeName

	hTarget, err := windows.OpenProcess(PROCESS_DUP_HANDLE|PROCESS_QUERY_INFORMATION, false, targetPID)
	if err != nil {
		return nil
	}
	defer windows.CloseHandle(hTarget)

	currentProcess := windows.CurrentProcess()

	for i := uint32(0); i < handleCount; i++ {
		entryAddr := base + uintptr(i)*entrySize
		if entryAddr+entrySize > bufEnd {
			break
		}
		entry := (*systemHandleEntryInfo)(unsafe.Pointer(entryAddr))
		if uint32(entry.UniqueProcessId) != targetPID {
			continue
		}

		// Try to dup and check if it's a pipe
		var dupHandle windows.Handle
		err := windows.DuplicateHandle(
			hTarget,
			windows.Handle(entry.HandleValue),
			currentProcess,
			&dupHandle,
			0, false, windows.DUPLICATE_SAME_ACCESS,
		)
		if err != nil {
			continue
		}

		pipeName := getHandlePipeName(ntQueryObject, dupHandle)
		windows.CloseHandle(dupHandle)
		if pipeName != "" {
			targetPipeObjects[entry.Object] = pipeName
		}
	}

	if len(targetPipeObjects) == 0 {
		return nil
	}

	// Step 3: Scan all handles again, find OTHER processes that reference the same kernel objects
	type rawHolder struct {
		pid         uint32
		handleValue uint16
		objectAddr  uintptr
		pipeName    string
	}
	var rawHolders []rawHolder

	for i := uint32(0); i < handleCount; i++ {
		entryAddr := base + uintptr(i)*entrySize
		if entryAddr+entrySize > bufEnd {
			break
		}
		entry := (*systemHandleEntryInfo)(unsafe.Pointer(entryAddr))
		otherPID := uint32(entry.UniqueProcessId)
		if otherPID == targetPID || otherPID == uint32(os.Getpid()) {
			continue
		}
		if pipeName, ok := targetPipeObjects[entry.Object]; ok {
			rawHolders = append(rawHolders, rawHolder{
				pid:         otherPID,
				handleValue: entry.HandleValue,
				objectAddr:  entry.Object,
				pipeName:    pipeName,
			})
		}
	}

	// Step 4: Resolve process names for holders
	var holders []PipeHolder
	procNameCache := make(map[uint32]Win32Process)
	for _, rh := range rawHolders {
		proc, cached := procNameCache[rh.pid]
		if !cached {
			p := getProcessByPid(rh.pid)
			if p != nil {
				proc = *p
			} else {
				proc = Win32Process{ProcessId: rh.pid, Name: "(unknown)"}
			}
			procNameCache[rh.pid] = proc
		}
		holders = append(holders, PipeHolder{
			PID:         rh.pid,
			ProcessName: proc.Name,
			HandleValue: rh.handleValue,
			ObjectAddr:  rh.objectAddr,
			PipeName:    rh.pipeName,
			CMD:         ptrStr(proc.CommandLine),
		})
	}

	return holders
}

// getHandlePipeName checks if a handle is a File type pointing to a pipe, returns the pipe name or ""
func getHandlePipeName(ntQueryObject *syscall.LazyProc, handle windows.Handle) string {
	// Check type
	typeBuf := make([]byte, 1024)
	var typeReturnLength uint32
	ret, _, _ := ntQueryObject.Call(
		uintptr(handle),
		uintptr(ObjectTypeInformation),
		uintptr(unsafe.Pointer(&typeBuf[0])),
		uintptr(len(typeBuf)),
		uintptr(unsafe.Pointer(&typeReturnLength)),
	)
	if ret != 0 || typeReturnLength < 8 {
		return ""
	}

	nameLen := *(*uint16)(unsafe.Pointer(&typeBuf[0]))
	if nameLen == 0 || nameLen >= 512 {
		return ""
	}
	strOffset := uintptr(unsafe.Sizeof(uintptr(0)))*2 + 2 + 2
	if strOffset+uintptr(nameLen) > uintptr(typeReturnLength) {
		return ""
	}
	chars := nameLen / 2
	utf16Slice := make([]uint16, chars)
	for ci := uint16(0); ci < chars; ci++ {
		off := strOffset + uintptr(ci)*2
		utf16Slice[ci] = *(*uint16)(unsafe.Pointer(uintptr(unsafe.Pointer(&typeBuf[0])) + off))
	}
	typeName := syscall.UTF16ToString(utf16Slice)
	if typeName != "File" {
		return ""
	}

	// Get object name — use a goroutine with timeout to avoid blocking on certain handle types
	type nameResult struct {
		name string
	}
	ch := make(chan nameResult, 1)
	go func() {
		nameBuf := make([]byte, 2048)
		var nameReturnLength uint32
		ret2, _, _ := ntQueryObject.Call(
			uintptr(handle),
			uintptr(ObjectNameInformation),
			uintptr(unsafe.Pointer(&nameBuf[0])),
			uintptr(len(nameBuf)),
			uintptr(unsafe.Pointer(&nameReturnLength)),
		)
		if ret2 != 0 || nameReturnLength < 8 {
			ch <- nameResult{""}
			return
		}
		objNameLen := *(*uint16)(unsafe.Pointer(&nameBuf[0]))
		if objNameLen == 0 || objNameLen >= 2048 {
			ch <- nameResult{""}
			return
		}
		objStrOffset := uintptr(unsafe.Sizeof(uintptr(0)))*2 + 2 + 2
		if objStrOffset+uintptr(objNameLen) > uintptr(nameReturnLength) {
			ch <- nameResult{""}
			return
		}
		objChars := objNameLen / 2
		objUtf16 := make([]uint16, objChars)
		for ci := uint16(0); ci < objChars; ci++ {
			off := objStrOffset + uintptr(ci)*2
			objUtf16[ci] = *(*uint16)(unsafe.Pointer(uintptr(unsafe.Pointer(&nameBuf[0])) + off))
		}
		objName := syscall.UTF16ToString(objUtf16)
		ch <- nameResult{objName}
	}()

	select {
	case result := <-ch:
		if strings.Contains(strings.ToLower(result.name), "pipe") {
			return result.name
		}
		return ""
	case <-time.After(200 * time.Millisecond):
		// NtQueryObject can deadlock on certain handle types, skip
		return ""
	}
}

func getThreads(pid uint32) []Win32Thread {
	query := fmt.Sprintf("SELECT ProcessHandle, Handle, ThreadState, ThreadWaitReason FROM Win32_Thread WHERE ProcessHandle='%d'", pid)
	var threads []Win32Thread
	wmi.Query(query, &threads)
	return threads
}

// countPipeHandles detects Pipe handles held by a process via NtQuerySystemInformation.
func countPipeHandles(pid uint32) (result int) {
	defer func() {
		if r := recover(); r != nil {
			result = -1
		}
	}()

	ntdll := syscall.NewLazyDLL("ntdll.dll")
	ntQuerySystemInformation := ntdll.NewProc("NtQuerySystemInformation")
	ntQueryObject := ntdll.NewProc("NtQueryObject")

	bufSize := uint32(1024 * 1024 * 4)
	buf := make([]byte, bufSize)

	var returnLength uint32
	for {
		ret, _, _ := ntQuerySystemInformation.Call(
			uintptr(SystemHandleInformation),
			uintptr(unsafe.Pointer(&buf[0])),
			uintptr(bufSize),
			uintptr(unsafe.Pointer(&returnLength)),
		)
		if ret == STATUS_INFO_LENGTH_MISMATCH {
			bufSize *= 2
			if bufSize > 256*1024*1024 {
				return 0
			}
			buf = make([]byte, bufSize)
			continue
		}
		if ret != 0 {
			return 0
		}
		break
	}

	handleCount := *(*uint32)(unsafe.Pointer(&buf[0]))
	if handleCount == 0 {
		return 0
	}

	entrySize := unsafe.Sizeof(systemHandleEntryInfo{})
	headerSize := uintptr(unsafe.Sizeof(uintptr(0)))
	if headerSize < 4 {
		headerSize = 4
	}
	base := uintptr(unsafe.Pointer(&buf[0])) + headerSize
	bufEnd := uintptr(unsafe.Pointer(&buf[0])) + uintptr(len(buf))

	hProcess, err := windows.OpenProcess(PROCESS_DUP_HANDLE|PROCESS_QUERY_INFORMATION, false, pid)
	if err != nil {
		return 0
	}
	defer windows.CloseHandle(hProcess)

	currentProcess := windows.CurrentProcess()
	pipeCount := 0
	checked := 0
	maxCheck := 300

	for i := uint32(0); i < handleCount && checked < maxCheck; i++ {
		entryAddr := base + uintptr(i)*entrySize
		if entryAddr+entrySize > bufEnd {
			break
		}

		entry := (*systemHandleEntryInfo)(unsafe.Pointer(entryAddr))
		if uint32(entry.UniqueProcessId) != pid {
			continue
		}
		checked++

		var dupHandle windows.Handle
		err := windows.DuplicateHandle(
			hProcess,
			windows.Handle(entry.HandleValue),
			currentProcess,
			&dupHandle,
			0,
			false,
			windows.DUPLICATE_SAME_ACCESS,
		)
		if err != nil {
			continue
		}

		typeBuf := make([]byte, 1024)
		var typeReturnLength uint32
		ret, _, _ := ntQueryObject.Call(
			uintptr(dupHandle),
			uintptr(ObjectTypeInformation),
			uintptr(unsafe.Pointer(&typeBuf[0])),
			uintptr(len(typeBuf)),
			uintptr(unsafe.Pointer(&typeReturnLength)),
		)
		if ret == 0 && typeReturnLength >= 8 {
			nameLen := *(*uint16)(unsafe.Pointer(&typeBuf[0]))
			if nameLen > 0 && nameLen < 512 {
				strOffset := uintptr(unsafe.Sizeof(uintptr(0)))*2 + 2 + 2
				if strOffset+uintptr(nameLen) <= uintptr(typeReturnLength) {
					chars := nameLen / 2
					utf16Slice := make([]uint16, chars)
					for ci := uint16(0); ci < chars; ci++ {
						off := strOffset + uintptr(ci)*2
						utf16Slice[ci] = *(*uint16)(unsafe.Pointer(uintptr(unsafe.Pointer(&typeBuf[0])) + off))
					}
					typeName := syscall.UTF16ToString(utf16Slice)
					if typeName == "File" {
						nameBuf := make([]byte, 2048)
						var nameReturnLength uint32
						ret2, _, _ := ntQueryObject.Call(
							uintptr(dupHandle),
							uintptr(ObjectNameInformation),
							uintptr(unsafe.Pointer(&nameBuf[0])),
							uintptr(len(nameBuf)),
							uintptr(unsafe.Pointer(&nameReturnLength)),
						)
						if ret2 == 0 && nameReturnLength >= 8 {
							objNameLen := *(*uint16)(unsafe.Pointer(&nameBuf[0]))
							if objNameLen > 0 && objNameLen < 2048 {
								objStrOffset := uintptr(unsafe.Sizeof(uintptr(0)))*2 + 2 + 2
								if objStrOffset+uintptr(objNameLen) <= uintptr(nameReturnLength) {
									objChars := objNameLen / 2
									objUtf16 := make([]uint16, objChars)
									for ci := uint16(0); ci < objChars; ci++ {
										off := objStrOffset + uintptr(ci)*2
										objUtf16[ci] = *(*uint16)(unsafe.Pointer(uintptr(unsafe.Pointer(&nameBuf[0])) + off))
									}
									objName := syscall.UTF16ToString(objUtf16)
									if strings.Contains(strings.ToLower(objName), "pipe") {
										pipeCount++
									}
								}
							}
						}
					}
				}
			}
		}
		windows.CloseHandle(dupHandle)
	}

	return pipeCount
}

func getWaitReasonName(reason uint32) string {
	if name, ok := threadWaitReasonNames[reason]; ok {
		return name
	}
	return fmt.Sprintf("Unknown(%d)", reason)
}

// ==================== WMI Helpers ====================

func getParentChain(pid uint32) []Win32Process {
	var chain []Win32Process
	currentPid := pid
	for {
		proc := getProcessByPid(currentPid)
		if proc == nil {
			break
		}
		chain = append(chain, *proc)
		if proc.ParentProcessId == 0 {
			break
		}
		currentPid = proc.ParentProcessId
	}
	for i, j := 0, len(chain)-1; i < j; i, j = i+1, j-1 {
		chain[i], chain[j] = chain[j], chain[i]
	}
	return chain
}

func getProcessByPid(pid uint32) *Win32Process {
	query := fmt.Sprintf("SELECT ProcessId, ParentProcessId, Name, ExecutablePath, CommandLine, CreationDate, HandleCount, ThreadCount FROM Win32_Process WHERE ProcessId=%d", pid)
	var procs []Win32Process
	if err := wmi.Query(query, &procs); err != nil || len(procs) == 0 {
		return nil
	}
	return &procs[0]
}

func getChildren(pid uint32) []Win32Process {
	query := fmt.Sprintf("SELECT ProcessId, ParentProcessId, Name, ExecutablePath, CommandLine, CreationDate, HandleCount, ThreadCount FROM Win32_Process WHERE ParentProcessId=%d", pid)
	var procs []Win32Process
	wmi.Query(query, &procs)
	return procs
}

// ==================== Utility ====================

func ptrStr(s *string) string {
	if s == nil {
		return "(N/A)"
	}
	return *s
}

func truncStr(s string, max int) string {
	if len(s) > max {
		return s[:max] + "..."
	}
	return s
}

func printColored(color, text string) {
	fmt.Printf("%s%s%s\n", color, text, colorReset)
}

func parseWmiDate(s string) time.Time {
	if len(s) < 14 {
		return time.Time{}
	}
	t, err := time.ParseInLocation("20060102150405", s[:14], time.Local)
	if err != nil {
		return time.Time{}
	}
	return t
}

func formatDuration(d time.Duration) string {
	if d < time.Minute {
		return fmt.Sprintf("%ds", int(d.Seconds()))
	}
	if d < time.Hour {
		return fmt.Sprintf("%dm%ds", int(d.Minutes()), int(d.Seconds())%60)
	}
	hours := int(d.Hours())
	mins := int(d.Minutes()) % 60
	if hours >= 24 {
		days := hours / 24
		hours = hours % 24
		return fmt.Sprintf("%dd%dh%dm", days, hours, mins)
	}
	return fmt.Sprintf("%dh%dm", hours, mins)
}

// ==================== Admin Elevation ====================

func isAdmin() bool {
	var sid *windows.SID
	err := windows.AllocateAndInitializeSid(
		&windows.SECURITY_NT_AUTHORITY, 2,
		windows.SECURITY_BUILTIN_DOMAIN_RID, windows.DOMAIN_ALIAS_RID_ADMINS,
		0, 0, 0, 0, 0, 0, &sid,
	)
	if err != nil {
		return false
	}
	defer windows.FreeSid(sid)
	member, err := windows.Token(0).IsMember(sid)
	return err == nil && member
}

func runAsAdmin() {
	exe, _ := os.Executable()
	args := strings.Join(os.Args[1:], " ")
	shell32 := syscall.NewLazyDLL("shell32.dll")
	shellExecute := shell32.NewProc("ShellExecuteW")
	verb, _ := syscall.UTF16PtrFromString("runas")
	file, _ := syscall.UTF16PtrFromString(exe)
	param, _ := syscall.UTF16PtrFromString(args)
	dir, _ := syscall.UTF16PtrFromString("")
	shellExecute.Call(0,
		uintptr(unsafe.Pointer(verb)),
		uintptr(unsafe.Pointer(file)),
		uintptr(unsafe.Pointer(param)),
		uintptr(unsafe.Pointer(dir)),
		syscall.SW_SHOWNORMAL,
	)
}

func enableVirtualTerminal() {
	kernel32 := syscall.NewLazyDLL("kernel32.dll")
	setConsoleMode := kernel32.NewProc("SetConsoleMode")
	getConsoleMode := kernel32.NewProc("GetConsoleMode")
	handle, _ := syscall.GetStdHandle(syscall.STD_OUTPUT_HANDLE)
	var mode uint32
	getConsoleMode.Call(uintptr(handle), uintptr(unsafe.Pointer(&mode)))
	mode |= 0x0004
	setConsoleMode.Call(uintptr(handle), uintptr(mode))
}

func waitExit() {
	fmt.Println()
	printColored(colorGreen, "Press Enter to exit...")
	bufio.NewReader(os.Stdin).ReadBytes('\n')
}
