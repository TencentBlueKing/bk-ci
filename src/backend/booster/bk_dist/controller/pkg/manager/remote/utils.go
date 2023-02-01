/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package remote

import (
	"fmt"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/types"
)

func getFileDetailsFromExecuteRequest(req *types.RemoteTaskExecuteRequest) []*types.FilesDetails {
	fd := make([]*types.FilesDetails, 0, len(req.Req.Commands[0].Inputfiles))
	for _, c := range req.Req.Commands {
		for _, f := range c.Inputfiles {
			fd = append(fd, &types.FilesDetails{
				Servers: []*dcProtocol.Host{req.Server},
				File:    f,
			})
		}
	}
	return fd
}

func getMaxSizeFile(req *types.RemoteTaskExecuteRequest, threshold int64) (string, int64) {
	var maxsize int64
	fpath := ""
	for _, c := range req.Req.Commands {
		for _, v := range c.Inputfiles {
			if v.FileSize > maxsize {
				fpath = v.FilePath
				maxsize = v.FileSize
			}
		}
	}

	if maxsize > threshold {
		return fpath, maxsize
	}

	return "", 0
}

// updateTaskRequestInputFilesReady 根据给定的baseDirs, 标记request中对应index的文件为"已经发送", 可以直接使用
func updateTaskRequestInputFilesReady(req *types.RemoteTaskExecuteRequest, baseDirs []string) error {
	index := 0
	for i, c := range req.Req.Commands {
		for j := range c.Inputfiles {
			if index >= len(baseDirs) {
				return fmt.Errorf("baseDirs length not equals to input files")
			}

			baseDir := baseDirs[index]
			if baseDir == "" {
				index++
				continue
			}

			req.Req.Commands[i].Inputfiles[j].FileSize = -1
			req.Req.Commands[i].Inputfiles[j].CompressedSize = -1
			req.Req.Commands[i].Inputfiles[j].Targetrelativepath = baseDir
			index++
		}
	}
	return nil
}

func getFileDetailsFromSendFileRequest(req *types.RemoteTaskSendFileRequest) []*types.FilesDetails {
	fd := make([]*types.FilesDetails, 0, 100)
	for _, f := range req.Req {
		fd = append(fd, &types.FilesDetails{
			Servers: []*dcProtocol.Host{req.Server},
			File:    f,
		})
	}
	return fd
}

func getIPFromServer(server string) string {
	return strings.Split(server, ":")[0]
}

func workerSideCache(sandbox *dcSyscall.Sandbox) bool {
	if sandbox == nil || sandbox.Env == nil {
		return false
	}
	return sandbox.Env.GetEnv(env.KeyExecutorWorkerSideCache) != ""
}
