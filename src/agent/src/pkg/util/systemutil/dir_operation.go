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

package systemutil

import (
	"fmt"
	"os"
	"syscall"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
)

// MkBuildTmpDir 创建构建提供的临时目录
// 对于指定构建帐号与当前agent运行帐号不同时，常用是用root安装运行agent，但配置文件中devops.slave.user指定其他普通帐号
// 需要设置最大权限，以便任何runUser能够使用, 不考虑用chown切换目录属主，会导致之前的运行中所产生的子目录/文件的清理权限问题。
func MkBuildTmpDir() (string, error) {
	tmpDir := fmt.Sprintf("%s/build_tmp", GetWorkDir())
	err := os.MkdirAll(tmpDir, os.ModePerm)
	err2 := Chmod(tmpDir, os.ModePerm)
	if err == nil && err2 != nil {
		err = err2
	}
	return tmpDir, err
}

func MkDir(dir string) error {
	err := os.MkdirAll(dir, os.ModePerm)
	if err != nil {
		return err
	}
	err = Chmod(dir, os.ModePerm)
	if err != nil {
		return err
	}
	return nil
}

// Chmod 对指定file进行修改权限
func Chmod(file string, perm os.FileMode) error {
	stat, err := os.Stat(file)
	if stat != nil && stat.Mode() != perm { // 修正目录权限
		mask := syscall.Umask(0)   // 临时消除用户权限掩码
		defer syscall.Umask(mask)  // 重置掩码
		err = os.Chmod(file, perm) // 修改权限
	}
	if err == nil {
		logs.Infof("chmod %o %s ok!", perm, file)
	} else {
		logs.Warnf("chmod %o %s msg: %s", perm, file, err.Error())
	}
	return err
}
