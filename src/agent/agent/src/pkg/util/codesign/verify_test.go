/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package codesign

import (
	"os"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

// TestVerify_EmptyAnchor 空信任锚时 Verify 必须返回 nil，且不对 path 做任何校验。
// 此时传一个不存在的路径也不应该报错（空锚路径连 preCheckPath 都不走）。
func TestVerify_EmptyAnchor(t *testing.T) {
	origWin := config.WinCertOrgName
	origMac := config.MacosTeamId
	t.Cleanup(func() {
		config.WinCertOrgName = origWin
		config.MacosTeamId = origMac
	})
	config.WinCertOrgName = ""
	config.MacosTeamId = ""

	if err := Verify("/path/does/not/exist"); err != nil {
		t.Fatalf("Verify with empty anchor should be nil, got: %v", err)
	}
}

// TestPreCheckPath_RejectSymlink 验证公共层拒绝符号链接。
// 仅在支持 symlink 且有校验锚的平台下运行（Windows 对 symlink 要求管理员，跳过）。
func TestPreCheckPath_RejectSymlink(t *testing.T) {
	if runtime.GOOS == "windows" {
		t.Skip("symlink creation on Windows requires elevated privileges")
	}
	dir := t.TempDir()
	real := filepath.Join(dir, "real")
	if err := os.WriteFile(real, []byte("x"), 0600); err != nil {
		t.Fatalf("write real file: %v", err)
	}
	link := filepath.Join(dir, "link")
	if err := os.Symlink(real, link); err != nil {
		t.Fatalf("symlink: %v", err)
	}

	if err := preCheckPath(link); err == nil {
		t.Fatal("preCheckPath should reject symlink")
	}
}

// TestPreCheckPath_NotFound 路径不存在应报错。
func TestPreCheckPath_NotFound(t *testing.T) {
	if err := preCheckPath(filepath.Join(t.TempDir(), "nope")); err == nil {
		t.Fatal("preCheckPath should error on missing file")
	}
}

// TestPreCheckPath_Regular 正常文件应通过。
func TestPreCheckPath_Regular(t *testing.T) {
	dir := t.TempDir()
	p := filepath.Join(dir, "f")
	if err := os.WriteFile(p, []byte("x"), 0600); err != nil {
		t.Fatalf("write: %v", err)
	}
	if err := preCheckPath(p); err != nil {
		t.Fatalf("preCheckPath regular file failed: %v", err)
	}
}
