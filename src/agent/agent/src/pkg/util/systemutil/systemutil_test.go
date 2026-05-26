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
	"net"
	"os"
	"path/filepath"
	"runtime"
	"testing"
	"time"

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

func TestGetAgentIpCache(t *testing.T) {
	logs.UNTestDebugInit()
	origResolver := resolveAgentIPValue
	origNow := agentIPNow
	origTTL := agentIPCacheTTL
	defer func() {
		resolveAgentIPValue = origResolver
		agentIPNow = origNow
		agentIPCacheTTL = origTTL
		resetAgentIPCache()
	}()

	resetAgentIPCache()
	now := time.Date(2026, 4, 27, 13, 0, 0, 0, time.UTC)
	agentIPNow = func() time.Time { return now }
	agentIPCacheTTL = 5 * time.Second

	callCount := 0
	resolveAgentIPValue = func(ignoreIps []string) string {
		callCount++
		return "10.0.0.8"
	}

	if got := GetAgentIp([]string{"192.168.128.0/24"}); got != "10.0.0.8" {
		t.Fatalf("GetAgentIp()=%s", got)
	}
	if got := GetAgentIp([]string{"192.168.128.0/24"}); got != "10.0.0.8" {
		t.Fatalf("GetAgentIp()=%s", got)
	}
	if callCount != 1 {
		t.Fatalf("resolver call count=%d, want 1", callCount)
	}

	now = now.Add(6 * time.Second)
	if got := GetAgentIp([]string{"192.168.128.0/24"}); got != "10.0.0.8" {
		t.Fatalf("GetAgentIp()=%s", got)
	}
	if callCount != 2 {
		t.Fatalf("resolver call count=%d, want 2", callCount)
	}
}

func TestGetAgentIpCacheKey(t *testing.T) {
	tests := []struct {
		name      string
		ignoreIps []string
		want      string
	}{
		{name: "空列表", ignoreIps: nil, want: ""},
		{name: "排序后相同", ignoreIps: []string{"10.0.0.1", "192.168.0.0/24"}, want: "10.0.0.1,192.168.0.0/24"},
		{name: "顺序归一化", ignoreIps: []string{"192.168.0.0/24", "10.0.0.1"}, want: "10.0.0.1,192.168.0.0/24"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := buildAgentIPCacheKey(tt.ignoreIps); got != tt.want {
				t.Fatalf("buildAgentIPCacheKey()=%q, want %q", got, tt.want)
			}
		})
	}
}

func TestShouldIgnoreIP(t *testing.T) {
	tests := []struct {
		name      string
		ip        string
		ignoreIps []string
		want      bool
	}{
		{
			name:      "精确匹配",
			ip:        "192.168.128.1",
			ignoreIps: []string{"192.168.128.1"},
			want:      true,
		},
		{
			name:      "CIDR匹配",
			ip:        "192.168.128.1",
			ignoreIps: []string{"192.168.128.0/24"},
			want:      true,
		},
		{
			name:      "不匹配",
			ip:        "10.0.0.12",
			ignoreIps: []string{"192.168.128.0/24"},
			want:      false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := shouldIgnoreIP(tt.ip, tt.ignoreIps); got != tt.want {
				t.Errorf("shouldIgnoreIP() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestBuildAgentIPCandidate(t *testing.T) {
	tests := []struct {
		name    string
		iface   net.Interface
		ip      net.IP
		routeIp string
		detail  agentInterfaceDetail
		want    int
	}{
		{
			name: "物理网卡且命中出站IP",
			iface: net.Interface{
				Name:         "Ethernet0",
				Flags:        net.FlagUp | net.FlagBroadcast,
				HardwareAddr: net.HardwareAddr{0x00, 0x11, 0x22, 0x33, 0x44, 0x55},
			},
			ip:      net.ParseIP("10.0.0.8"),
			routeIp: "10.0.0.8",
			detail: agentInterfaceDetail{
				friendlyName: "Ethernet0",
				description:  "Intel Ethernet",
				ifType:       6,
				isVirtual:    false,
			},
			want: 380,
		},
		{
			name: "虚拟网卡降权",
			iface: net.Interface{
				Name:         "vEthernet (Default Switch)",
				Flags:        net.FlagUp | net.FlagBroadcast,
				HardwareAddr: net.HardwareAddr{0x00, 0x15, 0x5d, 0x00, 0x00, 0x01},
			},
			ip:      net.ParseIP("192.168.128.1"),
			routeIp: "192.168.128.1",
			detail: agentInterfaceDetail{
				friendlyName: "vEthernet (Default Switch)",
				description:  "Hyper-V Virtual Ethernet Adapter",
				ifType:       6,
				isVirtual:    true,
			},
			want: -20,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := buildAgentIPCandidate(tt.iface, tt.ip, tt.routeIp, tt.detail)
			if got.score != tt.want {
				t.Errorf("buildAgentIPCandidate().score = %d, want %d", got.score, tt.want)
			}
		})
	}
}

func TestAgentInterfaceLookupGet(t *testing.T) {
	lookup := agentInterfaceLookup{
		byIndex: map[int]agentInterfaceDetail{
			7: {
				friendlyName: "Ethernet 7",
				description:  "Intel Ethernet",
				isVirtual:    false,
			},
		},
		byName: map[string]agentInterfaceDetail{
			"ethernet backup": {
				friendlyName: "Ethernet Backup",
				description:  "Realtek Ethernet",
				isVirtual:    false,
			},
		},
	}

	byIndex := lookup.get(net.Interface{Index: 7, Name: "ignored"})
	if byIndex.friendlyName != "Ethernet 7" {
		t.Fatalf("lookup by index failed: %+v", byIndex)
	}

	byName := lookup.get(net.Interface{Index: 99, Name: "Ethernet Backup"})
	if byName.description != "Realtek Ethernet" {
		t.Fatalf("lookup by name failed: %+v", byName)
	}

	fallback := lookup.get(net.Interface{Index: 88, Name: "vEthernet (Default Switch)"})
	if !fallback.isVirtual {
		t.Fatalf("fallback should detect virtual interface: %+v", fallback)
	}
}

func TestIsLikelyVirtualInterface(t *testing.T) {
	tests := []struct {
		name string
		want bool
	}{
		{name: "vEthernet (Default Switch)", want: true},
		{name: "NGNClient Adapter", want: true},
		{name: "Ethernet0", want: false},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := isLikelyVirtualInterface(tt.name); got != tt.want {
				t.Fatalf("isLikelyVirtualInterface(%q)=%v, want %v", tt.name, got, tt.want)
			}
		})
	}
}
