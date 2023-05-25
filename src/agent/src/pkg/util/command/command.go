/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package command

import (
	"errors"
	"fmt"
	"os"
	"os/exec"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
)

func RunCommand(command string, args []string, workDir string, envMap map[string]string) (output []byte, err error) {
	cmd := exec.Command(command)

	if len(args) > 0 {
		cmd.Args = append(cmd.Args, args...)
	}

	if workDir != "" {
		cmd.Dir = workDir
	}

	cmd.Env = os.Environ()
	if envMap != nil {
		for k, v := range envMap {
			cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", k, v))
		}
	}

	logs.Info("cmd.Path: ", cmd.Path)
	logs.Info("cmd.Args: ", cmd.Args)
	logs.Info("cmd.workDir: ", cmd.Dir)

	outPut, err := cmd.CombinedOutput()
	logs.Info("output: ", string(outPut))
	if err != nil {
		return outPut, err
	}

	return outPut, nil
}

func StartProcess(command string, args []string, workDir string, envMap map[string]string, runUser string) (int, error) {
	cmd := exec.Command(command)

	if len(args) > 0 {
		cmd.Args = append(cmd.Args, args...)
	}

	if workDir != "" {
		cmd.Dir = workDir
	}

	cmd.Env = os.Environ()
	if envMap != nil {
		for k, v := range envMap {
			cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", k, v))
		}
	}

	err := setUser(cmd, runUser)
	if err != nil {
		logs.Error("set user failed: ", err.Error())
		return -1, errors.New(
			fmt.Sprintf("%s, Please check [devops.slave.user] in the {agent_dir}/.agent.properties", err.Error()))
	}

	logs.Info("cmd.Path: ", cmd.Path)
	logs.Info("cmd.Args: ", cmd.Args)
	logs.Info("cmd.workDir: ", cmd.Dir)
	logs.Info("runUser: ", runUser)

	err = cmd.Start()
	if err != nil {
		return -1, err
	}
	return cmd.Process.Pid, nil
}
