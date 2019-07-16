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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
	"github.com/astaxie/beego/logs"
	"os/exec"
	"os/user"
	"pkg/util/systemutil"
	"strconv"
	"syscall"
)

func setUser(cmd *exec.Cmd, runUser string) error {
	logs.Info("set user(linux or darwin): ", runUser)
	if len(runUser) == 0 || runUser == systemutil.GetCurrentUser().Username {
		return nil
	}

	user, err := user.Lookup(runUser)
	if err != nil {
		logs.Error("user lookup failed, user: -", runUser, "-, error: ", err.Error())
		return errors.New("user lookup failed, user: " + runUser)
	}
	uid, _ := strconv.Atoi(user.Uid)
	gid, _ := strconv.Atoi(user.Gid)
	cmd.SysProcAttr = &syscall.SysProcAttr{}
	cmd.SysProcAttr.Credential = &syscall.Credential{Uid: uint32(uid), Gid: uint32(gid)}

	cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", "HOME", user.HomeDir))
	cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", "USER", runUser))
	cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", "USERNAME", runUser))
	cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", "LOGNAME", runUser))

	return nil
}
