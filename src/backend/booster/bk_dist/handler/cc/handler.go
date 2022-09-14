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
	"os"
	"os/user"
	"path/filepath"
	"strconv"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/manager/analyser"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler"
	commonUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/handler/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/types"
	"github.com/Tencent/bk-ci/src/booster/common/util"
)

// TaskCC 定义了c/c++编译的描述处理对象
type TaskCC struct {
	tag     string
	sandbox *dcSyscall.Sandbox

	ccacheEnable bool
	ccacheStats  types.Ccache

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
	sendFiles        []dcSDK.FileDesc
	preprocessedFile string
	includeHeaders   []string
	outputFile       []string
	firstIncludeFile string
	pchFile          string

	supportDirectives bool

	pchFileDesc *dcSDK.FileDesc
}

// NewTaskCC get a new task-cc handler
func NewTaskCC() (handler.Handler, error) {
	return &TaskCC{
		tag:         util.RandomString(5),
		sandbox:     &dcSyscall.Sandbox{},
		tmpFileList: make([]string, 0, 10),
	}, nil
}

// InitSandbox set sandbox to task-cc
func (cc *TaskCC) InitSandbox(sandbox *dcSyscall.Sandbox) {
	cc.sandbox = sandbox
}

// PreExecuteNeedLock 如果编译本身是预处理, 那么不需要pre-lock, 因为它会在pre-execute中转本地, 不会真正地执行预处理
func (cc *TaskCC) PreExecuteNeedLock(command []string) bool {
	for _, arg := range command {
		if arg == "-E" {
			return false
		}

		if isPreprocessedFile(arg) {
			return false
		}
	}

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
	cc.finalExecute(args, cc.sandbox)
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
	blog.Infof("cc: [%s] start pre execute for: %v", cc.tag, command)

	cc.originArgs = command
	compilerEnsuredArgs, err := ensureCompiler(command)
	if err != nil {
		blog.Warnf("cc: [%s] pre execute ensure compiler %v: %v", cc.tag, command, err)
		return nil, err
	}
	args, err := expandOptions(cc.sandbox, compilerEnsuredArgs)
	if err != nil {
		blog.Warnf("cc: [%s] pre execute expand options %v: %v", cc.tag, compilerEnsuredArgs, err)
		return nil, err
	}
	cc.ensuredArgs = args

	if err = cc.preBuild(args); err != nil {
		blog.Warnf("cc: [%s] pre execute pre-build %v: %v", cc.tag, args, err)
		return nil, err
	}

	// generate the input files for pre-process file
	if cc.preprocessedFile != "" {
		existed, fileSize, modifyTime, fileMode := dcFile.Stat(cc.preprocessedFile).Batch()
		if !existed {
			err := fmt.Errorf("result file %s not existed", cc.preprocessedFile)
			blog.Warnf("cc: [%s] %v", cc.tag, err)
			return nil, err
		}

		cc.sendFiles = append(cc.sendFiles, dcSDK.FileDesc{
			FilePath:       cc.preprocessedFile,
			Compresstype:   protocol.CompressLZ4,
			FileSize:       fileSize,
			Lastmodifytime: modifyTime,
			Md5:            "",
			Filemode:       fileMode,
		})
	}

	// if there is a pch file, add it into the inputFiles, it should be also sent to remote
	if cc.pchFileDesc != nil {
		cc.sendFiles = append(cc.sendFiles, *cc.pchFileDesc)
	}

	// debugRecordFileName(fmt.Sprintf("cc: success done pre execute for: %v", command))
	blog.Infof("cc: [%s] success done pre execute and going to execute compile: %v", cc.tag, cc.serverSideArgs)

	for i, item := range cc.outputFile {
		if !filepath.IsAbs(item) {
			cc.outputFile[i] = filepath.Join(cc.sandbox.Dir, item)
		}
	}
	blog.Infof("cc: [%s] expect result files: %v", cc.tag, cc.outputFile)

	return &dcSDK.BKDistCommand{
		Commands: []dcSDK.BKCommand{
			{
				WorkDir:         cc.sandbox.Dir,
				ExePath:         "",
				ExeName:         cc.serverSideArgs[0],
				ExeToolChainKey: dcSDK.GetJsonToolChainKey(command[0]),
				Params:          cc.serverSideArgs[1:],
				Inputfiles:      cc.sendFiles,
				ResultFiles:     cc.outputFile,
			},
		},
	}, nil
}

func (cc *TaskCC) postExecute(r *dcSDK.BKDistResult) error {
	blog.Infof("cc: [%s] start post execute", cc.tag)
	if r == nil || len(r.Results) == 0 {
		return ErrorInvalidParam
	}

	if r.Results[0].RetCode == 0 {
		blog.Infof("cc: [%s] success done post execute", cc.tag)
		return nil
	}

	// write error message into
	if cc.saveTemp() && len(r.Results[0].ErrorMessage) > 0 {
		// make the tmp file for storing the stderr from server compiler.
		stderrFile, _, err := makeTmpFile(commonUtil.GetHandlerTmpDir(cc.sandbox),
			"cc", "server_stderr.txt")
		if err != nil {
			blog.Warnf("cc: [%s] make tmp file for stderr from server failed: %v", cc.tag, err)
		} else {
			if f, err := os.OpenFile(stderrFile, os.O_RDWR, 0644); err == nil {
				_, _ = f.Write(r.Results[0].ErrorMessage)
				_ = f.Close()
				blog.Debugf("cc: [%s] save error message to %s", cc.tag, stderrFile)
			}
		}
	}

	return fmt.Errorf("cc: [%s] failed to remote execute, retcode %d, error message:%s, output message:%s",
		cc.tag,
		r.Results[0].RetCode,
		r.Results[0].ErrorMessage,
		r.Results[0].OutputMessage)
}

func (cc *TaskCC) ensureOwner(fdl []string) {
	if commonUtil.GetHandlerEnv(cc.sandbox, envEnsureFileOwner) == "" {
		return
	}

	u := cc.sandbox.User
	if u.Username == "" {
		blog.Warnf("cc: [%s] change file owner get sandbox user failed: user name empty", cc.tag)
		return
	}

	uc, err := user.Current()
	if err != nil {
		blog.Warnf("cc: [%s] change file owner get current user failed: %v", cc.tag, err)
		return
	}

	if u.Uid == uc.Uid {
		return
	}

	gid, _ := strconv.Atoi(u.Gid)
	uid, _ := strconv.Atoi(u.Uid)
	for _, fd := range fdl {
		if fd == "" {
			continue
		}
		f := cc.sandbox.GetAbsPath(fd)
		if err := os.Chown(f, uid, gid); err != nil {
			blog.Warnf("cc: [%s] change file owner(%s) failed: %v", cc.tag, f, err)
		}
		blog.Infof("cc: [%s] success to change file owner(%s) from %s to %s", cc.tag, f, uc.Username, u.Username)
	}
}

func (cc *TaskCC) finalExecute(args []string, sandbox *dcSyscall.Sandbox) {
	cc.ensureOwner(getOutputFile(args, sandbox))

	if !cc.saveTemp() {
		cc.cleanTmpFile()
	}
}

func (cc *TaskCC) saveTemp() bool {
	return commonUtil.GetHandlerEnv(cc.sandbox, envSaveTempFile) != ""
}

func (cc *TaskCC) isPump() bool {
	return cc.sandbox.Env.GetEnv(env.KeyExecutorPump) != ""
}

func (cc *TaskCC) isPumpDisableMacro() bool {
	return cc.sandbox.Env.GetEnv(env.KeyExecutorPumpDisableMacro) != ""
}

func (cc *TaskCC) isPumpCheck() bool {
	return cc.sandbox.Env.GetEnv(env.KeyExecutorPumpCheck) != ""
}

func (cc *TaskCC) isPumpIncludeSystemHeader() bool {
	return cc.sandbox.Env.GetEnv(env.KeyExecutorPumpIncludeSysHeader) != ""
}

func (cc *TaskCC) isUseDirectives() bool {
	return cc.sandbox.Env.GetEnv(env.KeyExecutorSupportDirectives) != ""
}

func (cc *TaskCC) getWorkID() string {
	return cc.sandbox.Env.GetEnv(env.KeyExecutorControllerWorkID)
}

func (cc *TaskCC) preBuild(args []string) error {
	// debugRecordFileName(fmt.Sprintf("cc: preBuild in..."))

	blog.Debugf("cc: [%s] pre-build begin got args: %v", cc.tag, args)

	// debugRecordFileName(fmt.Sprintf("cc: pre-build ready expandPreprocessorOptions"))

	var err error
	// expand args to make the following process easier.
	if cc.expandArgs, err = expandPreprocessorOptions(args); err != nil {
		blog.Warnf("cc: [%s] pre-build expand pre-process options %v: %v", cc.tag, args, err)
		return err
	}

	// debugRecordFileName(fmt.Sprintf("cc: pre-build ready scanArgs"))

	// scan the args, check if it can be compiled remotely, wrap some un-used options,
	// and get the real input&output file.
	scannedData, err := scanArgs(cc.expandArgs, cc.sandbox)
	if err != nil {
		// blog.Warnf("cc: [%s] pre-build not support, scan args %v: %v", cc.tag, cc.expandArgs, err)
		return err
	}
	cc.scannedArgs = scannedData.args
	cc.firstIncludeFile = getFirstIncludeFile(scannedData.args)
	cc.inputFile = scannedData.inputFile
	cc.outputFile = append([]string{scannedData.outputFile}, scannedData.additionOutputFile...)

	// handle the cross-compile issues.
	targetArgs := cc.scannedArgs
	if commonUtil.GetHandlerEnv(cc.sandbox, envNoRewriteCross) != "1" {
		var targetArgsGeneric, targetArgsACT, targetArgsGRF []string

		if targetArgsGeneric, err = rewriteGenericCompiler(targetArgs); err != nil {
			blog.Warnf("cc: [%s] pre-build rewrite generic compiler %v: %v",
				cc.tag, targetArgs, err)
			return err
		}

		if targetArgsACT, err = addClangTarget(targetArgsGeneric); err != nil {
			blog.Warnf("cc: [%s] pre-build add clang target %v: %v",
				cc.tag, targetArgsGeneric, err)
			return err
		}

		if targetArgsGRF, err = gccRewriteFqn(targetArgsACT); err != nil {
			blog.Warnf("cc: [%s] pre-build gcc rewrite fqn %v: %v", cc.tag, targetArgsACT, err)
			return err
		}

		targetArgs = targetArgsGRF
	}
	cc.rewriteCrossArgs = targetArgs

	// handle the pch options
	finalArgs := cc.scanPchFile(targetArgs)

	cc.serverSideArgs = finalArgs
	if checkFiles, ok, pumpErr := cc.tryPump(); !ok {
		// do the pre-process, store result in the file.
		if cc.preprocessedFile, cc.includeHeaders, err = cc.doPreProcess(finalArgs, cc.inputFile); err != nil {
			blog.Warnf("cc: [%s] pre-build do pre-process %v: %v", cc.tag, finalArgs, err)
			return err
		}

		// strip the args and get the server side args.
		serverSideArgs := stripLocalArgs(finalArgs)
		if cc.supportDirectives {
			serverSideArgs = append(serverSideArgs, "-fdirectives-only")
		}

		// replace the input file into preprocessedFile, for the next server side process.
		for index := range serverSideArgs {
			if serverSideArgs[index] == cc.inputFile {
				serverSideArgs[index] = cc.preprocessedFile
				break
			}
		}
		cc.serverSideArgs = serverSideArgs

		if cc.isPumpCheck() && pumpErr == nil {
			found := false

			for _, hdr := range cc.includeHeaders {
				if !filepath.IsAbs(hdr) {
					hdr = filepath.Join(cc.sandbox.Dir, hdr)
				}
				hdr, _ = filepath.EvalSymlinks(hdr)

				found = false
				for _, ph := range checkFiles {
					if hdr == ph {
						found = true
						break
					}
				}

				if !found {
					blog.Warnf("cc: [%s] pump check compiling %s lack of header %s", cc.tag, cc.inputFile, hdr)
					blog.Warnf("cc: [%s] pump files: \n%s", strings.Join(checkFiles, "\n"))
					break
				}
			}

			if found {
				blog.Infof("cc: [%s] pump check compiling %s has complete headers", cc.tag, cc.inputFile)
			}
		}
	} else {
		cc.outputFile = append(cc.outputFile, scannedData.mfOutputFile...)
	}

	blog.Debugf("cc: [%s] pre-build success for enter args: %v", cc.tag, args)
	return nil
}

func (cc *TaskCC) tryPump() ([]string, bool, error) {
	if !cc.isPump() && !cc.isPumpCheck() {
		return nil, false, nil
	}

	// My Analyser
	f, r, _ := commonUtil.GetV1Manager().GetPumpCache(cc.getWorkID())
	anal := analyser.NewWithCache(f, r)
	files, err := anal.Do(cc.sandbox.Dir, cc.serverSideArgs, cc.sandbox.Env,
		cc.isPumpIncludeSystemHeader(), cc.isPumpDisableMacro())
	if err != nil {
		blog.Warnf("cc: [%s] get pump info for %s : %v", cc.tag, cc.inputFile, err)
		return nil, false, err
	}

	if cc.isPumpCheck() {
		fl := make([]string, 0, len(files.DependentFile))
		for _, f := range files.DependentFile {
			fl = append(fl, f.FilePath)
		}
		return fl, false, nil
	}

	priority := dcSDK.MaxFileDescPriority
	for _, f := range files.DependentSymlink {
		cc.sendFiles = append(cc.sendFiles, dcSDK.FileDesc{
			FilePath:           f.FilePath,
			Compresstype:       protocol.CompressLZ4,
			FileSize:           f.FileSize,
			Lastmodifytime:     f.Lastmodifytime,
			Md5:                "",
			Filemode:           f.Filemode,
			Targetrelativepath: f.Targetrelativepath,
			LinkTarget:         f.LinkTarget,
			NoDuplicated:       true,
			Priority:           priority,
		})
		priority++

		blog.Debugf("cc: [%s] get pump link, from %s to %s", cc.tag, f.FilePath, f.LinkTarget)
	}

	for _, f := range files.DependentFile {
		cc.sendFiles = append(cc.sendFiles, dcSDK.FileDesc{
			FilePath:           f.FilePath,
			Compresstype:       protocol.CompressLZ4,
			FileSize:           f.FileSize,
			Lastmodifytime:     f.Lastmodifytime,
			Md5:                "",
			Filemode:           f.Filemode,
			Targetrelativepath: f.Targetrelativepath,
			NoDuplicated:       true,
			Priority:           priority,
		})

		if len(cc.serverSideArgs) > 0 {
			cc.serverSideArgs[0] = filepath.Base(cc.serverSideArgs[0])
		}
		blog.Debugf("cc: [%s] get pump file: %s", cc.tag, f.FilePath)
	}

	blog.Infof("cc: [%s] pump mode to compile", cc.tag)
	return nil, true, nil
}

func (cc *TaskCC) addTmpFile(filename string) {
	cc.tmpFileList = append(cc.tmpFileList, filename)
}

func (cc *TaskCC) cleanTmpFile() {
	for _, filename := range cc.tmpFileList {
		if err := os.RemoveAll(filename); err != nil {
			blog.Warnf("cc: [%s] clean tmp file %s failed: %v", cc.tag, filename, err)
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

// doPreProcess also receive a inspectHeader, if so, then add -H in the end of pre-process command,
// and get the stderr output and return in second param.
func (cc *TaskCC) doPreProcess(args []string, inputFile string) (string, []string, error) {

	// debugRecordFileName(fmt.Sprintf("cc: doPreProcess in..."))

	if isPreprocessedFile(inputFile) {
		blog.Infof("cc: [%s] input \"%s\" is already preprocessed", cc.tag, inputFile)

		// input file already preprocessed
		return inputFile, nil, nil
	}

	preprocessedF, err := outputFromSource(cc.inputFile, getPreprocessedExt(inputFile))
	if err != nil {
		blog.Warnf("cc: [%s] get preprocessed file name failed: %v", cc.tag, err)
		return "", nil, err
	}
	blog.Infof("cc: [%s] get preprocessed file: %s", cc.tag, preprocessedF)

	outputFile, tmpFile, err := makeTmpFile(commonUtil.GetHandlerTmpDir(cc.sandbox), "cc", preprocessedF)
	cc.addTmpFile(tmpFile)
	if err != nil {
		blog.Warnf("cc: [%s] do pre-process get output file failed: %v", cc.tag, err)
		return "", nil, err
	}

	// debugRecordFileName(fmt.Sprintf("cc: doPreProcess ready setActionOptionE"))

	// ++ for debug
	// info := fmt.Sprintf("%s %s %s\n", strings.Join(args, " "), inputFile, outputFile)
	// debugRecordFileName(info)
	// --

	// We strip the -o option and allow cpp to write to stdout, which is
	// caught in a file.  Sun cc doesn't understand -E -o, and gcc screws up
	// -MD -E -o.
	//
	// There is still a problem here with -MD -E -o, gcc writes dependencies
	// to a file determined by the source filename.  We could fix it by
	// generating a -MF option, but that would break compilation with older
	// versions of gcc.  This is only a problem for people who have the source
	// and objects in different directories, and who don't specify -MF.  They
	// can fix it by specifying -MF.
	// newArgs1 : arg with -fdirectives-only
	// newArgs2 : arg without -fdirectives-only
	newArgs1, newArgs2, err := setActionOptionE(stripDashO(args), cc.isUseDirectives(), cc.isPumpCheck())
	if err != nil {
		blog.Warnf("cc: [%s] set action option -E failed: %v", cc.tag, err)
		return "", nil, err
	}

	output, err := os.OpenFile(outputFile, os.O_WRONLY, 0666)
	if err != nil {
		blog.Errorf("cc: [%s] failed to open output file \"%s\" when pre-processing: %v",
			cc.tag, outputFile, err)
		return "", nil, err
	}
	defer func() {
		_ = output.Close()
	}()
	var errBuf bytes.Buffer
	sandbox := cc.sandbox.Fork()
	sandbox.Stdout = output
	sandbox.Stderr = &errBuf

	// try preprocess with "-E -fdirectives-only" firstly
	if newArgs1 != nil {
		cc.preProcessArgs = newArgs1

		blog.Debugf("cc: [%s] going to execute pre-process with -fdirectives-only: %s",
			strings.Join(newArgs1, " "))
		if _, err = sandbox.ExecCommand(newArgs1[0], newArgs1[1:]...); err != nil {
			blog.Warnf("cc: [%s] do pre-process with -fdirectives-only: %s, err: %v, %s",
				cc.tag, strings.Join(newArgs1, " "), err, errBuf.String())
			_ = output.Truncate(0)
		} else {
			cc.supportDirectives = true
			blog.Infof("cc: [%s] success to execute pre-process with -fdirectives-only and get %s: %s",
				cc.tag, outputFile, strings.Join(newArgs1, " "))
			return outputFile, parseInspectHeader(errBuf.String()), nil
		}
	}

	// try preprocess with "-E"
	cc.preProcessArgs = newArgs2

	blog.Debugf("cc: [%s] going to execute pre-process: %s", strings.Join(newArgs2, " "))
	if _, err = sandbox.ExecCommand(newArgs2[0], newArgs2[1:]...); err != nil {
		blog.Warnf("cc: [%s] do pre-process %v: %v, %s", cc.tag, newArgs2, err, errBuf.String())
		return "", nil, err
	}
	blog.Infof("cc: [%s] success to execute pre-process and get %s: %s",
		cc.tag, outputFile, strings.Join(newArgs2, " "))

	return outputFile, parseInspectHeader(errBuf.String()), nil
}

// try to get pch file desc and the args according to firstIncludeFile
// if pch is valid, there must be a option -include xx.h(xx.hpp)
// and must be the first seen -include option(if there are multiple -include)
func (cc *TaskCC) scanPchFile(args []string) []string {
	if cc.firstIncludeFile == "" {
		return args
	}

	filename := fmt.Sprintf("%s.gch", cc.firstIncludeFile)

	existed, fileSize, modifyTime, fileMode := dcFile.Stat(filename).Batch()

	if !existed {
		blog.Debugf("cc: [%s] try to get pch file for %s but %s is not exist",
			cc.tag, cc.firstIncludeFile, filename)

		filename = cc.sandbox.GetAbsPath(filename)
		existed, fileSize, modifyTime, _ = dcFile.Stat(filename).Batch()
		if !existed {
			return args
		}
		blog.Infof("cc: find gch file in relative path(%s), filesize(%v), modifytime:(%v)", filename, fileSize, modifyTime)
	}
	cc.pchFile = filename

	blog.Debugf("cc: [%s] success to find pch file %s for %s", cc.tag, filename, cc.firstIncludeFile)

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

func (cc *TaskCC) statisticsCCache() (*types.Ccache, error) {
	sandbox := cc.sandbox.Fork()
	var buf bytes.Buffer
	sandbox.Stdout = &buf
	if _, err := sandbox.ExecScripts("ccache -s"); err != nil {
		return nil, err
	}

	ccache := &types.Ccache{}
	arr := strings.Split(buf.String(), "\n")
	for _, str := range arr {
		str = strings.TrimSpace(str)
		if str == "" {
			continue
		}
		kv := strings.Split(str, "  ")
		if len(kv) < 2 {
			continue
		}
		key := strings.TrimSpace(kv[0])
		value := strings.TrimSpace(kv[len(kv)-1])
		switch key {
		case "cache directory":
			ccache.CacheDir = value
		case "primary config":
			ccache.PrimaryConfig = value
		case "secondary config":
			ccache.SecondaryConfig = value
		case "cache hit (direct)":
			i, err := strconv.Atoi(value)
			if err != nil {
				return nil, err
			}
			ccache.DirectHit = i
		case "cache hit (preprocessed)":
			i, err := strconv.Atoi(value)
			if err != nil {
				return nil, err
			}
			ccache.PreprocessedHit = i
		case "cache miss":
			i, err := strconv.Atoi(value)
			if err != nil {
				return nil, err
			}
			ccache.CacheMiss = i
		case "files in cache":
			i, err := strconv.Atoi(value)
			if err != nil {
				return nil, err
			}
			ccache.FilesInCache = i
		case "cache size":
			ccache.CacheSize = value
		case "max cache size":
			ccache.MaxCacheSize = value
		}
	}

	return ccache, nil
}
