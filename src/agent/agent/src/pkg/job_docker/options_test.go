package job_docker

import (
	"path/filepath"
	"strings"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
)

func TestBuildUserDockerArgs(t *testing.T) {
	args, err := BuildUserDockerArgs(api.DockerOptions{
		Volumes:    []string{"./data:/data:ro"},
		Mounts:     []string{"type=bind,source=/tmp,target=/tmp,readonly"},
		Gpus:       "all",
		Privileged: true,
		Network:    []string{"bridge"},
		User:       "root",
	})
	if err != nil {
		t.Fatal(err)
	}
	if len(args) == 0 {
		t.Fatal("expected docker args")
	}
	foundVolume := false
	for i := 0; i < len(args)-1; i++ {
		if args[i] == "--volume" {
			foundVolume = true
			if !strings.Contains(args[i+1], ":/data:ro") {
				t.Fatalf("unexpected normalized volume: %s", args[i+1])
			}
		}
	}
	if !foundVolume {
		t.Fatal("expected --volume in args")
	}
}

func TestBuildUserDockerArgs_Invalid(t *testing.T) {
	_, err := BuildUserDockerArgs(api.DockerOptions{
		Volumes: []string{""},
	})
	if err == nil {
		t.Fatal("expected error for empty volume")
	}
}

func TestNormalizeVolumeArg_RelativePath(t *testing.T) {
	got := normalizeVolumeArg("./data:/data:ro")
	if got == "./data:/data:ro" {
		t.Fatal("expected relative volume host path to be normalized to absolute path")
	}
	if !strings.HasSuffix(got, string(filepath.Separator)+"data:/data:ro") && !strings.Contains(got, "data:/data:ro") {
		t.Fatalf("unexpected normalized relative volume: %s", got)
	}
}

func TestNormalizeVolumeArg_WindowsDrivePathUnchanged(t *testing.T) {
	tests := []string{
		`C:\data:/data:ro`,
		`D:\cache:C:\container\cache`,
	}
	for _, tt := range tests {
		if got := normalizeVolumeArg(tt); got != tt {
			t.Fatalf("normalizeVolumeArg(%q)=%q, want unchanged", tt, got)
		}
	}
}

func TestHasCustomNetwork(t *testing.T) {
	if HasCustomNetwork(api.DockerOptions{}) {
		t.Fatal("expected false")
	}
	if !HasCustomNetwork(api.DockerOptions{Network: []string{"bridge"}}) {
		t.Fatal("expected true")
	}
}
