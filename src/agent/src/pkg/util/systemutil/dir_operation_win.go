//go:build windows
// +build windows

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
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"os"
)

// MkBuildTmpDir 创建构建提供的临时目录
func MkBuildTmpDir() (string, error) {
	tmpDir := fmt.Sprintf("%s/build_tmp", GetWorkDir())
	err := MkDir(tmpDir)
	return tmpDir, err
}

func MkDir(dir string) error {
	err := os.MkdirAll(dir, os.ModePerm)
	return err
}

// Chmod windows go的win实现只有 0400 只读和 0600 读写的区分，所以这里暂时先和0666对比
func Chmod(file string, perm os.FileMode) error {
	stat, err := os.Stat(file)
	if stat != nil && stat.Mode() != 0666 {
		err = os.Chmod(file, perm)
	}
	if err == nil {
		logs.Infof("chmod %o %s ok!", perm, file)
	} else {
		logs.Warnf("chmod %o %s msg: %s", perm, file, err.Error())
	}
	return err
}
