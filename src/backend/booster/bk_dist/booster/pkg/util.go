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
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net"
	"os"
	"os/exec"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"syscall"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"

	"github.com/shirou/gopsutil/process"
)

func getExitCodeFromError(err error) int {
	if err == nil {
		return 0
	}

	exitErr, ok := err.(*exec.ExitError)
	if !ok {
		return -1
	}

	status, ok := exitErr.Sys().(syscall.WaitStatus)
	if !ok {
		return -1
	}

	return status.ExitStatus()
}

func fileReadable(name string) error {
	f, err := os.Stat(name)
	if err != nil {
		return err
	}

	if f.IsDir() {
		return fmt.Errorf("%s is a dir", name)
	}

	if f.Mode()&0444 == 0 {
		return fmt.Errorf("%s is unreadable", name)
	}

	return nil
}

func checkDNS(domain string) error {
	if _, err := net.LookupIP(domain); err != nil {
		return fmt.Errorf("check dns failed: %v", err)
	}
	return nil
}

func replaceArgsReversedKey(s, key, value string) string {
	key = "@BK_" + key

	return strings.ReplaceAll(s, key, value)
}

func getIPFromServer(server string) string {
	return strings.Split(server, ":")[0]
}

// saveMapAsJSON : save map as json file
func saveMapAsJSON(m map[string]string, filename string) error {
	jsonData, err := json.Marshal(m)
	if err != nil {
		return err
	}

	temp := fmt.Sprintf("%s_temp", filename)
	err = ioutil.WriteFile(temp, jsonData, os.ModePerm)
	if err != nil {
		return err
	}

	_ = os.Rename(temp, filename)
	return nil
}

// saveMapAsSource : save map as source file
func saveMapAsSource(m map[string]string, filename string) error {
	content := ""
	for k, v := range m {
		vv := strings.ReplaceAll(v, "\"", "\\\"")
		vv = strings.ReplaceAll(vv, "$", "\\$")
		content += fmt.Sprintf("export %s=\"%s\"\n", k, vv)
	}

	return ioutil.WriteFile(filename, []byte(content), os.ModePerm)
}

func resolveToolChainJSON(filename string) (*dcSDK.Toolchain, error) {
	blog.Debugf("resolve tool chain json file %s", filename)

	data, err := ioutil.ReadFile(filename)
	if err != nil {
		blog.Errorf("failed to read tool chain json file %s with error %v", filename, err)
		return nil, err
	}

	var t dcSDK.Toolchain
	if err = codec.DecJSON(data, &t); err != nil {
		blog.Errorf("failed to decode json content[%s] failed: %v", string(data), err)
		return nil, err
	}

	return &t, nil
}

// KillChildren kill the children processes of given process
func KillChildren(p *process.Process) {
	children, err := p.Children()
	if err == nil && len(children) > 0 {
		for _, v := range children {
			n, err := v.Name()
			// do not kill bk-dist-controller, for it may be used by other process
			if err == nil && strings.Contains(n, "bk-dist-controller") {
				continue
			}
			KillChildren(v)
			_ = v.Kill()
		}
	}
}

func getHosts(hostList []string) []*protocol.Host {
	hosts := make([]*protocol.Host, 0)
	for _, v := range hostList {
		hostField := strings.Split(v, "/")

		if len(hostField) < 2 {
			blog.Warnf("booster: got invalid host %s", v)
			continue
		}

		jobs, err := strconv.Atoi(hostField[1])
		if err != nil {
			blog.Warnf("booster: got invalid jobs for host %s", v)
			continue
		}

		host := &protocol.Host{
			Server:       hostField[0],
			TokenString:  hostField[0],
			Hosttype:     protocol.HostRemote,
			Jobs:         jobs,
			Compresstype: protocol.CompressLZ4,
			Protocol:     "tcp",
		}

		hosts = append(hosts, host)
	}

	blog.Debugf("booster: got host: %+v", hosts)
	return hosts
}

type ByModTime []os.FileInfo

func (fis ByModTime) Len() int {
	return len(fis)
}

func (fis ByModTime) Swap(i, j int) {
	fis[i], fis[j] = fis[j], fis[i]
}

func (fis ByModTime) Less(i, j int) bool {
	return fis[i].ModTime().Before(fis[j].ModTime())
}

func cleanDirByTime(dir string, limitsize int64) {
	f, err := os.Open(dir)
	if err != nil {
		blog.Warnf("booster: failed to open dir %s with error:%v", dir, err)
		return
	}
	fis, _ := f.Readdir(-1)
	f.Close()
	sort.Sort(ByModTime(fis))

	var totalsize int64
	for _, fi := range fis {
		totalsize += fi.Size()
	}

	blog.Infof("booster: dir %s total size:%d, limit size:%d", dir, totalsize, limitsize)
	fullpath := ""
	for _, fi := range fis {
		if totalsize < limitsize {
			break
		}
		fullpath = filepath.Join(dir, fi.Name())
		blog.Infof("booster: ready remove file:%s", fullpath)
		os.Remove(fullpath)
		totalsize -= fi.Size()
	}
}
