//go:build windows

package upgrader

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

func TestReplaceAgentFile(t *testing.T) {
	logs.UNTestDebugInit()

	origDir, err := os.Getwd()
	if err != nil {
		t.Fatal(err)
	}
	tmpDir := t.TempDir()
	if err := os.Chdir(tmpDir); err != nil {
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
			name:       "replace_existing_file",
			srcContent: "new binary v2",
			dstExists:  true,
			dstContent: "old binary v1",
		},
		{
			name:       "dst_not_exists",
			srcContent: "brand new binary",
			dstExists:  false,
		},
		{
			name:       "same_content",
			srcContent: "same content",
			dstExists:  true,
			dstContent: "same content",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			subDir := t.TempDir()
			if err := os.Chdir(subDir); err != nil {
				t.Fatal(err)
			}
			defer os.Chdir(tmpDir)

			upgradeDir := filepath.Join(subDir, "tmp")
			if err := os.MkdirAll(upgradeDir, 0755); err != nil {
				t.Fatal(err)
			}

			fileName := "test.exe"
			srcPath := filepath.Join(upgradeDir, fileName)
			if err := os.WriteFile(srcPath, []byte(tt.srcContent), 0644); err != nil {
				t.Fatal(err)
			}

			dstPath := filepath.Join(subDir, fileName)
			if tt.dstExists {
				if err := os.WriteFile(dstPath, []byte(tt.dstContent), 0644); err != nil {
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
				t.Errorf("replaced content = %q, want %q", got, tt.srcContent)
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
	if err := os.Chdir(tmpDir); err != nil {
		t.Fatal(err)
	}
	defer os.Chdir(origDir)

	if err := os.MkdirAll("tmp", 0755); err != nil {
		t.Fatal(err)
	}

	err = replaceAgentFile("nonexistent.exe")
	if err == nil {
		t.Error("replaceAgentFile with nonexistent source should return error")
	}
}

func TestReplaceMaxRetries(t *testing.T) {
	if replaceMaxRetries < 1 {
		t.Errorf("replaceMaxRetries = %d, should be >= 1", replaceMaxRetries)
	}
	if replaceMaxRetries > 30 {
		t.Errorf("replaceMaxRetries = %d, unreasonably high", replaceMaxRetries)
	}
}
