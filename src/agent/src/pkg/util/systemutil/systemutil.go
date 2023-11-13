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
	"errors"
	"fmt"
	"net"
	"net/url"
	"os"
	"os/user"
	"runtime"
	"strings"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/fileutil"

	"github.com/gofrs/flock"
)

var GExecutableDir string

var DevopsGateway string

const (
	osWindows = "windows"
	osLinux   = "linux"
	osMacos   = "darwin"
	osOther   = "other"

	osNameWindows = "windows"
	osNameLinux   = "linux"
	osNameMacos   = "macos"
	osNameOther   = "other"

	TotalLock = "total-lock"
)

// IsWindows 是否是Windows OS
func IsWindows() bool {
	return runtime.GOOS == osWindows
}

// IsLinux IsLinux
func IsLinux() bool {
	return runtime.GOOS == osLinux
}

// IsMacos IsMacos
func IsMacos() bool {
	return runtime.GOOS == osMacos
}

// GetCurrentUser get current process user, log & exit when error was found
func GetCurrentUser() *user.User {
	currentUser, err := user.Current()
	if currentUser == nil {
		logs.Fatalf("GetCurrentUser cache return nil: error[%v]", err) //
	}
	return currentUser
}

// GetWorkDir get agent work dir
func GetWorkDir() string {
	dir, _ := os.Getwd()
	return strings.Replace(dir, "\\", "/", -1)
}

// GetUpgradeDir get upgrader dir
func GetUpgradeDir() string {
	return GetWorkDir() + "/tmp"
}

// GetLogDir get agent logs dir
func GetLogDir() string {
	return GetWorkDir() + "/logs"
}

// GetRuntimeDir get agent runtime dir
func GetRuntimeDir() string {
	runDir := fmt.Sprintf("%s/runtime", GetWorkDir())
	if err := os.MkdirAll(runDir, 0755); err == nil {
		return runDir
	}
	return GetWorkDir()
}

// GetExecutableDir get excutable jar dir
func GetExecutableDir() string {
	if len(GExecutableDir) == 0 {
		executable := strings.Replace(os.Args[0], "\\", "/", -1)
		index := strings.LastIndex(executable, "/")
		GExecutableDir = executable[0:index]
	}
	return GExecutableDir
}

// GetOsName GetOsName
func GetOsName() string {
	switch runtime.GOOS {
	case osLinux:
		return osNameLinux
	case osWindows:
		return osNameWindows
	case osMacos:
		return osNameMacos
	default:
		return osNameOther
	}
}

// GetOs get os
func GetOs() string {
	switch runtime.GOOS {
	case osLinux:
		return osLinux
	case osWindows:
		return osWindows
	case osMacos:
		return osMacos
	default:
		return osOther
	}
}

// GetHostName GetHostName
func GetHostName() string {
	name, _ := os.Hostname()
	return name
}

// GetAgentIp 返回本机IP，但允许忽略指定的IP ignoreIps, 如果所有IP都命中ignoreIps，则最终会返回127.0.0.1或者真正通信的IP
func GetAgentIp(ignoreIps []string) string {
	defaultIp := "127.0.0.1"
	ip, err := getLocalIp()
	if err == nil {
		defaultIp = ip
		logs.Infof("get local ip %s", defaultIp)
	} else {
		logs.Warn("failed to get ip by udp", err)
	}

	ncs, err := net.Interfaces()
	if err != nil {
		return defaultIp
	}
	for _, nc := range ncs {
		if nc.HardwareAddr == nil { // #3626 二次确认，需要排除虚拟网卡情况
			logs.Infof("%s have no MAC, skip!", nc.Name)
			continue
		}
		addresses, err := nc.Addrs()
		if err != nil {
			logs.Warnf("can not get addr for [%s]: %s", nc.Name, err.Error())
			continue
		}

		for _, addr := range addresses {
			ipNet, ok := addr.(*net.IPNet)
			if !ok || // 异常
				ipNet.IP.IsLoopback() ||
				ipNet.IP.IsLinkLocalUnicast() || // 链路本地地址（Link-local address）
				!ipNet.IP.IsGlobalUnicast() ||
				ipNet.IP.To4() == nil {
				continue
			}

			logs.Infof("localIp=%s|net=%s|flag=%s|ip=%s", ip, nc.Name, nc.Flags, ipNet.IP)
			if util.Contains(ignoreIps, ipNet.IP.String()) {
				logs.Infof("skipIp=%s", ipNet.IP)
				continue
			}
			if ip == ipNet.IP.String() {
				return ip // 匹配到该通信IP是真正的网卡IP
			} else if defaultIp == ip { // 仅限于第一次找到合法ip，做赋值
				logs.Infof("localIp=%s|change defaultIp [%s] to [%s]", ip, defaultIp, ipNet.IP.String())
				defaultIp = ipNet.IP.String()
			}
		}
	}

	return defaultIp
}

// ExitProcess Exit by code
func ExitProcess(exitCode int) {
	os.Exit(exitCode)
}

var processLock *flock.Flock

// KeepProcessAlive keep process alive
func KeepProcessAlive() {
	runtime.KeepAlive(processLock)
}

// CheckProcess check process and lock
func CheckProcess(name string) bool {
	processLockFile := fmt.Sprintf("%s/%s.lock", GetRuntimeDir(), name)
	pidFile := fmt.Sprintf("%s/%s.pid", GetRuntimeDir(), name)

	processLock = flock.New(processLockFile)
	ok, err := processLock.TryLock()
	if err != nil {
		logs.Errorf("failed to get process lock(%s), exit: %v", processLockFile, err)
		return false
	}

	if !ok {
		logs.Errorf("failed to get process lock(%s), exit: maybe already running.", processLockFile)
		return false
	}

	totalLock := flock.New(fmt.Sprintf("%s/%s.lock", GetRuntimeDir(), TotalLock))
	if err = totalLock.Lock(); err != nil {
		logs.Error("get total lock failed, exit", err.Error())
		return false
	}
	defer func() {
		_ = totalLock.Unlock()
	}()

	if err = fileutil.WriteString(pidFile, fmt.Sprintf("%d", os.Getpid())); err != nil {
		logs.Errorf("failed to save pid file(%s): %v", pidFile, err)
		return false
	}

	logs.Info("success to get process lock and save pid")
	return true
}

// getLocalIp 网关初始化顺序在前, 上报与devops网关通信的网卡ip
func getLocalIp() (string, error) {
	gateway := DevopsGateway
	if !strings.HasPrefix(gateway, "http") {
		gateway = "http://" + gateway
	}
	devopsUrl, err := url.Parse(gateway)
	if err != nil {
		return "", err
	}
	host := devopsUrl.Host
	if devopsUrl.Port() == "" {
		host += ":80"
	}
	conn, err := net.Dial("udp", host)
	if err != nil {
		return "", err
	}
	defer func() { _ = conn.Close() }()
	if localAddr, ok := conn.LocalAddr().(*net.UDPAddr); ok {
		return localAddr.IP.String(), nil
	} else {
		return "", errors.New("failed to get ip")
	}
}
