/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package cl

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"runtime"
	"strings"

	dcConfig "github.com/Tencent/bk-ci/src/booster/bk_dist/common/config"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcEnv "github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler"
	commonUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/handler/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

const (
	hookConfigPathDefault  = "bk_default_rules.json"
	hookConfigPathCCCommon = "bk_cl_rules.json"

	MaxWindowsCommandLength = 30000
)

var (
	// ForceLocalFileKeys force some module to compile locally
	// /GL for ProxyLOD
	// wrong inlcude for NetCore by user
	// unsuport PUSH_MACRO and POP_MACRO in NvClothIncludes.h for ClothingSystemRuntime
	DefaultForceLocalResponseFileKeys = []string{
		"dte80a",
		"ProxyLOD",
		"NetCore",
		"ClothingSystemRuntime",
		"Module.LuaTools.cpp",
		"Module.Client.1_of_4.cpp",
		"Module.HoloLensTargetPlatform.cpp",
	}
	// ForceLocalCppFileKeys force some cpp to compile locally
	DefaultForceLocalCppFileKeys = []string{
		"dte80a",
		"ProxyLOD",
		"NetCore",
		"ClothingSystemRuntime",
		"Module.LuaTools.cpp",
		"Module.Client.1_of_4.cpp",
		"Module.HoloLensTargetPlatform.cpp",
	}
	// DisabledWarnings for ue4 ,disable some warnings
	DisabledWarnings = []string{"/wd4828"}
)

// TaskCL 定义了cl.exe编译的描述处理对象, 一般用来处理ue4-win下的cl编译
type TaskCL struct {
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

	// to save preprocessed file content
	preprocessedBuffer []byte

	// for /showIncludes
	showinclude          bool
	preprocessedErrorBuf string

	pchFileDesc *dcSDK.FileDesc

	ForceLocalResponseFileKeys []string
	ForceLocalCppFileKeys      []string
}

// NewTaskCL get a new cl-handler
func NewTaskCL() handler.Handler {
	key1 := make([]string, len(DefaultForceLocalResponseFileKeys))
	copy(key1, DefaultForceLocalResponseFileKeys)

	key2 := make([]string, len(DefaultForceLocalCppFileKeys))
	copy(key2, DefaultForceLocalCppFileKeys)

	return &TaskCL{
		sandbox:                    &dcSyscall.Sandbox{},
		tmpFileList:                make([]string, 0, 10),
		ForceLocalResponseFileKeys: key1,
		ForceLocalCppFileKeys:      key2,
	}
}

// ++for cl-filter
func NewCL() *TaskCL {
	key1 := make([]string, len(DefaultForceLocalResponseFileKeys))
	copy(key1, DefaultForceLocalResponseFileKeys)

	key2 := make([]string, len(DefaultForceLocalCppFileKeys))
	copy(key2, DefaultForceLocalCppFileKeys)

	return &TaskCL{
		sandbox:                    &dcSyscall.Sandbox{},
		tmpFileList:                make([]string, 0, 10),
		ForceLocalResponseFileKeys: key1,
		ForceLocalCppFileKeys:      key2,
	}
}

// GetPreprocessedBuf return preprocessedErrorBuf
func (cl *TaskCL) GetPreprocessedBuf() string {
	return cl.preprocessedErrorBuf
}

// --

// InitSandbox set sandbox to task-cl
func (cl *TaskCL) InitSandbox(sandbox *dcSyscall.Sandbox) {
	cl.sandbox = sandbox
}

// InitExtra no need
func (cl *TaskCL) InitExtra(extra []byte) {
}

// ResultExtra no need
func (cl *TaskCL) ResultExtra() []byte {
	return nil
}

// RenderArgs no need change
func (cl *TaskCL) RenderArgs(config dcType.BoosterConfig, originArgs string) string {
	return originArgs
}

// PreWork no need
func (cl *TaskCL) PreWork(config *dcType.BoosterConfig) error {
	return nil
}

// PostWork no need
func (cl *TaskCL) PostWork(config *dcType.BoosterConfig) error {
	return nil
}

// GetPreloadConfig get preload config
func (cl *TaskCL) GetPreloadConfig(config dcType.BoosterConfig) (*dcSDK.PreloadConfig, error) {
	return getPreloadConfig(cl.getPreLoadConfigPath(config))
}

func (cl *TaskCL) getPreLoadConfigPath(config dcType.BoosterConfig) string {
	if config.Works.HookConfigPath != "" {
		return config.Works.HookConfigPath
	}

	// degrade will not contain the CL
	if config.Works.Degraded {
		return dcConfig.GetFile(hookConfigPathDefault)
	}

	return dcConfig.GetFile(hookConfigPathCCCommon)
}

// PreExecuteNeedLock 防止预处理跑满本机CPU
func (cl *TaskCL) PreExecuteNeedLock(command []string) bool {
	return true
}

// PostExecuteNeedLock 防止回传的文件读写跑满本机磁盘
func (cl *TaskCL) PostExecuteNeedLock(result *dcSDK.BKDistResult) bool {
	return true
}

// PreLockWeight decide pre-execute lock weight, default 1
func (cl *TaskCL) PreLockWeight(command []string) int32 {
	return 1
}

// PreExecute 预处理
func (cl *TaskCL) PreExecute(command []string) (*dcSDK.BKDistCommand, error) {
	return cl.preExecute(command)
}

// NeedRemoteResource check whether this command need remote resource
func (cl *TaskCL) NeedRemoteResource(command []string) bool {
	return true
}

// RemoteRetryTimes will return the remote retry times
func (cl *TaskCL) RemoteRetryTimes() int {
	return 0
}

// LocalLockWeight decide local-execute lock weight, default 1
func (cl *TaskCL) LocalLockWeight(command []string) int32 {
	return 1
}

// PostExecute 后置处理
func (cl *TaskCL) PostExecute(r *dcSDK.BKDistResult) error {
	return cl.postExecute(r)
}

// LocalExecuteNeed no need
func (cl *TaskCL) LocalExecuteNeed(command []string) bool {
	return false
}

// PostLockWeight decide post-execute lock weight, default 1
func (cl *TaskCL) PostLockWeight(result *dcSDK.BKDistResult) int32 {
	return 1
}

// LocalExecute no need
func (cl *TaskCL) LocalExecute(command []string) (int, error) {
	if len(command) < 1 {
		return 0, fmt.Errorf("cl: failed to execute command, for args is empty")
	}

	sandbox := cl.sandbox.Fork()
	flag, rspfile, err := cl.needSaveResponseFile(command)
	if flag && err == nil {
		return sandbox.ExecCommand(command[0], fmt.Sprintf("@%s", rspfile))
	} else {
		return sandbox.ExecCommand(command[0], command[1:]...)
	}
}

// FinalExecute 清理临时文件
func (cl *TaskCL) FinalExecute(args []string) {
	cl.finalExecute(args)
}

// GetFilterRules add file send filter
func (cl *TaskCL) GetFilterRules() ([]dcSDK.FilterRuleItem, error) {
	return []dcSDK.FilterRuleItem{
		{
			Rule:     dcSDK.FilterRuleFileSuffix,
			Operator: dcSDK.FilterRuleOperatorEqual,
			Standard: ".pch",
		},
	}, nil
}

func (cl *TaskCL) preExecute(command []string) (*dcSDK.BKDistCommand, error) {
	blog.Infof("cl: start pre execute for: %v", command)

	// debugRecordFileName(fmt.Sprintf("cl: start pre execute for: %v", command))

	cl.originArgs = command
	responseFile, args, showinclude, err := ensureCompiler(command)
	if err != nil {
		blog.Warnf("cl: pre execute ensure compiler failed %v: %v", args, err)
		return nil, err
	}

	// obtain force key set by booster
	if cl.sandbox != nil && cl.sandbox.Env != nil {
		forcekeystr := cl.sandbox.Env.GetEnv(dcEnv.KeyExecutorForceLocalKeys)
		if forcekeystr != "" {
			blog.Infof("cl: got force local key string: %s", forcekeystr)
			forcekeylist := strings.Split(forcekeystr, dcEnv.CommonBKEnvSepKey)
			if len(forcekeylist) > 0 {
				cl.ForceLocalResponseFileKeys = append(cl.ForceLocalResponseFileKeys, forcekeylist...)
				cl.ForceLocalCppFileKeys = append(cl.ForceLocalCppFileKeys, forcekeylist...)
				blog.Infof("cl: ForceLocalResponseFileKeys: %v, ForceLocalCppFileKeys: %v",
					cl.ForceLocalResponseFileKeys, cl.ForceLocalCppFileKeys)
			}
		}
	}

	// ++ by tomtian 20201030
	if responseFile != "" {
		for _, v := range cl.ForceLocalResponseFileKeys {
			if v != "" && strings.Contains(responseFile, v) {
				blog.Warnf("cl: pre execute found response %s is in force local list, do not deal now",
					responseFile)
				return nil, fmt.Errorf("response file %s is in force local list", responseFile)
			}
		}
	}
	// --

	for _, v := range args {
		if strings.HasSuffix(v, ".cpp") {
			for _, v1 := range cl.ForceLocalCppFileKeys {
				if v1 != "" && strings.Contains(v, v1) {
					blog.Warnf("cl: pre execute found %s is in force local list, do not deal now", v)
					return nil, fmt.Errorf("arg %s is in force local cpp list", v)
				}
			}
			break
		}
	}

	cl.responseFile = responseFile
	cl.ensuredArgs = args
	cl.showinclude = showinclude

	// debugRecordFileName("preBuild begin")

	if err = cl.preBuild(args); err != nil {
		blog.Warnf("cl: pre execute pre-build %v: %v", args, err)
		return nil, err
	}

	// debugRecordFileName("FileInfo begin")

	var existed bool
	var fileSize int64
	var modifyTime int64
	var fileMode uint32
	if cl.preprocessedBuffer != nil {
		fileSize = int64(len(cl.preprocessedBuffer))
		modifyTime = 0
		fileMode = uint32(os.ModePerm)
	} else {
		existed, fileSize, modifyTime, fileMode = dcFile.Stat(cl.preprocessedFile).Batch()
		if !existed {
			err := fmt.Errorf("input pre file %s not existed", cl.preprocessedFile)
			blog.Errorf("%v", err)
			return nil, err
		}
	}

	// generate the input files for pre-process file
	inputFiles := []dcSDK.FileDesc{{
		FilePath:       cl.preprocessedFile,
		Compresstype:   protocol.CompressLZ4,
		FileSize:       fileSize,
		Lastmodifytime: modifyTime,
		Md5:            "",
		Filemode:       fileMode,
		Buffer:         cl.preprocessedBuffer,
	}}

	// if there is a pch file, add it into the inputFiles, it should be also sent to remote
	if cl.pchFileDesc != nil {
		inputFiles = append(inputFiles, *cl.pchFileDesc)
	}

	// debugRecordFileName(fmt.Sprintf("cl: success done pre execute for: %v", command))

	blog.Infof("cl: success done pre execute for: %v", command)
	blog.Infof("cl: going to execute compile: %v", cl.serverSideArgs)

	// to check whether need to compile with response file
	exeName := cl.serverSideArgs[0]
	params := cl.serverSideArgs[1:]
	flag, rspfile, err := cl.needSaveResponseFile(cl.serverSideArgs)
	if flag && err == nil {
		cl.addTmpFile(rspfile)
		params = []string{fmt.Sprintf("@%s", rspfile)}
		existed, fileSize, modifyTime, fileMode := dcFile.Stat(rspfile).Batch()
		if !existed {
			err := fmt.Errorf("input response file %s not existed", rspfile)
			blog.Errorf("%v", err)
			return nil, err
		}
		inputFiles = append(inputFiles, dcSDK.FileDesc{
			FilePath:       rspfile,
			Compresstype:   protocol.CompressLZ4,
			FileSize:       fileSize,
			Lastmodifytime: modifyTime,
			Md5:            "",
			Filemode:       fileMode,
		})
	}

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
					cl.outputFile,
				},
			},
		},
		CustomSave: true,
	}, nil
}

func (cl *TaskCL) postExecute(r *dcSDK.BKDistResult) error {
	blog.Infof("cl: start post execute for: %v", cl.originArgs)
	if r == nil || len(r.Results) == 0 {
		return ErrorInvalidParam
	}

	resultfilenum := 0
	// by tomtian 20201224,to ensure existed result file
	if len(r.Results[0].ResultFiles) == 0 {
		blog.Warnf("cl: not found result file for: %v", cl.originArgs)
		goto ERROREND
	}
	blog.Infof("cl: found %d result files for result[0]", len(r.Results[0].ResultFiles))

	// resultfilenum := 0
	if len(r.Results[0].ResultFiles) > 0 {
		for _, f := range r.Results[0].ResultFiles {
			if f.Buffer != nil {
				if err := saveResultFile(&f); err != nil {
					blog.Errorf("cl: failed to save file [%s]", f.FilePath)
					return err
				}
				resultfilenum++
			}
		}
	}

	// by tomtian 20201224,to ensure existed result file
	if resultfilenum == 0 {
		blog.Warnf("cl: not found result file for: %v", cl.originArgs)
		goto ERROREND
	}

	if r.Results[0].RetCode == 0 {
		blog.Infof("cl: success done post execute for: %v", cl.originArgs)
		if cl.showinclude {
			// simulate output with preprocessed error output
			r.Results[0].OutputMessage = []byte(cl.preprocessedErrorBuf)
		} else {
			// simulate output with inputFile
			r.Results[0].OutputMessage = []byte(filepath.Base(cl.inputFile))
		}
		return nil
	}

ERROREND:
	// write error message into
	if cl.saveTemp() && len(r.Results[0].ErrorMessage) > 0 {
		// make the tmp file for storing the stderr from server compiler.
		stderrFile, err := makeTmpFile(commonUtil.GetHandlerTmpDir(cl.sandbox), "cl_server_stderr", ".txt")
		if err != nil {
			blog.Warnf("cl: make tmp file for stderr from server failed: %v", err)
		} else {
			if f, err := os.OpenFile(stderrFile, os.O_RDWR, 0644); err == nil {
				_, _ = f.Write(r.Results[0].ErrorMessage)
				_ = f.Close()
				blog.Debugf("cl: save error message to %s for: %s", stderrFile, cl.originArgs)
			}
		}
	}

	return fmt.Errorf("cl: failed to remote execute, retcode %d, error message:%s, output message:%s",
		r.Results[0].RetCode,
		r.Results[0].ErrorMessage,
		r.Results[0].OutputMessage)
}

func (cl *TaskCL) finalExecute([]string) {
	if cl.saveTemp() {
		return
	}

	cl.cleanTmpFile()
}

func (cl *TaskCL) saveTemp() bool {
	return commonUtil.GetHandlerEnv(cl.sandbox, envSaveTempFile) != ""
}

func (cl *TaskCL) preBuild(args []string) error {
	blog.Infof("cl: pre-build begin got args: %v", args)

	var err error
	cl.expandArgs = args

	// scan the args, check if it can be compiled remotely, wrap some un-used options,
	// and get the real input&output file.
	scannedData, err := scanArgs(cl.expandArgs)
	if err != nil {
		// blog.Warnf("cl: pre-build not support, scan args %v: %v", cl.expandArgs, err)
		return err
	}
	cl.scannedArgs = scannedData.args
	// ++ by tomtian 20201126, pch has no effect for compile
	// cl.firstIncludeFile = getFirstIncludeFile(scannedData.args)
	cl.firstIncludeFile = ""
	// --
	cl.inputFile = scannedData.inputFile
	cl.outputFile = scannedData.outputFile
	cl.rewriteCrossArgs = cl.scannedArgs

	// handle the pch options
	finalArgs := cl.scanPchFile(cl.scannedArgs)

	// disable some warning here
	for _, v := range DisabledWarnings {
		finalArgs = append(finalArgs, v)
	}

	// do the pre-process, store result in the file.
	if cl.preprocessedFile, cl.preprocessedBuffer, err = cl.doPreProcess(finalArgs, cl.inputFile); err != nil {
		blog.Errorf("cl: pre-build error, do pre-process %v: %v", finalArgs, err)
		return err
	}

	// strip the args and get the server side args.
	serverSideArgs := stripLocalArgs(finalArgs)

	// replace the input file into preprocessedFile, for the next server side process.
	for index := range serverSideArgs {
		if serverSideArgs[index] == cl.inputFile {
			serverSideArgs[index] = cl.preprocessedFile
			break
		}
	}

	// quota result file if it's path contains space
	if runtime.GOOS == "windows" {
		if hasSpace(cl.outputFile) && !strings.HasPrefix(cl.outputFile, "\"") {
			for index := range serverSideArgs {
				if strings.HasPrefix(serverSideArgs[index], "/Fo") {
					if len(serverSideArgs[index]) > 3 {
						serverSideArgs[index] = fmt.Sprintf("/Fo\"%s\"", cl.outputFile)
					} else {
						index++
						if index < len(serverSideArgs) {
							serverSideArgs[index] = fmt.Sprintf("\"%s\"", cl.outputFile)
						}
					}
					break
				}
			}
		}
	}

	cl.serverSideArgs = serverSideArgs

	blog.Infof("cl: pre-build success for enter args: %v", args)
	return nil
}

func (cl *TaskCL) addTmpFile(filename string) {
	cl.tmpFileList = append(cl.tmpFileList, filename)
}

func (cl *TaskCL) cleanTmpFile() {
	for _, filename := range cl.tmpFileList {
		if err := os.Remove(filename); err != nil {
			blog.Warnf("cl: clean tmp file %s failed: %v", filename, err)
		}
	}
}

func formatArg(arg string) string {
	if arg != "" && strings.HasPrefix(arg, "\"") && strings.HasSuffix(arg, "\"") {
		return strings.Trim(arg, "\"")
	}

	return arg
}

// If the input filename is a plain source file rather than a
// preprocessed source file, then preprocess it to a temporary file
// and return the name.
//
// The preprocessor may still be running when we return; you have to
// wait for cpp_pid to exit before the output is complete.  This
// allows us to overlap opening the TCP socket, which probably doesn't
// use many cycles, with running the preprocessor.
func (cl *TaskCL) doPreProcess(args []string, inputFile string) (string, []byte, error) {
	if isPreprocessedFile(inputFile) {
		blog.Infof("cl: input \"%s\" is already preprocessed", inputFile)

		// input file already preprocessed
		return inputFile, nil, nil
	}

	// to check whether need save to memroy
	savetomemroy := false
	if cl.sandbox.Env.IsSet(env.KeyExecutorWriteMemory) {
		savetomemroy = true
		blog.Infof("cl: ready save processed file to memory")
	}

	outputExt := getPreprocessedExt(inputFile)
	var err error
	outputFile := ""
	if !savetomemroy {
		outputFile, err = makeTmpFile(commonUtil.GetHandlerTmpDir(cl.sandbox), "cl", outputExt)
		if err != nil {
			blog.Errorf("cl: do pre-process get output file failed: %v", err)
			return "", nil, err
		}
		cl.addTmpFile(outputFile)
	} else {
		outputFile = makeTmpFileName(commonUtil.GetHandlerTmpDir(cl.sandbox), "cl", outputExt)
	}

	newArgs, err := setActionOptionE(stripDashO(args))
	if err != nil {
		blog.Warnf("cl: set action option /E : %v", err)
		return "", nil, err
	}

	tempArgs := []string{newArgs[0]}
	if len(newArgs) > 1 {
		for _, v := range newArgs[1:] {
			tempArgs = append(tempArgs, formatArg(v))
		}
	}
	newArgs = tempArgs

	cl.preProcessArgs = newArgs

	var execName string
	var execArgs []string
	flag, rspfile, err := cl.needSaveResponseFile(newArgs)
	if flag && err == nil {
		rspargs := []string{newArgs[0], fmt.Sprintf("@%s", rspfile)}
		blog.Infof("cl: going to execute pre-process: %s", strings.Join(rspargs, " "))
		execName = rspargs[0]
		execArgs = rspargs[1:]
		cl.addTmpFile(rspfile)
	} else {
		blog.Infof("cl: going to execute pre-process: %s", strings.Join(newArgs, " "))
		execName = newArgs[0]
		execArgs = newArgs[1:]
	}

	sandbox := cl.sandbox.Fork()
	var outBuf bytes.Buffer

	if !savetomemroy {
		output, err := os.OpenFile(outputFile, os.O_WRONLY, 0666)
		if err != nil {
			blog.Errorf("cc: failed to open output file \"%s\" when pre-processing: %v", outputFile, err)
			return "", nil, err
		}
		defer func() {
			_ = output.Close()
		}()

		sandbox.Stdout = output
	} else {
		sandbox.Stdout = &outBuf
	}

	var errBuf bytes.Buffer
	sandbox.Stderr = &errBuf

	if _, err = sandbox.ExecCommand(execName, execArgs...); err != nil {
		blog.Warnf("cl: failed to do pre-process %s: %v, %s",
			strings.Join(newArgs, " "), err, errBuf.String())
		return "", nil, err
	}
	blog.Infof("cl: success to execute pre-process and get %s: %s", outputFile, strings.Join(newArgs, " "))
	if cl.showinclude {
		cl.preprocessedErrorBuf = errBuf.String()
	}

	if !savetomemroy {
		return outputFile, nil, nil
	}

	return outputFile, outBuf.Bytes(), nil
}

// TODO : not ok for windows pch
func (cl *TaskCL) scanPchFile(args []string) []string {
	//  ++ by tomtian, do not send pch files, it has no effect for compile
	return args
	// --

	//if cl.firstIncludeFile == "" {
	//	return args
	//}
	//
	//filename := cl.firstIncludeFile
	//if !strings.HasSuffix(cl.firstIncludeFile, ".pch") {
	//	filename = fmt.Sprintf("%s.pch", cl.firstIncludeFile)
	//}
	//
	//existed, fileSize, modifyTime, fileMode := dcFile.Stat(filename).Batch()
	//if !existed {
	//	blog.Debugf("cl: try to get pch file for %s but %s is not exist", cl.firstIncludeFile, filename)
	//	return args
	//}
	//cl.pchFile = filename
	//
	//blog.Debugf("cl: success to find pch file %s for %s", filename, cl.firstIncludeFile)
	//
	//cl.pchFileDesc = &dcSDK.FileDesc{
	//	FilePath:       filename,
	//	Compresstype:   protocol.CompressLZ4,
	//	FileSize:       fileSize,
	//	Lastmodifytime: modifyTime,
	//	Md5:            "",
	//	Filemode:       fileMode,
	//}

	// for _, arg := range args {
	// 	if arg == pchPreProcessOption {
	// 		return args
	// 	}
	// }

	// return append(args, pchPreProcessOption)
}

func (cl *TaskCL) needSaveResponseFile(args []string) (bool, string, error) {
	exe := args[0]
	if strings.HasSuffix(exe, "cl.exe") {
		if len(args) > 1 {
			fullArgs := MakeCmdLine(args[1:])
			if len(fullArgs) >= MaxWindowsCommandLength {
				rspFile, err := makeTmpFile(commonUtil.GetHandlerTmpDir(cl.sandbox), "cl", ".response")
				if err != nil {
					return false, "", fmt.Errorf("cl: cmd too long and failed to create rsp file")
				}

				err = ioutil.WriteFile(rspFile, []byte(fullArgs), os.ModePerm)
				if err != nil {
					blog.Errorf("cl: failed to write data to file[%s], error:%v", rspFile, err)
					return false, "", err
				}

				return true, rspFile, nil
			}
		}
	}

	return false, "", nil
}
