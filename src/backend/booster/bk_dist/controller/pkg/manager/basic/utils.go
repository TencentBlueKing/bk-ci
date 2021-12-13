/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package basic

import (
	"fmt"
	"strings"

	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/types"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

func uniqFiles(files []dcSDK.FileDesc) ([]dcSDK.FileDesc, error) {
	uniqfiles := make([]dcSDK.FileDesc, 0, 0)
	uniqmap := make(map[string]bool)
	for _, f := range files {
		uniqkey := fmt.Sprintf("%s_^|^_%s", f.FilePath, f.Targetrelativepath)
		if _, ok := uniqmap[uniqkey]; !ok {
			uniqmap[uniqkey] = true
			uniqfiles = append(uniqfiles, f)
		} else {
			blog.Infof("basic: file: %s %s repreated", f.FilePath, f.Targetrelativepath)
		}
	}

	blog.Infof("basic: uniq before file num: %d after num: %d", len(files), len(uniqfiles))

	// ++for debug by tomtian
	for _, v := range uniqfiles {
		blog.Debugf("basic: toolchain file: %s", v.FilePath)
	}
	// --
	return uniqfiles, nil
}

func getToolChainFiles(t *types.ToolChain) ([]dcSDK.FileDesc, error) {
	if t == nil {
		return nil, fmt.Errorf("tool chain is nil when get tool chain files")
	}

	sdkOneToolchain := dcSDK.OneToolChain{
		ToolName:               t.ToolName,
		ToolLocalFullPath:      t.ToolLocalFullPath,
		ToolRemoteRelativePath: t.ToolRemoteRelativePath,
		Files:                  t.Files,
	}

	sdkToolchain := &dcSDK.Toolchain{
		Toolchains: []dcSDK.OneToolChain{sdkOneToolchain},
	}

	files, err := sdkToolchain.ToFileDesc()
	if err != nil {
		return nil, err
	}

	return files, err
}

func diffToolChainFiles(oldfs, newfs *[]dcSDK.FileDesc) (bool, string, error) {
	same := true
	diffdesc := ""

	if oldfs != nil && newfs == nil {
		same = false
		diffdesc = fmt.Sprintf("new file list is nil")
		return same, diffdesc, nil
	}

	blog.Infof("basic: old files[%d], new files[%d]", len(*oldfs), len(*newfs))

	newfiles := make([]string, 0, 0)
	deletefiles := make([]string, 0, 0)
	samenamefiles := make([]string, 0, 0)
	sizechangedfiles := make([]string, 0, 0) // only compare size now, not md5

	// check samename / new / sizechanged
	found := false
	for _, newf := range *newfs {
		found = false
		for _, oldf := range *oldfs {
			if newf.FilePath == oldf.FilePath {
				found = true
				samenamefiles = append(samenamefiles, newf.FilePath)
				if newf.FileSize != oldf.FileSize {
					blog.Infof("basic: file[%s] size changed, need send again", newf.FilePath)
					sizechangedfiles = append(sizechangedfiles, newf.FilePath)
				} else if newf.Lastmodifytime != oldf.Lastmodifytime {
					blog.Infof("basic: file[%s] moidfy time changed, need send again", newf.FilePath)
					sizechangedfiles = append(sizechangedfiles, newf.FilePath)
				}
				break
			}
		}
		if !found {
			newfiles = append(newfiles, newf.FilePath)
		}
	}

	// check deletefiles
	if len(samenamefiles) != len(*oldfs) {
		for _, oldf := range *oldfs {
			found = false
			for _, newf := range *newfs {
				if oldf.FilePath == newf.FilePath {
					found = true
					break
				}
			}
			if !found {
				deletefiles = append(deletefiles, oldf.FilePath)
			}
		}
	}

	// maybe we can ignore delete files
	if len(newfiles) > 0 || len(sizechangedfiles) > 0 || len(deletefiles) > 0 {
		same = false
		diffdesc = fmt.Sprintf("new files[%v], size changed files[%v], deleted files[%v]",
			newfiles, sizechangedfiles, deletefiles)
		return same, diffdesc, nil
	}

	blog.Infof("basic: new files[%d], size changed files[%d], deleted files[%d] same files[%d]",
		len(newfiles), len(sizechangedfiles), len(deletefiles), len(samenamefiles))

	return same, diffdesc, nil
}

func replaceTaskID(uniqid string, toolchain *types.ToolChain) error {
	blog.Debugf("basic: try to render tool chain with ID: %s, toolchain:%+v", uniqid, *toolchain)

	if strings.Contains(toolchain.ToolRemoteRelativePath, toolchainTaskIDKey) {
		toolchain.ToolRemoteRelativePath = strings.Replace(
			toolchain.ToolRemoteRelativePath, toolchainTaskIDKey, uniqid, -1)
	}

	for i, f := range toolchain.Files {
		if strings.Contains(f.RemoteRelativePath, toolchainTaskIDKey) {
			toolchain.Files[i].RemoteRelativePath = strings.Replace(
				f.RemoteRelativePath, toolchainTaskIDKey, uniqid, -1)
		}
	}

	return nil
}
