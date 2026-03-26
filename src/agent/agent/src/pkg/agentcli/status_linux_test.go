//go:build linux
// +build linux

package agentcli

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestDirStatus(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	t.Run("existing_dir", func(t *testing.T) {
		dir := t.TempDir()
		got := dirStatus(dir)
		if !strings.Contains(got, "OK") {
			t.Errorf("dirStatus(existing) = %q, expected OK", got)
		}
	})

	t.Run("missing", func(t *testing.T) {
		got := dirStatus("/tmp/nonexistent_dir_agentcli_test_42")
		if !strings.Contains(got, "missing") {
			t.Errorf("dirStatus(missing) = %q, expected missing", got)
		}
	})

	t.Run("file_not_dir", func(t *testing.T) {
		dir := t.TempDir()
		f := filepath.Join(dir, "afile")
		os.WriteFile(f, []byte("x"), 0644)
		got := dirStatus(f)
		if !strings.Contains(got, "not a directory") {
			t.Errorf("dirStatus(file) = %q, expected 'not a directory'", got)
		}
	})
}

func TestDirStatus_Chinese(t *testing.T) {
	old := useChinese
	useChinese = true
	defer func() { useChinese = old }()

	dir := t.TempDir()
	got := dirStatus(dir)
	if !strings.Contains(got, "正常") {
		t.Errorf("dirStatus(chinese) = %q, expected 正常", got)
	}

	got = dirStatus("/tmp/nonexistent_dir_agentcli_test_42")
	if !strings.Contains(got, "缺失") {
		t.Errorf("dirStatus(chinese missing) = %q, expected 缺失", got)
	}
}

func TestFileStatus(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	t.Run("missing", func(t *testing.T) {
		got := fileStatus("/tmp/nonexistent_file_agentcli_test_42")
		if !strings.Contains(got, "missing") {
			t.Errorf("fileStatus(missing) = %q, expected missing", got)
		}
	})

	t.Run("empty_file", func(t *testing.T) {
		dir := t.TempDir()
		f := filepath.Join(dir, "empty")
		os.WriteFile(f, []byte{}, 0644)
		got := fileStatus(f)
		if !strings.Contains(got, "empty") {
			t.Errorf("fileStatus(empty) = %q, expected empty", got)
		}
	})

	t.Run("normal_file", func(t *testing.T) {
		dir := t.TempDir()
		f := filepath.Join(dir, "normal")
		os.WriteFile(f, make([]byte, 1024*1024), 0644)
		got := fileStatus(f)
		if !strings.Contains(got, "OK") {
			t.Errorf("fileStatus(1MB) = %q, expected OK", got)
		}
		if !strings.Contains(got, "1.0 MB") {
			t.Errorf("fileStatus(1MB) = %q, expected size info", got)
		}
	})

	t.Run("small_file", func(t *testing.T) {
		dir := t.TempDir()
		f := filepath.Join(dir, "small")
		os.WriteFile(f, []byte("hello"), 0644)
		got := fileStatus(f)
		if !strings.Contains(got, "OK") {
			t.Errorf("fileStatus(small) = %q, expected OK", got)
		}
		if !strings.Contains(got, "0.0 MB") {
			t.Errorf("fileStatus(small) = %q, expected 0.0 MB", got)
		}
	})
}

func TestReadPid(t *testing.T) {
	dir := t.TempDir()

	tests := []struct {
		name    string
		content string
		want    int
	}{
		{"valid", "12345\n", 12345},
		{"with_whitespace", "  42  \n", 42},
		{"no_newline", "999", 999},
		{"invalid", "not-a-number", 0},
		{"empty", "", 0},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			pidFile := filepath.Join(dir, tt.name+".pid")
			os.WriteFile(pidFile, []byte(tt.content), 0644)
			if got := readPid(pidFile); got != tt.want {
				t.Errorf("readPid(%q) = %d, want %d", tt.content, got, tt.want)
			}
		})
	}

	t.Run("missing_file", func(t *testing.T) {
		if got := readPid(filepath.Join(dir, "nope.pid")); got != 0 {
			t.Errorf("readPid(missing) = %d, want 0", got)
		}
	})
}

func TestPidStatus(t *testing.T) {
	old := useChinese
	useChinese = false
	defer func() { useChinese = old }()

	t.Run("zero", func(t *testing.T) {
		got := pidStatus(0)
		if got != "not running" {
			t.Errorf("pidStatus(0) = %q, want %q", got, "not running")
		}
	})

	t.Run("negative", func(t *testing.T) {
		got := pidStatus(-1)
		if got != "not running" {
			t.Errorf("pidStatus(-1) = %q, want %q", got, "not running")
		}
	})

	t.Run("self_process", func(t *testing.T) {
		got := pidStatus(os.Getpid())
		if !strings.Contains(got, "(running)") {
			t.Errorf("pidStatus(self) = %q, expected to contain '(running)'", got)
		}
	})

	t.Run("nonexistent", func(t *testing.T) {
		got := pidStatus(99999999)
		if !strings.Contains(got, "(not running)") {
			t.Errorf("pidStatus(99999999) = %q, expected to contain '(not running)'", got)
		}
	})
}

func TestCurrentUser(t *testing.T) {
	orig := os.Getenv("USER")
	defer setOrUnset("USER", orig)

	os.Setenv("USER", "testuser")
	if got := currentUser(); got != "testuser" {
		t.Errorf("currentUser() = %q, want testuser", got)
	}
}
