//go:build linux

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

package config

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"strings"

	"github.com/pkg/errors"
	"golang.org/x/sys/unix"
)

func GetWinTaskType() string {
	return ""
}

func GetOsVersion() (string, error) {
	i, err := getOSInfo()
	if err != nil {
		return "", err
	}
	b, err := json.Marshal(i)
	if err != nil {
		return "", errors.Wrap(err, "GetOsVersion format json error")
	}
	return string(b), nil
}

// OSInfo 结构体用于存储解析后的操作系统信息
type OSInfo struct {
	Name          string // 发行版名称，例如 "Ubuntu"
	Version       string // 完整的版本字符串，例如 "22.04.3 LTS (Jammy Jellyfish)"
	ID            string // 发行版的 ID，例如 "ubuntu"
	IDLike        string // 可能的父发行版，例如 "debian"
	PrettyName    string // 更易读的发行版名称，例如 "Ubuntu 22.04.3 LTS"
	VersionID     string // 版本号，例如 "22.04"
	KernelVersion string // 内核版本
	Architecture  string // CPU 架构
}

// getOSInfo 是获取 Linux 系统信息的主函数
func getOSInfo() (*OSInfo, error) {
	// 1. 从 /etc/os-release 文件获取发行版信息
	info, err := getDistributionInfo("/etc/os-release")
	if err != nil {
		// 作为备选，可以尝试读取其他文件，如 /etc/lsb-release
		// 但为了示例清晰，我们这里只处理主要情况
		return nil, fmt.Errorf("could not read distribution info: %w", err)
	}

	// 2. 使用 uname 系统调用获取内核和架构信息
	uts, err := getKernelInfo()
	if err != nil {
		return nil, fmt.Errorf("could not get kernel info: %w", err)
	}
	info.KernelVersion = uts.Release
	info.Architecture = uts.Machine

	return info, nil
}

// getDistributionInfo 读取并解析 /etc/os-release 文件
func getDistributionInfo(filepath string) (*OSInfo, error) {
	file, err := os.Open(filepath)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	return parseOSRelease(file)
}

// parseOSRelease 是解析 os-release 文件内容的核心逻辑
func parseOSRelease(r io.Reader) (*OSInfo, error) {
	info := &OSInfo{}
	scanner := bufio.NewScanner(r)
	properties := make(map[string]string)

	for scanner.Scan() {
		line := scanner.Text()
		// 使用 SplitN 确保值中的等号不会被错误分割
		parts := strings.SplitN(line, "=", 2)
		if len(parts) != 2 {
			continue // 忽略格式不正确的行
		}
		key := parts[0]
		// 去除值两边的引号
		value := strings.Trim(parts[1], "\"")
		properties[key] = value
	}

	if err := scanner.Err(); err != nil {
		return nil, fmt.Errorf("error scanning os-release file: %w", err)
	}

	// 从 map 中填充结构体
	info.Name = properties["NAME"]
	info.Version = properties["VERSION"]
	info.ID = properties["ID"]
	info.IDLike = properties["ID_LIKE"]
	info.PrettyName = properties["PRETTY_NAME"]
	info.VersionID = properties["VERSION_ID"]

	return info, nil
}

// UtsnameInfo 包装了 unix.Utsname 的字段，并将其转换为 Go 字符串
type UtsnameInfo struct {
	Release string
	Machine string
}

// getKernelInfo 调用 uname 并将 C 风格的字符数组转换为 Go 字符串
func getKernelInfo() (*UtsnameInfo, error) {
	var uts unix.Utsname
	if err := unix.Uname(&uts); err != nil {
		return nil, err
	}

	return &UtsnameInfo{
		Release: charsToString(uts.Release[:]),
		Machine: charsToString(uts.Machine[:]),
	}, nil
}

// charsToString 是一个辅助函数，用于将 C 风格的 null-terminated 字符数组转换为 Go 字符串
func charsToString(ca []byte) string {
	// 寻找第一个 null 字符
	n := -1
	for i, b := range ca {
		if b == 0 {
			n = i
			break
		}
	}
	if n == -1 {
		return string(ca)
	}
	return string(ca[:n])
}
