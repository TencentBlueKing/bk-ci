//go:build linux || darwin
// +build linux darwin

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
	"os/exec"
	"os/user"
	"strconv"
	"strings"
	"syscall"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
)

var envHome = "HOME"
var envUser = "USER"
var envLogName = "LOGNAME"

func setUser(cmd *exec.Cmd, runUser string) error {

	if len(runUser) == 0 { // 传空则直接返回
		return nil
	}
	// 解决重启构建机后，Linux的 /etc/rc.local 自动启动的agent，读取到HOME等系统变量为空的问题
	if runUser == systemutil.GetCurrentUser().Username {
		envHomeFound := false
		envUserFound := false
		envLogNameFound := false
		for i := range cmd.Env {
			splits := strings.Split(cmd.Env[i], "=")
			if splits[0] == envHome && len(splits[1]) > 0 {
				envHomeFound = true
			} else if splits[0] == envUser && len(splits[1]) > 0 {
				envUserFound = true
			} else if splits[0] == envLogName && len(splits[1]) > 0 {
				envLogNameFound = true
			}
		}
		if envHomeFound && envUserFound && envLogNameFound {
			return nil
		}
	}

	logs.Info("set user(linux or darwin): ", runUser)

	rUser, err := user.Lookup(runUser)
	if err != nil {
		logs.Error("user lookup failed, user: -", runUser, "-, error: ", err.Error())
		return errors.New("user lookup failed, user: " + runUser)
	}
	uid, _ := strconv.Atoi(rUser.Uid)
	gid, _ := strconv.Atoi(rUser.Gid)
	cmd.SysProcAttr = &syscall.SysProcAttr{}
	cmd.SysProcAttr.Credential = &syscall.Credential{Uid: uint32(uid), Gid: uint32(gid)}

	cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", envHome, rUser.HomeDir))
	cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", envUser, runUser))
	cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", envLogName, runUser))

	return nil
}
