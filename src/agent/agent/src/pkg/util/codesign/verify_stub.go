//go:build !windows && !darwin
// +build !windows,!darwin

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

// Verify 在 Linux 等未支持签名校验的平台上始终返回 nil。
// 首次调用时输出一次 Info 日志，便于排查。
func Verify(path string) error {
	logDisabledOnce("platform does not support code signature verification")
	return nil
}
