//go:build windows
// +build windows

package agentcli

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestUnzipCandidateDirs(t *testing.T) {
	dest := `D:\agents\dev-cli-test`
	dirs := unzipCandidateDirs(dest)
	if len(dirs) == 0 {
		t.Fatal("expected at least one unzip candidate dir")
	}
	if dirs[0] != dest {
		t.Fatalf("first candidate = %q, want %q", dirs[0], dest)
	}

	if exePath, err := os.Executable(); err == nil {
		exeDir := filepath.Dir(exePath)
		found := false
		for _, dir := range dirs {
			if strings.EqualFold(dir, exeDir) {
				found = true
				break
			}
		}
		if !found {
			t.Fatalf("expected executable dir %q in candidates, got %v", exeDir, dirs)
		}
	}
	if len(dirs) > 1 && strings.EqualFold(dirs[0], dirs[1]) {
		t.Fatalf("candidate dirs should not duplicate: %v", dirs)
	}
}
