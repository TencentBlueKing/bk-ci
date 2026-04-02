//go:build linux
// +build linux

package agentcli

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestReadInstallMode(t *testing.T) {
	tests := []struct {
		name    string
		content string
		want    string
	}{
		{"service_upper", "SERVICE", modeService},
		{"service_lower", "service", modeService},
		{"service_mixed", "Service", modeService},
		{"user_upper", "USER", modeUser},
		{"user_lower", "user", modeUser},
		{"direct_upper", "DIRECT", modeDirect},
		{"direct_lower", "direct", modeDirect},
		{"empty", "", modeDirect},
		{"whitespace", "  SERVICE  \n", modeService},
		{"unknown", "TASK", modeDirect},
		{"garbage", "xyz123", modeDirect},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			dir := t.TempDir()
			if tt.content != "" || tt.name == "empty" {
				os.WriteFile(filepath.Join(dir, installTypeFile), []byte(tt.content), 0644)
			}
			got := readInstallMode(dir)
			if got != tt.want {
				t.Errorf("readInstallMode(%q) = %q, want %q", tt.content, got, tt.want)
			}
		})
	}

	t.Run("missing_file", func(t *testing.T) {
		dir := t.TempDir()
		got := readInstallMode(dir)
		if got != modeDirect {
			t.Errorf("readInstallMode(missing) = %q, want %q", got, modeDirect)
		}
	})
}

func TestWriteInstallType(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	modes := []string{modeService, modeUser, modeDirect}
	for _, mode := range modes {
		t.Run(mode, func(t *testing.T) {
			dir := t.TempDir()
			writeInstallType(dir, mode)

			got := readInstallMode(dir)
			if got != mode {
				t.Errorf("writeInstallType(%q) then readInstallMode() = %q, want %q", mode, got, mode)
			}

			data, err := os.ReadFile(filepath.Join(dir, installTypeFile))
			if err != nil {
				t.Fatalf("read install type file: %v", err)
			}
			if string(data) != mode {
				t.Errorf("file content = %q, want %q", string(data), mode)
			}
		})
	}
}

func TestHasLinger(t *testing.T) {
	origLingerDir := lingerDir
	defer func() { lingerDir = origLingerDir }()

	t.Run("linger_enabled", func(t *testing.T) {
		dir := t.TempDir()
		lingerDir = dir

		username := currentUser()
		if username == "" || username == "unknown" {
			username = os.Getenv("USER")
		}
		if username == "" {
			t.Skip("cannot determine current username")
		}

		os.WriteFile(filepath.Join(dir, username), []byte{}, 0644)
		if !hasLinger() {
			t.Error("hasLinger() = false, want true when linger file exists")
		}
	})

	t.Run("linger_disabled", func(t *testing.T) {
		dir := t.TempDir()
		lingerDir = dir

		if hasLinger() {
			t.Error("hasLinger() = true, want false when no linger file")
		}
	})

	t.Run("linger_dir_missing", func(t *testing.T) {
		lingerDir = "/tmp/nonexistent_linger_test_dir_42"

		if hasLinger() {
			t.Error("hasLinger() = true, want false when linger dir missing")
		}
	})
}

func TestUserSystemdUnitPath(t *testing.T) {
	path := userSystemdUnitPath("devops_agent_test123")
	if !strings.HasSuffix(path, "devops_agent_test123.service") {
		t.Errorf("userSystemdUnitPath() = %q, expected to end with devops_agent_test123.service", path)
	}
	if !strings.Contains(path, ".config/systemd/user") {
		t.Errorf("userSystemdUnitPath() = %q, expected to contain .config/systemd/user", path)
	}
}

func TestUserSystemdUnitDir(t *testing.T) {
	dir := userSystemdUnitDir()
	if !strings.HasSuffix(dir, ".config/systemd/user") {
		t.Errorf("userSystemdUnitDir() = %q, expected to end with .config/systemd/user", dir)
	}
}

func TestSystemdUnitPath(t *testing.T) {
	path := systemdUnitPath("devops_agent_abc")
	want := "/etc/systemd/system/devops_agent_abc.service"
	if path != want {
		t.Errorf("systemdUnitPath() = %q, want %q", path, want)
	}
}

func TestIsProcessAlive(t *testing.T) {
	t.Run("self_process", func(t *testing.T) {
		if !isProcessAlive(os.Getpid()) {
			t.Error("isProcessAlive(self) = false, want true")
		}
	})

	t.Run("nonexistent", func(t *testing.T) {
		if isProcessAlive(99999999) {
			t.Error("isProcessAlive(99999999) = true, want false")
		}
	})
}

func TestInstallModeConstants(t *testing.T) {
	if modeService != "SERVICE" {
		t.Errorf("modeService = %q, want SERVICE", modeService)
	}
	if modeUser != "USER" {
		t.Errorf("modeUser = %q, want USER", modeUser)
	}
	if modeDirect != "DIRECT" {
		t.Errorf("modeDirect = %q, want DIRECT", modeDirect)
	}
	if installTypeFile != ".install_type" {
		t.Errorf("installTypeFile = %q, want .install_type", installTypeFile)
	}
}

func TestCleanupBeforeInstall(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	t.Run("no_install_type_file", func(t *testing.T) {
		dir := t.TempDir()
		os.WriteFile(filepath.Join(dir, ".agent.properties"), []byte("devops.agent.id=test1\n"), 0644)
		cleanupBeforeInstall(dir, modeDirect)
		if _, err := os.Stat(filepath.Join(dir, installTypeFile)); err == nil {
			t.Error(".install_type should not exist after cleanup without prior install")
		}
	})

	t.Run("with_install_type_file", func(t *testing.T) {
		dir := t.TempDir()
		os.WriteFile(filepath.Join(dir, ".agent.properties"), []byte("devops.agent.id=test2\n"), 0644)
		os.WriteFile(filepath.Join(dir, installTypeFile), []byte("SERVICE"), 0644)
		cleanupBeforeInstall(dir, modeUser)
		if _, err := os.Stat(filepath.Join(dir, installTypeFile)); err == nil {
			t.Error(".install_type should be deleted after cleanup (uninstall removes it)")
		}
	})
}
