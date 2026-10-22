package dockercli

import (
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"os"
	"testing"
)

func TestTaskRuntimeOverridesLegacySwitchWithoutMutatingOtherTasks(t *testing.T) {
	t.Setenv(constant.DevopsAgentContainerRuntime, "podman")
	old := NewRunner(t.TempDir(), nil)
	docker := NewRunner(t.TempDir(), nil).WithRuntime("docker")
	podman := NewRunner(t.TempDir(), nil).WithRuntime("podman")
	if old.Binary() != "podman" || docker.Binary() != "docker" || podman.Binary() != "podman" {
		t.Fatal("task runtime was not isolated")
	}
	if os.Getenv(constant.DevopsAgentContainerRuntime) != "podman" {
		t.Fatal("task must not modify process environment")
	}
}
