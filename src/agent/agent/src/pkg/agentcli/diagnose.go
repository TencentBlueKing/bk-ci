package agentcli

import (
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"net"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"strings"
	"time"

	"golang.org/x/net/http/httpproxy"
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
}

func checkEndpoint(label, host, path string, tlsConfig *tls.Config, proxyFunc func(*http.Request) (*url.URL, error), workDir string) {
	baseURL := normalizeGateway(host)
	u, err := url.Parse(baseURL)
	if err != nil {
		statusLine("  "+label, fmt.Sprintf("FAIL: invalid URL %q ✗", host))
		return
	}
	printStep(fmt.Sprintf("  %s (%s):", label, u.Host))

	// Step 1: DNS
	start := time.Now()
	ips, err := net.LookupHost(u.Hostname())
	elapsed := time.Since(start)
	if err != nil {
		statusLine("    DNS resolve", fmt.Sprintf("FAIL: %v ✗", err))
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
		statusLine("    TCP connect", fmt.Sprintf("FAIL: %v ✗", err))
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
			statusLine("    TLS handshake", fmt.Sprintf("FAIL: %v ✗", err))
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
		statusLine("    HTTP GET", fmt.Sprintf("FAIL: %v ✗", err))
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
		statusLine(msg("  Disk writable", "  磁盘可写"), fmt.Sprintf("FAIL: %v ✗", err))
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
		return fmt.Sprintf("error: %v", err)
	}
	if proxyURL == nil {
		return msg("direct (no proxy)", "直连 (无代理)")
	}
	return proxyURL.Redacted()
}

func printCertStatus(certPath string) {
	data, err := os.ReadFile(certPath)
	if err != nil {
		statusLine(msg("  Cert (.cert)", "  证书 (.cert)"), msg("not configured", "未配置"))
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
