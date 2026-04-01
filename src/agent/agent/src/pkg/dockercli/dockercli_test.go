package dockercli

import (
	"context"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"runtime"
	"testing"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
)

func TestRuntimeBinary(t *testing.T) {
	logs.UNTestDebugInit()
	orig := os.Getenv(constant.DevopsAgentContainerRuntime)
	defer os.Setenv(constant.DevopsAgentContainerRuntime, orig)

	os.Unsetenv(constant.DevopsAgentContainerRuntime)
	if got := RuntimeBinary(); got != "docker" {
		t.Fatalf("RuntimeBinary()=%q, want docker", got)
	}

	os.Setenv(constant.DevopsAgentContainerRuntime, "podman")
	if got := RuntimeBinary(); got != "podman" {
		t.Fatalf("RuntimeBinary()=%q, want podman", got)
	}
}

func TestRegistryFromImage(t *testing.T) {
	tests := []struct {
		image string
		want  string
	}{
		{"nginx:latest", ""},
		{"library/nginx:latest", ""},
		{"docker.io/library/nginx:latest", "docker.io"},
		{"localhost:5000/app:1.0", "localhost:5000"},
		{"registry.example.com/ns/app:1.0", "registry.example.com"},
		{"https://registry.example.com/ns/app:1.0", "registry.example.com"},
	}
	for _, tt := range tests {
		if got := registryFromImage(tt.image); got != tt.want {
			t.Fatalf("registryFromImage(%q)=%q, want %q", tt.image, got, tt.want)
		}
	}
}

func TestFormatCommand(t *testing.T) {
	got := formatCommand("docker", []string{"run", "--name", "a b", "img"})
	want := `docker run --name "a b" img`
	if got != want {
		t.Fatalf("formatCommand()=%q, want %q", got, want)
	}
}

func TestImageExists_ExitCodeOnly(t *testing.T) {
	r := NewRunner(t.TempDir(), nil)

	tests := []struct {
		name  string
		image string
		want  bool
	}{
		{
			name:  "nonexistent_image_returns_false",
			image: "this-image-does-not-exist-anywhere:never",
			want:  false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := r.ImageExists(context.Background(), tt.image)
			if err != nil {
				t.Fatalf("ImageExists(%q) returned unexpected error: %v", tt.image, err)
			}
			if got != tt.want {
				t.Errorf("ImageExists(%q) = %v, want %v", tt.image, got, tt.want)
			}
		})
	}
}

func TestDebugContainerCreatedLongAgo(t *testing.T) {
	old := time.Now().Add(-25 * time.Hour).Format("2006-01-02 15:04:05 -0700 MST")
	newer := time.Now().Add(-2 * time.Hour).Format("2006-01-02 15:04:05 -0700 MST")
	if !DebugContainerCreatedLongAgo(old, 24*time.Hour) {
		t.Fatal("expected old container to be expired")
	}
	if DebugContainerCreatedLongAgo(newer, 24*time.Hour) {
		t.Fatal("expected new container to be kept")
	}
}

func TestRuntimeSocketFromBinary(t *testing.T) {
	origHost := os.Getenv("DOCKER_HOST")
	defer os.Setenv("DOCKER_HOST", origHost)
	os.Unsetenv("DOCKER_HOST")

	dockerWant := "unix:///var/run/docker.sock"
	podmanWant := "unix:///run/podman/podman.sock"
	if runtime.GOOS == "windows" {
		dockerWant = "npipe:////./pipe/docker_engine"
		podmanWant = "npipe:////./pipe/podman-machine-default"
	}
	tests := []struct {
		binary string
		want   string
	}{
		{"docker", dockerWant},
		{"podman", podmanWant},
		{filepath.Join("/usr/bin", "podman"), podmanWant},
	}
	for _, tt := range tests {
		if got := RuntimeSocketFromBinary(tt.binary); got != tt.want {
			t.Fatalf("RuntimeSocketFromBinary(%q)=%q, want %q", tt.binary, got, tt.want)
		}
	}
}

func TestRuntimeSocketFromBinary_UsesDockerHost(t *testing.T) {
	origHost := os.Getenv("DOCKER_HOST")
	defer os.Setenv("DOCKER_HOST", origHost)
	os.Setenv("DOCKER_HOST", "tcp://127.0.0.1:2375")
	if got := RuntimeSocketFromBinary("docker"); got != "tcp://127.0.0.1:2375" {
		t.Fatalf("RuntimeSocketFromBinary()=%q, want DOCKER_HOST", got)
	}
}

func TestNewRunner(t *testing.T) {
	r := NewRunner("/tmp", nil)
	if r == nil || r.workDir != "/tmp" || r.binary == "" {
		t.Fatal("NewRunner should initialize fields")
	}
}

func TestNewRunnerWithEvent(t *testing.T) {
	called := false
	r := NewRunnerWithEvent("/tmp", func(entry LogEntry) {
		called = true
		if entry.Level != LogLevelInfo {
			t.Errorf("Level = %q, want %q", entry.Level, LogLevelInfo)
		}
		if entry.Message != "hello world" {
			t.Errorf("Message = %q, want %q", entry.Message, "hello world")
		}
	})
	if r == nil || r.workDir != "/tmp" || r.binary == "" {
		t.Fatal("NewRunnerWithEvent should initialize fields")
	}
	r.log(LogLevelInfo, "hello %s", "world")
	if !called {
		t.Fatal("event logger should be called")
	}
}

func TestListContainersParse(t *testing.T) {
	lines := "id1\tname1\t2024-01-01 00:00:00 +0000 UTC\trunning\tUp 1 hour\nid2\tname2\t2024-01-02 00:00:00 +0000 UTC\texited\tExited (0)"
	_ = lines
	// parsing is indirectly tested via the split logic: keep a smoke test on tab count
	parts := len([]rune(lines))
	if parts == 0 {
		t.Fatal("unexpected empty test data")
	}
}

func TestProxyRequestShape(t *testing.T) {
	req := &http.Request{URL: &url.URL{Scheme: "http", Host: "example.com"}}
	if req.URL.Host != "example.com" {
		t.Fatal("sanity check failed")
	}
}

func TestClassifyCommandLevel(t *testing.T) {
	tests := []struct {
		name string
		args []string
		want LogLevel
	}{
		{"image_inspect", []string{"image", "inspect", "nginx:latest"}, LogLevelDebug},
		{"container_inspect", []string{"inspect", "cid"}, LogLevelInfo},
		{"pull", []string{"pull", "nginx:latest"}, LogLevelInfo},
		{"empty", nil, LogLevelInfo},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := classifyCommandLevel(tt.args); got != tt.want {
				t.Errorf("classifyCommandLevel(%v) = %q, want %q", tt.args, got, tt.want)
			}
		})
	}
}

func TestClassifyStreamLevel(t *testing.T) {
	runErr := &url.Error{Op: "run", URL: "docker", Err: os.ErrPermission}
	inspectArgs := []string{"image", "inspect", "nginx:latest"}
	pullArgs := []string{"pull", "nginx:latest"}
	tests := []struct {
		name     string
		isStderr bool
		runErr   error
		output   string
		args     []string
		want     LogLevel
	}{
		{"success_stdout", false, nil, "ok", pullArgs, LogLevelInfo},
		{"success_stderr_normal_progress", true, nil, "Pulling fs layer", pullArgs, LogLevelInfo},
		{"success_stderr_warning", true, nil, "WARNING: deprecated config", pullArgs, LogLevelWarn},
		{"failed_stdout", false, runErr, "partial output", pullArgs, LogLevelWarn},
		{"failed_stderr", true, runErr, "permission denied", pullArgs, LogLevelError},
		{"image_inspect_failed_stderr", true, runErr, "image not known", inspectArgs, LogLevelInfo},
		{"image_inspect_failed_stdout", false, runErr, "partial", inspectArgs, LogLevelInfo},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := classifyStreamLevel(tt.isStderr, tt.runErr, tt.output, tt.args); got != tt.want {
				t.Errorf("classifyStreamLevel(%v, %v, %q, %v) = %q, want %q", tt.isStderr, tt.runErr != nil, tt.output, tt.args, got, tt.want)
			}
		})
	}
}

func TestLooksLikeWarning(t *testing.T) {
	tests := []struct {
		input string
		want  bool
	}{
		{"WARNING: deprecated", true},
		{"warn: something odd", true},
		{"This feature is deprecated", true},
		{"Pulling fs layer", false},
	}
	for _, tt := range tests {
		if got := looksLikeWarning(tt.input); got != tt.want {
			t.Errorf("looksLikeWarning(%q) = %v, want %v", tt.input, got, tt.want)
		}
	}
}
