/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

// Package codesign 提供对 agent/daemon/upgrader 二进制的代码签名校验能力。
//
// 信任锚通过 -ldflags 注入到 config 包：
//   - Windows 用 config.WinCertOrgName 匹配证书 Subject 的 O 字段（Authenticode）
//   - macOS   用 config.MacosTeamId    匹配开发者 Team ID（codesign/SecCode）
//
// 对应信任锚为空时 Verify 返回 nil，并在首次调用时输出一条 Info 日志提示。
// Linux 及其他平台走 stub 实现，永远返回 nil。
package codesign

import (
	"fmt"
	"os"
	"sync"

	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

var disabledLogOnce sync.Once

// logDisabledOnce 首次调用时输出一次签名校验被跳过的日志，避免刷屏。
func logDisabledOnce(reason string) {
	disabledLogOnce.Do(func() {
		logs.Infof("codesign|signature check disabled: %s", reason)
	})
}

// preCheckPath 在调用平台相关实现前做通用检查：
//   - 文件存在且不是符号链接（避免 TOCTOU / symlink-following 攻击）
//   - 必须是常规文件
//
// 返回值 path 即入参，便于链式写法。
func preCheckPath(path string) error {
	if path == "" {
		return errors.New("codesign: empty path")
	}
	info, err := os.Lstat(path)
	if err != nil {
		return errors.Wrapf(err, "codesign: lstat %s failed", path)
	}
	if info.Mode()&os.ModeSymlink != 0 {
		return fmt.Errorf("codesign: %s is a symlink, refused", path)
	}
	if !info.Mode().IsRegular() {
		return fmt.Errorf("codesign: %s is not a regular file", path)
	}
	return nil
}
