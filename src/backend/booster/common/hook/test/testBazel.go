// +build ignore

/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package main

import (
	"fmt"
	"github.com/Tencent/bk-ci/src/booster/common/hook"
)

func main() {
	ldPreloadSo := "/data/home/tomtian/hook/hook.so"
	configPath := "/data/home/tomtian/hook/config_file.json"
	envs := make(map[string]string)
	envs[hook.EnvKeyDistccHost] = "--randomize 127.0.0.1:3632,lzo"
	bazelCmd := "bazel build main:all -s"

	_, err := hook.RunBazelProcess(ldPreloadSo, configPath, envs, bazelCmd)
	if err != nil {
		fmt.Printf("failed to run bazel cmd for [%s]\n", err.Error())
	}
}
