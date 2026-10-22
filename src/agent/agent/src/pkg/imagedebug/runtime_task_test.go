package imagedebug

import (
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
)

func TestConsoleUsesTaskRuntimeWithoutChangingExistingSessions(t *testing.T) {
	logs.UNTestDebugInit()
	Init()
	t.Setenv(constant.DevopsAgentContainerRuntime, "docker")
	podman := NewManager(&ConsoleProxyConfig{ContainerRuntime: "podman"}, NewOnceChan[struct{}]()).(*manager)
	if err := podman.Start(); err != nil {
		t.Fatal(err)
	}
	docker := NewManager(&ConsoleProxyConfig{ContainerRuntime: "docker"}, NewOnceChan[struct{}]()).(*manager)
	if err := docker.Start(); err != nil {
		t.Fatal(err)
	}
	if podman.runner.Binary() != "podman" || docker.runner.Binary() != "docker" {
		t.Fatal("console must use the engine selected for its debug task")
	}
}
