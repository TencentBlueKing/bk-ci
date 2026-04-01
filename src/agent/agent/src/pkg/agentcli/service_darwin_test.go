//go:build darwin
// +build darwin

package agentcli

import (
	"fmt"
	"os"
	"os/user"
	"path/filepath"
	"strings"
	"testing"
)

func TestLaunchdDomain(t *testing.T) {
	domain := launchdDomain()
	u, _ := user.Current()
	if u == nil {
		t.Skip("cannot get current user")
	}
	if u.Uid == "0" {
		if domain != "system" {
			t.Errorf("launchdDomain() = %q for root, want \"system\"", domain)
		}
	} else {
		want := "user/" + u.Uid
		if domain != want {
			t.Errorf("launchdDomain() = %q, want %q", domain, want)
		}
	}
}

func TestServiceTarget(t *testing.T) {
	domain := launchdDomain()
	tests := []struct {
		name        string
		serviceName string
		want        string
	}{
		{"simple", "devops_agent_foo", domain + "/devops_agent_foo"},
		{"with_dash", "devops_agent_bar-baz", domain + "/devops_agent_bar-baz"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := serviceTarget(tt.serviceName)
			if got != tt.want {
				t.Errorf("serviceTarget(%q) = %q, want %q", tt.serviceName, got, tt.want)
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
	if u.Uid == "0" {
		if dir != "/Library/LaunchDaemons" {
			t.Errorf("plistDir() = %q for root, want /Library/LaunchDaemons", dir)
		}
	} else {
		home, _ := os.UserHomeDir()
		want := home + "/Library/LaunchAgents"
		if dir != want {
			t.Errorf("plistDir() = %q, want %q", dir, want)
		}
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

func TestWritePlist(t *testing.T) {
	tmpDir := t.TempDir()

	daemonPath := tmpDir + "/devopsDaemon"
	agentPath := tmpDir + "/devopsAgent"
	os.WriteFile(daemonPath, []byte("fake"), 0755)
	os.WriteFile(agentPath, []byte("fake"), 0755)

	plistDir := tmpDir + "/LaunchAgents"
	os.MkdirAll(plistDir, 0755)

	serviceName := "devops_agent_unittest"
	err := writePlist(tmpDir, serviceName)
	if err != nil {
		t.Fatalf("writePlist() error: %v", err)
	}

	pp := plistPath(serviceName)
	data, err := os.ReadFile(pp)
	if err != nil {
		t.Fatalf("cannot read plist at %s: %v", pp, err)
	}
	content := string(data)

	checks := []struct {
		label string
		value string
	}{
		{"Label", fmt.Sprintf("<string>%s</string>", serviceName)},
		{"Program", fmt.Sprintf("<string>%s</string>", daemonPath)},
		{"WorkingDirectory", fmt.Sprintf("<string>%s</string>", tmpDir)},
		{"RunAtLoad", "<true/>"},
		{"KeepAlive", "<false/>"},
	}
	for _, c := range checks {
		if !strings.Contains(content, c.value) {
			t.Errorf("plist missing %s: expected %q in content", c.label, c.value)
		}
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

func TestHandleStartLegacy_AlreadyRunning(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	workDir := t.TempDir()
	runtimeDir := filepath.Join(workDir, "runtime")
	os.MkdirAll(runtimeDir, 0755)

	pidFile := filepath.Join(runtimeDir, "daemon.pid")
	os.WriteFile(pidFile, []byte(fmt.Sprintf("%d", os.Getpid())), 0644)

	err := handleStartLegacy(workDir)
	if err != nil {
		t.Errorf("handleStartLegacy() with running PID returned error: %v", err)
	}
}

func TestHandleStopLegacy(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	workDir := t.TempDir()
	runtimeDir := filepath.Join(workDir, "runtime")
	os.MkdirAll(runtimeDir, 0755)

	os.WriteFile(filepath.Join(runtimeDir, "daemon.pid"), []byte("99999999"), 0644)
	os.WriteFile(filepath.Join(runtimeDir, "agent.pid"), []byte("99999999"), 0644)

	err := handleStopLegacy(workDir)
	if err != nil {
		t.Errorf("handleStopLegacy() returned error: %v", err)
	}
}
