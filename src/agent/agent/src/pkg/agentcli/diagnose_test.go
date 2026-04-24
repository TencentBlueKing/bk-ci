package agentcli

import (
	"crypto/tls"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

func TestNormalizeGateway(t *testing.T) {
	tests := []struct {
		input string
		want  string
	}{
		{"devops.example.com", "http://devops.example.com"},
		{"http://devops.example.com", "http://devops.example.com"},
		{"https://devops.example.com", "https://devops.example.com"},
		{"http://devops.example.com:8080", "http://devops.example.com:8080"},
	}
	for _, tt := range tests {
		t.Run(tt.input, func(t *testing.T) {
			if got := normalizeGateway(tt.input); got != tt.want {
				t.Errorf("normalizeGateway(%q) = %q, want %q", tt.input, got, tt.want)
			}
		})
	}
}

func TestBuildProxyFunc_NoProxy(t *testing.T) {
	pf := buildProxyFunc("", "", "")
	req := &http.Request{URL: &url.URL{Scheme: "http", Host: "example.com"}}
	proxyURL, err := pf(req)
	if err != nil {
		t.Fatal(err)
	}
	// When no proxy env is set, should return nil (direct)
	_ = proxyURL
}

func TestBuildProxyFunc_WithProxy(t *testing.T) {
	pf := buildProxyFunc("http://proxy:8080", "", "")
	req := &http.Request{URL: &url.URL{Scheme: "http", Host: "example.com"}}
	proxyURL, err := pf(req)
	if err != nil {
		t.Fatal(err)
	}
	if proxyURL == nil {
		t.Error("expected proxy URL, got nil")
	} else if proxyURL.Host != "proxy:8080" {
		t.Errorf("proxy host = %q, want %q", proxyURL.Host, "proxy:8080")
	}
}

func TestBuildProxyFunc_NoProxyExclusion(t *testing.T) {
	pf := buildProxyFunc("http://proxy:8080", "", "example.com")
	req := &http.Request{URL: &url.URL{Scheme: "http", Host: "example.com"}}
	proxyURL, err := pf(req)
	if err != nil {
		t.Fatal(err)
	}
	if proxyURL != nil {
		t.Errorf("expected nil (no proxy for excluded host), got %v", proxyURL)
	}
}

func TestLoadCertIfExists_NoCert(t *testing.T) {
	cfg := loadCertIfExists("/nonexistent/cert")
	if cfg == nil {
		t.Fatal("expected non-nil TLS config")
	}
}

func TestLoadCertIfExists_InvalidPEM(t *testing.T) {
	dir := t.TempDir()
	certFile := filepath.Join(dir, ".cert")
	os.WriteFile(certFile, []byte("not a valid PEM"), 0644)
	cfg := loadCertIfExists(certFile)
	if cfg == nil {
		t.Fatal("expected non-nil TLS config even for invalid PEM")
	}
}

func TestDetectProxyUsed_Direct(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	pf := func(r *http.Request) (*url.URL, error) { return nil, nil }
	target := &url.URL{Scheme: "http", Host: "example.com"}
	got := detectProxyUsed(target, pf)
	if got != "direct (no proxy)" {
		t.Errorf("detectProxyUsed = %q, want 'direct (no proxy)'", got)
	}
}

func TestTlsVersionName(t *testing.T) {
	tests := []struct {
		v    uint16
		want string
	}{
		{tls.VersionTLS12, "TLS 1.2"},
		{tls.VersionTLS13, "TLS 1.3"},
		{0x0999, "0x0999"},
	}
	for _, tt := range tests {
		if got := tlsVersionName(tt.v); got != tt.want {
			t.Errorf("tlsVersionName(%#x) = %q, want %q", tt.v, got, tt.want)
		}
	}
}

func TestCheckDiskWritable(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	dir := t.TempDir()
	// Should not panic, should print OK
	checkDiskWritable(dir)

	// Verify temp file is cleaned up
	testFile := filepath.Join(dir, ".health_check_write_test")
	if _, err := os.Stat(testFile); !os.IsNotExist(err) {
		t.Error("write test file should be cleaned up")
	}
}

func TestCheckDiskSpace(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	dir := t.TempDir()
	// Should not panic
	checkDiskSpace(dir)
}

func TestReadRecentErrors(t *testing.T) {
	now := time.Now()
	fmtTs := func(t time.Time) string {
		return t.Format("2006-01-02 15:04:05.000")
	}

	// 构造日志内容：info/debug 应忽略，error/fatal/panic 应命中，
	// 超过 cutoff 的 error 应被跳过，命中按时间升序返回。
	oldErr := fmtTs(now.Add(-3*time.Hour)) + "|error|too old should be skipped"
	info := fmtTs(now.Add(-30*time.Minute)) + "|info|not an error"
	err1 := fmtTs(now.Add(-20*time.Minute)) + "|error|first recent error"
	warn := fmtTs(now.Add(-15*time.Minute)) + "|warning|just a warning"
	fatal := fmtTs(now.Add(-10*time.Minute)) + "|fatal|boom"
	err2 := fmtTs(now.Add(-5 * time.Minute)) + "|error|second recent error"

	content := strings.Join([]string{oldErr, info, err1, warn, fatal, err2}, "\n") + "\n"
	path := filepath.Join(t.TempDir(), "devopsAgent.log")
	if err := os.WriteFile(path, []byte(content), 0644); err != nil {
		t.Fatal(err)
	}

	cutoff := now.Add(-2 * time.Hour)
	got, err := readRecentErrors(path, cutoff, 5)
	if err != nil {
		t.Fatalf("readRecentErrors: %v", err)
	}

	wantCount := 3
	if len(got) != wantCount {
		t.Fatalf("got %d lines, want %d: %v", len(got), wantCount, got)
	}
	// 期望时间升序
	if !strings.Contains(got[0], "first recent error") {
		t.Errorf("got[0] should be first recent error, got: %s", got[0])
	}
	if !strings.Contains(got[1], "boom") {
		t.Errorf("got[1] should be fatal 'boom', got: %s", got[1])
	}
	if !strings.Contains(got[2], "second recent error") {
		t.Errorf("got[2] should be second recent error, got: %s", got[2])
	}

	// max 限制生效
	limited, err := readRecentErrors(path, cutoff, 2)
	if err != nil {
		t.Fatalf("readRecentErrors limited: %v", err)
	}
	if len(limited) != 2 {
		t.Errorf("max=2 should return 2 lines, got %d", len(limited))
	}
	// 应该是最新的 2 条，按升序: fatal, err2
	if !strings.Contains(limited[1], "second recent error") {
		t.Errorf("limited[1] should be latest error, got: %s", limited[1])
	}
}

func TestReadRecentErrors_EmptyFile(t *testing.T) {
	path := filepath.Join(t.TempDir(), "devopsAgent.log")
	if err := os.WriteFile(path, nil, 0644); err != nil {
		t.Fatal(err)
	}
	got, err := readRecentErrors(path, time.Now().Add(-2*time.Hour), 5)
	if err != nil {
		t.Fatalf("readRecentErrors: %v", err)
	}
	if len(got) != 0 {
		t.Errorf("empty file should return no errors, got %v", got)
	}
}

func TestMatchErrorLine(t *testing.T) {
	tests := []struct {
		line    string
		wantHit bool
	}{
		{"2026-04-24 10:00:00.123|error|boom", true},
		{"2026-04-24 10:00:00.123|ERROR|upper-case level also matches", true},
		{"2026-04-24 10:00:00.123|fatal|fatal is considered an error", true},
		{"2026-04-24 10:00:00.123|panic|panic is considered an error", true},
		{"2026-04-24 10:00:00.123|info|not an error", false},
		{"2026-04-24 10:00:00.123|warning|also not an error", false},
		{"no timestamp prefix", false},
		{"", false},
	}
	for _, tt := range tests {
		hit, _ := matchErrorLine([]byte(tt.line))
		if hit != tt.wantHit {
			t.Errorf("matchErrorLine(%q) hit=%v, want %v", tt.line, hit, tt.wantHit)
		}
	}
}
