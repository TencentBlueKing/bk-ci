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
	"strings"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/flock"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/monitor/types"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/shirou/gopsutil/process"
)

// KillChildren kill all process children of given process
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
	lockfile = "bk-dist-controller.lock"
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
		blog.Errorf("[controller]: failed to start with error:%v\n", err)
		return false
	}
	blog.Infof("[controller]: ready lock file: %s\n", f)
	flag, err := flock.TryLock(f)
	if err != nil {
		blog.Errorf("[controller]: failed to start with error:%v\n", err)
		return false
	}
	if !flag {
		blog.Infof("[controller]: program is maybe running for lock file has been locked \n")
		return false
	}

	return true
}

func Unlock() {
	flock.Unlock()
}
