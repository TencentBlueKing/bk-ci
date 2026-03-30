/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
	"os"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

func TestGetExecutableDir_AlwaysAbsolute(t *testing.T) {
	tests := []struct {
		name    string
		argv0   string
		wantAbs bool
	}{
		{"relative_dot_slash", "./devopsAgent", true},
		{"relative_subdir", "bin/devopsAgent", true},
		{"absolute_path", "/usr/local/bin/devopsAgent", true},
		{"bare_name_no_slash", "devopsAgent", true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			origArg0 := os.Args[0]
			origCache := GExecutableDir
			defer func() {
				os.Args[0] = origArg0
				GExecutableDir = origCache
			}()

			GExecutableDir = ""
			os.Args[0] = tt.argv0

			got := GetExecutableDir()
			if filepath.IsAbs(got) != tt.wantAbs {
				t.Errorf("GetExecutableDir() with Args[0]=%q = %q, absolute=%v, want absolute=%v",
					tt.argv0, got, filepath.IsAbs(got), tt.wantAbs)
			}
			if got == "" {
				t.Errorf("GetExecutableDir() with Args[0]=%q returned empty string", tt.argv0)
			}
		})
	}
}

func TestGetExecutableDir_Cached(t *testing.T) {
	origCache := GExecutableDir
	defer func() { GExecutableDir = origCache }()

	GExecutableDir = "/test/cached/path"
	got := GetExecutableDir()
	if got != "/test/cached/path" {
		t.Errorf("GetExecutableDir() = %q, want cached value %q", got, "/test/cached/path")
	}
}

func TestGetExecutableDir_AbsoluteUnchanged(t *testing.T) {
	origArg0 := os.Args[0]
	origCache := GExecutableDir
	defer func() {
		os.Args[0] = origArg0
		GExecutableDir = origCache
	}()

	GExecutableDir = ""

	var testPath, wantDir string
	if runtime.GOOS == "windows" {
		testPath = `C:\opt\bk-ci\agent\devopsAgent.exe`
		wantDir = `C:\opt\bk-ci\agent`
	} else {
		testPath = "/opt/bk-ci/agent/devopsAgent"
		wantDir = "/opt/bk-ci/agent"
	}

	os.Args[0] = testPath
	got := filepath.Clean(GetExecutableDir())
	want := filepath.Clean(wantDir)
	if got != want {
		t.Errorf("GetExecutableDir() = %q, want %q", got, want)
	}
}

func TestGetAgentIp(t *testing.T) {
	logs.UNTestDebugInit()
	type args struct {
		ignoreIps []string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "测试获取agentIP默认值",
			args: args{[]string{}},
			want: "127.0.0.1",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := GetAgentIp(tt.args.ignoreIps); got == tt.want {
				t.Errorf("GetAgentIp() = %v, no want %v", got, tt.want)
			}
		})
	}
}
