package agentcli

import (
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"

	"gopkg.in/ini.v1"
)

func TestIsSubcommand(t *testing.T) {
	tests := []struct {
		arg  string
		want bool
	}{
		{"install", true},
		{"uninstall", true},
		{"start", true},
		{"stop", true},
		{"repair", true},
		{"reinstall", true},
		{"status", true},
		{"configure-session", false},
		{"configure-service", false},
		{"-h", true},
		{"--help", true},
		{"help", true},
		{"version", true},
		{"debug", true},
		{"unknown", false},
		{"", false},
		{"INSTALL", false},
	}
	for _, tt := range tests {
		t.Run("arg_"+tt.arg, func(t *testing.T) {
			if got := IsSubcommand(tt.arg); got != tt.want {
				t.Errorf("IsSubcommand(%q) = %v, want %v", tt.arg, got, tt.want)
			}
		})
	}
}

func TestReadProperty(t *testing.T) {
	dir := t.TempDir()
	content := "devops.project.id=proj123\ndevops.agent.id=agent456\n# comment\ndevops.language=zh_CN\ndevops.empty=\n"
	if err := os.WriteFile(filepath.Join(dir, ".agent.properties"), []byte(content), 0644); err != nil {
		t.Fatal(err)
	}

	tests := []struct {
		key     string
		want    string
		wantErr bool
	}{
		{"devops.project.id", "proj123", false},
		{"devops.agent.id", "agent456", false},
		{"devops.language", "zh_CN", false},
		{"devops.empty", "", false},
		{"nonexistent", "", true},
	}
	for _, tt := range tests {
		t.Run(tt.key, func(t *testing.T) {
			got, err := readProperty(dir, tt.key)
			if tt.wantErr {
				if err == nil {
					t.Errorf("expected error for key %q", tt.key)
				}
				return
			}
			if err != nil {
				t.Fatalf("unexpected error: %v", err)
			}
			if got != tt.want {
				t.Errorf("readProperty(%q) = %q, want %q", tt.key, got, tt.want)
			}
		})
	}
}

func TestReadProperty_NoFile(t *testing.T) {
	dir := t.TempDir()
	_, err := readProperty(dir, "any.key")
	if err == nil {
		t.Error("expected error when .agent.properties does not exist")
	}
}

func TestReadProperty_CommentedLine(t *testing.T) {
	dir := t.TempDir()
	content := "# devops.key1=hidden\ndevops.key1=visible\n"
	os.WriteFile(filepath.Join(dir, ".agent.properties"), []byte(content), 0644)

	got, err := readProperty(dir, "devops.key1")
	if err != nil {
		t.Fatal(err)
	}
	if got != "visible" {
		t.Errorf("readProperty(key1) = %q, want %q", got, "visible")
	}
}

func TestReadProperty_ValueWithEquals(t *testing.T) {
	dir := t.TempDir()
	content := "devops.key=value=with=equals\n"
	os.WriteFile(filepath.Join(dir, ".agent.properties"), []byte(content), 0644)

	got, err := readProperty(dir, "devops.key")
	if err != nil {
		t.Fatal(err)
	}
	if got != "value=with=equals" {
		t.Errorf("readProperty(key) = %q, want %q", got, "value=with=equals")
	}
}

func TestGetServiceName(t *testing.T) {
	dir := t.TempDir()
	os.WriteFile(filepath.Join(dir, ".agent.properties"), []byte("devops.agent.id=abc123\n"), 0644)

	name, err := getServiceName(dir)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if name != "devops_agent_abc123" {
		t.Errorf("getServiceName() = %q, want %q", name, "devops_agent_abc123")
	}
}

func TestGetServiceName_NoProperties(t *testing.T) {
	dir := t.TempDir()
	_, err := getServiceName(dir)
	if err == nil {
		t.Error("expected error when .agent.properties is missing")
	}
}

func TestPreserveSet(t *testing.T) {
	ps := preserveSet()
	required := []string{
		".agent.properties",
		".install_type",
		".cert",
		".debug",
		".env",
		".path",
		"workspace",
		agentBinary(),
	}
	for _, name := range required {
		if !ps[name] {
			t.Errorf("preserveSet missing required entry %q", name)
		}
	}
	if len(ps) != len(required) {
		t.Errorf("preserveSet has %d entries, want %d", len(ps), len(required))
	}
}

func TestAgentBinary(t *testing.T) {
	name := agentBinary()
	if runtime.GOOS == "windows" {
		if name != "devopsAgent.exe" {
			t.Errorf("agentBinary() = %q, want devopsAgent.exe", name)
		}
	} else {
		if name != "devopsAgent" {
			t.Errorf("agentBinary() = %q, want devopsAgent", name)
		}
	}
}

func TestDaemonBinary(t *testing.T) {
	name := daemonBinary()
	if runtime.GOOS == "windows" {
		if name != "devopsDaemon.exe" {
			t.Errorf("daemonBinary() = %q, want devopsDaemon.exe", name)
		}
	} else {
		if name != "devopsDaemon" {
			t.Errorf("daemonBinary() = %q, want devopsDaemon", name)
		}
	}
}

func TestCleanupPreservesFiles(t *testing.T) {
	dir := t.TempDir()
	keep := preserveSet()

	for name := range keep {
		p := filepath.Join(dir, name)
		if name == "workspace" {
			os.MkdirAll(p, 0755)
			os.WriteFile(filepath.Join(p, "build.log"), []byte("data"), 0644)
		} else {
			os.WriteFile(p, []byte("keep"), 0644)
		}
	}

	deleteNames := []string{"devopsDaemon", "jdk17.zip", "jre.zip", "worker-agent.jar", "logs"}
	for _, name := range deleteNames {
		os.WriteFile(filepath.Join(dir, name), []byte("delete"), 0644)
	}
	os.MkdirAll(filepath.Join(dir, "jdk17"), 0755)
	os.MkdirAll(filepath.Join(dir, "runtime"), 0755)
	os.WriteFile(filepath.Join(dir, "runtime", "daemon.pid"), []byte("1"), 0644)

	entries, err := os.ReadDir(dir)
	if err != nil {
		t.Fatal(err)
	}
	for _, entry := range entries {
		name := entry.Name()
		if keep[name] {
			continue
		}
		os.RemoveAll(filepath.Join(dir, name))
	}

	for name := range keep {
		if _, err := os.Stat(filepath.Join(dir, name)); os.IsNotExist(err) {
			t.Errorf("preserved file %q was deleted", name)
		}
	}

	for _, name := range append(deleteNames, "jdk17", "runtime") {
		if _, err := os.Stat(filepath.Join(dir, name)); !os.IsNotExist(err) {
			t.Errorf("file %q should have been deleted", name)
		}
	}

	// workspace contents should survive
	if _, err := os.Stat(filepath.Join(dir, "workspace", "build.log")); os.IsNotExist(err) {
		t.Error("workspace/build.log should have been preserved")
	}
}

func TestUnzipIfNeeded_NoZip(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	dir := t.TempDir()
	// Should not panic when zip doesn't exist
	unzipIfNeeded(filepath.Join(dir, "nonexistent.zip"), filepath.Join(dir, "dest"), false)
}

func TestUnzipIfNeeded_DestExists(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	dir := t.TempDir()
	zipPath := filepath.Join(dir, "test.zip")
	destDir := filepath.Join(dir, "dest")

	os.WriteFile(zipPath, []byte("fake"), 0644)
	os.MkdirAll(destDir, 0755)

	// force=false: should skip if dest exists
	unzipIfNeeded(zipPath, destDir, false)
	// dest still exists (no error, no removal)
	if _, err := os.Stat(destDir); os.IsNotExist(err) {
		t.Error("dest should still exist when force=false")
	}
}

func TestHandleDebug(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	dir := t.TempDir()
	debugPath := filepath.Join(dir, ".debug")

	t.Run("status_off", func(t *testing.T) {
		err := handleDebug(dir, []string{})
		if err != nil {
			t.Fatal(err)
		}
	})

	t.Run("enable", func(t *testing.T) {
		err := handleDebug(dir, []string{"on"})
		if err != nil {
			t.Fatal(err)
		}
		if _, err := os.Stat(debugPath); os.IsNotExist(err) {
			t.Error(".debug file should exist after 'debug on'")
		}
	})

	t.Run("status_on", func(t *testing.T) {
		err := handleDebug(dir, []string{})
		if err != nil {
			t.Fatal(err)
		}
	})

	t.Run("disable", func(t *testing.T) {
		err := handleDebug(dir, []string{"off"})
		if err != nil {
			t.Fatal(err)
		}
		if _, err := os.Stat(debugPath); !os.IsNotExist(err) {
			t.Error(".debug file should not exist after 'debug off'")
		}
	})

	t.Run("invalid_action", func(t *testing.T) {
		err := handleDebug(dir, []string{"invalid"})
		if err == nil {
			t.Error("expected error for invalid debug action")
		}
	})
}

func TestHandleVersion(t *testing.T) {
	t.Run("plain", func(t *testing.T) {
		if err := handleVersion([]string{}); err != nil {
			t.Fatal(err)
		}
	})
	t.Run("full", func(t *testing.T) {
		if err := handleVersion([]string{"-f"}); err != nil {
			t.Fatal(err)
		}
	})
	t.Run("invalid_flag", func(t *testing.T) {
		if err := handleVersion([]string{"--bad"}); err == nil {
			t.Fatal("expected invalid flag error")
		}
	})
}

func TestAgentArch(t *testing.T) {
	arch := agentArch()
	switch runtime.GOARCH {
	case "arm64":
		if arch != "arm64" {
			t.Fatalf("agentArch()=%q on arm64, want arm64", arch)
		}
	case "mips64", "mips64le":
		if arch != "mips64" {
			t.Fatalf("agentArch()=%q on mips64, want mips64", arch)
		}
	default:
		if arch != "" {
			t.Fatalf("agentArch()=%q on %s, want empty", arch, runtime.GOARCH)
		}
	}
}

func TestDebugFileExists(t *testing.T) {
	dir := t.TempDir()

	if DebugFileExists(dir) {
		t.Error("DebugFileExists should return false when .debug doesn't exist")
	}

	os.WriteFile(filepath.Join(dir, ".debug"), []byte("1"), 0644)
	if !DebugFileExists(dir) {
		t.Error("DebugFileExists should return true when .debug exists")
	}
}

func TestParsePropertiesFile(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	t.Run("missing_file", func(t *testing.T) {
		status, conf := parsePropertiesFile("/tmp/nonexistent_agentcli_test_42/.agent.properties")
		if conf != nil {
			t.Error("expected nil conf for missing file")
		}
		if !strings.Contains(status, "MISSING") {
			t.Errorf("status = %q, expected MISSING", status)
		}
	})

	t.Run("is_directory", func(t *testing.T) {
		dir := t.TempDir()
		status, conf := parsePropertiesFile(dir)
		if conf != nil {
			t.Error("expected nil conf for directory")
		}
		if !strings.Contains(status, "directory") {
			t.Errorf("status = %q, expected directory error", status)
		}
	})

	t.Run("invalid_ini", func(t *testing.T) {
		dir := t.TempDir()
		path := filepath.Join(dir, ".agent.properties")
		os.WriteFile(path, []byte("[invalid section\n"), 0644)
		status, conf := parsePropertiesFile(path)
		if conf != nil {
			t.Error("expected nil conf for invalid INI")
		}
		if !strings.Contains(status, "PARSE ERROR") {
			t.Errorf("status = %q, expected PARSE ERROR", status)
		}
	})

	t.Run("valid_file", func(t *testing.T) {
		dir := t.TempDir()
		path := filepath.Join(dir, ".agent.properties")
		content := "devops.project.id=proj1\ndevops.agent.id=agent1\n"
		os.WriteFile(path, []byte(content), 0644)
		status, conf := parsePropertiesFile(path)
		if conf == nil {
			t.Fatal("expected non-nil conf for valid file")
		}
		if !strings.Contains(status, "OK") {
			t.Errorf("status = %q, expected OK", status)
		}
		if !strings.Contains(status, fmt.Sprintf("%d bytes", len(content))) {
			t.Errorf("status = %q, expected byte count", status)
		}
	})
}

func TestRequiredKeyStatus(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	content := []byte("devops.project.id=proj1\ndevops.agent.secret.key=s3cret\ndevops.empty=\n")
	conf, err := ini.Load(content)
	if err != nil {
		t.Fatal(err)
	}

	tests := []struct {
		name     string
		key      string
		mask     bool
		wantSub  string
		wantFail bool
	}{
		{"present_unmasked", "devops.project.id", false, "proj1", false},
		{"present_masked", "devops.agent.secret.key", true, "configured", false},
		{"empty_value", "devops.empty", false, "missing or empty", true},
		{"missing_key", "nonexistent.key", false, "missing or empty", true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := requiredKeyStatus(conf, tt.key, tt.mask)
			if !strings.Contains(got, tt.wantSub) {
				t.Errorf("requiredKeyStatus(%q, mask=%v) = %q, want substring %q", tt.key, tt.mask, got, tt.wantSub)
			}
			hasCross := strings.Contains(got, "✗")
			if tt.wantFail && !hasCross {
				t.Errorf("requiredKeyStatus(%q) = %q, expected ✗", tt.key, got)
			}
			if !tt.wantFail && hasCross {
				t.Errorf("requiredKeyStatus(%q) = %q, unexpected ✗", tt.key, got)
			}
		})
	}
}

func TestIntKeyStatus(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	content := []byte("devops.parallel.task.count=4\nbad.count=abc\nneg.count=-1\nzero.count=0\n")
	conf, err := ini.Load(content)
	if err != nil {
		t.Fatal(err)
	}

	tests := []struct {
		name     string
		key      string
		minVal   int
		wantSub  string
		wantFail bool
	}{
		{"valid", "devops.parallel.task.count", 0, "4", false},
		{"missing", "nonexistent.key", 0, "missing", true},
		{"not_a_number", "bad.count", 0, "invalid number", true},
		{"negative_below_min", "neg.count", 0, "too small", true},
		{"zero_at_min", "zero.count", 0, "0", false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := intKeyStatus(conf, tt.key, tt.minVal)
			if !strings.Contains(got, tt.wantSub) {
				t.Errorf("intKeyStatus(%q, %d) = %q, want substring %q", tt.key, tt.minVal, got, tt.wantSub)
			}
			hasCross := strings.Contains(got, "✗")
			if tt.wantFail && !hasCross {
				t.Errorf("intKeyStatus(%q) = %q, expected ✗", tt.key, got)
			}
			if !tt.wantFail && hasCross {
				t.Errorf("intKeyStatus(%q) = %q, unexpected ✗", tt.key, got)
			}
		})
	}
}

func TestParsePropertiesFile_Chinese(t *testing.T) {
	old := useChinese
	useChinese = true
	defer func() { useChinese = old }()

	t.Run("missing", func(t *testing.T) {
		status, _ := parsePropertiesFile("/tmp/nonexistent_agentcli_test_42/.agent.properties")
		if !strings.Contains(status, "缺失") {
			t.Errorf("status = %q, expected 缺失", status)
		}
	})

	t.Run("valid", func(t *testing.T) {
		dir := t.TempDir()
		path := filepath.Join(dir, ".agent.properties")
		os.WriteFile(path, []byte("k=v\n"), 0644)
		status, _ := parsePropertiesFile(path)
		if !strings.Contains(status, "正常") {
			t.Errorf("status = %q, expected 正常", status)
		}
	})
}

func TestHandleInstall_AcceptsArgs(t *testing.T) {
	// Use "background" so that readInstallMode (defaults to LOGIN)
	// differs from the requested mode, avoiding the "already installed"
	// short-circuit. This forces the function to reach getServiceName,
	// which fails because .agent.properties is missing.
	dir := t.TempDir()
	err := handleInstall(dir, []string{"background"})
	if err == nil {
		t.Error("handleInstall with empty dir should return error")
	}
}

func TestStatusValueHasIssue(t *testing.T) {
	tests := []struct {
		name  string
		label string
		value string
		want  bool
	}{
		{"ok_value", "JDK 17", "OK ✓", false},
		{"fail_marker", "Disk writable", "FAIL: permission denied ✗", true},
		{"warning_marker", "Scheduled task", "exists (legacy) ⚠", true},
		{"daemon_not_running", "Daemon PID", "not running", true},
		{"daemon_running", "Daemon PID", "1234 (running)", false},
		{"session_not_configured_not_counted", "Session credentials", "not configured", false},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := statusValueHasIssue(tt.label, tt.value); got != tt.want {
				t.Errorf("statusValueHasIssue(%q, %q) = %v, want %v", tt.label, tt.value, got, tt.want)
			}
		})
	}
}
