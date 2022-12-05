/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package winclangcl

import (
	"fmt"
	"io/ioutil"
	"os"

	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/winclangcl/cmd"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/util"
)

// WinClangCl 定义了windows clang-cl.exe编译
type WinClangCl struct {
	tag              string
	sandbox          *dcSyscall.Sandbox
	sendFiles        []dcSDK.FileDesc
	preprocessedFile string
}

func NewWinClangCl() (handler.Handler, error) {
	return &WinClangCl{
		tag:     util.RandomString(5),
		sandbox: &dcSyscall.Sandbox{},
	}, nil
}

// InitSandbox set sandbox to task-cc
func (cc *WinClangCl) InitSandbox(sandbox *dcSyscall.Sandbox) {
	cc.sandbox = sandbox
}

// InitExtra receive the extra data from project settings and init the handle extra data
func (cc *WinClangCl) InitExtra(extra []byte) {

}

// ResultExtra return the extra message to record in project info
func (cc *WinClangCl) ResultExtra() (extra []byte) {
	return nil
}

func (cc *WinClangCl) PreWork(config *dcType.BoosterConfig) error {
	return nil
}

func (cc *WinClangCl) PostWork(config *dcType.BoosterConfig) error {
	return nil
}

// RenderArgs return the actual executing args with the origin args and config
func (cc *WinClangCl) RenderArgs(config dcType.BoosterConfig, originArgs string) string {

	return originArgs
}

func (cc *WinClangCl) PreExecuteNeedLock(command []string) bool {

	return true
}

func (cc *WinClangCl) PostExecuteNeedLock(result *dcSDK.BKDistResult) bool {
	return false
}

func (cc *WinClangCl) PreLockWeight(command []string) int32 {
	return 1
}

// PreExecute 预处理
func (cc *WinClangCl) PreExecute(command []string) (*dcSDK.BKDistCommand, error) {
	commandline, err := cmd.Parse(command)
	if err != nil {
		return nil, err
	}
	err = cc.preprocess(commandline)
	if err != nil {
		return nil, err
	}
	serverSideArgs := commandline.RenderToServerSide(cc.preprocessedFile)

	return &dcSDK.BKDistCommand{
		Commands: []dcSDK.BKCommand{
			{
				WorkDir:     cc.sandbox.Dir,
				ExePath:     "",
				ExeName:     "clang",
				Params:      serverSideArgs[1:],
				Inputfiles:  cc.sendFiles,
				ResultFiles: []string{commandline.Obj},
			},
		},
	}, nil
}

// LocalExecuteNeed 无需自定义本地处理
func (cc *WinClangCl) LocalExecuteNeed(command []string) bool {
	return false
}

// LocalLockWeight decide local-execute lock weight, default 1
func (cc *WinClangCl) LocalLockWeight(command []string) int32 {
	return 1
}

// LocalExecute 无需自定义本地处理
func (cc *WinClangCl) LocalExecute(command []string) (int, error) {
	return 0, nil
}

// NeedRemoteResource check whether this command need remote resource
func (cc *WinClangCl) NeedRemoteResource(command []string) bool {
	return true
}

// RemoteRetryTimes will return the remote retry times
func (cc *WinClangCl) RemoteRetryTimes() int {
	return 0
}

// PostLockWeight decide post-execute lock weight, default 1
func (cc *WinClangCl) PostLockWeight(result *dcSDK.BKDistResult) int32 {
	return 1
}

// PostExecute 后置处理, 判断远程执行的结果是否正确
func (cc *WinClangCl) PostExecute(r *dcSDK.BKDistResult) error {
	blog.Infof("cc: [%s] start post execute", cc.tag)
	if r == nil || len(r.Results) == 0 {
		return fmt.Errorf("cc: remote execute error")
	}

	if r.Results[0].RetCode == 0 {
		blog.Infof("cc: [%s] success done post execute", cc.tag)
		return nil
	}
	return fmt.Errorf("cc: [%s] failed to remote execute, retcode %d, error message:%s, output message:%s",
		cc.tag,
		r.Results[0].RetCode,
		r.Results[0].ErrorMessage,
		r.Results[0].OutputMessage)
}

// FinalExecute 清理临时文件
func (cc *WinClangCl) FinalExecute(args []string) {
	if err := os.RemoveAll(cc.preprocessedFile); err != nil {
		blog.Warnf("cc: [%s] clean tmp file %s failed: %v", cc.tag, cc.preprocessedFile, err)
	}
}

// GetFilterRules add file send filter
func (cc *WinClangCl) GetFilterRules() ([]dcSDK.FilterRuleItem, error) {
	return []dcSDK.FilterRuleItem{
		{
			Rule:       dcSDK.FilterRuleFileSuffix,
			Operator:   dcSDK.FilterRuleOperatorEqual,
			Standard:   ".gch",
			HandleType: dcSDK.FilterRuleHandleAllDistribution,
		},
	}, nil
}

func (cc *WinClangCl) GetPreloadConfig(config dcType.BoosterConfig) (*dcSDK.PreloadConfig, error) {

	return nil, nil
}

//编译过程的预处理
func (cc *WinClangCl) preprocess(command cmd.Commandline) error {
	sandbox := cc.sandbox.Fork()
	tmpfile, err := ioutil.TempFile("", "bk_boost_*."+command.FileType)
	if err != nil {
		blog.Errorf("cc: [%s] %v", cc.tag, err)
		return err
	}
	sandbox.Stdout = tmpfile
	args := command.RenderToPreprocess()
	ret, err := sandbox.ExecCommand(args[0], args[1:]...)
	if ret != 0 {
		blog.Errorf("cc: [%s] %v, %v", cc.tag, err, sandbox)
		return err
	}

	existed, fileSize, modifyTime, fileMode := dcFile.Stat(tmpfile.Name()).Batch()
	if !existed {
		err := fmt.Errorf("preprocess result file %s not existed", tmpfile.Name())
		blog.Errorf("cc: [%s] %v", cc.tag, err)
		return err
	}
	cc.preprocessedFile = tmpfile.Name()
	cc.sendFiles = append(cc.sendFiles, dcSDK.FileDesc{
		FilePath:       tmpfile.Name(),
		Compresstype:   protocol.CompressLZ4,
		FileSize:       fileSize,
		Lastmodifytime: modifyTime,
		Md5:            "",
		Filemode:       fileMode,
	})
	tmpfile.Close()
	return err
}
