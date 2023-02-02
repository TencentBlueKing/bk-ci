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
	"io/ioutil"
	"os"
	"path/filepath"
	"runtime"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/flock"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/monitor/types"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/shirou/gopsutil/process"
)

func KillProcess(p *process.Process) error {
	var err error
	if runtime.GOOS != "windows" {
		err = p.Kill()
	} else {
		targetp, err := os.FindProcess(int(p.Pid))
		if err == nil {
			err = targetp.Kill()
			return err
		}
	}

	return err
}

// KillChildren kill all process children of given process
func KillChildren(p *process.Process) {
	children, err := p.Children()
	if err == nil && len(children) > 0 {
		for _, v := range children {
			KillChildren(v)
			err = KillProcess(v)
			if err != nil {
				name, _ := v.Name()
				blog.Infof("monitor: kill child process %s %d failed with err:%v", name, int32(v.Pid), err)
			}
		}
	}
}

func resolveRules(f string) (*types.Rules, error) {
	blog.Infof("resolve rules json file %s", f)

	data, err := ioutil.ReadFile(f)
	if err != nil {
		blog.Errorf("failed to read rules json file %s with error %v", f, err)
		return nil, err
	}

	var r types.Rules
	if err = codec.DecJSON(data, &r); err != nil {
		blog.Errorf("failed to decode json content[%s] failed: %v", string(data), err)
		return nil, err
	}

	return &r, nil
}

var (
	lockfile = "bk-dist-monitor.lock"
)

func getLockFile() (string, error) {
	dir := util.GetGlobalDir()
	if err := os.MkdirAll(dir, os.ModePerm); err != nil {
		return "", err
	}
	return filepath.Join(dir, lockfile), nil
}

func Lock() bool {
	f, err := getLockFile()
	if err != nil {
		blog.Errorf("[monitor]: failed to start with error:%v\n", err)
		return false
	}
	blog.Infof("[monitor]: ready lock file: %s\n", f)
	flag, err := flock.TryLock(f)
	if err != nil {
		blog.Errorf("[monitor]: failed to start with error:%v\n", err)
		return false
	}
	if !flag {
		blog.Infof("[monitor]: program is maybe running for lock file has been locked \n")
		return false
	}

	return true
}

func Unlock() {
	flock.Unlock()
}
