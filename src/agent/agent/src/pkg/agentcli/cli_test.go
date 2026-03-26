package agentcli

import (
	"os"
	"path/filepath"
	"runtime"
	"testing"
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
		{"configure-session", true},
		{"-h", true},
		{"--help", true},
		{"help", true},
		{"unknown", false},
		{"version", false},
		{"debug", false},
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
		"workspace",
		agentBinary(),
		installScriptName(),
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

func TestInstallScriptName(t *testing.T) {
	name := installScriptName()
	if runtime.GOOS == "windows" {
		if name != "download_install.ps1" {
			t.Errorf("installScriptName() = %q on windows, want download_install.ps1", name)
		}
	} else {
		if name != "install.sh" {
			t.Errorf("installScriptName() = %q on %s, want install.sh", name, runtime.GOOS)
		}
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
