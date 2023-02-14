/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package clfilter

import (
	"bufio"
	"io"
	"os"
	"path/filepath"
	"strings"

	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/ue4/cl"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// TaskCLFilter 定义了cl-filter编译的描述处理对象, 一般用来处理ue4-win下的cl编译
// cl-filter是ue4拉起的编译器, 套了一层cl
type TaskCLFilter struct {
	sandbox *dcSyscall.Sandbox

	// tmp file list to clean
	tmpFileList []string

	// different stages args
	originArgs []string
	cldArgs    []string

	// file names
	dependentFile string

	clhandle *cl.TaskCL
}

// NewTaskCLFilter get a new cl-filter handler
func NewTaskCLFilter() handler.Handler {

	return &TaskCLFilter{
		sandbox:     &dcSyscall.Sandbox{},
		tmpFileList: make([]string, 0, 10),
		clhandle:    cl.NewCL(),
	}
}

// InitSandbox set sandbox to task-cl-filter
func (cf *TaskCLFilter) InitSandbox(sandbox *dcSyscall.Sandbox) {
	cf.sandbox = sandbox
	if cf.clhandle != nil {
		cf.clhandle.InitSandbox(sandbox)
	}
}

// InitExtra no need
func (cf *TaskCLFilter) InitExtra(extra []byte) {
}

// ResultExtra no need
func (cf *TaskCLFilter) ResultExtra() []byte {
	return nil
}

// RenderArgs no need change
func (cf *TaskCLFilter) RenderArgs(config dcType.BoosterConfig, originArgs string) string {
	return originArgs
}

// PreWork no need
func (cf *TaskCLFilter) PreWork(config *dcType.BoosterConfig) error {
	return nil
}

// PostWork no need
func (cf *TaskCLFilter) PostWork(config *dcType.BoosterConfig) error {
	return nil
}

// GetPreloadConfig no preload config need
func (cf *TaskCLFilter) GetPreloadConfig(config dcType.BoosterConfig) (*dcSDK.PreloadConfig, error) {
	return nil, nil
}

// PreExecuteNeedLock 防止预处理跑满本机CPU
func (cf *TaskCLFilter) PreExecuteNeedLock(command []string) bool {
	return true
}

// PostExecuteNeedLock 防止回传的文件读写跑满本机磁盘
func (cf *TaskCLFilter) PostExecuteNeedLock(result *dcSDK.BKDistResult) bool {
	return true
}

// PreLockWeight decide pre-execute lock weight, default 1
func (cf *TaskCLFilter) PreLockWeight(command []string) int32 {
	if cf.clhandle != nil {
		return cf.clhandle.PreLockWeight(command)
	}
	return 1
}

// PreExecute 预处理, 复用cl-handler的逻辑
func (cf *TaskCLFilter) PreExecute(command []string) (*dcSDK.BKDistCommand, error) {
	return cf.preExecute(command)
}

// NeedRemoteResource check whether this command need remote resource
func (cf *TaskCLFilter) NeedRemoteResource(command []string) bool {
	return true
}

// RemoteRetryTimes will return the remote retry times
func (cf *TaskCLFilter) RemoteRetryTimes() int {
	return 0
}

// PostLockWeight decide post-execute lock weight, default 1
func (cf *TaskCLFilter) PostLockWeight(result *dcSDK.BKDistResult) int32 {
	if cf.clhandle != nil {
		return cf.clhandle.PostLockWeight(result)
	}
	return 1
}

// PostExecute 后置处理, 复用cl-handler的逻辑
func (cf *TaskCLFilter) PostExecute(r *dcSDK.BKDistResult) error {
	return cf.postExecute(r)
}

// LocalExecuteNeed no need
func (cf *TaskCLFilter) LocalExecuteNeed(command []string) bool {
	return false
}

// LocalLockWeight decide local-execute lock weight, default 1
func (cf *TaskCLFilter) LocalLockWeight(command []string) int32 {
	if cf.clhandle != nil {
		return cf.clhandle.LocalLockWeight(command)
	}
	return 1
}

// LocalExecute no need
func (cf *TaskCLFilter) LocalExecute(command []string) (int, error) {
	return 0, nil
}

// FinalExecute 清理临时文件
func (cf *TaskCLFilter) FinalExecute(args []string) {
	cf.clhandle.FinalExecute(args)
}

// GetFilterRules add file send filter
func (cf *TaskCLFilter) GetFilterRules() ([]dcSDK.FilterRuleItem, error) {
	return []dcSDK.FilterRuleItem{
		{
			Rule:     dcSDK.FilterRuleFileSuffix,
			Operator: dcSDK.FilterRuleOperatorEqual,
			Standard: ".pch",
		},
	}, nil
}

func (cf *TaskCLFilter) preExecute(command []string) (*dcSDK.BKDistCommand, error) {
	blog.Infof("cf: start pre execute for: %v", command)

	// debugRecordFileName(fmt.Sprintf("cl: start pre execute for: %v", command))

	cf.originArgs = command
	dependFile, args, err := ensureCompiler(command)
	if err != nil {
		blog.Errorf("cf: pre execute ensure compiler failed %v: %v", args, err)
		return nil, err
	}

	cf.dependentFile = dependFile
	cf.cldArgs = args
	blog.Infof("cf: after pre execute, got depend file: [%s], cl cmd:[%s]", cf.dependentFile, cf.cldArgs)

	cf.clhandle.InitSandbox(cf.sandbox)
	cf.clhandle.SetDepend(cf.dependentFile)

	return cf.clhandle.PreExecute(args)
}

func (cf *TaskCLFilter) postExecute(r *dcSDK.BKDistResult) error {
	blog.Infof("cf: start post execute for: %v", cf.originArgs)

	err := cf.clhandle.PostExecute(r)
	if err != nil {
		return err
	}

	// save include to txt file
	blog.Debugf("cf: ready parse ouput [%s] for: %v", r.Results[0].OutputMessage, cf.originArgs)
	output, err := cf.parseOutput(string(r.Results[0].OutputMessage))
	if err != nil {
		return err
	}

	r.Results[0].OutputMessage = []byte(output)
	blog.Debugf("cf: after parse ouput [%s] for: %v", r.Results[0].OutputMessage, cf.originArgs)
	return nil
}

func (cf *TaskCLFilter) parseOutput(s string) (string, error) {
	blog.Debugf("cf: start parse output: %s", s)

	output := make([]string, 0, 0)
	includes := make([]string, 0, 0)

	reader := bufio.NewReader(strings.NewReader(s))
	var line string
	var err error
	for {
		line, err = reader.ReadString('\n')
		if err != nil && err != io.EOF {
			break
		}

		// Process the line here.
		// Note: including file: Runtime\Core\Public\HAL/ThreadHeartBeat.h
		// Note: including file:     C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Tools\MSVC\14.31.31103\INCLUDE\sal.h
		columns := strings.Split(line, ":")
		if len(columns) == 3 {
			includefile := strings.Trim(columns[2], " \r\n")
			if !filepath.IsAbs(includefile) {
				includefile, _ = filepath.Abs(filepath.Join(cf.sandbox.Dir, includefile))
			}
			existed, _, _, _ := dcFile.Stat(includefile).Batch()
			if existed {
				includes = append(includes, includefile)
			} else {
				blog.Infof("cf: includefile [%s] not existed", includefile)
				output = append(output, line)
			}
		} else if len(columns) == 4 {
			includefile := columns[2] + ":" + columns[3]
			includefile = strings.Trim(includefile, " \r\n")
			existed, _, _, _ := dcFile.Stat(includefile).Batch()
			if existed {
				includes = append(includes, includefile)
			} else {
				blog.Infof("cf: includefile [%s] not existed", includefile)
				output = append(output, line)
			}
		} else {
			output = append(output, line)
		}

		if err != nil {
			break
		}
	}
	blog.Debugf("cf: got output: [%v], includes:[%v]", output, includes)

	// save includes to cf.dependentFile
	if len(includes) > 0 {
		f, err := os.Create(cf.dependentFile)
		if err != nil {
			blog.Errorf("cf: create file %s error: [%s]", cf.dependentFile, err.Error())
		} else {
			defer func() {
				_ = f.Close()
			}()
			_, err := f.Write([]byte(strings.Join(includes, "\r\n")))
			if err != nil {
				blog.Errorf("cf: save depend file [%s] error: [%s]", cf.dependentFile, err.Error())
				return strings.Join(output, "\n"), err
			}
		}
	}

	return strings.Join(output, ""), nil
}
