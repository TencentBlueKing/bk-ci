package upgrade

import (
	"os"
	"path/filepath"
	"testing"
	"time"

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

// resetDockerFileMd5 重置全局缓存状态，避免用例间相互污染
func resetDockerFileMd5() {
	DockerFileMd5.NeedUpgrade = false
	DockerFileMd5.Md5 = ""
	DockerFileMd5.FileModTime = time.Time{}
}

func TestSyncDockerInitFileMd5_DisabledBuild(t *testing.T) {
	origConfig := config.GAgentConfig
	defer func() { config.GAgentConfig = origConfig }()
	defer resetDockerFileMd5()

	config.GAgentConfig = &config.AgentConfig{EnableDockerBuild: false}
	DockerFileMd5.NeedUpgrade = true
	DockerFileMd5.Md5 = "old"

	if err := SyncDockerInitFileMd5(); err != nil {
		t.Fatalf("SyncDockerInitFileMd5() error = %v", err)
	}
	if DockerFileMd5.NeedUpgrade {
		t.Error("NeedUpgrade should be false when EnableDockerBuild=false")
	}
}

func TestSyncDockerInitFileMd5_FileNotExist(t *testing.T) {
	origConfig := config.GAgentConfig
	defer func() { config.GAgentConfig = origConfig }()
	defer resetDockerFileMd5()

	tmpDir := t.TempDir()
	origDir, _ := os.Getwd()
	_ = os.Chdir(tmpDir)
	defer func() { _ = os.Chdir(origDir) }()

	config.GAgentConfig = &config.AgentConfig{EnableDockerBuild: true}
	DockerFileMd5.Md5 = ""
	DockerFileMd5.NeedUpgrade = false

	if err := SyncDockerInitFileMd5(); err != nil {
		t.Fatalf("SyncDockerInitFileMd5() error = %v", err)
	}
	if !DockerFileMd5.NeedUpgrade {
		t.Error("NeedUpgrade should be true when EnableDockerBuild=true")
	}
	if DockerFileMd5.Md5 != "" {
		t.Errorf("Md5 should be empty when file doesn't exist, got %q", DockerFileMd5.Md5)
	}
}

func TestSyncDockerInitFileMd5_FileExists(t *testing.T) {
	origConfig := config.GAgentConfig
	defer func() { config.GAgentConfig = origConfig }()
	defer resetDockerFileMd5()

	tmpDir := t.TempDir()
	initFile := filepath.Join(tmpDir, config.DockerInitFile)
	if err := os.WriteFile(initFile, []byte("#!/bin/bash\necho hello"), 0755); err != nil {
		t.Fatal(err)
	}

	origDir, _ := os.Getwd()
	_ = os.Chdir(tmpDir)
	defer func() { _ = os.Chdir(origDir) }()

	config.GAgentConfig = &config.AgentConfig{EnableDockerBuild: true}
	DockerFileMd5.Md5 = ""
	DockerFileMd5.NeedUpgrade = false

	if err := SyncDockerInitFileMd5(); err != nil {
		t.Fatalf("SyncDockerInitFileMd5() error = %v", err)
	}
	if !DockerFileMd5.NeedUpgrade {
		t.Error("NeedUpgrade should be true when EnableDockerBuild=true")
	}
	if DockerFileMd5.Md5 == "" {
		t.Error("Md5 should be non-empty when file exists")
	}
}

// TestSyncDockerInitFileMd5_CacheHit 验证文件未修改时使用缓存，不重新计算 MD5
func TestSyncDockerInitFileMd5_CacheHit(t *testing.T) {
	origConfig := config.GAgentConfig
	defer func() { config.GAgentConfig = origConfig }()
	defer resetDockerFileMd5()

	tmpDir := t.TempDir()
	initFile := filepath.Join(tmpDir, config.DockerInitFile)
	if err := os.WriteFile(initFile, []byte("#!/bin/bash\necho hello"), 0755); err != nil {
		t.Fatal(err)
	}

	origDir, _ := os.Getwd()
	_ = os.Chdir(tmpDir)
	defer func() { _ = os.Chdir(origDir) }()

	config.GAgentConfig = &config.AgentConfig{EnableDockerBuild: true}
	resetDockerFileMd5()

	// 第一次调用，计算并缓存 MD5
	if err := SyncDockerInitFileMd5(); err != nil {
		t.Fatalf("first call error = %v", err)
	}
	firstMd5 := DockerFileMd5.Md5
	if firstMd5 == "" {
		t.Fatal("first call: Md5 should be non-empty")
	}

	// 篡改缓存 MD5，用于验证第二次调用是否真的走了缓存（没有重新计算）
	DockerFileMd5.Md5 = "cached-sentinel"

	// 第二次调用，文件未改动，应命中缓存，Md5 保持 "cached-sentinel"
	if err := SyncDockerInitFileMd5(); err != nil {
		t.Fatalf("second call error = %v", err)
	}
	if DockerFileMd5.Md5 != "cached-sentinel" {
		t.Errorf("cache hit: Md5 should remain %q, got %q", "cached-sentinel", DockerFileMd5.Md5)
	}
}

// TestSyncDockerInitFileMd5_CacheInvalidated 验证文件修改后缓存失效，MD5 重新计算
func TestSyncDockerInitFileMd5_CacheInvalidated(t *testing.T) {
	origConfig := config.GAgentConfig
	defer func() { config.GAgentConfig = origConfig }()
	defer resetDockerFileMd5()

	tmpDir := t.TempDir()
	initFile := filepath.Join(tmpDir, config.DockerInitFile)
	if err := os.WriteFile(initFile, []byte("#!/bin/bash\necho v1"), 0755); err != nil {
		t.Fatal(err)
	}

	origDir, _ := os.Getwd()
	_ = os.Chdir(tmpDir)
	defer func() { _ = os.Chdir(origDir) }()

	config.GAgentConfig = &config.AgentConfig{EnableDockerBuild: true}
	resetDockerFileMd5()

	// 第一次调用，建立缓存
	if err := SyncDockerInitFileMd5(); err != nil {
		t.Fatalf("first call error = %v", err)
	}
	firstMd5 := DockerFileMd5.Md5
	if firstMd5 == "" {
		t.Fatal("first call: Md5 should be non-empty")
	}

	// 等待 1 纳秒以上再写文件，确保 ModTime 变化（大多数 FS 精度足够）
	// 写入不同内容，ModTime 一定会变
	time.Sleep(10 * time.Millisecond)
	if err := os.WriteFile(initFile, []byte("#!/bin/bash\necho v2"), 0755); err != nil {
		t.Fatal(err)
	}

	// 第二次调用，文件已变更，缓存应失效，MD5 重新计算
	if err := SyncDockerInitFileMd5(); err != nil {
		t.Fatalf("second call error = %v", err)
	}
	secondMd5 := DockerFileMd5.Md5
	if secondMd5 == "" {
		t.Error("cache invalidated: Md5 should be non-empty after file change")
	}
	if secondMd5 == firstMd5 {
		t.Errorf("cache invalidated: Md5 should change after file content change, both = %q", firstMd5)
	}
}
