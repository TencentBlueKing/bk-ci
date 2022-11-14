/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package cc

import (
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"runtime"
	"strings"

	dcEnv "github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	commonUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/handler/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

const (
	MaxWindowsCommandLength = 30000
)

var (
	// ForceLocalFileKeys force some module to compile locally
	// ForceLocalFileKeys = []string{}

	DefaultForceLocalResponseFileKeys = make([]string, 0, 0)
	// DefaultForceLocalCppFileKeys force some cpp to compile locally
	DefaultForceLocalCppFileKeys = make([]string, 0, 0)
)

// TaskCC 定义了c/c++编译的描述处理对象, 一般用来处理ue4-mac下的clang编译
type TaskCC struct {
	sandbox *dcSyscall.Sandbox

	ccacheEnable bool

	// tmp file list to clean
	tmpFileList []string

	// different stages args
	originArgs       []string
	ensuredArgs      []string
	expandArgs       []string
	scannedArgs      []string
	rewriteCrossArgs []string
	preProcessArgs   []string
	serverSideArgs   []string

	// file names
	inputFile        string
	preprocessedFile string
	outputFile       string
	firstIncludeFile string
	pchFile          string
	responseFile     string

	pchFileDesc *dcSDK.FileDesc

	ForceLocalResponseFileKeys []string
	ForceLocalCppFileKeys      []string
}

// NewTaskCC get a new task-cc handler
func NewTaskCC() *TaskCC {
	key1 := make([]string, len(DefaultForceLocalResponseFileKeys))
	copy(key1, DefaultForceLocalResponseFileKeys)

	key2 := make([]string, len(DefaultForceLocalCppFileKeys))
	copy(key2, DefaultForceLocalCppFileKeys)

	return &TaskCC{
		sandbox:                    &dcSyscall.Sandbox{},
		tmpFileList:                make([]string, 0, 10),
		ForceLocalResponseFileKeys: key1,
		ForceLocalCppFileKeys:      key2,
	}
}

// InitSandbox set sandbox to task-cc
func (cc *TaskCC) InitSandbox(sandbox *dcSyscall.Sandbox) {
	cc.sandbox = sandbox
}

// PreExecuteNeedLock 需要pre-lock来保证预处理不会跑满本地资源
func (cc *TaskCC) PreExecuteNeedLock(command []string) bool {
	return true
}

// PostExecuteNeedLock 不需要post-lock
func (cc *TaskCC) PostExecuteNeedLock(result *dcSDK.BKDistResult) bool {
	return false
}

// PreLockWeight decide pre-execute lock weight, default 1
func (cc *TaskCC) PreLockWeight(command []string) int32 {
	return 1
}

// PreExecute 预处理
func (cc *TaskCC) PreExecute(command []string) (*dcSDK.BKDistCommand, error) {
	return cc.preExecute(command)
}

// LocalExecuteNeed 无需自定义本地处理
func (cc *TaskCC) LocalExecuteNeed(command []string) bool {
	return false
}

// LocalLockWeight decide local-execute lock weight, default 1
func (cc *TaskCC) LocalLockWeight(command []string) int32 {
	return 1
}

// LocalExecute 无需自定义本地处理
func (cc *TaskCC) LocalExecute(command []string) (int, error) {
	return 0, nil
}

// NeedRemoteResource check whether this command need remote resource
func (cc *TaskCC) NeedRemoteResource(command []string) bool {
	return true
}

// RemoteRetryTimes will return the remote retry times
func (cc *TaskCC) RemoteRetryTimes() int {
	return 0
}

// PostLockWeight decide post-execute lock weight, default 1
func (cc *TaskCC) PostLockWeight(result *dcSDK.BKDistResult) int32 {
	return 1
}

// PostExecute 后置处理, 判断远程执行的结果是否正确
func (cc *TaskCC) PostExecute(r *dcSDK.BKDistResult) error {
	return cc.postExecute(r)
}

// FinalExecute 清理临时文件
func (cc *TaskCC) FinalExecute(args []string) {
	cc.finalExecute(args)
}

// GetFilterRules add file send filter
func (cc *TaskCC) GetFilterRules() ([]dcSDK.FilterRuleItem, error) {
	return []dcSDK.FilterRuleItem{
		{
			Rule:       dcSDK.FilterRuleFileSuffix,
			Operator:   dcSDK.FilterRuleOperatorEqual,
			Standard:   ".gch",
			HandleType: dcSDK.FilterRuleHandleAllDistribution,
		},
	}, nil
}

func (cc *TaskCC) preExecute(command []string) (*dcSDK.BKDistCommand, error) {
	blog.Infof("cc: start pre execute for: %v", command)

	// debugRecordFileName(fmt.Sprintf("cc: start pre execute for: %v", command))

	cc.originArgs = command
	responseFile, args, err := ensureCompiler(command, cc.sandbox.Dir)
	if err != nil {
		blog.Warnf("cc: pre execute ensure compiler %v: %v", args, err)
		return nil, err
	}

	// obtain force key set by booster
	forcekeystr := cc.sandbox.Env.GetEnv(dcEnv.KeyExecutorForceLocalKeys)
	if forcekeystr != "" {
		blog.Infof("cc: got force local key string: %s", forcekeystr)
		forcekeylist := strings.Split(forcekeystr, dcEnv.CommonBKEnvSepKey)
		if len(forcekeylist) > 0 {
			cc.ForceLocalResponseFileKeys = append(cc.ForceLocalResponseFileKeys, forcekeylist...)
			cc.ForceLocalCppFileKeys = append(cc.ForceLocalCppFileKeys, forcekeylist...)
			blog.Infof("cc: ForceLocalResponseFileKeys: %v, ForceLocalCppFileKeys: %v",
				cc.ForceLocalResponseFileKeys, cc.ForceLocalCppFileKeys)
		}
	}

	if responseFile != "" {
		for _, v := range cc.ForceLocalResponseFileKeys {
			if v != "" && strings.Contains(responseFile, v) {
				blog.Warnf("cc: pre execute found response %s is in force local list, do not deal now",
					responseFile)
				return nil, fmt.Errorf("response file %s is in force local list", responseFile)
			}
		}
	}

	for _, v := range args {
		if strings.HasSuffix(v, ".cpp") {
			for _, v1 := range cc.ForceLocalCppFileKeys {
				if v1 != "" && strings.Contains(v, v1) {
					blog.Warnf("cc: pre execute found %s is in force local list, do not deal now", v)
					return nil, fmt.Errorf("arg %s is in force local cpp list", v)
				}
			}
			break
		}
	}

	cc.responseFile = responseFile
	cc.ensuredArgs = args

	// debugRecordFileName("preBuild begin")

	if err = cc.preBuild(args); err != nil {
		blog.Warnf("cc: pre execute pre-build %v: %v", args, err)
		return nil, err
	}

	// debugRecordFileName("FileInfo begin")

	existed, fileSize, modifyTime, fileMode := dcFile.Stat(cc.preprocessedFile).Batch()
	if !existed {
		err := fmt.Errorf("result file %s not existed", cc.preprocessedFile)
		blog.Errorf("%v", err)
		return nil, err
	}

	// generate the input files for pre-process file
	inputFiles := []dcSDK.FileDesc{{
		FilePath:       cc.preprocessedFile,
		Compresstype:   protocol.CompressLZ4,
		FileSize:       fileSize,
		Lastmodifytime: modifyTime,
		Md5:            "",
		Filemode:       fileMode,
	}}

	// if there is a pch file, add it into the inputFiles, it should be also sent to remote
	if cc.pchFileDesc != nil {
		inputFiles = append(inputFiles, *cc.pchFileDesc)
	}

	// debugRecordFileName(fmt.Sprintf("cc: success done pre execute for: %v", command))

	blog.Infof("cc: success done pre execute for: %v", command)
	blog.Infof("cc: going to execute compile: %v", cc.serverSideArgs)

	// to check whether need to compile with response file
	exeName := cc.serverSideArgs[0]
	params := cc.serverSideArgs[1:]

	return &dcSDK.BKDistCommand{
		Commands: []dcSDK.BKCommand{
			{
				WorkDir:         "",
				ExePath:         "",
				ExeName:         exeName,
				ExeToolChainKey: dcSDK.GetJsonToolChainKey(command[0]),
				Params:          params,
				Inputfiles:      inputFiles,
				ResultFiles: []string{
					cc.outputFile,
				},
			},
		},
		CustomSave: true,
	}, nil
}

func (cc *TaskCC) postExecute(r *dcSDK.BKDistResult) error {
	blog.Infof("cc: start post execute for: %v", cc.originArgs)
	if r == nil || len(r.Results) == 0 {
		return ErrorInvalidParam
	}

	resultfilenum := 0
	// by tomtian 20201224,to ensure existed result file
	if len(r.Results[0].ResultFiles) == 0 {
		blog.Warnf("cc: not found result file for: %v", cc.originArgs)
		goto ERROREND
	}
	blog.Infof("cc: found %d result files for result[0]", len(r.Results[0].ResultFiles))

	// resultfilenum := 0
	if len(r.Results[0].ResultFiles) > 0 {
		for _, f := range r.Results[0].ResultFiles {
			if f.Buffer != nil {
				if err := saveResultFile(&f, cc.sandbox.Dir); err != nil {
					blog.Errorf("cc: failed to save file [%s]", f.FilePath)
					return err
				}
				resultfilenum++
			}
		}
	}

	// by tomtian 20201224,to ensure existed result file
	if resultfilenum == 0 {
		blog.Warnf("cc: not found result file for: %v", cc.originArgs)
		goto ERROREND
	}

	if r.Results[0].RetCode == 0 {
		blog.Infof("cc: success done post execute for: %v", cc.originArgs)
		// set output to inputFile
		r.Results[0].OutputMessage = []byte(filepath.Base(cc.inputFile))
		return nil
	}

ERROREND:
	// write error message into
	if cc.saveTemp() && len(r.Results[0].ErrorMessage) > 0 {
		// make the tmp file for storing the stderr from server compiler.
		stderrFile, err := makeTmpFile(commonUtil.GetHandlerTmpDir(cc.sandbox), "cc_server_stderr", ".txt")
		if err != nil {
			blog.Warnf("cc: make tmp file for stderr from server failed: %v", err)
		} else {
			if f, err := os.OpenFile(stderrFile, os.O_RDWR, 0644); err == nil {
				_, _ = f.Write(r.Results[0].ErrorMessage)
				_ = f.Close()
				blog.Debugf("cc: save error message to %s for: %s", stderrFile, cc.originArgs)
			}
		}
	}

	return fmt.Errorf("cc: failed to remote execute, retcode %d, error message:%s, output message:%s",
		r.Results[0].RetCode,
		r.Results[0].ErrorMessage,
		r.Results[0].OutputMessage)
}

func (cc *TaskCC) finalExecute([]string) {
	if cc.saveTemp() {
		return
	}

	cc.cleanTmpFile()
}

func (cc *TaskCC) saveTemp() bool {
	return commonUtil.GetHandlerEnv(cc.sandbox, envSaveTempFile) != ""
}

func (cc *TaskCC) preBuild(args []string) error {
	// debugRecordFileName(fmt.Sprintf("cc: preBuild in..."))

	blog.Infof("cc: pre-build begin got args: %v", args)

	// debugRecordFileName(fmt.Sprintf("cc: pre-build ready expandPreprocessorOptions"))

	var err error
	// expand args to make the following process easier.
	if cc.expandArgs, err = expandPreprocessorOptions(args); err != nil {
		blog.Warnf("cc: pre-build error, expand pre-process options %v: %v", args, err)
		return err
	}

	// debugRecordFileName(fmt.Sprintf("cc: pre-build ready scanArgs"))

	// scan the args, check if it can be compiled remotely, wrap some un-used options,
	// and get the real input&output file.
	scannedData, err := scanArgs(cc.expandArgs)
	if err != nil {
		blog.Warnf("cc: pre-build not support, scan args %v: %v", cc.expandArgs, err)
		return err
	}
	cc.scannedArgs = scannedData.args
	cc.firstIncludeFile = getFirstIncludeFile(scannedData.args)
	cc.inputFile = scannedData.inputFile
	cc.outputFile = scannedData.outputFile

	// handle the cross-compile issues.
	targetArgs := cc.scannedArgs
	if commonUtil.GetHandlerEnv(cc.sandbox, envNoRewriteCross) != "1" {
		var targetArgsGeneric, targetArgsACT, targetArgsGRF []string

		if targetArgsGeneric, err = rewriteGenericCompiler(targetArgs); err != nil {
			blog.Warnf("cc: pre-build error, rewrite generic compiler %v: %v", targetArgs, err)
			return err
		}

		if targetArgsACT, err = addClangTarget(targetArgsGeneric); err != nil {
			blog.Warnf("cc: pre-build error, add clang target %v: %v", targetArgsGeneric, err)
			return err
		}

		if targetArgsGRF, err = gccRewriteFqn(targetArgsACT); err != nil {
			blog.Warnf("cc: pre-build error, gcc rewrite fqn %v: %v", targetArgsACT, err)
			return err
		}

		targetArgs = targetArgsGRF
	}
	cc.rewriteCrossArgs = targetArgs

	// debugRecordFileName(fmt.Sprintf("cc: pre-build ready scanPchFile"))

	// handle the pch options
	finalArgs := cc.scanPchFile(targetArgs)

	// debugRecordFileName(fmt.Sprintf("cc: pre-build ready makeTmpFile"))

	// debugRecordFileName(fmt.Sprintf("cc: pre-build ready doPreProcess"))

	// do the pre-process, store result in the file.
	if cc.preprocessedFile, err = cc.doPreProcess(finalArgs, cc.inputFile); err != nil {
		blog.Warnf("cc: pre-build error, do pre-process %v: %v", finalArgs, err)
		return err
	}

	// debugRecordFileName(fmt.Sprintf("cc: pre-build ready stripLocalArgs"))

	// strip the args and get the server side args.
	serverSideArgs := stripLocalArgs(finalArgs)

	// replace the input file into preprocessedFile, for the next server side process.
	for index := range serverSideArgs {
		if serverSideArgs[index] == cc.inputFile {
			serverSideArgs[index] = cc.preprocessedFile
			break
		}
	}

	// quota result file if it's path contains space
	if runtime.GOOS == "windows" {
		if hasSpace(cc.outputFile) && !strings.HasPrefix(cc.outputFile, "\"") {
			for index := range serverSideArgs {
				if strings.HasPrefix(serverSideArgs[index], "-o") {
					if len(serverSideArgs[index]) > 2 {
						serverSideArgs[index] = fmt.Sprintf("-o\"%s\"", cc.outputFile)
					} else {
						index++
						if index < len(serverSideArgs) {
							serverSideArgs[index] = fmt.Sprintf("\"%s\"", cc.outputFile)
						}
					}
					break
				}
			}
		}
	}

	cc.serverSideArgs = serverSideArgs

	// debugRecordFileName(fmt.Sprintf("cc: pre-build finished"))

	blog.Infof("cc: pre-build success for enter args: %v", args)
	return nil
}

func (cc *TaskCC) addTmpFile(filename string) {
	cc.tmpFileList = append(cc.tmpFileList, filename)
}

func (cc *TaskCC) cleanTmpFile() {
	for _, filename := range cc.tmpFileList {
		if err := os.Remove(filename); err != nil {
			blog.Warnf("cc: clean tmp file %s failed: %v", filename, err)
		}
	}
}

// If the input filename is a plain source file rather than a
// preprocessed source file, then preprocess it to a temporary file
// and return the name.
//
// The preprocessor may still be running when we return; you have to
// wait for cpp_pid to exit before the output is complete.  This
// allows us to overlap opening the TCP socket, which probably doesn't
// use many cycles, with running the preprocessor.
func (cc *TaskCC) doPreProcess(args []string, inputFile string) (string, error) {

	// debugRecordFileName(fmt.Sprintf("cc: doPreProcess in..."))

	if isPreprocessedFile(inputFile) {
		blog.Infof("cc: input \"%s\" is already preprocessed", inputFile)

		// input file already preprocessed
		return inputFile, nil
	}

	outputExt := getPreprocessedExt(inputFile)
	outputFile, err := makeTmpFile(commonUtil.GetHandlerTmpDir(cc.sandbox), "cc", outputExt)
	if err != nil {
		blog.Warnf("cc: do pre-process get output file failed: %v", err)
		return "", err
	}
	cc.addTmpFile(outputFile)

	newArgs, err := setActionOptionE(stripDashO(args))
	if err != nil {
		blog.Warnf("cc: set action option -E: %v", err)
		return "", err
	}
	cc.preProcessArgs = newArgs

	var execName string
	var execArgs []string
	flag, rspfile, err := cc.needSaveResponseFile(newArgs)
	if flag && err == nil {
		rspargs := []string{newArgs[0], fmt.Sprintf("@%s", rspfile)}
		blog.Infof("cc: going to execute pre-process: %s", strings.Join(rspargs, " "))
		execName = rspargs[0]
		execArgs = rspargs[1:]
		cc.addTmpFile(rspfile)

	} else {
		blog.Infof("cc: going to execute pre-process: %s", strings.Join(newArgs, " "))
		execName = newArgs[0]
		execArgs = newArgs[1:]
	}

	output, err := os.OpenFile(outputFile, os.O_WRONLY, 0666)
	if err != nil {
		blog.Errorf("cc: failed to open output file \"%s\" when pre-processing: %v", outputFile, err)
		return "", err
	}
	defer func() {
		_ = output.Close()
	}()
	sandbox := cc.sandbox.Fork()
	sandbox.Stdout = output

	if _, err = sandbox.ExecCommand(execName, execArgs...); err != nil {
		blog.Errorf("cc: failed to do pre-process %s %s: %v", execName, strings.Join(execArgs, " "), err)
		return "", err
	}
	blog.Infof("cc: success to execute pre-process and get %s: %s", outputFile, strings.Join(newArgs, " "))

	return outputFile, nil
}

// try to get pch file desc and the args according to firstIncludeFile
// if pch is valid, there must be a option -include xx.h(xx.hpp)
// and must be the first seen -include option(if there are multiple -include)
func (cc *TaskCC) scanPchFile(args []string) []string {
	if cc.firstIncludeFile == "" {
		return args
	}

	filename := cc.firstIncludeFile
	if !strings.HasSuffix(cc.firstIncludeFile, ".gch") {
		filename = fmt.Sprintf("%s.gch", cc.firstIncludeFile)
	}

	existed, fileSize, modifyTime, fileMode := dcFile.Stat(filename).Batch()
	if !existed {
		blog.Debugf("cc: try to get pch file for %s but %s is not exist", cc.firstIncludeFile, filename)
		return args
	}
	cc.pchFile = filename

	blog.Debugf("cc: success to find pch file %s for %s", filename, cc.firstIncludeFile)

	cc.pchFileDesc = &dcSDK.FileDesc{
		FilePath:           filename,
		Compresstype:       protocol.CompressLZ4,
		FileSize:           fileSize,
		Lastmodifytime:     modifyTime,
		Md5:                "",
		Filemode:           fileMode,
		Targetrelativepath: filepath.Dir(filename),
	}

	for _, arg := range args {
		if arg == pchPreProcessOption {
			return args
		}
	}

	return append(args, pchPreProcessOption)
}

func (cc *TaskCC) needSaveResponseFile(args []string) (bool, string, error) {
	exe := args[0]
	if strings.HasSuffix(exe, "clang.exe") || strings.HasSuffix(exe, "clang++.exe") {
		if len(args) > 1 {
			fullArgs := MakeCmdLine(args[1:])
			// if len(fullArgs) >= MaxWindowsCommandLength {
			if cc.responseFile != "" || len(fullArgs) >= MaxWindowsCommandLength {
				rspFile, err := makeTmpFile(commonUtil.GetHandlerTmpDir(cc.sandbox), "cc", ".response")
				if err != nil {
					return false, "", fmt.Errorf("cc: cmd too long and failed to create rsp file")
				}

				err = ioutil.WriteFile(rspFile, []byte(fullArgs), os.ModePerm)
				if err != nil {
					blog.Errorf("cc: failed to write data to file[%s], error:%v", rspFile, err)
					return false, "", err
				}

				return true, rspFile, nil
			}
		}
	}

	return false, "", nil
}
