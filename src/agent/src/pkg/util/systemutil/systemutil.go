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

package systemutil

import (
	"net"
	"os"
	"os/user"
	"runtime"
	"strings"
)

var GExecutableDir string

const osWindows = "windows"
const osLinux = "linux"
const osMacos = "darwin"
const amd64 = "amd64"
const osOther = "other"

const osNameWindows = "windows"
const osNameLinux = "linux"
const osNameMacos = "macos"
const osNameOther = "other"

func IsWindows() bool {
	return runtime.GOOS == osWindows
}

func IsLinux() bool {
	return runtime.GOOS == osLinux
}

func IsMacos() bool {
	return runtime.GOOS == osMacos
}

func IsAmd64() bool {
	return runtime.GOARCH == amd64
}

func GetCurrentUser() *user.User {
	currentUser, _ := user.Current()
	return currentUser
}

func GetWorkDir() string {
	dir, _ := os.Getwd()
	return strings.Replace(dir, "\\", "/", -1)
}

func GetExecutableDir() string {
	if len(GExecutableDir) == 0 {
		executable := strings.Replace(os.Args[0], "\\", "/", -1)
		index := strings.LastIndex(executable, "/")
		GExecutableDir = executable[0:index]
	}
	return GExecutableDir
}

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

func GetHostName() string {
	name, _ := os.Hostname()
	return name
}

func GetAgentIp() string {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return "127.0.0.1"
	}
	for _, addr := range addrs {
		if ipNet, ok := addr.(*net.IPNet); ok && !ipNet.IP.IsLoopback() && ipNet.IP.To4() != nil {
			if ipNet.IP.IsGlobalUnicast() {
				return ipNet.IP.String()
			}
		}
	}
	return "127.0.0.1"
}

func ExitProcess(exitCode int) {
	os.Exit(exitCode)
}
