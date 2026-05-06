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
	"fmt"
	"net"
	"net/url"
	"os"
	"os/user"
	"path/filepath"
	"runtime"
	"sort"
	"strings"
	"sync"
	"time"

	"github.com/gofrs/flock"
	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/utils/fileutil"
)

var GExecutableDir string

var DevopsGateway string

type agentIPCandidate struct {
	ip          string
	ifaceName   string
	description string
	isVirtual   bool
	score       int
}

type agentInterfaceDetail struct {
	friendlyName string
	description  string
	ifType       uint32
	isVirtual    bool
}

type agentInterfaceLookup struct {
	byIndex map[int]agentInterfaceDetail
	byName  map[string]agentInterfaceDetail
}

type agentIPCacheEntry struct {
	key       string
	ip        string
	expiresAt time.Time
}

type agentIPResolver func(ignoreIps []string) string

var (
	agentIPCacheMu      sync.Mutex
	agentIPCache        agentIPCacheEntry
	agentIPCacheTTL                     = 60 * time.Second
	agentIPNow                          = time.Now
	resolveAgentIPValue agentIPResolver = resolveAgentIP
)

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

// GetCurrentUser returns the current process user. If user.Current() fails
// (e.g. in Docker containers without /etc/passwd, or with CGO disabled),
// it falls back to OS primitives instead of crashing the process.
func GetCurrentUser() *user.User {
	currentUser, err := user.Current()
	if err == nil && currentUser != nil {
		return currentUser
	}
	logs.Warnf("user.Current() failed: %v, using fallback", err)
	return fallbackCurrentUser()
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

// GetExecutableDir returns the absolute directory containing the current executable.
func GetExecutableDir() string {
	if len(GExecutableDir) == 0 {
		executable := strings.Replace(os.Args[0], "\\", "/", -1)
		index := strings.LastIndex(executable, "/")
		var dir string
		if index >= 0 {
			dir = executable[0:index]
		} else {
			dir = "."
		}
		if !filepath.IsAbs(dir) {
			if abs, err := filepath.Abs(dir); err == nil {
				dir = abs
			}
		}
		GExecutableDir = dir
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
	now := agentIPNow()
	cacheKey := buildAgentIPCacheKey(ignoreIps)

	agentIPCacheMu.Lock()
	if agentIPCache.key == cacheKey && now.Before(agentIPCache.expiresAt) && agentIPCache.ip != "" {
		cachedIP := agentIPCache.ip
		agentIPCacheMu.Unlock()
		return cachedIP
	}
	agentIPCacheMu.Unlock()

	ip := resolveAgentIPValue(ignoreIps)

	agentIPCacheMu.Lock()
	agentIPCache = agentIPCacheEntry{
		key:       cacheKey,
		ip:        ip,
		expiresAt: now.Add(agentIPCacheTTL),
	}
	agentIPCacheMu.Unlock()

	return ip
}

func resolveAgentIP(ignoreIps []string) string {
	fallbackIp := "127.0.0.1"
	routeIp, err := getLocalIp()
	if err == nil {
		if !shouldIgnoreIP(routeIp, ignoreIps) {
			fallbackIp = routeIp
		}
	} else {
		logs.Warn("failed to get ip by udp", err)
	}

	candidates, err := listAgentIPCandidates(routeIp, ignoreIps)
	if err != nil || len(candidates) == 0 {
		return fallbackIp
	}

	selected := candidates[0]
	logs.Infof("select agent ip=%s routeIp=%s iface=%s desc=%s virtual=%t score=%d",
		selected.ip, routeIp, selected.ifaceName, selected.description, selected.isVirtual, selected.score)
	return selected.ip
}

func buildAgentIPCacheKey(ignoreIps []string) string {
	if len(ignoreIps) == 0 {
		return ""
	}
	cloned := append([]string(nil), ignoreIps...)
	sort.Strings(cloned)
	return strings.Join(cloned, ",")
}

func resetAgentIPCache() {
	agentIPCacheMu.Lock()
	defer agentIPCacheMu.Unlock()
	agentIPCache = agentIPCacheEntry{}
}

func listAgentIPCandidates(routeIp string, ignoreIps []string) ([]agentIPCandidate, error) {
	ifaceLookup, err := loadAgentInterfaceLookup()
	if err != nil {
		logs.Warnf("failed to load interface metadata: %s", err.Error())
	}

	ncs, err := net.Interfaces()
	if err != nil {
		return nil, err
	}

	candidates := make([]agentIPCandidate, 0)
	for _, nc := range ncs {
		// 忽略没有启用的接口
		if nc.Flags&net.FlagUp == 0 {
			continue
		}
		if nc.Flags&net.FlagLoopback != 0 {
			continue
		}
		// #3626 二次确认，需要排除虚拟网卡情况
		if len(nc.HardwareAddr) == 0 {
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

			if shouldIgnoreIP(ipNet.IP.String(), ignoreIps) {
				logs.Debugf("skipIp=%s", ipNet.IP)
				continue
			}

			detail := ifaceLookup.get(nc)
			candidates = append(candidates, buildAgentIPCandidate(nc, ipNet.IP, routeIp, detail))
		}
	}

	sort.SliceStable(candidates, func(i, j int) bool {
		if candidates[i].score != candidates[j].score {
			return candidates[i].score > candidates[j].score
		}
		if candidates[i].ifaceName != candidates[j].ifaceName {
			return candidates[i].ifaceName < candidates[j].ifaceName
		}
		return candidates[i].ip < candidates[j].ip
	})

	return candidates, nil
}

func (l agentInterfaceLookup) get(nc net.Interface) agentInterfaceDetail {
	if detail, ok := l.byIndex[nc.Index]; ok {
		return detail
	}
	if detail, ok := l.byName[strings.ToLower(nc.Name)]; ok {
		return detail
	}

	return agentInterfaceDetail{
		friendlyName: nc.Name,
		description:  nc.Name,
		isVirtual:    isLikelyVirtualInterface(nc.Name),
	}
}

func buildAgentIPCandidate(nc net.Interface, ip net.IP, routeIp string, detail agentInterfaceDetail) agentIPCandidate {
	score := 0
	if !detail.isVirtual {
		score += 200
	} else {
		score -= 200
	}
	if nc.Flags&net.FlagBroadcast != 0 {
		score += 20
	}
	if nc.Flags&net.FlagPointToPoint == 0 {
		score += 10
	}
	if len(nc.HardwareAddr) > 0 {
		score += 10
	}
	if detail.ifType == 6 || detail.ifType == 71 {
		score += 40
	}
	if ip.String() == routeIp {
		score += 100
	}

	return agentIPCandidate{
		ip:          ip.String(),
		ifaceName:   detail.friendlyName,
		description: detail.description,
		isVirtual:   detail.isVirtual,
		score:       score,
	}
}

func shouldIgnoreIP(ip string, ignoreIps []string) bool {
	target := net.ParseIP(ip)
	if target == nil {
		return false
	}

	for _, ignoreIp := range ignoreIps {
		if ignoreIp == ip {
			return true
		}
		if !strings.Contains(ignoreIp, "/") {
			continue
		}
		_, ipNet, err := net.ParseCIDR(ignoreIp)
		if err == nil && ipNet.Contains(target) {
			return true
		}
	}

	return false
}

func isLikelyVirtualInterface(name string) bool {
	lowerName := strings.ToLower(name)
	keywords := []string{
		"vethernet",
		"default switch",
		"wsl",
		"docker",
		"container",
		"virtualbox",
		"host-only",
		"vmware network adapter",
		"loopback",
		"npcap",
		"ngnclient",
		"vpn",
		"wireguard",
		"tap-",
		"hyper-v",
	}
	for _, keyword := range keywords {
		if strings.Contains(lowerName, keyword) {
			return true
		}
	}
	return false
}

// ExitProcess flushes logs and exits with the given code.
func ExitProcess(exitCode int) {
	logs.Close()
	os.Exit(exitCode)
}

var processLock *flock.Flock

// KeepProcessAlive keep process alive
func KeepProcessAlive() {
	runtime.KeepAlive(processLock)
}

// CheckProcess check process and lock
func CheckProcess(name string, totalLocked bool) bool {
	processLockFile := fmt.Sprintf("%s/%s.lock", GetRuntimeDir(), name)
	pidFile := fmt.Sprintf("%s/%s.pid", GetRuntimeDir(), name)

	processLock = flock.New(processLockFile)
	ok, err := processLock.TryLock()
	if err != nil {
		logs.WithError(err).Errorf("failed to get process lock(%s), exit", processLockFile)
		return false
	}

	if !ok {
		logs.Errorf("failed to get process lock(%s), exit: maybe already running.", processLockFile)
		return false
	}

	if !totalLocked {
		totalLock := flock.New(fmt.Sprintf("%s/%s.lock", GetRuntimeDir(), TotalLock))
		if err = totalLock.Lock(); err != nil {
			logs.WithError(err).Error("get total lock failed, exit")
			return false
		}
		defer func() {
			_ = totalLock.Unlock() // Unlock理论上不应失败，忽略错误
		}()
	}

	if err = fileutil.WriteString(pidFile, fmt.Sprintf("%d", os.Getpid())); err != nil {
		logs.WithError(err).Errorf("failed to save pid file(%s)", pidFile)
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
