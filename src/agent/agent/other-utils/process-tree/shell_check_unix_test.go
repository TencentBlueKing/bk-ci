//go:build linux || darwin

package main

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestBuildPrepareScriptLines(t *testing.T) {
	t.Run("bash login script", func(t *testing.T) {
		lines := buildPrepareScriptLines("/bin/bash", "/tmp/start.sh")
		if got, want := lines[1], "exec /bin/bash -l '/tmp/start.sh'"; got != want {
			t.Fatalf("unexpected exec line, got %q want %q", got, want)
		}
	})

	t.Run("tcsh login script", func(t *testing.T) {
		lines := buildPrepareScriptLines("/bin/tcsh", "/tmp/start.sh")
		if got, want := lines[1], "exec /bin/tcsh '/tmp/start.sh' -l"; got != want {
			t.Fatalf("unexpected exec line, got %q want %q", got, want)
		}
	})
}

func TestClassifyShellCheckResult(t *testing.T) {
	t.Run("pass when marker reached", func(t *testing.T) {
		result := &shellCheckResult{MarkerCreated: true, ExitCode: 0}
		classifyShellCheckResult(result)
		if result.Status != "PASS" {
			t.Fatalf("expected PASS, got %s", result.Status)
		}
	})

	t.Run("warn when marker reached with suspicious findings", func(t *testing.T) {
		result := &shellCheckResult{
			MarkerCreated: true,
			ExitCode:      0,
			Findings:      []shellCheckFinding{{Path: "/tmp/.bashrc", Line: 1, Content: "exec zsh"}},
		}
		classifyShellCheckResult(result)
		if result.Status != "WARN" {
			t.Fatalf("expected WARN, got %s", result.Status)
		}
	})

	t.Run("fail when no marker and shell exits 0", func(t *testing.T) {
		result := &shellCheckResult{MarkerCreated: false, ExitCode: 0}
		classifyShellCheckResult(result)
		if result.Status != "FAIL" {
			t.Fatalf("expected FAIL, got %s", result.Status)
		}
	})
}

func TestScanInitFiles(t *testing.T) {
	dir := t.TempDir()
	rcFile := filepath.Join(dir, ".bashrc")
	content := strings.Join([]string{
		"# comment",
		"exec zsh",
		"read foo",
		"echo ok",
	}, "\n")
	if err := os.WriteFile(rcFile, []byte(content), 0o644); err != nil {
		t.Fatalf("write rc file: %v", err)
	}

	findings := scanInitFiles([]string{rcFile})
	if len(findings) != 2 {
		t.Fatalf("expected 2 findings, got %d", len(findings))
	}
	if findings[0].Content != "exec zsh" {
		t.Fatalf("unexpected first finding: %+v", findings[0])
	}
	if findings[1].Content != "read foo" {
		t.Fatalf("unexpected second finding: %+v", findings[1])
	}
}
