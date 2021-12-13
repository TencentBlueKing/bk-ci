/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pkg

import (
	"fmt"
	"net"
	"os"
	"os/exec"
	"regexp"
	"runtime"
	"strings"
	"syscall"

	"github.com/Tencent/bk-ci/src/booster/common/hook"
)

const (
	localIPKKey  = "BK_DISTCC_LOCAL_IP"
	exitCodeFile = "bk_origin_exit_code"
)

var (
	_, classA, _  = net.ParseCIDR("10.0.0.0/8")
	_, classA1, _ = net.ParseCIDR("9.0.0.0/8")
	_, classAa, _ = net.ParseCIDR("100.64.0.0/10")
	_, classB, _  = net.ParseCIDR("172.16.0.0/12")
	_, classC, _  = net.ParseCIDR("192.168.0.0/16")
)

// compilerBitset for detect compilers
type compilerBitset int32

// define hook env keys and default path
const (
	hookSoEnvKey      = "BK_ENV_KEY_LD_PRELOAD_PATH"
	hookSoDefaultPath = "/lib64/bkhook.so"
	hookConfigEnvKey  = "BK_ENV_KEY_HOOK_CONFIG_PATH"

	hookConfigDefault          = "/etc/bkhook/bk_default_rules.json"
	hookConfigDistccPath       = "/etc/bkhook/bk_distcc_rules.json"
	hookConfigCCachePath       = "/etc/bkhook/bk_ccache_rules.json"
	hookConfigDistccCCachePath = "/etc/bkhook/bk_distcc_ccache_rules.json"

	bitGcc compilerBitset = 1 << iota
	bitGccxx
	bitClang
	bitClangxx
	bitDistcc
	bitCCache
)

// MapBitCompiler2Path to define map from compiles to hook config path
var (
	mapBitCompiler2Path = map[int32]string{
		int32(bitGcc) | int32(bitDistcc):                      hookConfigDistccPath,
		int32(bitGcc) | int32(bitCCache):                      hookConfigCCachePath,
		int32(bitGcc) | int32(bitDistcc) | int32(bitCCache):   hookConfigDistccCCachePath,
		int32(bitGccxx) | int32(bitDistcc):                    hookConfigDistccPath,
		int32(bitGccxx) | int32(bitCCache):                    hookConfigCCachePath,
		int32(bitGccxx) | int32(bitDistcc) | int32(bitCCache): hookConfigDistccCCachePath,

		int32(bitClang) | int32(bitDistcc):                      hookConfigDistccPath,
		int32(bitClang) | int32(bitCCache):                      hookConfigCCachePath,
		int32(bitClang) | int32(bitDistcc) | int32(bitCCache):   hookConfigDistccCCachePath,
		int32(bitClangxx) | int32(bitDistcc):                    hookConfigDistccPath,
		int32(bitClangxx) | int32(bitCCache):                    hookConfigCCachePath,
		int32(bitClangxx) | int32(bitDistcc) | int32(bitCCache): hookConfigDistccCCachePath,
	}

	mapCompilerStr2Bit = map[string]int32{
		"gcc":     int32(bitGcc),
		"g++":     int32(bitGccxx),
		"clang":   int32(bitClang),
		"clang++": int32(bitClangxx),
		"distcc":  int32(bitDistcc),
		"ccache":  int32(bitCCache),
	}
)

func getIPAddress() (addrList []string) {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return addrList
	}
	for _, addr := range addrs {
		if ip, ok := addr.(*net.IPNet); ok && !ip.IP.IsLoopback() && ip.IP.To4() != nil {
			if classA.Contains(ip.IP) {
				addrList = append(addrList, ip.IP.String())
				continue
			}
			if classA1.Contains(ip.IP) {
				addrList = append(addrList, ip.IP.String())
				continue
			}
			if classAa.Contains(ip.IP) {
				addrList = append(addrList, ip.IP.String())
				continue
			}
			if classB.Contains(ip.IP) {
				addrList = append(addrList, ip.IP.String())
				continue
			}
			if classC.Contains(ip.IP) {
				addrList = append(addrList, ip.IP.String())
				continue
			}
		}
	}
	return addrList
}

func getLocalIP() (string, error) {
	// get local ip from env first, if not exists or empty, then get from net
	ip := os.Getenv(localIPKKey)
	if ip != "" {
		return ip, nil
	}

	ips := getIPAddress()
	if len(ips) == 0 {
		return "", fmt.Errorf("get local IP failed, the client will exit")
	}

	return ips[0], nil
}

func setLimit(hosts string, limit int) string {
	if limit <= 0 {
		return hosts
	}

	hostList := strings.Split(hosts, " ")
	for i, host := range hostList {
		if !strings.Contains(host, ":") {
			continue
		}

		frag := strings.Split(host, ",")
		address := strings.Split(frag[0], "/")[0]
		frag[0] = fmt.Sprintf("%s/%d", address, limit)
		host = strings.Join(frag, ",")
		hostList[i] = host
	}
	return strings.Join(hostList, " ")
}

func setLocalSlots(hosts string, localSlots, localSlotsCpp int) string {
	if localSlots > 0 {
		hosts += fmt.Sprintf(" --localslots=%d", localSlots)
	}
	if localSlotsCpp > 0 {
		hosts += fmt.Sprintf(" --localslots_cpp=%d", localSlotsCpp)
	}
	return hosts
}

func saveExitCode(originErr error) {
	if !SaveCode {
		return
	}

	var code int

	if originErr != nil {
		exitErr, ok := originErr.(*exec.ExitError)
		if !ok {
			return
		}

		status, ok := exitErr.Sys().(syscall.WaitStatus)
		if !ok {
			return
		}

		code = status.ExitStatus()
	}

	_, err := os.Stat(exitCodeFile)
	if os.IsNotExist(err) {
		f, err := os.Create(exitCodeFile)
		if err != nil {
			fmt.Printf("failed to create exit code file: %v\n", err)
			return
		}
		defer func() {
			_ = f.Close()
		}()
	}

	file, err := os.OpenFile(exitCodeFile, os.O_RDWR, 0644)
	if err != nil {
		fmt.Printf("failed to open exit code file: %v\n", err)
		return
	}
	defer func() {
		_ = file.Close()
	}()

	_ = file.Truncate(0)
	_, err = file.WriteString(fmt.Sprintf("%d", code))
	if err != nil {
		fmt.Printf("failed to write exit code file: %v\n", err)
		return
	}

	fmt.Printf("success to write exit code(%d) in %s\n", code, exitCodeFile)
}

func getLocalJobs() int {
	jobs := runtime.NumCPU()
	if 0 < MaxLocalJobs && MaxLocalJobs < jobs {
		jobs = MaxLocalJobs
	}
	return jobs
}

var (
	versionTransformRule = []versionTransform{
		{key: "tvm", value: "gcc"},
		{key: "gcov", value: "gcc"},
		{key: "tbs-clang", value: "clang"},
		{key: "trpc", value: "gcc"},
		{key: "wxds", value: "gcc"},
		{key: "mmtest_tl2", value: "gcc"},
		{key: "mmtest", value: "gcc"},
		{key: "mac_clang", value: "clang"},
		{key: "happybuild", value: "gcc"},
	}
)

type versionTransform struct {
	key   string
	value string
}

// TransformGccVersion transfer old version to new one(for version checking)
func TransformGccVersion(oldVersion string) string {
	for _, rule := range versionTransformRule {
		if strings.Contains(oldVersion, rule.key) {
			return strings.Replace(oldVersion, rule.key, rule.value, -1)
		}
	}
	return oldVersion
}

func getBKLdPreloadPath() string {
	path := os.Getenv(hookSoEnvKey)
	if path != "" {
		return path
	}
	return hookSoDefaultPath
}

func getBKHookConfigPathByCompilers(compilers []string) string {
	var compilerBit int32
	for _, v1 := range compilers {
		if bitv, ok := mapCompilerStr2Bit[v1]; ok {
			compilerBit = compilerBit | bitv
		}
	}

	if configpath, ok := mapBitCompiler2Path[compilerBit]; ok {
		return configpath
	}

	return ""
}

func getBKHookConfigPath(cmd string) string {
	path := os.Getenv(hookConfigEnvKey)
	if path != "" {
		return path
	}

	// TODO : get config by cmd
	re := regexp.MustCompile("BK_CC='.*?'")
	bkccstr := re.FindString(cmd)
	strs := strings.FieldsFunc(bkccstr, bkCCSplit)
	if len(strs) > 0 {
		return getBKHookConfigPathByCompilers(strs)
	}
	return ""
}

func bkCCSplit(s rune) bool {
	if s == ' ' || s == '\'' || s == '=' {
		return true
	}

	return false
}

func hookCompiling(cmd string, envs map[string]string) error {
	ldPreloadSo := getBKLdPreloadPath()
	configPath := getBKHookConfigPath(cmd)
	if configPath == "" {
		fmt.Printf("use default hook config: %s\n", hookConfigDefault)
		configPath = hookConfigDefault
	}
	_, err := hook.RunProcess(ldPreloadSo, configPath, envs, cmd)
	return err
}

func hookCompilingWithCmdCompilers(cmd string, ccCompiler string, envs map[string]string) error {
	ldPreloadSo := getBKLdPreloadPath()
	configPath := os.Getenv(hookConfigEnvKey)
	if configPath == "" {
		strs := strings.FieldsFunc(ccCompiler, bkCCSplit)
		configPath = getBKHookConfigPathByCompilers(strs)
	}
	if configPath == "" {
		fmt.Printf("use default hook config: %s\n", hookConfigDefault)
		configPath = hookConfigDefault
	}
	_, err := hook.RunProcess(ldPreloadSo, configPath, envs, cmd)
	return err
}

func adjustBazelCmd(cmd string) string {
	re := regexp.MustCompile("--bazelrc=.*? ")
	rc := re.FindString(cmd)
	if rc != "" {
		cmd = strings.Replace(cmd, rc, " ", -1)
	}
	cmd = strings.Replace(cmd, "--noworkspace_rc", " ", -1)
	return cmd
}

func hookBazelCompiling(cmd string, envs map[string]string, ccCompiler string, jobs string) error {
	cmd = adjustBazelCmd(cmd)
	ldPreloadSo := getBKLdPreloadPath()
	configPath := os.Getenv(hookConfigEnvKey)
	if configPath == "" {
		strs := strings.FieldsFunc(ccCompiler, bkCCSplit)
		configPath = getBKHookConfigPathByCompilers(strs)
	}
	if configPath == "" {
		fmt.Printf("use default hook config: %s\n", hookConfigDefault)
		configPath = hookConfigDefault
	}

	_, err := hook.RunBazelProcess(ldPreloadSo, configPath, envs, cmd, jobs)
	return err
}
