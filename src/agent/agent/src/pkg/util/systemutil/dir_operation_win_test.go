//go:build windows
// +build windows

package systemutil

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

func init() {
	logs.UNTestDebugInit()
}

func TestMkBuildTmpDir_Windows(t *testing.T) {
	tmpDir, err := MkBuildTmpDir()
	if err != nil {
		t.Fatalf("MkBuildTmpDir failed: %v", err)
	}
	if tmpDir == "" {
		t.Fatal("tmpDir should not be empty")
	}

	info, err := os.Stat(tmpDir)
	if err != nil {
		t.Fatalf("tmpDir does not exist: %v", err)
	}
	if !info.IsDir() {
		t.Error("tmpDir should be a directory")
	}
}

func TestMkDir_Windows(t *testing.T) {
	base := t.TempDir()
	dir := filepath.Join(base, "a", "b", "c")

	err := MkDir(dir)
	if err != nil {
		t.Fatalf("MkDir failed: %v", err)
	}

	info, err := os.Stat(dir)
	if err != nil {
		t.Fatalf("dir does not exist: %v", err)
	}
	if !info.IsDir() {
		t.Error("should be a directory")
	}
}

func TestChmod_Windows(t *testing.T) {
	tmpFile := filepath.Join(t.TempDir(), "test.txt")
	if err := os.WriteFile(tmpFile, []byte("data"), 0644); err != nil {
		t.Fatalf("WriteFile failed: %v", err)
	}

	err := Chmod(tmpFile, os.ModePerm)
	if err != nil {
		t.Errorf("Chmod failed: %v", err)
	}
}

func TestChmod_NonexistentFile(t *testing.T) {
	err := Chmod(filepath.Join(t.TempDir(), "nonexistent.txt"), 0644)
	if err == nil {
		t.Error("expected error for nonexistent file")
	}
}
