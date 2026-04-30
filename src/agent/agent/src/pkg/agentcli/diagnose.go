package agentcli

import (
	"bytes"
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"

	"golang.org/x/net/http/httpproxy"
)

const (
	diagStatusOK  = "✓"
	diagStatusLow = "✗ LOW"
)

// printHealthChecks runs diagnostic checks and appends results to status output.
func printHealthChecks(workDir string) {
	fmt.Println()
	printStep(msg("Health Checks", "健康检查"))
	printStep("--------------------------------------------")

	checkDiskSpace(workDir)
	checkDiskWritable(workDir)

	gateway, _ := readProperty(workDir, "landun.gateway")
	fileGateway, _ := readProperty(workDir, "landun.fileGateway")
	httpProxy, _ := readProperty(workDir, "HTTP_PROXY")
	httpsProxy, _ := readProperty(workDir, "HTTPS_PROXY")
	noProxy, _ := readProperty(workDir, "NO_PROXY")
	certPath := filepath.Join(workDir, ".cert")

	tlsConfig := loadCertIfExists(certPath)
	proxyFunc := buildProxyFunc(httpProxy, httpsProxy, noProxy)

	if gateway != "" {
		fmt.Println()
		checkEndpoint(
			msg("Gateway", "网关"),
			gateway,
			"/ms/environment/api/buildAgent/agent/thirdPartyAgent/status",
			tlsConfig, proxyFunc, workDir,
		)
	}
	if fileGateway != "" && fileGateway != gateway {
		fmt.Println()
		checkEndpoint(
			msg("File gateway", "文件网关"),
			fileGateway, "/",
			tlsConfig, proxyFunc, workDir,
		)
	}

	fmt.Println()
	printCertStatus(certPath)

	printRecentErrorLogs(workDir)
}

func checkEndpoint(label, host, path string, tlsConfig *tls.Config, proxyFunc func(*http.Request) (*url.URL, error), workDir string) {
	baseURL := normalizeGateway(host)
	u, err := url.Parse(baseURL)
	if err != nil {
		statusLine("  "+label, msgf("FAIL: invalid URL %q ✗", "失败: 无效 URL %q ✗", host))
		return
	}
	printStep(fmt.Sprintf("  %s (%s):", label, u.Host))

	// Step 1: DNS
	start := time.Now()
	ips, err := net.LookupHost(u.Hostname())
	elapsed := time.Since(start)
	if err != nil {
		statusLine("    DNS resolve", msgf("FAIL: %v ✗", "失败: %v ✗", err))
		return
	}
	statusLine("    DNS resolve", fmt.Sprintf("%s (%v) ✓", strings.Join(ips, ", "), elapsed.Round(time.Millisecond)))

	// Step 2: TCP
	port := u.Port()
	if port == "" {
		if u.Scheme == "https" {
			port = "443"
		} else {
			port = "80"
		}
	}
	addr := net.JoinHostPort(ips[0], port)
	start = time.Now()
	conn, err := net.DialTimeout("tcp", addr, 5*time.Second)
	elapsed = time.Since(start)
	if err != nil {
		statusLine("    TCP connect", msgf("FAIL: %v ✗", "失败: %v ✗", err))
		return
	}
	conn.Close()
	statusLine("    TCP connect", fmt.Sprintf("%s (%v) ✓", addr, elapsed.Round(time.Millisecond)))

	// Step 3: TLS (if https)
	if u.Scheme == "https" {
		start = time.Now()
		dialer := &net.Dialer{Timeout: 5 * time.Second}
		tlsConn, err := tls.DialWithDialer(dialer, "tcp", u.Host, tlsConfig)
		elapsed = time.Since(start)
		if err != nil {
			statusLine("    TLS handshake", msgf("FAIL: %v ✗", "失败: %v ✗", err))
			return
		}
		state := tlsConn.ConnectionState()
		tlsConn.Close()
		statusLine("    TLS handshake", fmt.Sprintf("%s (%v) ✓",
			tlsVersionName(state.Version), elapsed.Round(time.Millisecond)))
	}

	// Step 4: HTTP request with proxy/cert config (same as agent runtime)
	transport := &http.Transport{
		TLSClientConfig: tlsConfig,
		Proxy:           proxyFunc,
	}
	httpClient := &http.Client{Transport: transport, Timeout: 10 * time.Second}

	fullURL := baseURL + path
	req, _ := http.NewRequest("GET", fullURL, nil)
	setAuthHeaders(req, workDir)

	start = time.Now()
	resp, err := httpClient.Do(req)
	elapsed = time.Since(start)
	if err != nil {
		statusLine("    HTTP GET", msgf("FAIL: %v ✗", "失败: %v ✗", err))
		return
	}
	resp.Body.Close()
	statusLine("    HTTP GET", fmt.Sprintf("%d %s (%v) ✓",
		resp.StatusCode, http.StatusText(resp.StatusCode), elapsed.Round(time.Millisecond)))

	// Proxy info
	proxyInfo := detectProxyUsed(u, proxyFunc)
	statusLine("    Proxy", proxyInfo)
}

func checkDiskWritable(workDir string) {
	testFile := filepath.Join(workDir, ".health_check_write_test")
	err := os.WriteFile(testFile, []byte("ok"), 0644)
	if err != nil {
		statusLine(msg("  Disk writable", "  磁盘可写"), msgf("FAIL: %v ✗", "失败: %v ✗", err))
		return
	}
	os.Remove(testFile)
	statusLine(msg("  Disk writable", "  磁盘可写"), msg("OK ✓", "正常 ✓"))
}

// ── helpers ──────────────────────────────────────────────────────────────

func normalizeGateway(gw string) string {
	if strings.HasPrefix(gw, "http://") || strings.HasPrefix(gw, "https://") {
		return gw
	}
	return "http://" + gw
}

func loadCertIfExists(certPath string) *tls.Config {
	data, err := os.ReadFile(certPath)
	if err != nil {
		return &tls.Config{}
	}
	pool, err := x509.SystemCertPool()
	if err != nil || pool == nil {
		pool = x509.NewCertPool()
	}
	pool.AppendCertsFromPEM(data)
	return &tls.Config{RootCAs: pool}
}

func buildProxyFunc(httpProxy, httpsProxy, noProxy string) func(*http.Request) (*url.URL, error) {
	if httpProxy == "" && httpsProxy == "" {
		return http.ProxyFromEnvironment
	}
	cfg := httpproxy.Config{
		HTTPProxy:  httpProxy,
		HTTPSProxy: httpsProxy,
		NoProxy:    noProxy,
	}
	pf := cfg.ProxyFunc()
	return func(req *http.Request) (*url.URL, error) {
		return pf(req.URL)
	}
}

func setAuthHeaders(req *http.Request, workDir string) {
	projectId, _ := readProperty(workDir, "devops.project.id")
	agentId, _ := readProperty(workDir, "devops.agent.id")
	secretKey, _ := readProperty(workDir, "devops.agent.secret.key")
	req.Header.Set("X-DEVOPS-PROJECT-ID", projectId)
	req.Header.Set("X-DEVOPS-AGENT-ID", agentId)
	req.Header.Set("X-DEVOPS-AGENT-SECRET-KEY", secretKey)
}

func detectProxyUsed(target *url.URL, proxyFunc func(*http.Request) (*url.URL, error)) string {
	req := &http.Request{URL: target}
	proxyURL, err := proxyFunc(req)
	if err != nil {
		return msgf("error: %v", "错误: %v", err)
	}
	if proxyURL == nil {
		return msg("direct (no proxy)", "直连 (无代理)")
	}
	return proxyURL.Redacted()
}

func printCertStatus(certPath string) {
	data, err := os.ReadFile(certPath)
	if err != nil {
		return
	}
	pool := x509.NewCertPool()
	ok := pool.AppendCertsFromPEM(data)
	if !ok {
		statusLine(msg("  Cert (.cert)", "  证书 (.cert)"), msg("FAIL: invalid PEM ✗", "失败: 无效 PEM ✗"))
		return
	}
	statusLine(msg("  Cert (.cert)", "  证书 (.cert)"), msg("loaded ✓", "已加载 ✓"))
}

func tlsVersionName(v uint16) string {
	switch v {
	case tls.VersionTLS10:
		return "TLS 1.0"
	case tls.VersionTLS11:
		return "TLS 1.1"
	case tls.VersionTLS12:
		return "TLS 1.2"
	case tls.VersionTLS13:
		return "TLS 1.3"
	default:
		return fmt.Sprintf("0x%04x", v)
	}
}

// ── recent error logs ────────────────────────────────────────────────────

// logLinePrefix 匹配 MyFormatter 生成的行首时间戳 + level。
// 例: "2026-04-24 10:00:00.123|error|..."。
var logLinePrefix = regexp.MustCompile(
	`^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\|([a-zA-Z]+)\|`)

// recentErrorLogWindow 定义 status 命令回看错误日志的时间窗口。
const recentErrorLogWindow = 2 * time.Hour

// maxErrorsPerFile 是每个日志文件最多输出的错误条数。
const maxErrorsPerFile = 5

// errorLogFiles 是 status 会扫描的日志文件名列表。顺序即输出顺序。
var errorLogFiles = []string{
	"devopsAgent.log",
	"devopsDaemon.log",
	"devopsUpgrader.log",
}

// printRecentErrorLogs 输出近 recentErrorLogWindow 内每个日志文件的错误条目
// （最多 maxErrorsPerFile 条），帮助用户通过 status 一次性定位问题。
func printRecentErrorLogs(workDir string) {
	fmt.Println()
	printStep(msgf("Recent errors (last %s, up to %d per file)",
		"近 %s 错误日志 (每文件最多 %d 条)",
		recentErrorLogWindow, maxErrorsPerFile))
	printStep("--------------------------------------------")

	logsDir := filepath.Join(workDir, "logs")
	cutoff := time.Now().Add(-recentErrorLogWindow)

	anyFound := false
	for _, name := range errorLogFiles {
		path := filepath.Join(logsDir, name)
		if _, err := os.Stat(path); os.IsNotExist(err) {
			statusLine("  "+name, msg("not found", "未找到"))
			continue
		} else if err != nil {
			statusLine("  "+name, msgf("stat failed: %v", "读取失败: %v", err))
			continue
		}

		errs, readErr := readRecentErrors(path, cutoff, maxErrorsPerFile)
		if readErr != nil {
			statusLine("  "+name, msgf("read failed: %v", "读取失败: %v", readErr))
			continue
		}
		if len(errs) == 0 {
			statusLine("  "+name, msg("no recent errors ✓", "无近期错误 ✓"))
			continue
		}
		anyFound = true
		statusLine("  "+name, msgf("%d error(s) ✗", "%d 条错误 ✗", len(errs)))
		for _, line := range errs {
			fmt.Printf("      %s\n", truncateLogLine(line, 500))
		}
	}

	if !anyFound {
		return
	}
	if currentStatusSummary != nil {
		currentStatusSummary.hasIssue = true
	}
}

// readRecentErrors 从 path 读取最近 cutoff 之后的 error/fatal/panic 行，
// 至多返回 max 条（按出现顺序：先于文件中更早出现的在前）。
//
// 实现策略：从文件尾部反向读，边读边匹配 MyFormatter 生成的
// "YYYY-MM-DD HH:MM:SS.mmm|level|" 前缀，命中 error/fatal/panic 且时间
// 晚于 cutoff 的行入队，凑够 max 条就停。最终按时间升序返回。
//
// 单行上限 1 MiB，超长行会被截断以防 OOM（日志里常见带堆栈的长 error）。
func readRecentErrors(path string, cutoff time.Time, max int) ([]string, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer f.Close()

	info, err := f.Stat()
	if err != nil {
		return nil, err
	}
	size := info.Size()
	if size == 0 {
		return nil, nil
	}

	// 反向按块扫描，保留跨块行的 "未完成前缀" 在下一块拼回。
	const chunk = int64(64 * 1024)
	const maxLineBytes = 1 << 20 // 单行 1 MiB 上限

	var (
		collected []string // 反向命中顺序（最新的在前）
		tail      []byte   // 当前未处理的末尾片段，在下一次前插
		offset    = size
		done      bool
	)

	for offset > 0 && !done {
		readSize := chunk
		if offset < readSize {
			readSize = offset
		}
		offset -= readSize
		buf := make([]byte, readSize)
		if _, err := f.ReadAt(buf, offset); err != nil && err != io.EOF {
			return nil, err
		}
		if len(tail) > 0 {
			buf = append(buf, tail...)
		}

		// 找到块内所有换行位置，从后向前切行。
		lineEnd := len(buf)
		for i := len(buf) - 1; i >= 0; i-- {
			if buf[i] != '\n' {
				continue
			}
			line := buf[i+1 : lineEnd]
			lineEnd = i
			if len(line) == 0 {
				continue
			}
			if hit, t := matchErrorLine(line); hit {
				if t.Before(cutoff) {
					// 再往前只会更旧，整体停扫。
					done = true
					break
				}
				if len(line) > maxLineBytes {
					line = line[:maxLineBytes]
				}
				collected = append(collected, string(bytes.TrimRight(line, "\r")))
				if len(collected) >= max {
					done = true
					break
				}
			}
		}
		// 块首未闭合的部分（不含已处理的换行）留到下一轮拼回。
		if lineEnd > 0 && offset > 0 {
			tail = make([]byte, lineEnd)
			copy(tail, buf[:lineEnd])
		} else if offset == 0 && lineEnd > 0 && !done {
			// 到文件开头，残留片段当作一整行处理。
			line := buf[:lineEnd]
			if hit, t := matchErrorLine(line); hit && !t.Before(cutoff) {
				if len(line) > maxLineBytes {
					line = line[:maxLineBytes]
				}
				collected = append(collected, string(bytes.TrimRight(line, "\r")))
			}
			tail = nil
		} else {
			tail = nil
		}
	}

	// collected 是逆序（新→旧），反转成时间升序（旧→新）输出更自然。
	for i, j := 0, len(collected)-1; i < j; i, j = i+1, j-1 {
		collected[i], collected[j] = collected[j], collected[i]
	}
	return collected, nil
}

// matchErrorLine 判断一行日志是否匹配 error/fatal/panic 级别，
// 并解析出时间戳（本地时区，与 logrus MyFormatter 一致）。
func matchErrorLine(line []byte) (bool, time.Time) {
	m := logLinePrefix.FindSubmatch(line)
	if m == nil {
		return false, time.Time{}
	}
	level := strings.ToLower(string(m[2]))
	switch level {
	case "error", "fatal", "panic":
		// 匹配
	default:
		return false, time.Time{}
	}
	t, err := time.ParseInLocation("2006-01-02 15:04:05.000", string(m[1]), time.Local)
	if err != nil {
		return false, time.Time{}
	}
	return true, t
}

// truncateLogLine 限制单行输出长度，避免终端被堆栈刷屏。
// 带堆栈的 error 行常见 2–5 KiB，截到 500 足够展示关键信息。
func truncateLogLine(s string, max int) string {
	if len(s) <= max {
		return s
	}
	return s[:max] + "..."
}
