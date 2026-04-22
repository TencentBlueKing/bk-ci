package envs

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestLoadEnvFile(t *testing.T) {
	tests := []struct {
		name    string
		content string
		create  bool
		want    map[string]string
	}{
		{
			name:   "file_not_exist",
			create: false,
			want:   nil,
		},
		{
			name:    "empty_file",
			content: "",
			create:  true,
			want:    map[string]string{},
		},
		{
			name:    "normal_vars",
			content: "JAVA_HOME=/usr/lib/jvm/java-11\nGRADLE_HOME=/opt/gradle\n",
			create:  true,
			want:    map[string]string{"JAVA_HOME": "/usr/lib/jvm/java-11", "GRADLE_HOME": "/opt/gradle"},
		},
		{
			name:    "comments_and_blanks",
			content: "# this is a comment\n\nJAVA_HOME=/usr/lib/jvm\n\n# another comment\nLANG=en_US.UTF-8\n",
			create:  true,
			want:    map[string]string{"JAVA_HOME": "/usr/lib/jvm", "LANG": "en_US.UTF-8"},
		},
		{
			name:    "value_with_equals",
			content: "OPTS=-Xmx=512m -Dfoo=bar\n",
			create:  true,
			want:    map[string]string{"OPTS": "-Xmx=512m -Dfoo=bar"},
		},
		{
			name:    "line_without_equals",
			content: "INVALID_LINE\nVALID=ok\n",
			create:  true,
			want:    map[string]string{"VALID": "ok"},
		},
		{
			name:    "empty_value",
			content: "EMPTY_VAR=\n",
			create:  true,
			want:    map[string]string{"EMPTY_VAR": ""},
		},
		{
			name:    "whitespace_around_key",
			content: "  MY_KEY  =value\n",
			create:  true,
			want:    map[string]string{"MY_KEY": "value"},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			dir := t.TempDir()
			path := filepath.Join(dir, ".env")
			if tt.create {
				if err := os.WriteFile(path, []byte(tt.content), 0644); err != nil {
					t.Fatalf("write test file: %v", err)
				}
			}
			got, err := loadEnvFile(path)
			if err != nil {
				t.Fatalf("loadEnvFile() error: %v", err)
			}
			if tt.want == nil {
				if got != nil {
					t.Errorf("loadEnvFile() = %v, want nil", got)
				}
				return
			}
			if len(got) != len(tt.want) {
				t.Errorf("loadEnvFile() returned %d entries, want %d", len(got), len(tt.want))
			}
			for k, wantV := range tt.want {
				if gotV, ok := got[k]; !ok {
					t.Errorf("missing key %q", k)
				} else if gotV != wantV {
					t.Errorf("key %q = %q, want %q", k, gotV, wantV)
				}
			}
		})
	}
}

func TestLoadPathFile(t *testing.T) {
	tests := []struct {
		name    string
		content string
		create  bool
		want    string
	}{
		{
			name:   "file_not_exist",
			create: false,
			want:   "",
		},
		{
			name:    "empty_file",
			content: "",
			create:  true,
			want:    "",
		},
		{
			name:    "single_line",
			content: "/usr/local/bin:/usr/bin:/usr/sbin",
			create:  true,
			want:    "/usr/local/bin:/usr/bin:/usr/sbin",
		},
		{
			name:    "trailing_newline",
			content: "/usr/local/bin:/usr/bin\n",
			create:  true,
			want:    "/usr/local/bin:/usr/bin",
		},
		{
			name:    "surrounding_whitespace",
			content: "  /usr/bin:/usr/sbin  \n",
			create:  true,
			want:    "/usr/bin:/usr/sbin",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			dir := t.TempDir()
			path := filepath.Join(dir, ".path")
			if tt.create {
				if err := os.WriteFile(path, []byte(tt.content), 0644); err != nil {
					t.Fatalf("write test file: %v", err)
				}
			}
			got, err := loadPathFile(path)
			if err != nil {
				t.Fatalf("loadPathFile() error: %v", err)
			}
			if got != tt.want {
				t.Errorf("loadPathFile() = %q, want %q", got, tt.want)
			}
		})
	}
}

func TestMergePath(t *testing.T) {
	tests := []struct {
		name        string
		savedPath   string
		currentPath string
		want        string
	}{
		{
			name:        "no_overlap",
			savedPath:   "/home/user/go/bin:/home/user/.nvm/bin",
			currentPath: "/usr/local/bin:/usr/bin",
			want:        "/home/user/go/bin:/home/user/.nvm/bin:/usr/local/bin:/usr/bin",
		},
		{
			name:        "with_overlap",
			savedPath:   "/usr/local/bin:/home/user/go/bin",
			currentPath: "/usr/local/bin:/usr/bin",
			want:        "/usr/local/bin:/home/user/go/bin:/usr/bin",
		},
		{
			name:        "saved_empty",
			savedPath:   "",
			currentPath: "/usr/local/bin:/usr/bin",
			want:        "/usr/local/bin:/usr/bin",
		},
		{
			name:        "current_empty",
			savedPath:   "/usr/local/bin:/usr/bin",
			currentPath: "",
			want:        "/usr/local/bin:/usr/bin",
		},
		{
			name:        "both_empty",
			savedPath:   "",
			currentPath: "",
			want:        "",
		},
		{
			name:        "all_duplicates",
			savedPath:   "/usr/bin:/usr/sbin",
			currentPath: "/usr/bin:/usr/sbin",
			want:        "/usr/bin:/usr/sbin",
		},
		{
			name:        "whitespace_entries",
			savedPath:   "/usr/bin:  :/usr/sbin",
			currentPath: " :/usr/local/bin",
			want:        "/usr/bin:/usr/sbin:/usr/local/bin",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := mergePath(tt.savedPath, tt.currentPath)
			if got != tt.want {
				t.Errorf("mergePath(%q, %q) = %q, want %q", tt.savedPath, tt.currentPath, got, tt.want)
			}
		})
	}
}

func TestLoadEnvFiles(t *testing.T) {
	t.Run("both_files_exist", func(t *testing.T) {
		dir := t.TempDir()
		os.WriteFile(filepath.Join(dir, ".env"), []byte("TEST_BKCI_EF_VAR1=hello\nTEST_BKCI_EF_VAR2=world\n"), 0644)
		os.WriteFile(filepath.Join(dir, ".path"), []byte("/test/bkci/bin:/test/bkci/sbin\n"), 0644)

		oldPath := os.Getenv("PATH")
		defer os.Setenv("PATH", oldPath)
		defer os.Unsetenv("TEST_BKCI_EF_VAR1")
		defer os.Unsetenv("TEST_BKCI_EF_VAR2")

		LoadEnvFiles(dir)

		if v := os.Getenv("TEST_BKCI_EF_VAR1"); v != "hello" {
			t.Errorf("TEST_BKCI_EF_VAR1 = %q, want %q", v, "hello")
		}
		if v := os.Getenv("TEST_BKCI_EF_VAR2"); v != "world" {
			t.Errorf("TEST_BKCI_EF_VAR2 = %q, want %q", v, "world")
		}
		newPath := os.Getenv("PATH")
		if !strings.Contains(newPath, "/test/bkci/bin") {
			t.Errorf("PATH should contain /test/bkci/bin, got %q", newPath)
		}
		if !strings.Contains(newPath, "/test/bkci/sbin") {
			t.Errorf("PATH should contain /test/bkci/sbin, got %q", newPath)
		}
	})

	t.Run("only_env_file", func(t *testing.T) {
		dir := t.TempDir()
		os.WriteFile(filepath.Join(dir, ".env"), []byte("TEST_BKCI_EF_ONLY=env_only\n"), 0644)

		oldPath := os.Getenv("PATH")
		defer os.Setenv("PATH", oldPath)
		defer os.Unsetenv("TEST_BKCI_EF_ONLY")

		LoadEnvFiles(dir)

		if v := os.Getenv("TEST_BKCI_EF_ONLY"); v != "env_only" {
			t.Errorf("TEST_BKCI_EF_ONLY = %q, want %q", v, "env_only")
		}
		if os.Getenv("PATH") != oldPath {
			t.Errorf("PATH should not change when .path is missing")
		}
	})

	t.Run("only_path_file", func(t *testing.T) {
		dir := t.TempDir()
		os.WriteFile(filepath.Join(dir, ".path"), []byte("/test/bkci/path_only\n"), 0644)

		oldPath := os.Getenv("PATH")
		defer os.Setenv("PATH", oldPath)

		LoadEnvFiles(dir)

		newPath := os.Getenv("PATH")
		if !strings.HasPrefix(newPath, "/test/bkci/path_only") {
			t.Errorf("PATH should start with /test/bkci/path_only, got %q", newPath)
		}
	})

	t.Run("no_files", func(t *testing.T) {
		dir := t.TempDir()

		oldPath := os.Getenv("PATH")
		defer os.Setenv("PATH", oldPath)

		LoadEnvFiles(dir)

		if os.Getenv("PATH") != oldPath {
			t.Errorf("PATH should not change when no files exist")
		}
	})

	t.Run("path_merge_dedup", func(t *testing.T) {
		dir := t.TempDir()
		os.WriteFile(filepath.Join(dir, ".path"), []byte("/usr/local/bin:/test/bkci/new\n"), 0644)

		os.Setenv("PATH", "/usr/local/bin:/usr/bin")
		defer os.Setenv("PATH", "/usr/local/bin:/usr/bin")

		LoadEnvFiles(dir)

		newPath := os.Getenv("PATH")
		count := strings.Count(newPath, "/usr/local/bin")
		if count != 1 {
			t.Errorf("/usr/local/bin appears %d times in PATH, want 1; PATH=%q", count, newPath)
		}
		if !strings.Contains(newPath, "/test/bkci/new") {
			t.Errorf("PATH should contain /test/bkci/new, got %q", newPath)
		}
		if !strings.Contains(newPath, "/usr/bin") {
			t.Errorf("PATH should contain /usr/bin, got %q", newPath)
		}
	})
}
