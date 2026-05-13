//go:build darwin && !cgo
// +build darwin,!cgo

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
	"bytes"
	"fmt"
	"os/exec"
	"strings"

	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

// 此实现用于 CGO 关闭的 darwin 构建（如 build_macos_no_cgo 或交叉编译），
// 通过调用系统自带的 /usr/bin/codesign 命令行做签名校验。
//
// 流程：
//  1. codesign --verify --deep --strict --no-cache-path  → 链完整性校验
//  2. codesign -dv --verbose=4  → 解析 TeamIdentifier，与 config.MacosTeamId 比对
//
// 相比 CGO 方案的局限：
//  - 多一次 fork/exec 开销（可忽略，启动路径）
//  - codesign 文本输出格式理论上可能变动（苹果很少改）

const codesignPath = "/usr/bin/codesign"

// Verify 调用 /usr/bin/codesign 校验签名和 Team ID。
// 若 config.MacosTeamId 为空则跳过校验返回 nil。
func Verify(path string) error {
	teamID := strings.TrimSpace(config.MacosTeamId)
	if teamID == "" {
		logDisabledOnce("MacosTeamId empty")
		return nil
	}
	if err := preCheckPath(path); err != nil {
		return err
	}

	// 1. 链 / 完整性校验
	//    --strict          拒绝资源目录被篡改
	//    --deep            递归校验嵌套组件（与 CGO 实现的 kSecCSCheckNestedCode 对齐）
	//    --no-cache-path   不用系统缓存，强制重新读取文件计算哈希
	//    codesign 的 CLI 默认就会校验所有架构，无需额外 flag
	if out, err := runCodesign("--verify", "--deep", "--strict", "--no-cache-path", path); err != nil {
		return errors.Wrapf(err, "codesign --verify failed: %s", strings.TrimSpace(out))
	}

	// 2. 提取 TeamIdentifier
	out, err := runCodesign("-dv", "--verbose=4", path)
	if err != nil {
		return errors.Wrapf(err, "codesign -dv failed: %s", strings.TrimSpace(out))
	}
	actual := parseTeamIdentifier(out)
	if actual == "" {
		return fmt.Errorf("codesign: no TeamIdentifier in codesign output for %s", path)
	}
	if !strings.EqualFold(actual, teamID) {
		return fmt.Errorf("codesign: TeamIdentifier mismatch: expected %q, got %q", teamID, actual)
	}

	logs.Infof("codesign|signature check passed for %s, teamId=%s", path, actual)
	return nil
}

// runCodesign 运行 codesign，合并 stdout+stderr（codesign 默认把信息写到 stderr）。
func runCodesign(args ...string) (string, error) {
	var buf bytes.Buffer
	cmd := exec.Command(codesignPath, args...)
	cmd.Stdout = &buf
	cmd.Stderr = &buf
	err := cmd.Run()
	return buf.String(), err
}

// parseTeamIdentifier 从 codesign -dv 输出中抽取 `TeamIdentifier=XXXXXXXXXX` 行。
// 未签名或无 Team ID 的二进制可能输出 `TeamIdentifier=not set`，此时返回空串。
func parseTeamIdentifier(out string) string {
	for _, line := range strings.Split(out, "\n") {
		line = strings.TrimSpace(line)
		const prefix = "TeamIdentifier="
		if !strings.HasPrefix(line, prefix) {
			continue
		}
		val := strings.TrimSpace(strings.TrimPrefix(line, prefix))
		if val == "" || strings.EqualFold(val, "not set") {
			return ""
		}
		return val
	}
	return ""
}
