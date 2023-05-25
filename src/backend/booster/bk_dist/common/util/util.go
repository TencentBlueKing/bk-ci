/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package util

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"net"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"runtime"
	"strings"
	"time"

	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"

	"github.com/pierrec/lz4"
	"github.com/shirou/gopsutil/process"
	"golang.org/x/text/encoding/simplifiedchinese"
	"golang.org/x/text/transform"
)

// define vars
var (
	_, classA, _  = net.ParseCIDR("10.0.0.0/8")
	_, classA1, _ = net.ParseCIDR("9.0.0.0/8")
	_, classAa, _ = net.ParseCIDR("100.64.0.0/10")
	_, classB, _  = net.ParseCIDR("172.16.0.0/12")
	_, classC, _  = net.ParseCIDR("192.168.0.0/16")
)

// GetCaller return the current caller functions
func GetCaller() string {
	_, file, line, ok := runtime.Caller(2)
	if !ok {
		file = "???"
		line = 1
	} else {
		slash := strings.LastIndex(file, "/")
		if slash >= 0 {
			file = file[slash+1:]
		}
	}

	return fmt.Sprintf("%s:%d", file, line)
}

// GetIPAddress get local usable inner ip address
func GetIPAddress() (addrList []string) {
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

// GetHomeDir get home dir by system env
func GetHomeDir() string {
	homeDir, _ := os.UserHomeDir()

	return homeDir
}

// GetGlobalDir
func GetGlobalDir() string {
	if runtime.GOOS == "windows" {
		return "C:\\bk_dist"
	}

	if runtime.GOOS == "darwin" {
		return GetRuntimeDir()
	}

	return "/etc/bk_dist"
}

// GetLogsDir get the runtime log dir
func GetLogsDir() string {
	dir := path.Join(GetRuntimeDir(), "logs")
	_ = os.MkdirAll(dir, os.ModePerm)
	return dir
}

// GetPumpCacheDir get the runtime pump cache dir
func GetPumpCacheDir() string {
	dir := path.Join(GetRuntimeDir(), "pump_cache")
	_ = os.MkdirAll(dir, os.ModePerm)
	return dir
}

// GetRecordDir get the record dir
func GetRecordDir() string {
	dir := filepath.Join(GetGlobalDir(), "record")
	_ = os.MkdirAll(dir, os.ModePerm)
	return dir
}

// GetRuntimeDir get the runtime tmp dir
func GetRuntimeDir() string {
	dir := path.Join(GetHomeDir(), protocol.BKDistDir)
	_ = os.MkdirAll(dir, os.ModePerm)
	return dir
}

// PrintLevel define log level
type PrintLevel int32

// define log levels
const (
	PrintFatal PrintLevel = iota
	PrintError
	PrintWarn
	PrintInfo
	PrintDebug
	PrintNothing
)

var (
	printlevel2StringMap = map[PrintLevel]string{
		PrintFatal:   "fatal",
		PrintError:   "error",
		PrintWarn:    "warn",
		PrintInfo:    "info",
		PrintDebug:   "debug",
		PrintNothing: "nothing",
	}
)

// String return the string of PrintLevel
func (p PrintLevel) String() string {
	if v, ok := printlevel2StringMap[p]; ok {
		return v
	}

	return "unknown"
}

// Lz4Compress to implement lz4 compress
func Lz4Compress(src []byte) ([]byte, error) {
	if src == nil || len(src) == 0 {
		return nil, fmt.Errorf("src is invalid")
	}

	dst := make([]byte, lz4.CompressBlockBound(len(src)))
	if dst == nil || len(dst) == 0 {
		return nil, fmt.Errorf("failed to allocate memory for compress dst")
	}

	compressedsize, err := lz4.CompressBlock(src, dst, nil)
	if err != nil {
		return nil, err
	}

	return dst[:compressedsize], nil
}

// Lz4Uncompress to implement lz4 uncompress
func Lz4Uncompress(src []byte, dst []byte) ([]byte, error) {
	if src == nil || len(src) == 0 {
		return nil, fmt.Errorf("src is empty")
	}

	if dst == nil || len(dst) == 0 {
		return nil, fmt.Errorf("dst is empty")
	}

	uncompressedsize, err := lz4.UncompressBlock(src, dst)
	if err != nil {
		return nil, err
	}

	return dst[:uncompressedsize], nil
}

// CheckExecutable check executable is exist
func CheckExecutable(target string) (string, error) {
	return CheckExecutableWithPath(target, "", "")
}

// CheckExecutableWithPath check executable is exist
// if path is set, then will look from it,
// if path is empty, then will look from env PATH
// pathExt define the PATHEXT in windows
func CheckExecutableWithPath(target, path, pathExt string) (string, error) {
	absPath, err := LookPath(target, path, pathExt)
	if err != nil {
		return "", err
	}

	return absPath, nil
}

// try search file in caller path
func CheckFileWithCallerPath(target string) (string, error) {
	callpath := GetExcPath()
	newtarget := filepath.Join(callpath, target)
	if !dcFile.Stat(newtarget).Exist() {
		return "", fmt.Errorf("not found controller[%s]", newtarget)
	}

	absPath, err := filepath.Abs(newtarget)
	if err != nil {
		return "", err
	}

	return absPath, nil
}

// Now get the current time
func Now(t *time.Time) {
	*t = now()
}

func now() time.Time {
	return time.Now().Local()
}

// Environ return the current environment variables in map
func Environ() map[string]string {
	items := make(map[string]string)
	for _, i := range os.Environ() {
		key, val := func(item string) (key, val string) {
			splits := strings.Split(item, "=")
			key = splits[0]
			val = splits[1]
			return
		}(i)
		items[key] = val
	}
	return items
}

// GetExcPath get current Exec caller and return the dir
func GetExcPath() string {
	file, _ := exec.LookPath(os.Args[0])
	p, err := filepath.Abs(file)
	if err != nil {
		return ""
	}

	return filepath.Dir(p)
}

// ProjectSetting describe the project setting
type ProjectSetting struct {
	ProjectID string `json:"project_id"`
}

// SearchProjectID try get project id
func SearchProjectID() string {
	exepath := GetExcPath()
	if exepath != "" {
		jsonfile := filepath.Join(exepath, "bk_project_setting.json")
		if dcFile.Stat(jsonfile).Exist() {
			data, err := ioutil.ReadFile(jsonfile)
			if err != nil {
				blog.Debugf("util: failed to read file[%s]", jsonfile)
				return ""
			}

			var setting ProjectSetting
			if err = codec.DecJSON(data, &setting); err != nil {
				blog.Debugf("util: failed to decode file[%s]", jsonfile)
				return ""
			}

			return setting.ProjectID
		}
	}
	return ""
}

// Utf8ToGbk transfer utf-8 to gbk
func Utf8ToGbk(s []byte) ([]byte, error) {
	reader := transform.NewReader(bytes.NewReader(s), simplifiedchinese.GBK.NewEncoder())
	d, e := ioutil.ReadAll(reader)
	if e != nil {
		return nil, e
	}
	return d, nil
}

// ProcessExist check if target process exsit
func ProcessExist(target string) bool {
	processes, err := process.Processes()
	if err != nil {
		return false
	}

	for _, p := range processes {
		name, err := p.Name()
		if err != nil {
			continue
		}

		if target == name {
			return true
		}
	}

	return false
}

// ListProcess list process by process name
func ListProcess(target string) ([]*process.Process, error) {
	processes, err := process.Processes()
	if err != nil {
		return nil, err
	}

	procs := []*process.Process{}
	for _, p := range processes {
		name, err := p.Name()
		if err != nil {
			continue
		}

		if target == name {
			procs = append(procs, p)
		}
	}

	return procs, nil
}

// ProcessExistTimeoutAndKill check target process with name, and kill it after timeout
func ProcessExistTimeoutAndKill(target string, timeout time.Duration) bool {
	processes, err := process.Processes()
	if err != nil {
		return false
	}

	for _, p := range processes {
		name, err := p.Name()
		if err != nil {
			continue
		}

		createTime, err := p.CreateTime()
		if err != nil {
			continue
		}

		if target == name && time.Unix(createTime/1000, 0).Local().Add(timeout).Before(time.Now().Local()) {
			if err = p.Kill(); err != nil {
				blog.Warnf("util: process %s by pid %d killed failed: %v", name, p.Pid, err)
			}

			blog.Infof("util: process %s by pid %d trying to kill", name, p.Pid)
			return true
		}
	}

	return false
}

func hasSpace(s string) bool {
	if s == "" {
		return false
	}

	for _, v := range s {
		if v == ' ' {
			return true
		}
	}

	return false
}

// QuoteSpacePath quote path if include space
func QuoteSpacePath(s string) string {
	if s == "" {
		return ""
	}

	if !hasSpace(s) {
		return s
	}

	if s[0] == '"' || s[0] == '\'' {
		return s
	}

	return "\"" + s + "\""
}
