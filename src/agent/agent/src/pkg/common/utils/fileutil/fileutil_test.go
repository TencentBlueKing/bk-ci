package fileutil

import (
	"archive/zip"
	"os"
	"path/filepath"
	"testing"
)

func TestUnzip_CreatesParentDirsForFiles(t *testing.T) {
	dir := t.TempDir()
	archive := filepath.Join(dir, "nested.zip")
	createZipArchive(t, archive, map[string]string{
		"tmp/upgrader.exe": "binary",
	})

	target := filepath.Join(dir, "out")
	if err := Unzip(archive, target); err != nil {
		t.Fatalf("Unzip() error = %v", err)
	}

	if _, err := os.Stat(filepath.Join(target, "tmp", "upgrader.exe")); err != nil {
		t.Fatalf("expected nested file after unzip: %v", err)
	}
}

func TestUnzip_RejectsZipSlip(t *testing.T) {
	dir := t.TempDir()
	archive := filepath.Join(dir, "zipslip.zip")
	createZipArchive(t, archive, map[string]string{
		"../evil.txt": "boom",
	})

	target := filepath.Join(dir, "out")
	if err := Unzip(archive, target); err == nil {
		t.Fatal("expected Zip Slip archive to be rejected")
	}

	if _, err := os.Stat(filepath.Join(dir, "evil.txt")); !os.IsNotExist(err) {
		t.Fatal("zip slip target should not be created")
	}
}

func createZipArchive(t *testing.T, zipPath string, files map[string]string) {
	t.Helper()

	f, err := os.Create(zipPath)
	if err != nil {
		t.Fatal(err)
	}
	defer f.Close()

	w := zip.NewWriter(f)
	for name, content := range files {
		entry, err := w.Create(name)
		if err != nil {
			_ = w.Close()
			t.Fatal(err)
		}
		if _, err := entry.Write([]byte(content)); err != nil {
			_ = w.Close()
			t.Fatal(err)
		}
	}
	if err := w.Close(); err != nil {
		t.Fatal(err)
	}
}
