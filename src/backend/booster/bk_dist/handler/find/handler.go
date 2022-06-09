/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package find

import (
	"fmt"
	"os"
	"strings"

	dcConfig "github.com/Tencent/bk-ci/src/booster/bk_dist/common/config"
	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/disttask"
)

const (
	hookConfig = ".\\bk_counter_rules.json"
)

// NewFinder get a new finder handler
func NewFinder() (handler.Handler, error) {
	return &Finder{
		sandbox: &dcSyscall.Sandbox{},
	}, nil
}

// Finder describe a handler for counting strings in files in Windows.
// Finder handles multiple "find" commands in parallel
//
// For instance, each following line is a independent command and they runs in parallel at the some time:
// - find /C "a" test1.txt test2.txt
// - find /C "b" test3.txt
// - find /C "c" test4.txt
//
// No matter what tools manages the parallel process, Finder take over the "find" commands. And here are the steps:
// 1. run InitExtra with the extra data
// 2. run RenderArgs && GetPreloadConfig && GetFilterRules to prepare the process settings.
// 3. do PreWork before all process begin
// 4. do the Command from user, when the user command running, multiple "find" commands brings up.
// 5. in each "find" command:
//      - do PreExecute
//      - run command (in local or remote)
//      - do PostExecute
// 6. do PostWork after all process finish
//
// Particularly, MParallel.exe is recommended to used to manage the parallel process for "find" commands.
//
// In remote, Finder use a linux platform to execute the "find" commands
// then some convert should be done from windows to linux
// WINDOWS               LINUX
// find                  grep
// find /c               grep -c
// find /v               grep -v
// find /n               grep -n
// find /i               grep -i
// and the output should be converted from linux to windows
// LINUX                 WINDOWS
// test1.txt:23          ----------TEST1.TXT: 23
//
// test2.txt: foobar     ----------TEST2.TXT:
//                       foobar
//
type Finder struct {
	sandbox    *dcSyscall.Sandbox
	inputFiles []string
	count      bool
}

// InitSandbox to init sandbox
func (c *Finder) InitSandbox(sandbox *dcSyscall.Sandbox) {
	c.sandbox = sandbox
}

// ResultExtra return the extra data for recording in project info.
func (c *Finder) ResultExtra() []byte {
	return nil
}

// InitExtra receive the extra data after resources applied.
// Finder use the disttask engine, so that it should decode extra data with disttask.CustomData.
func (c *Finder) InitExtra(extra []byte) {
	var data disttask.CustomData
	if err := codec.DecJSON(extra, &data); err != nil {
		return
	}

	// print some information got from extra data.
	fmt.Printf("counter: got worker version %s\n", data.WorkerVersion)
}

// RenderArgs receive the origin command from user, and decide whether it should be rendered before execution.
func (c *Finder) RenderArgs(config dcType.BoosterConfig, originArgs string) string {

	// nothing need to be rendered.
	return originArgs
}

// PreWork provide a chance to do some process before all processes begin.
func (c *Finder) PreWork(config *dcType.BoosterConfig) error {
	fmt.Printf("counter: nothing to do in prework\n")
	return nil
}

// PostWork provide a chance to do some process after all processes finish.
func (c *Finder) PostWork(config *dcType.BoosterConfig) error {
	fmt.Printf("\ncounter: nothing to do in postwork\n")
	return nil
}

// GetPreloadConfig open the preload config file and return the settings.
func (c *Finder) GetPreloadConfig(config dcType.BoosterConfig) (*dcSDK.PreloadConfig, error) {
	configfile := hookConfig

	if config.Works.HookConfigPath != "" {
		configfile = config.Works.HookConfigPath
	}

	f, err := os.Open(dcConfig.GetFile(configfile))
	if err != nil {
		return nil, err
	}
	defer func() {
		_ = f.Close()
	}()

	var pConfig dcSDK.PreloadConfig
	if err = codec.DecJSONReader(f, &pConfig); err != nil {
		return nil, err
	}

	return &pConfig, nil
}

// GetFilterRules return the sending file filter rules.
func (c *Finder) GetFilterRules() ([]dcSDK.FilterRuleItem, error) {

	// nothing need to be filtered.
	return nil, nil
}

// PreExecuteNeedLock decide whether should lock when executor do the pre-process
func (c *Finder) PreExecuteNeedLock(command []string) bool {
	return false
}

// PostExecuteNeedLock decide whether should lock when executor do the post-process
func (c *Finder) PostExecuteNeedLock(result *dcSDK.BKDistResult) bool {
	return true
}

// LocalExecuteNeed no need
func (c *Finder) LocalExecuteNeed(command []string) bool {
	return false
}

// LocalLockWeight decide local-execute lock weight, default 1
func (c *Finder) LocalLockWeight(command []string) int32 {
	return 1
}

// LocalExecute no need
func (c *Finder) LocalExecute(command []string) (int, error) {
	return 0, nil
}

// PreLockWeight decide pre-execute lock weight, default 1
func (c *Finder) PreLockWeight(command []string) int32 {
	return 1
}

// PreExecute do the pre-process in one executor command.
// PreExecute should analyse the input command and generate the DistCommand to send to remote.
func (c *Finder) PreExecute(command []string) (*dcSDK.BKDistCommand, error) {
	// command must be "find"
	//if len(command) < 3 || (strings.ToUpper(command[0]) != "FIND" && strings.ToUpper(command[0]) != "FIND.EXE") {
	upperCommand := strings.ToUpper(command[0])
	if len(command) < 3 || (!strings.HasSuffix(upperCommand, "FIND") &&
		!strings.HasSuffix(upperCommand, "FIND.EXE")) {
		return nil, fmt.Errorf("invalid command,len(command):[%d], command[0]:[%s]", len(command), command[0])
	}

	strSeen := false
	inputFiles := make([]dcSDK.FileDesc, 0, 10)
	inputFilesName := make([]string, 0, 10)
	args := make([]string, 0, 10)

	//args = append(args, "/C")
	//args = append(args, command[0])
	// try to find out all input files.
	for _, arg := range command[1:] {

		// start with "/" means it is a option, then should be convert to "-"
		if strings.HasPrefix(arg, "/") {
			//args = append(args, strings.ToLower(strings.ReplaceAll(arg, "/", "-")))
			args = append(args, arg)

			if strings.ToUpper(arg) == "/C" {
				c.count = true
			}
			continue
		}

		// first no-option arg should be the string to be found
		if !strSeen {
			strSeen = true
			if strings.HasPrefix(arg, "\"") {
				args = append(args, arg)
			} else {
				args = append(args, "\""+arg+"\"")
			}
			continue
		}

		// all the others are input files.
		existed, fileSize, modifyTime, fileMode := dcFile.Stat(arg).Batch()
		if !existed {
			return nil, fmt.Errorf("input file %s not exist", arg)
		}
		inputFiles = append(inputFiles, dcSDK.FileDesc{
			FilePath:       arg,
			Compresstype:   protocol.CompressLZ4,
			FileSize:       fileSize,
			Lastmodifytime: modifyTime,
			Md5:            "",
			Filemode:       fileMode,
		})
		inputFilesName = append(inputFilesName, arg)
		args = append(args, arg)
	}

	c.inputFiles = inputFilesName
	return &dcSDK.BKDistCommand{
		Commands: []dcSDK.BKCommand{
			{
				WorkDir: "",
				ExePath: "",
				//ExeName:    "grep",
				ExeName:    command[0],
				Params:     args,
				Inputfiles: inputFiles,
			},
		},
	}, nil
}

// NeedRemoteResource check whether this command need remote resource
func (c *Finder) NeedRemoteResource(command []string) bool {
	return true
}

// RemoteRetryTimes will return the remote retry times
func (c *Finder) RemoteRetryTimes() int {
	return 0
}

// PostLockWeight decide post-execute lock weight, default 1
func (c *Finder) PostLockWeight(result *dcSDK.BKDistResult) int32 {
	return 1
}

// PostExecute do the post-process in one executor command.
// PostExecute should check the DistResult and judge whether the remote processing succeeded.
// Also PostExecute should manages the output message.
func (c *Finder) PostExecute(r *dcSDK.BKDistResult) error {
	if r == nil || len(r.Results) == 0 {
		return fmt.Errorf("invalid param")
	}
	result := r.Results[0]

	if result.RetCode != 0 {
		return fmt.Errorf("failed to execute on remote: %s", string(result.ErrorMessage))
	}

	belongs := ""
	tail := "\n"
	if c.count {
		tail = ": "
	}

	if len(c.inputFiles) == 1 {
		belongs = c.inputFiles[0]

		fmt.Printf("\n---------- %s%s", strings.ToUpper(belongs), tail)
	}

	for _, line := range strings.Split(string(result.OutputMessage), "\n") {
		inputFile, originResult := c.hasBelongsMark(line)
		if inputFile != "" && inputFile != belongs {
			belongs = inputFile
			fmt.Printf("\n---------- %s%s", strings.ToUpper(belongs), tail)
		}

		fmt.Printf("%s\n", originResult)
	}
	return nil
}

// FinalExecute provide a chance to do process before the process exit.
func (c *Finder) FinalExecute([]string) {
	return
}

// hasBelongsMark receive one line string from linux-grep
// return the belong file and the origin result.
func (c *Finder) hasBelongsMark(line string) (string, string) {
	for _, inputFile := range c.inputFiles {
		if strings.HasPrefix(line, inputFile+":") {
			return inputFile, strings.TrimPrefix(line, inputFile+":")
		}
	}

	return "", line
}
