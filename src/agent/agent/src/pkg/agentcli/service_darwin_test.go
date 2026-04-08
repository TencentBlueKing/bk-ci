//go:build darwin
// +build darwin

package agentcli

import (
	"os"
	"os/user"
	"path/filepath"
	"strings"
	"testing"
)

func TestLaunchdDomain(t *testing.T) {
	u, _ := user.Current()
	if u == nil {
		t.Skip("cannot get current user")
	}

	tests := []struct {
		name string
		mode string
		want string
	}{
		{"login_mode", modeLogin, "gui/" + u.Uid},
		{"background_mode", modeBackground, "user/" + u.Uid},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := launchdDomain(tt.mode)
			if got != tt.want {
				t.Errorf("launchdDomain(%q) = %q, want %q", tt.mode, got, tt.want)
			}
		})
	}
}

func TestServiceTarget(t *testing.T) {
	tests := []struct {
		name        string
		serviceName string
		mode        string
	}{
		{"login", "devops_agent_foo", modeLogin},
		{"background", "devops_agent_foo", modeBackground},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := serviceTarget(tt.serviceName, tt.mode)
			want := launchdDomain(tt.mode) + "/" + tt.serviceName
			if got != want {
				t.Errorf("serviceTarget(%q, %q) = %q, want %q", tt.serviceName, tt.mode, got, want)
			}
		})
	}
}

func TestPlistDir(t *testing.T) {
	dir := plistDir()
	u, _ := user.Current()
	if u == nil {
		t.Skip("cannot get current user")
	}
	home, _ := os.UserHomeDir()
	want := home + "/Library/LaunchAgents"
	if dir != want {
		t.Errorf("plistDir() = %q, want %q", dir, want)
	}
}

func TestPlistPath(t *testing.T) {
	path := plistPath("devops_agent_test")
	dir := plistDir()
	want := dir + "/devops_agent_test.plist"
	if path != want {
		t.Errorf("plistPath() = %q, want %q", path, want)
	}
}

func TestWritePlist_LoginMode(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	tmpDir := t.TempDir()
	os.WriteFile(filepath.Join(tmpDir, "devopsDaemon"), []byte("fake"), 0755)
	os.WriteFile(filepath.Join(tmpDir, "devopsAgent"), []byte("fake"), 0755)

	serviceName := "devops_agent_unittest"
	err := writePlist(tmpDir, serviceName, modeLogin)
	if err != nil {
		t.Fatalf("writePlist(LOGIN) error: %v", err)
	}

	pp := plistPath(serviceName)
	data, err := os.ReadFile(pp)
	if err != nil {
		t.Fatalf("cannot read plist: %v", err)
	}
	content := string(data)

	if !strings.Contains(content, "<string>"+serviceName+"</string>") {
		t.Error("plist missing Label")
	}
	if !strings.Contains(content, "<true/>") {
		t.Error("plist missing RunAtLoad")
	}
	if strings.Contains(content, "LimitLoadToSessionType") {
		t.Error("LOGIN mode plist should NOT contain LimitLoadToSessionType")
	}
}

func TestWritePlist_BackgroundMode(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	tmpDir := t.TempDir()
	os.WriteFile(filepath.Join(tmpDir, "devopsDaemon"), []byte("fake"), 0755)
	os.WriteFile(filepath.Join(tmpDir, "devopsAgent"), []byte("fake"), 0755)

	serviceName := "devops_agent_unittest_bg"
	err := writePlist(tmpDir, serviceName, modeBackground)
	if err != nil {
		t.Fatalf("writePlist(BACKGROUND) error: %v", err)
	}

	pp := plistPath(serviceName)
	data, err := os.ReadFile(pp)
	if err != nil {
		t.Fatalf("cannot read plist: %v", err)
	}
	content := string(data)

	if !strings.Contains(content, "LimitLoadToSessionType") {
		t.Error("BACKGROUND mode plist should contain LimitLoadToSessionType")
	}
	if !strings.Contains(content, "<string>Background</string>") {
		t.Error("BACKGROUND mode plist should have Background session type")
	}
}

func TestReadInstallMode(t *testing.T) {
	tests := []struct {
		name    string
		content string
		want    string
	}{
		{"no_file", "", modeLogin},
		{"login_explicit", "LOGIN", modeLogin},
		{"background", "BACKGROUND", modeBackground},
		{"background_lowercase", "background", modeBackground},
		{"unknown_value", "SOMETHING", modeLogin},
		{"with_whitespace", "  BACKGROUND  \n", modeBackground},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			dir := t.TempDir()
			if tt.content != "" {
				os.WriteFile(filepath.Join(dir, installTypeFile), []byte(tt.content), 0644)
			}
			got := readInstallMode(dir)
			if got != tt.want {
				t.Errorf("readInstallMode() = %q, want %q", got, tt.want)
			}
		})
	}
}

func TestWriteInstallType(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	dir := t.TempDir()
	writeInstallType(dir, modeBackground)

	data, err := os.ReadFile(filepath.Join(dir, installTypeFile))
	if err != nil {
		t.Fatalf("failed to read .install_type: %v", err)
	}
	if string(data) != modeBackground {
		t.Errorf("got %q, want %q", string(data), modeBackground)
	}

	writeInstallType(dir, modeLogin)
	data, _ = os.ReadFile(filepath.Join(dir, installTypeFile))
	if string(data) != modeLogin {
		t.Errorf("got %q, want %q", string(data), modeLogin)
	}
}

func TestIsProcessAlive(t *testing.T) {
	t.Run("current_process", func(t *testing.T) {
		if !isProcessAlive(os.Getpid()) {
			t.Error("isProcessAlive(os.Getpid()) = false, want true")
		}
	})
	t.Run("nonexistent_pid", func(t *testing.T) {
		if isProcessAlive(99999999) {
			t.Error("isProcessAlive(99999999) = true, want false")
		}
	})
}

func TestStartByMode_NoPlist(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	workDir := t.TempDir()
	// startByMode without a plist should fall back to startDirect,
	// which will fail because there's no real daemon binary — that's expected.
	err := startByMode(workDir, "devops_agent_noplist", modeLogin)
	if err == nil {
		t.Error("startByMode() with no plist and no daemon binary should return error")
	}
}

func TestStopByMode_Login(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	workDir := t.TempDir()
	runtimeDir := filepath.Join(workDir, "runtime")
	os.MkdirAll(runtimeDir, 0755)
	os.WriteFile(filepath.Join(runtimeDir, "daemon.pid"), []byte("99999999"), 0644)
	os.WriteFile(filepath.Join(runtimeDir, "agent.pid"), []byte("99999999"), 0644)

	err := stopByMode(workDir, "devops_agent_test", modeLogin)
	if err != nil {
		t.Errorf("stopByMode(LOGIN) returned error: %v", err)
	}
}

func TestCurrentUID(t *testing.T) {
	uid := currentUID()
	if uid == "" {
		t.Error("currentUID() returned empty string")
	}
	u, _ := user.Current()
	if u != nil && uid != u.Uid {
		t.Errorf("currentUID() = %q, want %q", uid, u.Uid)
	}
}

func TestHasModernLaunchctl(t *testing.T) {
	old := cachedModernLaunchctl
	defer func() { cachedModernLaunchctl = old }()

	t.Run("cache_hit", func(t *testing.T) {
		cachedModernLaunchctl = 1
		if !hasModernLaunchctl() {
			t.Error("cached=1 should return true")
		}
		cachedModernLaunchctl = 0
		if hasModernLaunchctl() {
			t.Error("cached=0 should return false")
		}
	})

	t.Run("probe_runs_on_real_system", func(t *testing.T) {
		cachedModernLaunchctl = -1
		// On any real macOS test environment (10.10+), this should return true.
		// We just verify it doesn't panic or hang.
		_ = hasModernLaunchctl()
	})
}
