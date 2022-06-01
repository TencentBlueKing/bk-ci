/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package astc

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"

	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// NewTextureCompressor get a new tc handler
func NewTextureCompressor() handler.Handler {
	return &TextureCompressor{
		sandbox: &dcSyscall.Sandbox{},
	}
}

type tcType string

const (
	unknown tcType = "unknown"
	astcArm tcType = "astc-arm"
)

// return inputfile,tempoutput,finaloutput,params,error
func (t tcType) scanParam(param []string) (string, string, string, []string, error) {
	switch t {
	case astcArm:
		// astc at least has 4 options: -cs input output 6x6
		if len(param) < 4 {
			return "", "", "", nil, fmt.Errorf("invalid astc command, too few params")
		}

		abspathinput, _ := filepath.Abs(param[1])
		abspathoutput, _ := filepath.Abs(param[2])
		base := filepath.Base(abspathoutput)
		dir := filepath.Dir(abspathoutput)
		outputTempFile := filepath.Join(dir, "bktemp_"+base)

		newparam := make([]string, len(param))
		copy(newparam, param)
		newparam[2] = outputTempFile
		return abspathinput, outputTempFile, abspathoutput, newparam, nil

	default:
		return "", "", "", nil, fmt.Errorf("invalid command, unsupported type %s for seeking output file", t)
	}
}

func getTCType(command string) (tcType, error) {
	switch filepath.Base(command) {
	case "astcenc", "astcenc.exe", "astcenc-sse2.exe":
		return astcArm, nil
	default:
		return unknown, fmt.Errorf("unknown texture compressor type")
	}
}

// TextureCompressor describe the handler to handle texture compress in unity3d
type TextureCompressor struct {
	sandbox *dcSyscall.Sandbox

	outputTempFile string
	outputRealFile string
}

// InitSandbox init sandbox
func (tc *TextureCompressor) InitSandbox(sandbox *dcSyscall.Sandbox) {
	tc.sandbox = sandbox
}

// InitExtra no need
func (tc *TextureCompressor) InitExtra(extra []byte) {

}

// ResultExtra no need
func (tc *TextureCompressor) ResultExtra() []byte {
	return nil
}

// RenderArgs no need change
func (tc *TextureCompressor) RenderArgs(config dcType.BoosterConfig, originArgs string) string {
	return originArgs
}

// PreWork no need
func (tc *TextureCompressor) PreWork(config *dcType.BoosterConfig) error {
	return nil
}

// PostWork no need
func (tc *TextureCompressor) PostWork(config *dcType.BoosterConfig) error {
	return nil
}

// GetPreloadConfig no need
func (tc *TextureCompressor) GetPreloadConfig(config dcType.BoosterConfig) (*dcSDK.PreloadConfig, error) {
	return nil, nil
}

// GetFilterRules no need
func (tc *TextureCompressor) GetFilterRules() ([]dcSDK.FilterRuleItem, error) {
	return nil, nil
}

// PreExecuteNeedLock no need
func (tc *TextureCompressor) PreExecuteNeedLock(command []string) bool {
	return false
}

// PostExecuteNeedLock no need
func (tc *TextureCompressor) PostExecuteNeedLock(result *dcSDK.BKDistResult) bool {
	return false
}

// PreLockWeight decide pre-execute lock weight, default 1
func (tc *TextureCompressor) PreLockWeight(command []string) int32 {
	return 1
}

// PreExecute parse the input and output file, and then just run the origin command in remote
func (tc *TextureCompressor) PreExecute(command []string) (*dcSDK.BKDistCommand, error) {
	if len(command) == 0 {
		return nil, fmt.Errorf("invalid command")
	}

	blog.Infof("tc: pre execute for: %v", command)

	t, err := getTCType(command[0])
	if err != nil {
		return nil, err
	}

	var inputFile string
	var newparam []string
	inputFile, tc.outputTempFile, tc.outputRealFile, newparam, err = t.scanParam(command[1:])
	if err != nil {
		return nil, err
	}
	blog.Infof("tc: got new param [%s]", strings.Join(newparam, " "))

	existed, fileSize, modifyTime, fileMode := dcFile.Stat(inputFile).Batch()
	if !existed {
		return nil, fmt.Errorf("input file %s not exist", inputFile)
	}

	return &dcSDK.BKDistCommand{
		Commands: []dcSDK.BKCommand{
			{
				WorkDir: "",
				ExePath: "",
				// ExeName: filepath.Base(command[0]),
				ExeName:         command[0],
				ExeToolChainKey: dcSDK.GetJsonToolChainKey(command[0]),
				Params:          newparam,
				Inputfiles: []dcSDK.FileDesc{{
					FilePath:           inputFile,
					Compresstype:       protocol.CompressLZ4,
					FileSize:           fileSize,
					Lastmodifytime:     modifyTime,
					Md5:                "",
					Filemode:           fileMode,
					Targetrelativepath: filepath.Dir(inputFile),
				}},
				ResultFiles: []string{tc.outputTempFile},
			},
		},
	}, nil
}

// RemoteRetryTimes will return the remote retry times
func (tc *TextureCompressor) RemoteRetryTimes() int {
	return 0
}

// PostLockWeight decide post-execute lock weight, default 1
func (tc *TextureCompressor) PostLockWeight(result *dcSDK.BKDistResult) int32 {
	return 1
}

// PostExecute judge the result
func (tc *TextureCompressor) PostExecute(r *dcSDK.BKDistResult) error {
	if r == nil || len(r.Results) == 0 {
		return fmt.Errorf("invalid param")
	}
	result := r.Results[0]

	if result.RetCode != 0 {
		return fmt.Errorf("failed to execute on remote: %s", string(result.ErrorMessage))
	}

	if len(result.ResultFiles) == 0 {
		return fmt.Errorf("tc: not found result file, retcode %d, error message:[%s], output message:[%s]",
			result.RetCode,
			result.ErrorMessage,
			result.OutputMessage)
	}

	// move result temp to real
	blog.Infof("tc: ready rename file from [%s] to [%s]", tc.outputTempFile, tc.outputRealFile)
	_ = os.Rename(tc.outputTempFile, tc.outputRealFile)

	return nil
}

// LocalExecuteNeed no need
func (tc *TextureCompressor) LocalExecuteNeed(command []string) bool {
	return false
}

// LocalLockWeight decide local-execute lock weight, default 1
func (tc *TextureCompressor) LocalLockWeight(command []string) int32 {
	return 1
}

// NeedRemoteResource check whether this command need remote resource
func (tc *TextureCompressor) NeedRemoteResource(command []string) bool {
	return true
}

// LocalExecute no need
func (tc *TextureCompressor) LocalExecute(command []string) (int, error) {
	return 0, nil
}

// FinalExecute no need
func (tc *TextureCompressor) FinalExecute([]string) {
}
