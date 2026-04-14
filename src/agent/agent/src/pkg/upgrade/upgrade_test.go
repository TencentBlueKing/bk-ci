package upgrade

import (
	"os"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

func init() {
	logs.UNTestDebugInit()
}

func TestUpgradeItems_NoChange(t *testing.T) {
	tests := []struct {
		name string
		item upgradeItems
		want bool
	}{
		{"all_false", upgradeItems{}, true},
		{"agent_true", upgradeItems{Agent: true}, false},
		{"worker_true", upgradeItems{Worker: true}, false},
		{"jdk_true", upgradeItems{Jdk: true}, false},
		{"docker_init_true", upgradeItems{DockerInitFile: true}, false},
		{"all_true", upgradeItems{Agent: true, Worker: true, Jdk: true, DockerInitFile: true}, false},
		{"mixed", upgradeItems{Agent: false, Worker: true, Jdk: false, DockerInitFile: false}, false},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.item.NoChange(); got != tt.want {
				t.Errorf("NoChange() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestSyncDockerInitFileMd5_DisabledBuild(t *testing.T) {
	origConfig := config.GAgentConfig
	defer func() { config.GAgentConfig = origConfig }()

	config.GAgentConfig = &config.AgentConfig{EnableDockerBuild: false}
	DockerFileMd5.NeedUpgrade = true
	DockerFileMd5.Md5 = "old"

	err := SyncDockerInitFileMd5()
	if err != nil {
		t.Fatalf("SyncDockerInitFileMd5() error = %v", err)
	}
	if DockerFileMd5.NeedUpgrade {
		t.Error("NeedUpgrade should be false when EnableDockerBuild=false")
	}
}

func TestSyncDockerInitFileMd5_PlatformSkip(t *testing.T) {
	origConfig := config.GAgentConfig
	defer func() { config.GAgentConfig = origConfig }()

	config.GAgentConfig = &config.AgentConfig{EnableDockerBuild: true}
	DockerFileMd5.NeedUpgrade = true

	err := SyncDockerInitFileMd5()
	if err != nil {
		t.Fatalf("SyncDockerInitFileMd5() error = %v", err)
	}
	if DockerFileMd5.NeedUpgrade {
		t.Errorf("NeedUpgrade should be false on %s", runtime.GOOS)
	}
}

func TestSyncDockerInitFileMd5_FileNotExist(t *testing.T) {
	origConfig := config.GAgentConfig
	defer func() { config.GAgentConfig = origConfig }()

	tmpDir := t.TempDir()
	origDir, _ := os.Getwd()
	os.Chdir(tmpDir)
	defer os.Chdir(origDir)

	config.GAgentConfig = &config.AgentConfig{EnableDockerBuild: true}
	DockerFileMd5.Md5 = ""
	DockerFileMd5.NeedUpgrade = false

	err := SyncDockerInitFileMd5()
	if err != nil {
		t.Fatalf("SyncDockerInitFileMd5() error = %v", err)
	}
	if !DockerFileMd5.NeedUpgrade {
		t.Error("NeedUpgrade should be true on Linux with EnableDockerBuild=true")
	}
	if DockerFileMd5.Md5 != "" {
		t.Errorf("Md5 should be empty when file doesn't exist, got %q", DockerFileMd5.Md5)
	}
}

func TestSyncDockerInitFileMd5_FileExists(t *testing.T) {
	origConfig := config.GAgentConfig
	defer func() { config.GAgentConfig = origConfig }()

	tmpDir := t.TempDir()
	initFile := filepath.Join(tmpDir, config.DockerInitFile)
	if err := os.WriteFile(initFile, []byte("#!/bin/bash\necho hello"), 0755); err != nil {
		t.Fatal(err)
	}

	origDir, _ := os.Getwd()
	os.Chdir(tmpDir)
	defer os.Chdir(origDir)

	config.GAgentConfig = &config.AgentConfig{EnableDockerBuild: true}
	DockerFileMd5.Md5 = ""
	DockerFileMd5.NeedUpgrade = false

	err := SyncDockerInitFileMd5()
	if err != nil {
		t.Fatalf("SyncDockerInitFileMd5() error = %v", err)
	}
	if !DockerFileMd5.NeedUpgrade {
		t.Error("NeedUpgrade should be true on Linux")
	}
	if DockerFileMd5.Md5 == "" {
		t.Error("Md5 should be non-empty when file exists")
	}
}
