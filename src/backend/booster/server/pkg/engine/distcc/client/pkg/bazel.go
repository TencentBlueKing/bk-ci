/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pkg

import (
	"crypto/md5"
	"encoding/hex"
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/types"

	commandCli "github.com/urfave/cli"
)

var (
	workSpacePath    = "."
	bazelRcFilePath  = "bk_bazelrc"
	bazelCommandPath = "bazel"

	templatetoolchainDir = "@BK_TOOLCHAIN_DIR"
	templateCc           = "@BK_CC"
	templateCxx          = "@BK_CXX"
	templateJobs         = "@BK_JOBS"
	templateDistccDir    = "@BK_DISTCC_DIR"

	tempBazelRcFileName  = ""
	temptoolchainDir     = ""
	temptoolchainDirBase = ""
	projectDistccDir     = ""
	toolchainDir         = "bkdistcctoolchain"

	ccShellFile  = "wrapper_cc.sh"
	cxxShellFile = "wrapper_cxx.sh"

	timestamp     = time.Now().UnixNano()
	tmpFileSuffix = "_bktmp"
)

// BazelProcess do the bazel build process:
// 1. apply resources.
// 2. keep heartbeat.
// 3. execute the bazel command when resources are ready.
// 4. release the resources when cancel or finish.
func BazelProcess(c *commandCli.Context) error {
	initProcess(c)
	return bazelProcess(c)
}

func bazelProcess(c *commandCli.Context) error {
	// get args
	hassetrcpath := false
	if c.IsSet(FlagBazelRcPath) {
		bazelRcFilePath = c.String(FlagBazelRcPath)
		fmt.Printf("bazel rc template path is set to [%s] by user\n", bazelRcFilePath)
		hassetrcpath = true
	}

	hassetworkspacepath := false
	if c.IsSet(FlagWorkspacePath) {
		workSpacePath = c.String(FlagWorkspacePath)
		fmt.Printf("bazel workspace path is set to [%s] by user\n", bazelRcFilePath)
		hassetworkspacepath = true
		if !hassetrcpath {
			bazelRcFilePath = path.Join(workSpacePath, bazelRcFilePath)
		}
		toolchainDir = path.Join(workSpacePath, toolchainDir)
	} else {
		toolchainDir = "./" + toolchainDir
	}

	if !hassetrcpath && !hassetworkspacepath {
		bazelRcFilePath = "./" + bazelRcFilePath
	}

	if c.IsSet(FlagCommandPath) {
		bazelCommandPath = c.String(FlagCommandPath)
		fmt.Printf("bazel command path is set to [%s] by user\n", bazelCommandPath)
	}

	projectID := c.String(FlagProjectID)
	if projectID == "" {
		fmt.Printf("WARNING: %s must be specified\n", FlagProjectID)
	}

	buildID := c.String(FlagBuildID)
	if buildID == "" {
		fmt.Println("buildID is not specified, will be ignored")
	}

	if c.IsSet(FlagCCacheEnabled) {
		tmp := c.String(FlagCCacheEnabled) == "true"
		cacheEnabled = &tmp
		hasSetCache = true
	}

	gccVersion := c.String(FlagGccVersion)
	args := c.String(FlagArgs)

	// ++ by tomtian 2020-02-17, to support hook
	hookMode = c.Bool(FlagHook)
	if !hookMode {
		// create the bkdistcc host dir
		projectDistccDir = path.Join(RunDir, ".bkdistcc")
		if err := os.MkdirAll(projectDistccDir, os.ModePerm); err != nil {
			fmt.Printf("failed to create bazel distcc hosts dir(%s), %v", projectDistccDir, err)
			return err
		}

		defer bazelRemoveAllTmpConfigFile()
		// create tmp config file first
		if err := bazelCreateTmpConfigFile(projectID); err != nil {
			return err
		}
	}
	// --

	ccShellFile = path.Join(temptoolchainDir, ccShellFile)
	cxxShellFile = path.Join(temptoolchainDir, cxxShellFile)

	// apply task
	task, ok, err := applyDistCCResources(types.DistccServerSets{
		ProjectId:     projectID,
		BuildId:       buildID,
		Params:        args,
		GccVersion:    gccVersion,
		CCacheEnabled: cacheEnabled,
		Command:       bazelCommandPath,
		CommandType:   types.CommandBazel,
		ExtraVars: types.ExtraVars{
			BazelRC: tempBazelRcFileName,
			MaxJobs: MaxJobs,
		},
	})
	// only server communicate ok and error then return error, if communicate failed then compile locally.
	if ok && err != nil {
		fmt.Printf("request compiler resource failed: %v, degraded to local compiling.\n", err)
		return bazelLocalCompiling(args, "")
	} else if !ok {
		fmt.Printf("failed to connect to server, degraded to local compiling.\n")
		return bazelLocalCompiling(args, "")
	}
	fmt.Printf("success to apply new task: %s\n", task.TaskID)
	fmt.Printf("status(%s): %s\n", task.Status, task.Message)

	var data []byte
	_ = codec.EncJSON(task, &data)
	DebugPrintf("request info: %s\n", string(data))

	go loopHeartBeat(task.TaskID)
	go handlerSysSignal(task.TaskID)

	var waitedsecs = 0
	var waittimeout = false
	for {
		time.Sleep(time.Duration(SleepSecsPerWait) * time.Second)
		waitedsecs += SleepSecsPerWait

		info, err := inspectDistCCServers(task.TaskID)
		if err != nil {
			fmt.Printf("inspect distcc servers error: %v\n", err)
			if waitedsecs >= TotalWaitServerSecs {
				fmt.Printf("has waited server for [%d]seconds,quit to local compile\n", waitedsecs)
				waittimeout = true
				break
			}
			continue
		}

		fmt.Printf("status(%s): %s\n", info.Status, info.Message)
		if info.Status == types.ServerStatusRunning || info.Status == types.ServerStatusFailed ||
			info.Status == types.ServerStatusFinish {
			task = info
			break
		}
	}

	if task.Status == types.ServerStatusFailed ||
		task.Status == types.ServerStatusFinish ||
		waittimeout {
		fmt.Printf("init distcc server error: %s\n", task.Message)
		return bazelLocalCompiling(args, task.CCCompiler)
	}

	_ = codec.EncJSON(task, &data)
	DebugPrintf("distcc server: %s\n", string(data))
	var clientInfo *types.DistccClientInfo

	// transform gcc version from some mask tag
	task.GccVersion = TransformGccVersion(task.GccVersion)

	if !strings.Contains(task.GccVersion, string(Compiler)) {
		err = fmt.Errorf("settings BazelCompiler version: %s, "+
			"seems like not fit for this tool which provide BazelCompiler: %s", task.GccVersion, Compiler)
		fmt.Printf("%v\n", err)
		clientInfo = &types.DistccClientInfo{
			TaskID:  task.TaskID,
			Status:  types.ClientStatusFailed,
			Message: err.Error(),
		}
		distCCDone(clientInfo)
		os.Exit(1)
	}

	// check local BazelCompiler version and remote settings
	switch Compiler {
	case CompilerGcc:
		err = checkLocalGccVersion(strings.TrimPrefix(task.GccVersion, "gcc"))
	case CompilerClang:
		err = checkLocalClangVersion(strings.TrimPrefix(task.GccVersion, "clang"))
	}
	if err != nil {
		clientInfo = &types.DistccClientInfo{
			TaskID:  task.TaskID,
			Status:  types.ClientStatusFailed,
			Message: err.Error(),
		}
		distCCDone(clientInfo)
		os.Exit(1)
	}

	if !strings.Contains(task.DistccHosts, ":") {
		return bazelLocalCompiling(args, task.CCCompiler)
	}

	// exec the limit progress
	task.DistccHosts = setLimit(task.DistccHosts, SlotsLimit)
	task.DistccHosts = setLocalSlots(task.DistccHosts, LocalLimit, LocalLimitCpp)

	if err = writeDistCCHost(task.DistccHosts); err != nil {
		return err
	}

	if task.CCacheEnabled {
		cmd := exec.Command("/bin/bash", "-c", "ccache -z")
		err = cmd.Run()
		if err != nil {
			fmt.Printf("exec command ccache -z error: %v\n", err)
		}
	}

	// ++ by tomtian 2020-02-17, to support hook
	if hookMode {
		err = hookBazelCompiling(task.Cmds, task.Envs, task.CCCompiler, strconv.Itoa(int(task.JobServer)))
	} else {
		// render tmp config files according to task info
		if err := bazelRenderTmpConfigFile(task); err != nil {
			fmt.Printf("failed to render bazel config files: %v\n", err)
			return err
		}

		if err := bazelCompareTmpConfigFileAndReplace(); err != nil {
			fmt.Printf("failed to compare and replace bazel tmp config files: %v\n", err)
			return err
		}

		fmt.Printf("exec command: %s\n", task.Cmds)
		cmd := exec.Command("/bin/bash", "-c", task.Cmds)
		dir, _ := os.Getwd()
		cmd.Dir = dir
		cmd.Stdout = os.Stdout
		cmd.Stderr = os.Stderr
		err = cmd.Run()
	}
	// --

	saveExitCode(err)
	if err != nil {
		fmt.Printf("exec command: %s, failed: %v\n", task.Cmds, err)
		clientInfo = &types.DistccClientInfo{
			TaskID:  task.TaskID,
			Status:  types.ClientStatusFailed,
			Message: err.Error(),
		}
		err = ErrCompile
	} else {
		clientInfo = &types.DistccClientInfo{
			TaskID: task.TaskID,
			Status: types.ClientStatusSuccess,
		}
	}

	if task.CCacheEnabled {
		cCache, cacheErr := statisticsCCache()
		if cacheErr != nil {
			fmt.Printf("get ccache stats failed: %v\n", cacheErr)
		} else {
			clientInfo.Ccache = cCache
		}
	}

	distCCDone(clientInfo)

	fmt.Printf("\n* %s done *\n", ClientBazel.Name())
	return err
}

func bazelGenTempRcName(projectID string) error {
	tempBazelRcFileName = path.Join(filepath.Dir(bazelRcFilePath), "bk_bazelrc_"+projectID)
	fmt.Printf("set temp bazel rc file:[%s]\n", tempBazelRcFileName)
	return nil
}

func bazelGentemptoolchainDir(projectID string) error {
	temptoolchainDirBase = "bkdistcctoolchain_" + projectID
	temptoolchainDir = path.Join(workSpacePath, temptoolchainDirBase)
	fmt.Printf("set temp bazel toolchain dir:[%s]\n", temptoolchainDir)
	return nil
}

func bazelCopyFile(srcfile string, dstfile string) (written int64, err error) {
	//fmt.Printf("copy file from [%s] to [%s]\n", srcfile, dstfile)
	src, err := os.Open(srcfile)
	if err != nil {
		return 0, err
	}
	defer func() {
		_ = src.Close()
	}()

	fi, err := os.Stat(srcfile)
	if err != nil {
		return 0, err
	}
	dst, err := os.OpenFile(dstfile, os.O_WRONLY|os.O_CREATE, fi.Mode())
	_ = dst.Truncate(0)
	if err != nil {
		return 0, err
	}
	defer func() {
		_ = dst.Close()
	}()
	return io.Copy(dst, src)
}

func bazelCopyDir(srcdir string, destdir string) error {
	if err := os.MkdirAll(destdir, os.ModePerm); err != nil {
		err = fmt.Errorf("failed to create temp toolchain dir for error[%v], check the system", err)
		return err
	}

	//fmt.Printf("copy dir from [%s] to [%s]\n", srcdir, destdir)
	if srcInfo, err := os.Stat(srcdir); err != nil {
		err = fmt.Errorf(err.Error())
		return err
	} else {
		if !srcInfo.IsDir() {
			err = fmt.Errorf("[%s] is not valid dir, check it", srcdir)
			return err
		}
	}
	if destInfo, err := os.Stat(destdir); err != nil {
		fmt.Println(err.Error())
		return err
	} else {
		if !destInfo.IsDir() {
			err = fmt.Errorf("[%s] is not valid dir, check it", destdir)
			return err
		}
	}

	files, err := ioutil.ReadDir(srcdir)
	if err != nil {
		return fmt.Errorf("failed to read toolchain dir[%s] for error[%v], check the system", srcdir, err)
	}

	for _, f := range files {
		if !f.IsDir() {
			srcPath := filepath.Join(srcdir, f.Name())
			dstPath := bazelGenerateTmpFileName(filepath.Join(destdir, f.Name()))
			//fmt.Printf("srcpath[%s] dstPath[%s] srcdir[%s] destdir[%s]\n", srcPath, dstPath, srcdir, destdir)
			_, err := bazelCopyFile(srcPath, dstPath)
			if err != nil {
				fmt.Printf(err.Error())
				return err
			}
		}
	}

	return nil
}

func bazelReplaceFileWith(filepath string, oldstring string, newstring string) error {
	// read content
	buf, err := ioutil.ReadFile(filepath)
	if err != nil {
		return err
	}
	content := string(buf)

	// replace content
	newContent := strings.Replace(content, oldstring, newstring, -1)

	// write back
	return ioutil.WriteFile(filepath, []byte(newContent), 0)
}

func bazelLocalCompiling(param string, ccCompilerFromSvr string) error {
	if NoLocal {
		return ErrNoLocal
	}

	// ++ by tomtian 2020-02-21, to support hook
	if hookMode {
		return bazelLocalCompilingHook(param, ccCompilerFromSvr)
	}
	// --

	if err := writeDistCCHost(""); err != nil {
		return err
	}

	if err := bazelRenderTmpConfigFileWhenLocal(); err != nil {
		fmt.Printf("failed to render file for local compiling\n")
		return err
	}

	if err := bazelCompareTmpConfigFileAndReplace(); err != nil {
		fmt.Printf("failed to compare and replace bazel tmp config files: %v\n", err)
		return err
	}

	var command string
	command = fmt.Sprintf("%s --bazelrc=%s --noworkspace_rc %s", bazelCommandPath, tempBazelRcFileName, param)
	fmt.Printf("exec command: %s\n", command)
	cmd := exec.Command("/bin/bash", "-c", command)
	dir, _ := os.Getwd()
	cmd.Dir = dir
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	err := cmd.Run()
	saveExitCode(err)
	if err != nil {
		return ErrCompile
	}
	return nil
}

func bazelLocalCompilingHook(param string, ccCompilerFromSvr string) error {
	var command string
	command = fmt.Sprintf("%s %s", bazelCommandPath, param)
	fmt.Printf("exec command: %s\n", command)

	var commandCC string
	// 如果是因为禁止了distcc转本地，可以优先用server的命令
	if ccCompilerFromSvr != "" {
		commandCC = ccCompilerFromSvr
	} else {
		switch Compiler {
		case CompilerGcc:
			commandCC = "gcc"
		case CompilerClang:
			commandCC = "clang"
		}

		if hasSetCache && *cacheEnabled {
			commandCC = "ccache " + commandCC
		}
	}
	envs := map[string]string{}
	err := hookBazelCompiling(command, envs, commandCC, "0")
	saveExitCode(err)
	if err != nil {
		return ErrCompile
	}
	return nil
}

func bazelCreateTmpConfigFile(projectID string) error {
	// generate temp toolchain dir, copy file, and rm when exit
	if err := bazelGentemptoolchainDir(projectID); err != nil {
		err = fmt.Errorf("failed to generate temp toolchain dir for error[%v], check the system", err)
		return err
	}

	if err := bazelCopyDir(toolchainDir, temptoolchainDir); err != nil {
		err = fmt.Errorf("failed to copy files for error[%v], check the system", err)
		return err
	} else {
		fmt.Printf("succeed to copy toolchain dir[%s]\n", temptoolchainDir)
	}

	// generate temp bazel rc file and rm when exit
	if err := bazelGenTempRcName(projectID); err != nil {
		err = fmt.Errorf("failed to generate temp bazel rc file for error[%v], check the system", err)
		return err
	}
	_, err := bazelCopyFile(bazelRcFilePath, bazelGenerateTmpFileName(tempBazelRcFileName))
	if err != nil {
		err = fmt.Errorf("failed to copy bazel for error[%v], check the system", err)
		return err
	} else {
		fmt.Printf("succeed to copy bazelrc[%s]\n", tempBazelRcFileName)
	}

	return nil
}

func bazelRenderTmpConfigFile(task *types.DistccServerInfo) error {
	fileReplaceSettings := [][]string{
		{bazelGenerateTmpFileName(tempBazelRcFileName), templateDistccDir, projectDistccDir},
		{bazelGenerateTmpFileName(tempBazelRcFileName), templatetoolchainDir, temptoolchainDirBase},
		{bazelGenerateTmpFileName(tempBazelRcFileName), templateJobs, strconv.Itoa(int(task.JobServer))},
		{bazelGenerateTmpFileName(ccShellFile), templateCc, task.CCCompiler},
		{bazelGenerateTmpFileName(cxxShellFile), templateCxx, task.CXXCompiler},
	}

	for _, block := range fileReplaceSettings {
		if len(block) < 3 {
			fmt.Printf("failed to render rule: %v, will be skipped\n", block)
			continue
		}
		if err := bazelReplaceFileWith(block[0], block[1], block[2]); err != nil {
			err = fmt.Errorf("failed to render config file(%s) for error[%v], check the system", block[0], err)
			return err
		}
	}

	DebugPrintf("success to render all tmp config file\n")
	return nil
}

func bazelRenderTmpConfigFileWhenLocal() error {
	var commandCC string
	var commandCXX string
	switch Compiler {
	case CompilerGcc:
		commandCC = "gcc"
		commandCXX = "g++"
	case CompilerClang:
		commandCC = "clang"
		commandCXX = "clang++"
	}

	if hasSetCache && *cacheEnabled {
		commandCC = "ccache " + commandCC
		commandCXX = "ccache " + commandCXX
	}

	localJobs := strconv.Itoa(getLocalJobs())

	fileReplaceSettings := [][]string{
		{bazelGenerateTmpFileName(tempBazelRcFileName), templateDistccDir, projectDistccDir},
		{bazelGenerateTmpFileName(tempBazelRcFileName), templatetoolchainDir, temptoolchainDirBase},
		{bazelGenerateTmpFileName(tempBazelRcFileName), templateJobs, localJobs},
		{bazelGenerateTmpFileName(ccShellFile), templateCc, commandCC},
		{bazelGenerateTmpFileName(cxxShellFile), templateCxx, commandCXX},
	}

	for _, block := range fileReplaceSettings {
		if len(block) < 3 {
			fmt.Printf("failed to render rule: %v, will be skipped\n", block)
			continue
		}
		if err := bazelReplaceFileWith(block[0], block[1], block[2]); err != nil {
			err = fmt.Errorf("failed to render config file(%s) for error[%v], check the system", block[0], err)
			return err
		}
	}

	// if old shell file exists, then remove the tmp one, for enable analysis cache of bazel
	if _, err := os.Stat(ccShellFile); err == nil {
		DebugPrintf("local compiling, cc shell file exists, the tmp one will be removed.\n")
		if err = os.Remove(bazelGenerateTmpFileName(ccShellFile)); err != nil {
			fmt.Printf("failed to remove tmp cc shell file(%s): %v\n", ccShellFile, err)
		}
	} else {
		DebugPrintf("stat %s: %v\n", ccShellFile, err)
	}
	if _, err := os.Stat(cxxShellFile); err == nil {
		DebugPrintf("local compiling, cxx shell file exists, the tmp one will be removed.\n")
		if err = os.Remove(bazelGenerateTmpFileName(cxxShellFile)); err != nil {
			fmt.Printf("failed to remove tmp cxx shell file(%s): %v\n", cxxShellFile, err)
		}
	} else {
		DebugPrintf("stat %s: %v\n", cxxShellFile, err)
	}

	DebugPrintf("success to render all tmp config file\n")
	return nil
}

func bazelRemoveAllTmpConfigFile() {
	tmpRcFile := bazelGenerateTmpFileName(tempBazelRcFileName)
	if err := os.Remove(tmpRcFile); err != nil {
		fmt.Printf("failed to remove tmp config file: %s, %v\n", tmpRcFile, err)
	} else {
		DebugPrintf("success to remove tmp config file: %s\n", tmpRcFile)
	}

	_ = filepath.Walk(temptoolchainDir, func(path string, f os.FileInfo, err error) error {
		if f == nil {
			return err
		}
		if !f.IsDir() {
			if strings.HasPrefix(temptoolchainDir, "./") && !strings.HasPrefix(path, "./") {
				path = "./" + path
			}
			tmpFile := strings.Replace(path, "\\", "/", -1)
			if ok, _ := bazelGenerateConfigFileName(tmpFile); !ok {
				return nil
			}

			if err := os.Remove(tmpFile); err != nil {
				fmt.Printf("failed to remove tmp config file: %s, %v\n", tmpFile, err)
			} else {
				DebugPrintf("success to remove tmp config file: %s\n", tmpFile)
			}
		}
		return nil
	})
}

func bazelCopyFileIfDifferent(src, dist string) error {
	if _, err := os.Stat(src); err != nil {
		return err
	}

	_, err := os.Stat(dist)
	if err != nil && !os.IsNotExist(err) {
		return err
	}

	if !os.IsNotExist(err) {
		ok, err := bazelCompareMD5(src, dist)
		if err != nil {
			return err
		}
		if ok {
			DebugPrintf("file(%s) has same md5 with old one(%s), will not override\n", src, dist)
			return nil
		}
	}

	DebugPrintf("file(%s) has different md5 with old one(%s), will override\n", src, dist)
	_, err = bazelCopyFile(src, dist)
	return err
}

func bazelCompareMD5(src, dist string) (bool, error) {
	srcHash, err := hashFileMd5(src)
	if err != nil {
		return false, err
	}

	distHash, err := hashFileMd5(dist)
	if err != nil {
		return false, err
	}

	return srcHash == distHash, nil
}

func bazelCompareTmpConfigFileAndReplace() error {
	if err := bazelCopyFileIfDifferent(bazelGenerateTmpFileName(tempBazelRcFileName), tempBazelRcFileName); err != nil {
		fmt.Printf("failed to copy config file: %v\n", err)
		return err
	}

	if err := filepath.Walk(temptoolchainDir, func(path string, f os.FileInfo, err error) error {
		if f == nil {
			return err
		}
		if !f.IsDir() {
			if strings.HasPrefix(temptoolchainDir, "./") && !strings.HasPrefix(path, "./") {
				path = "./" + path
			}
			tempsrcpath := strings.Replace(path, "\\", "/", -1)
			ok, tempdstPath := bazelGenerateConfigFileName(tempsrcpath)
			if !ok {
				return nil
			}

			if err := bazelCopyFileIfDifferent(tempsrcpath, tempdstPath); err != nil {
				fmt.Printf("failed to copy config file: %v\n", err)
				return err
			}
		}
		return nil
	}); err != nil {
		return err
	}

	if err := os.Chmod(ccShellFile, os.ModePerm); err != nil {
		err = fmt.Errorf("failed to chmod for file[%s] error[%v], check the system", ccShellFile, err)
		return err
	}
	if err := os.Chmod(cxxShellFile, os.ModePerm); err != nil {
		err = fmt.Errorf("failed to chmod for file[%s] error[%v], check the system", cxxShellFile, err)
		return err
	}

	return nil
}

func bazeltmpFileSuffix() string {
	return fmt.Sprintf(".%d%s", timestamp, tmpFileSuffix)
}

func bazelGenerateTmpFileName(filename string) string {
	return fmt.Sprintf("%s%s", filename, bazeltmpFileSuffix())
}

func bazelGenerateConfigFileName(tmpFilename string) (bool, string) {
	if !strings.HasSuffix(tmpFilename, bazeltmpFileSuffix()) {
		return false, ""
	}

	return true, tmpFilename[:strings.LastIndex(tmpFilename, ".")]
}

func hashFileMd5(filePath string) (string, error) {
	var returnMD5String string
	file, err := os.Open(filePath)
	if err != nil {
		return returnMD5String, err
	}
	defer func() {
		_ = file.Close()
	}()
	hash := md5.New()
	if _, err := io.Copy(hash, file); err != nil {
		return returnMD5String, err
	}
	hashInBytes := hash.Sum(nil)[:16]
	returnMD5String = hex.EncodeToString(hashInBytes)
	return returnMD5String, nil
}

func writeDistCCHost(content string) error {
	hostFile := path.Join(projectDistccDir, "hosts")
	fi, err := os.OpenFile(hostFile, os.O_WRONLY|os.O_CREATE, os.ModePerm)
	if err != nil {
		return err
	}

	if err = fi.Truncate(0); err != nil {
		return err
	}

	_, err = fi.WriteString(content)
	return err
}
