package job

import (
	"os"
	"strings"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/job_docker"
)

func TestBuildNoMountPreservesRequiredFilesAndCustomVolumes(t *testing.T) {
	oldConfig := config.GAgentConfig
	oldDir, err := os.Getwd()
	if err != nil {
		t.Fatal(err)
	}
	defer func() { config.GAgentConfig = oldConfig; _ = os.Chdir(oldDir) }()
	if err = os.Chdir(t.TempDir()); err != nil {
		t.Fatal(err)
	}
	config.GAgentConfig = &config.AgentConfig{JdkDirPath: "/test/jdk"}
	info := &api.ThirdPartyBuildInfo{BuildId: "b1", PipelineId: "p1", VmSeqId: "v1", Workspace: constant.DockerNoMount}
	args, err := parseContainerMountArgs(info)
	if err != nil {
		t.Fatal(err)
	}
	joined := strings.Join(args, " ")
	if strings.Contains(joined, "target="+constant.DockerDataDir) || strings.Contains(joined, constant.DockerNoMount) {
		t.Fatalf("automatic workspace bind must be absent: %s", joined)
	}
	for _, required := range []string{"target=/usr/local/jre", "target=/data/worker-agent.jar", "target=/data/init.sh", "target=" + job_docker.DockerLogDir} {
		if !strings.Contains(joined, required) {
			t.Fatalf("required mount lost: %s", required)
		}
	}
	custom, err := job_docker.BuildUserDockerArgs(api.DockerOptions{Volumes: []string{"/custom:" + constant.DockerDataDir}})
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(strings.Join(custom, " "), "/custom:"+constant.DockerDataDir) {
		t.Fatal("explicit user workspace volume must remain supported")
	}
}
