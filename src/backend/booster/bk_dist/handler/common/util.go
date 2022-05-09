/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package common

import (
	"fmt"
	"os"
	"path"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
)

// GetHandlerEnv get env by booster type
func GetHandlerEnv(sandBox *dcSyscall.Sandbox, key string) string {
	format := "%s_%s"
	if sandBox == nil || sandBox.Env == nil {
		return env.GetEnv(fmt.Sprintf(format, strings.ToUpper(env.GetEnv(env.BoosterType)), key))
	}

	return sandBox.Env.GetEnv(fmt.Sprintf(format, strings.ToUpper(sandBox.Env.GetEnv(env.BoosterType)), key))
}

// GetHandlerTmpDir get temp dir by booster type
func GetHandlerTmpDir(sandBox *dcSyscall.Sandbox) string {
	var baseTmpDir, bt string
	if sandBox == nil {
		baseTmpDir = os.TempDir()
		bt = env.GetEnv(env.BoosterType)
	} else {
		if baseTmpDir = sandBox.Env.GetOriginEnv("TMPDIR"); baseTmpDir == "" {
			baseTmpDir = os.TempDir()
		}

		bt = sandBox.Env.GetEnv(env.BoosterType)
	}

	if baseTmpDir != "" {
		fullTmpDir := path.Join(baseTmpDir, protocol.BKDistDir, types.GetBoosterType(bt).String())
		if !dcFile.Stat(fullTmpDir).Exist() {
			if err := os.MkdirAll(fullTmpDir, os.ModePerm); err != nil {
				return ""
			}
		}
		return fullTmpDir
	}

	return ""
}
