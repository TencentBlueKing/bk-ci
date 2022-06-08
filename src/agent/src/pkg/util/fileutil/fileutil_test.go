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

package fileutil

import (
	"testing"

	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/fileutil"
)

func Test_CopyFile_01(t *testing.T) {
	if _, err := fileutil.CopyFile("d:\\a.conf", "d:\\b.conf", true); err != nil {
		t.Error("failed", err)
	}
}

func Test_Md5_01(t *testing.T) {
	md5, err := fileutil.GetFileMd5("d:\\time.exe")
	if err != nil {
		t.Error("err: ", err.Error())
		return
	}
	t.Log("md5: " + md5)
}

func Test_SetExecutable_01(t *testing.T) {
	md5, err := fileutil.GetFileMd5("d:\\time.exe")
	if err != nil {
		t.Error("err: ", err.Error())
		return
	}
	t.Log("md5: " + md5)
}

func Test_unzip(t *testing.T) {
	err := fileutil.Unzip("/Users/xxx/Downloads/1/agent.zip", "/Users/xxx/Downloads/1/")
	if err != nil {
		t.Error("err: ", err.Error())
		return
	}
}
