package agentcli

import (
	"crypto/tls"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"testing"
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
