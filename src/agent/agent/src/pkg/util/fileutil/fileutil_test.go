package fileutil

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

func init() {
	logs.UNTestDebugInit()
}

func TestAtomicWriteFile(t *testing.T) {
	t.Run("create_new_file", func(t *testing.T) {
		dir := t.TempDir()
		dst := filepath.Join(dir, "new_file.txt")

		err := AtomicWriteFile(dst, strings.NewReader("hello world"), 0644)
		if err != nil {
			t.Fatalf("AtomicWriteFile() error = %v", err)
		}

		got, err := os.ReadFile(dst)
		if err != nil {
			t.Fatalf("read file: %v", err)
		}
		if string(got) != "hello world" {
			t.Errorf("content = %q, want %q", got, "hello world")
		}
	})

	t.Run("overwrite_existing", func(t *testing.T) {
		dir := t.TempDir()
		dst := filepath.Join(dir, "existing.txt")
		os.WriteFile(dst, []byte("old content"), 0644)

		err := AtomicWriteFile(dst, strings.NewReader("new content"), 0644)
		if err != nil {
			t.Fatalf("AtomicWriteFile() error = %v", err)
		}

		got, _ := os.ReadFile(dst)
		if string(got) != "new content" {
			t.Errorf("content = %q, want %q", got, "new content")
		}
	})

	t.Run("empty_content", func(t *testing.T) {
		dir := t.TempDir()
		dst := filepath.Join(dir, "empty.txt")

		err := AtomicWriteFile(dst, strings.NewReader(""), 0644)
		if err != nil {
			t.Fatalf("AtomicWriteFile() error = %v", err)
		}

		info, _ := os.Stat(dst)
		if info.Size() != 0 {
			t.Errorf("size = %d, want 0", info.Size())
		}
	})

	t.Run("invalid_dir", func(t *testing.T) {
		err := AtomicWriteFile("/nonexistent_dir_xyz/file.txt", strings.NewReader("data"), 0644)
		if err == nil {
			t.Error("expected error for nonexistent directory")
		}
	})

	t.Run("large_content", func(t *testing.T) {
		dir := t.TempDir()
		dst := filepath.Join(dir, "large.bin")

		data := strings.Repeat("A", 1024*1024) // 1MB
		err := AtomicWriteFile(dst, strings.NewReader(data), 0644)
		if err != nil {
			t.Fatalf("AtomicWriteFile() error = %v", err)
		}

		info, _ := os.Stat(dst)
		if info.Size() != int64(len(data)) {
			t.Errorf("size = %d, want %d", info.Size(), len(data))
		}
	})

	t.Run("no_temp_files_left", func(t *testing.T) {
		dir := t.TempDir()
		dst := filepath.Join(dir, "clean.txt")

		AtomicWriteFile(dst, strings.NewReader("test"), 0644)

		entries, _ := os.ReadDir(dir)
		if len(entries) != 1 {
			names := make([]string, len(entries))
			for i, e := range entries {
				names[i] = e.Name()
			}
			t.Errorf("expected 1 file, got %d: %v", len(entries), names)
		}
	})
}
