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
	"bytes"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcEnv "github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcPump "github.com/Tencent/bk-ci/src/booster/bk_dist/common/pump"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	commonUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/handler/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

const (
	MaxWindowsCommandLength = 30000

	appendEnvKey = "INCLUDE="
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
	pumpArgs         []string

	// file names
	inputFile        string
	preprocessedFile string
	outputFile       string
	firstIncludeFile string
	pchFile          string
	responseFile     string

	pchFileDesc *dcSDK.FileDesc

	// for /showIncludes
	showinclude bool

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

func (cc *TaskCC) getIncludeExe() (string, error) {
	blog.Debugf("cc: ready get include exe")

	target := "bk-includes"
	if runtime.GOOS == "windows" {
		target = "bk-includes.exe"
	}

	includePath, err := dcUtil.CheckExecutable(target)
	if err != nil {
		// blog.Infof("cc: not found exe file with default path, info: %v", err)

		includePath, err = dcUtil.CheckFileWithCallerPath(target)
		if err != nil {
			blog.Errorf("cc: not found exe file with error: %v", err)
			return includePath, err
		}
	}
	absPath, err := filepath.Abs(includePath)
	if err == nil {
		includePath = absPath
	}
	includePath = dcUtil.QuoteSpacePath(includePath)
	// blog.Infof("cc: got include exe file full path: %s", includePath)

	return includePath, nil
}

func uniqArr(arr []string) []string {
	newarr := make([]string, 0)
	tempMap := make(map[string]bool, len(newarr))
	for _, v := range arr {
		if tempMap[v] == false {
			tempMap[v] = true
			newarr = append(newarr, v)
		}
	}

	return newarr
}

func (cc *TaskCC) analyzeIncludes(f string, workdir string) ([]string, error) {
	data, err := ioutil.ReadFile(f)
	if err != nil {
		return nil, err
	}

	lines := strings.Split(string(data), "\r\n")
	includes := []string{}
	uniqlines := uniqArr(lines)
	blog.Infof("cc: got %d uniq include file from file: %s", len(uniqlines), f)

	for _, l := range uniqlines {
		if !filepath.IsAbs(l) {
			l, _ = filepath.Abs(filepath.Join(workdir, l))
		}
		fstat := dcFile.Stat(l)
		if fstat.Exist() && !fstat.Basic().IsDir() {
			includes = append(includes, l)
		} else {
			blog.Infof("cc: do not deal include file: %s in file:%s for not existed or is dir", l, f)
		}
	}

	return includes, nil
}

// search all include files for this compile command
func (cc *TaskCC) Includes(exepath, responseFile string, args []string, workdir string, forcefresh bool) ([]string, error) {
	pumpdir := dcPump.PumpCacheDir(cc.sandbox.Env)
	if pumpdir == "" {
		pumpdir = dcUtil.GetPumpCacheDir()
	}

	if !dcFile.Stat(pumpdir).Exist() {
		if err := os.MkdirAll(pumpdir, os.ModePerm); err != nil {
			return nil, err
		}
	}

	// TOOD : maybe we should pass responseFile to calc md5, to ensure unique
	outputFile, err := getPumpIncludeFile(pumpdir, "pump_heads", ".txt", args)
	if err != nil {
		blog.Errorf("cc: do includes get output file failed: %v", err)
		return nil, err
	}

	existed, fileSize, _, _ := dcFile.Stat(outputFile).Batch()
	if dcPump.IsPumpCache(cc.sandbox.Env) && !forcefresh && existed && fileSize > 0 {
		return cc.analyzeIncludes(outputFile, workdir)
	}

	err = createFile(outputFile)
	if err != nil {
		return nil, err
	}

	execName, err := cc.getIncludeExe()
	if err != nil {
		return nil, err
	}

	// do not delete to use when cache mode
	// cc.addTmpFile(outputFile)

	// execArgs := []string{"-Xtbs", "--verbose=0", "--driver-mode=cl"}
	// TODO : not sure for linux or mac, only tested in windows now
	execArgs := []string{"-Xtbs", "--verbose=0", "-D__clang__=1"}
	// "-resource-dir", "D:\\NVPACK\\android-sdk-windows\\ndk\\21.4.7075529\\toolchains\\llvm\\prebuilt\\windows-x86_64\\lib64\\clang\\9.0.9"}

	resourcedir, err := getResourceDir(exepath)
	if err == nil && len(resourcedir) > 0 {
		execArgs = append(execArgs, "-resource-dir")
		execArgs = append(execArgs, resourcedir)
	}
	if responseFile != "" {
		execArgs = append(execArgs, "-Xtbs")
		farg := fmt.Sprintf("--cmd_file=%s", responseFile)
		execArgs = append(execArgs, farg)
	} else {
		execArgs = append(execArgs, args[1:]...)
	}

	// TODO : ensure all absolute file path
	sandbox := cc.sandbox.Fork()

	output, err := os.OpenFile(outputFile, os.O_WRONLY, 0666)
	if err != nil {
		blog.Errorf("cc: failed to open output file \"%s\" when pre-processing: %v", outputFile, err)
		return nil, err
	}
	defer func() {
		_ = output.Close()
	}()

	sandbox.Stdout = output
	var errBuf bytes.Buffer
	sandbox.Stderr = &errBuf

	blog.Infof("cc: ready to do Includes %s %s", execName, strings.Join(execArgs, " "))
	if _, err = sandbox.ExecCommand(execName, execArgs...); err != nil {
		blog.Warnf("cc: failed to do Includes %s %s with error:%v", execName, strings.Join(execArgs, " "), err)
		return nil, err
	}

	return cc.analyzeIncludes(outputFile, workdir)
}

func (cc *TaskCC) trypump(command []string) (*dcSDK.BKDistCommand, error) {
	blog.Infof("cc: trypump: %v", command)

	// TODO : !! ensureCompilerRaw changed the command slice, it maybe not we need !!
	tstart := time.Now().Local()
	responseFile, args, showinclude, sourcedependfile, objectfile, pchfile, err := ensureCompilerRaw(command, cc.sandbox.Dir)
	if err != nil {
		blog.Debugf("cc: pre execute ensure compiler failed %v: %v", args, err)
		return nil, err
	} else {
		blog.Infof("cc: after parse command, got responseFile:%s,sourcedepent:%s,objectfile:%s,pchfile:%s",
			responseFile, sourcedependfile, objectfile, pchfile)
	}
	tend := time.Now().Local()
	blog.Debugf("cc: trypump time record: %s for ensureCompilerRaw for rsp file:%s", tend.Sub(tstart), responseFile)
	tstart = tend

	_, err = scanArgs(args)
	if err != nil {
		blog.Debugf("cc: try pump not support, scan args %v: %v", args, err)
		return nil, err
	}

	tend = time.Now().Local()
	blog.Debugf("cc: trypump time record: %s for scanArgs for rsp file:%s", tend.Sub(tstart), responseFile)
	tstart = tend

	cc.responseFile = responseFile
	cc.showinclude = showinclude
	cc.pumpArgs = args

	includes, err := cc.Includes(command[0], responseFile, args, cc.sandbox.Dir, false)

	tend = time.Now().Local()
	blog.Debugf("cc: trypump time record: %s for Includes for rsp file:%s", tend.Sub(tstart), responseFile)
	tstart = tend

	if err == nil {
		blog.Infof("cc: parse command,got total %d includes files", len(includes))

		// add pch file as input
		if pchfile != "" {
			includes = append(includes, pchfile)
		}

		// add response file as input
		if responseFile != "" {
			includes = append(includes, responseFile)
		}

		inputFiles := []dcSDK.FileDesc{}
		// priority := dcSDK.MaxFileDescPriority
		for _, f := range includes {
			existed, fileSize, modifyTime, fileMode := dcFile.Stat(f).Batch()
			if !existed {
				err := fmt.Errorf("input response file %s not existed", f)
				blog.Errorf("%v", err)
				return nil, err
			}
			inputFiles = append(inputFiles, dcSDK.FileDesc{
				FilePath:           f,
				Compresstype:       protocol.CompressLZ4,
				FileSize:           fileSize,
				Lastmodifytime:     modifyTime,
				Md5:                "",
				Filemode:           fileMode,
				Targetrelativepath: filepath.Dir(f),
				NoDuplicated:       true,
				// Priority:           priority,
			})
			// priority++
			// blog.Infof("cc: added include file:%s with modify time %d", f, modifyTime)

			blog.Debugf("cc: added include file:%s for object:%s", f, objectfile)
		}

		results := []string{objectfile}
		// add source depend file as result
		if sourcedependfile != "" {
			results = append(results, sourcedependfile)
		}

		// set env which need append to remote
		envs := []string{}
		for _, v := range cc.sandbox.Env.Source() {
			if strings.HasPrefix(v, appendEnvKey) {
				envs = append(envs, v)
				// set flag we hope append env, not overwrite
				flag := fmt.Sprintf("%s=true", dcEnv.GetEnvKey(env.KeyRemoteEnvAppend))
				envs = append(envs, flag)
				break
			}
		}
		blog.Infof("cc: env which ready sent to remote:[%v]", envs)

		exeName := command[0]
		params := command[1:]
		blog.Infof("cc: parse command,server command:[%s %s],dir[%s]",
			exeName, strings.Join(params, " "), cc.sandbox.Dir)
		return &dcSDK.BKDistCommand{
			Commands: []dcSDK.BKCommand{
				{
					WorkDir:         cc.sandbox.Dir,
					ExePath:         "",
					ExeName:         exeName,
					ExeToolChainKey: dcSDK.GetJsonToolChainKey(command[0]),
					Params:          params,
					Inputfiles:      inputFiles,
					ResultFiles:     results,
					Env:             envs,
				},
			},
			CustomSave: true,
		}, nil
	}

	tend = time.Now().Local()
	blog.Debugf("cc: trypump time record: %s for return dcSDK.BKCommand for rsp file:%s", tend.Sub(tstart), responseFile)

	return nil, err
}

func (cc *TaskCC) preExecute(command []string) (*dcSDK.BKDistCommand, error) {
	blog.Infof("cc: start pre execute for: %v", command)

	// debugRecordFileName(fmt.Sprintf("cc: start pre execute for: %v", command))

	cc.originArgs = command

	// ++ try with pump,only support windows now
	if dcPump.SupportPump(cc.sandbox.Env) {
		req, err := cc.trypump(command)
		if err != nil {
			blog.Warnf("cc: pre execute failed to try pump %v: %v", command, err)
		} else {
			// for debug
			blog.Debugf("cc: after try pump, req: %+v", *req)

			return req, err
		}
	}
	// --

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
