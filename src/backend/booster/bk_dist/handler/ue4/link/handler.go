/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package link

import (
	"fmt"
	"strings"

	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

const (
	// do not distribute if over this
	MaxInputFileSize = 1024 * 1024 * 50
)

var (
	// ForceLocalFileKeys force some module to compile locally
	// mscoree.lib missed for SwarmInterface
	ForceLocalFileKeys = []string{"UE4Editor-SwarmInterface"}
)

// TaskLink 定义了link.exe链接的描述处理对象, 一般用来处理ue4-win下的link链接
type TaskLink struct {
	sandbox *dcSyscall.Sandbox

	// different stages args
	originArgs  []string
	ensuredArgs []string
	scannedArgs []string

	// file names
	inputFile    []string
	outputFile   []string
	responseFile string
}

// NewTaskLink get a new link handler
func NewTaskLink() *TaskLink {
	return &TaskLink{
		sandbox: &dcSyscall.Sandbox{},
	}
}

// InitSandbox set sandbox to task-link
func (l *TaskLink) InitSandbox(sandbox *dcSyscall.Sandbox) {
	l.sandbox = sandbox
}

// InitExtra no need
func (l *TaskLink) InitExtra(extra []byte) {
}

// ResultExtra no need
func (l *TaskLink) ResultExtra() []byte {
	return nil
}

// RenderArgs no need change
func (l *TaskLink) RenderArgs(config dcType.BoosterConfig, originArgs string) string {
	return originArgs
}

// PreWork no need
func (l *TaskLink) PreWork(config *dcType.BoosterConfig) error {
	return nil
}

// PostWork no need
func (l *TaskLink) PostWork(config *dcType.BoosterConfig) error {
	return nil
}

// GetPreloadConfig no preload config need
func (l *TaskLink) GetPreloadConfig(config dcType.BoosterConfig) (*dcSDK.PreloadConfig, error) {
	return nil, nil
}

// PreExecuteNeedLock 没有在本地执行的预处理步骤, 无需pre-lock
func (l *TaskLink) PreExecuteNeedLock(command []string) bool {
	return false
}

// PostExecuteNeedLock 防止回传的文件读写跑满本机磁盘
func (l *TaskLink) PostExecuteNeedLock(result *dcSDK.BKDistResult) bool {
	return true
}

// PreLockWeight decide pre-execute lock weight, default 1
func (l *TaskLink) PreLockWeight(command []string) int32 {
	return 1
}

// PreExecute 预处理
func (l *TaskLink) PreExecute(command []string) (*dcSDK.BKDistCommand, error) {
	return l.preExecute(command)
}

// NeedRemoteResource check whether this command need remote resource
func (l *TaskLink) NeedRemoteResource(command []string) bool {
	return true
}

// RemoteRetryTimes will return the remote retry times
func (l *TaskLink) RemoteRetryTimes() int {
	return 0
}

// PostLockWeight decide post-execute lock weight, default 1
func (l *TaskLink) PostLockWeight(result *dcSDK.BKDistResult) int32 {
	return 1
}

// PostExecute 后置处理
func (l *TaskLink) PostExecute(r *dcSDK.BKDistResult) error {
	return l.postExecute(r)
}

// LocalExecuteNeed no need
func (l *TaskLink) LocalExecuteNeed(command []string) bool {
	return false
}

// LocalLockWeight decide local-execute lock weight, default 1
func (l *TaskLink) LocalLockWeight(command []string) int32 {
	return 1
}

// LocalExecute no need
func (l *TaskLink) LocalExecute(command []string) (int, error) {
	return 0, nil
}

// FinalExecute no need
func (l *TaskLink) FinalExecute([]string) {
}

// GetFilterRules add file send filter
func (l *TaskLink) GetFilterRules() ([]dcSDK.FilterRuleItem, error) {
	return nil, nil
}

func (l *TaskLink) preExecute(command []string) (*dcSDK.BKDistCommand, error) {
	blog.Infof("link: start pre execute for: %v", command)

	l.originArgs = command
	responseFile, args, err := ensureCompiler(command)
	if err != nil {
		blog.Errorf("link: pre execute ensure compiler failed %v: %v", args, err)
		return nil, err
	}

	for _, v := range ForceLocalFileKeys {
		if strings.Contains(responseFile, v) {
			blog.Errorf("link: pre execute found response %s is in force local list, do not deal now", responseFile)
			return nil, fmt.Errorf("response file %s is in force local list", responseFile)
		}
	}

	l.responseFile = responseFile
	l.ensuredArgs = args

	if err = l.scan(args); err != nil {
		blog.Errorf("link: scan args[%v] failed : %v", args, err)
		return nil, err
	}

	inputFiles := make([]dcSDK.FileDesc, 0, 0)
	for _, v := range l.inputFile {
		existed, fileSize, modifyTime, fileMode := dcFile.Stat(v).Batch()
		if !existed {
			err := fmt.Errorf("input pre file %s not existed", v)
			blog.Errorf("%v", err)
			return nil, err
		}

		// generate the input files for pre-process file
		inputFiles = append(inputFiles, dcSDK.FileDesc{
			FilePath:       v,
			Compresstype:   protocol.CompressLZ4,
			FileSize:       fileSize,
			Lastmodifytime: modifyTime,
			Md5:            "",
			Filemode:       fileMode,
		})
	}

	blog.Infof("link: success done pre execute for: %v", command)

	// ++ for debug
	var totalinputsize int64
	for _, v := range inputFiles {
		totalinputsize += v.FileSize
	}
	blog.Infof("link: [%d] input files, total size[%d]", len(inputFiles), totalinputsize)

	if totalinputsize > MaxInputFileSize {
		err := fmt.Errorf("input files total size %d over max size %d", totalinputsize, MaxInputFileSize)
		blog.Errorf("link: pre execute ensure compiler failed %v: %v", args, err)
		return nil, err
	}
	// --

	// to check whether need to compile with response file
	exeName := l.scannedArgs[0]
	params := l.scannedArgs[1:]

	return &dcSDK.BKDistCommand{
		Commands: []dcSDK.BKCommand{
			{
				WorkDir:     "",
				ExePath:     "",
				ExeName:     exeName,
				Params:      params,
				Inputfiles:  inputFiles,
				ResultFiles: l.outputFile,
			},
		},
		CustomSave: true,
	}, nil
}

func (l *TaskLink) postExecute(r *dcSDK.BKDistResult) error {
	blog.Infof("link: start post execute for: %v", l.originArgs)
	if r == nil || len(r.Results) == 0 {
		return ErrorInvalidParam
	}

	if len(r.Results[0].ResultFiles) > 0 {
		for _, f := range r.Results[0].ResultFiles {
			if f.Buffer != nil {
				if err := saveResultFile(&f); err != nil {
					blog.Errorf("link: failed to save file [%s]", f.FilePath)
					return err
				}
			}
		}
	}

	if r.Results[0].RetCode == 0 {
		blog.Infof("link: success done post execute for: %v", l.originArgs)
		return nil
	}

	return fmt.Errorf("link: failed to remote execute, retcode %d, error message:%s, output message:%s",
		r.Results[0].RetCode,
		r.Results[0].ErrorMessage,
		r.Results[0].OutputMessage)
}

func (l *TaskLink) scan(args []string) error {
	blog.Infof("link: scan begin got args: %v", args)

	var err error

	scannedData, err := scanArgs(args)
	if err != nil {
		blog.Errorf("link: scan args failed %v: %v", args, err)
		return err
	}

	l.scannedArgs = scannedData.args
	l.inputFile = scannedData.inputFile
	l.outputFile = scannedData.outputFile

	blog.Infof("link: scan success for enter args: %v", args)
	return nil
}
