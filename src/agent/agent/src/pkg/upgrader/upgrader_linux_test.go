//go:build linux

package upgrader

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

func TestCheckUpgradeFileChange(t *testing.T) {
	logs.UNTestDebugInit()

	origDir, err := os.Getwd()
	if err != nil {
		t.Fatal(err)
	}
	defer os.Chdir(origDir)

	tests := []struct {
		name       string
		oldContent string
		newContent string
		wantChange bool
		wantErr    bool
	}{
		{
			name:       "different_content",
			oldContent: "binary v1",
			newContent: "binary v2",
			wantChange: true,
		},
		{
			name:       "same_content",
			oldContent: "identical",
			newContent: "identical",
			wantChange: false,
		},
		{
			name:       "empty_vs_content",
			oldContent: "",
			newContent: "new content",
			wantChange: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			tmpDir := t.TempDir()
			if err := os.Chdir(tmpDir); err != nil {
				t.Fatal(err)
			}

			upgradeDir := filepath.Join(tmpDir, "tmp")
			if err := os.MkdirAll(upgradeDir, 0755); err != nil {
				t.Fatal(err)
			}

			fileName := "test_binary"
			if err := os.WriteFile(filepath.Join(tmpDir, fileName), []byte(tt.oldContent), 0644); err != nil {
				t.Fatal(err)
			}
			if err := os.WriteFile(filepath.Join(upgradeDir, fileName), []byte(tt.newContent), 0644); err != nil {
				t.Fatal(err)
			}

			changed, err := checkUpgradeFileChange(fileName)
			if (err != nil) != tt.wantErr {
				t.Fatalf("checkUpgradeFileChange() error = %v, wantErr %v", err, tt.wantErr)
			}
			if changed != tt.wantChange {
				t.Errorf("checkUpgradeFileChange() = %v, want %v", changed, tt.wantChange)
			}
		})
	}
}

func TestCheckUpgradeFileChange_MissingFiles(t *testing.T) {
	logs.UNTestDebugInit()

	origDir, err := os.Getwd()
	if err != nil {
		t.Fatal(err)
	}
	defer os.Chdir(origDir)

	// GetFileMd5 returns ("", nil) for missing files, so checkUpgradeFileChange
	// treats missing files as having an empty MD5 rather than returning an error.
	t.Run("old_file_missing_reports_changed", func(t *testing.T) {
		tmpDir := t.TempDir()
		os.Chdir(tmpDir)
		os.MkdirAll(filepath.Join(tmpDir, "tmp"), 0755)
		os.WriteFile(filepath.Join(tmpDir, "tmp", "agent"), []byte("new"), 0644)

		changed, err := checkUpgradeFileChange("agent")
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if !changed {
			t.Error("expected changed=true when old file missing (empty MD5 vs real MD5)")
		}
	})

	t.Run("new_file_missing_reports_changed", func(t *testing.T) {
		tmpDir := t.TempDir()
		os.Chdir(tmpDir)
		os.MkdirAll(filepath.Join(tmpDir, "tmp"), 0755)
		os.WriteFile(filepath.Join(tmpDir, "agent"), []byte("old"), 0644)

		changed, err := checkUpgradeFileChange("agent")
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if !changed {
			t.Error("expected changed=true when new file missing (real MD5 vs empty MD5)")
		}
	})

	t.Run("both_missing_reports_no_change", func(t *testing.T) {
		tmpDir := t.TempDir()
		os.Chdir(tmpDir)
		os.MkdirAll(filepath.Join(tmpDir, "tmp"), 0755)

		changed, err := checkUpgradeFileChange("agent")
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if changed {
			t.Error("expected changed=false when both files missing (both empty MD5)")
		}
	})
}

func TestReplaceAgentFile(t *testing.T) {
	logs.UNTestDebugInit()

	origDir, err := os.Getwd()
	if err != nil {
		t.Fatal(err)
	}
	defer os.Chdir(origDir)

	tests := []struct {
		name       string
		srcContent string
		dstExists  bool
		dstContent string
	}{
		{
			name:       "replace_existing",
			srcContent: "new binary v2",
			dstExists:  true,
			dstContent: "old binary v1",
		},
		{
			name:       "create_new",
			srcContent: "brand new binary",
			dstExists:  false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			tmpDir := t.TempDir()
			if err := os.Chdir(tmpDir); err != nil {
				t.Fatal(err)
			}

			upgradeDir := filepath.Join(tmpDir, "tmp")
			os.MkdirAll(upgradeDir, 0755)

			fileName := "testAgent"
			if err := os.WriteFile(filepath.Join(upgradeDir, fileName), []byte(tt.srcContent), 0755); err != nil {
				t.Fatal(err)
			}

			dstPath := filepath.Join(tmpDir, fileName)
			if tt.dstExists {
				if err := os.WriteFile(dstPath, []byte(tt.dstContent), 0755); err != nil {
					t.Fatal(err)
				}
			}

			if err := replaceAgentFile(fileName); err != nil {
				t.Fatalf("replaceAgentFile() error = %v", err)
			}

			got, err := os.ReadFile(dstPath)
			if err != nil {
				t.Fatalf("read replaced file: %v", err)
			}
			if string(got) != tt.srcContent {
				t.Errorf("content = %q, want %q", got, tt.srcContent)
			}
		})
	}
}

func TestReplaceAgentFile_SrcNotExists(t *testing.T) {
	logs.UNTestDebugInit()

	origDir, err := os.Getwd()
	if err != nil {
		t.Fatal(err)
	}
	tmpDir := t.TempDir()
	os.Chdir(tmpDir)
	defer os.Chdir(origDir)

	os.MkdirAll("tmp", 0755)

	if err := replaceAgentFile("nonexistent"); err == nil {
		t.Error("expected error when source file missing")
	}
}

func TestReplaceAgentFile_PreservesPermission(t *testing.T) {
	logs.UNTestDebugInit()

	origDir, err := os.Getwd()
	if err != nil {
		t.Fatal(err)
	}
	tmpDir := t.TempDir()
	os.Chdir(tmpDir)
	defer os.Chdir(origDir)

	upgradeDir := filepath.Join(tmpDir, "tmp")
	os.MkdirAll(upgradeDir, 0755)

	fileName := "testAgent"
	os.WriteFile(filepath.Join(upgradeDir, fileName), []byte("new"), 0644)
	os.WriteFile(filepath.Join(tmpDir, fileName), []byte("old"), 0755)

	if err := replaceAgentFile(fileName); err != nil {
		t.Fatalf("replaceAgentFile() error = %v", err)
	}

	stat, err := os.Stat(filepath.Join(tmpDir, fileName))
	if err != nil {
		t.Fatal(err)
	}
	wantPerm := os.FileMode(0755)
	gotPerm := stat.Mode().Perm()
	if gotPerm != wantPerm {
		t.Errorf("permission = %o, want %o", gotPerm, wantPerm)
	}
}

func TestModifyScriptPrivateTmp(t *testing.T) {
	tests := []struct {
		name         string
		input        string
		wantModified bool
		wantOutput   string
	}{
		{
			name:         "replace_true_to_false",
			input:        "ExecStart=/usr/bin/agent\nPrivateTmp=true\nRestart=always\n",
			wantModified: true,
			wantOutput:   "ExecStart=/usr/bin/agent\nPrivateTmp=false\nRestart=always\n",
		},
		{
			name:         "already_false",
			input:        "ExecStart=/usr/bin/agent\nPrivateTmp=false\nRestart=always\n",
			wantModified: false,
			wantOutput:   "ExecStart=/usr/bin/agent\nPrivateTmp=false\nRestart=always\n",
		},
		{
			name:         "no_private_tmp_line",
			input:        "ExecStart=/usr/bin/agent\nRestart=always\n",
			wantModified: false,
			wantOutput:   "ExecStart=/usr/bin/agent\nRestart=always\n",
		},
		{
			name:         "with_spaces",
			input:        "  PrivateTmp = true  \nOther=line\n",
			wantModified: true,
		},
		{
			name:         "multiple_lines_only_first_privatetmp",
			input:        "A=1\nPrivateTmp=true\nB=2\n",
			wantModified: true,
			wantOutput:   "A=1\nPrivateTmp=false\nB=2\n",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			logs.UNTestDebugInit()

			tmpFile := filepath.Join(t.TempDir(), "install.sh")
			if err := os.WriteFile(tmpFile, []byte(tt.input), 0755); err != nil {
				t.Fatal(err)
			}

			modified, err := modifyScriptPrivateTmp(tmpFile)
			if err != nil {
				t.Fatalf("modifyScriptPrivateTmp() error = %v", err)
			}
			if modified != tt.wantModified {
				t.Errorf("modified = %v, want %v", modified, tt.wantModified)
			}

			if tt.wantOutput != "" {
				got, _ := os.ReadFile(tmpFile)
				if string(got) != tt.wantOutput {
					t.Errorf("output =\n%q\nwant =\n%q", got, tt.wantOutput)
				}
			}
		})
	}
}

func TestModifyScriptPrivateTmp_FileNotExists(t *testing.T) {
	logs.UNTestDebugInit()

	_, err := modifyScriptPrivateTmp("/nonexistent/path/install.sh")
	if err == nil {
		t.Error("expected error for nonexistent file")
	}
}

func TestModifyScriptPrivateTmp_PreservesPermission(t *testing.T) {
	logs.UNTestDebugInit()

	tmpFile := filepath.Join(t.TempDir(), "install.sh")
	os.WriteFile(tmpFile, []byte("PrivateTmp=true\n"), 0750)

	modified, err := modifyScriptPrivateTmp(tmpFile)
	if err != nil {
		t.Fatal(err)
	}
	if !modified {
		t.Error("expected modification")
	}

	stat, _ := os.Stat(tmpFile)
	if stat.Mode().Perm() != 0750 {
		t.Errorf("permission = %o, want %o", stat.Mode().Perm(), 0750)
	}
}
