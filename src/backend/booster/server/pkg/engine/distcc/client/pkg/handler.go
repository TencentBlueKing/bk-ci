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
	"bytes"
	"fmt"
	"os"
	"os/exec"
	"os/signal"
	"runtime"
	"strconv"
	"strings"
	"syscall"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/types"
	"github.com/Tencent/bk-ci/src/booster/common/version"
)

func applyDistCCResources(sets types.DistccServerSets) (*types.DistccServerInfo, bool, error) {
	sets.ClientCPU = runtime.NumCPU()
	sets.ClientIp = LocalIP
	sets.RunDir = RunDir
	sets.ClientVersion = version.Version

	var data []byte
	_ = codec.EncJSON(sets, &data)

	DebugPrintf("request info: %s\n", string(data))

	data, ok, err := requestServer("POST", applyDistCCServerURI, data)
	if err != nil {
		return nil, ok, err
	}

	var server types.DistccServerInfo
	if err = codec.DecJSON(data, &server); err != nil {
		return nil, ok, err
	}

	return &server, ok, nil
}

/*  parse ccache -s stdout

$ ccache -s
cache directory                     /home/nick/.ccache
primary config                      /home/nick/.ccache/ccache.conf
secondary config      (readonly)    /etc/ccache.conf
cache hit (direct)                  3232
cache hit (preprocessed)               7
cache miss                             3
called for link                        6
called for preprocessing             538
unsupported source language           66
no input file                        108
files in cache                      9734
cache size                         432.8 MB
max cache size                       5.0 GB
*/
/* ccache 3.1.6
cache directory                     /root/.ccache
cache hit (direct)                    71
cache hit (preprocessed)               0
cache miss                           145
called for link                        9
unsupported compiler option            8
files in cache                       436
cache size                          18.3 Mbytes

*/
func statisticsCCache() (*types.Ccache, error) {
	cmd := exec.Command("/bin/bash", "-c", "ccache -s")
	buf := bytes.NewBuffer(make([]byte, 1024))
	cmd.Stdout = buf
	err := cmd.Run()
	if err != nil {
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

func distCCDone(info *types.DistccClientInfo) {
	var data []byte
	_ = codec.EncJSON(info, &data)
	fmt.Printf("try to release task, taskID: %s\n", info.TaskID)
	_, _, err := requestServer("POST", distCCTaskDoneURI, data)
	if err != nil {
		fmt.Printf("release distcc servers error: %v\n", err)
	}
	fmt.Println("distcc server released")
}

func loopHeartBeat(taskId string) {
	ping := &types.HeartBeat{
		TaskID: taskId,
		Type:   types.HeartBeatPing,
	}
	var data []byte
	_ = codec.EncJSON(ping, &data)

	ticker := time.NewTicker(types.ClientHeartBeatTickTime)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			_, _, err := requestServer("POST", heartBeatURI, data)
			if err != nil {
				fmt.Printf("ping distcc server error: %v\n", err)
			}
		}
	}
}

func handlerSysSignal(taskId string) {
	interrupt := make(chan os.Signal)
	signal.Notify(interrupt, syscall.SIGINT, syscall.SIGTERM)

	select {
	case sig := <-interrupt:
		fmt.Printf("get system signal %s, and exit\n", sig.String())
		clientInfo := &types.DistccClientInfo{
			TaskID:  taskId,
			Status:  types.ClientStatusFailed,
			Message: fmt.Sprintf("get system signal: %s", sig.String()),
		}

		distCCDone(clientInfo)

		// catch control-C and should return code 130(128+0x2)
		if sig == syscall.SIGINT {
			os.Exit(130)
		}

		// catch kill and should return code 143(128+0xf)
		if sig == syscall.SIGTERM {
			os.Exit(143)
		}
		os.Exit(1)
	}
}

func checkLocalGccVersion(remoteGccVersion string) error {
	cmd := exec.Command("/bin/bash", "-c", "gcc -dumpfullversion")
	output, err := cmd.Output()
	if err != nil {
		cmd = exec.Command("/bin/bash", "-c", "gcc -dumpversion")
		output, err = cmd.Output()
		if err != nil {
			err = fmt.Errorf("get local gcc version failed: %v, compile will be canceled", err)
			fmt.Printf("%v\n", err)
			return err
		}
	}

	output = bytes.Trim(output, "\n\r")
	if string(output) != remoteGccVersion {
		err = fmt.Errorf("local gcc version: %s, different from settings: %s, compile will be canceled",
			output, remoteGccVersion)
		fmt.Printf("%v\n", err)
		return err
	}
	return nil
}

func checkLocalClangVersion(remoteClangVersion string) error {
	cmd := exec.Command("/bin/bash", "-c", "clang --version | grep version | "+
		"sed 's/.*version \\([0-9]*.[0-9]*.[0-9]*\\).*/\\1/g'")
	output, err := cmd.Output()
	if err != nil {
		err = fmt.Errorf("get local clang version failed: %v, compile will be canceled", err)
		fmt.Printf("%v\n", err)
		return err
	}

	output = bytes.Trim(output, "\n\r")
	if string(output) != remoteClangVersion {
		err = fmt.Errorf("local clang version: %s, different from settings: %s, compile will be canceled",
			output, remoteClangVersion)
		fmt.Printf("%v\n", err)
		return err
	}
	return nil
}

func inspectDistCCServers(taskId string) (*types.DistccServerInfo, error) {
	data, _, err := requestServer("GET",
		fmt.Sprintf("%s?task_id=%s", inspectDistCCServerURI, taskId), nil)
	if err != nil {
		return nil, err
	}

	var servers types.DistccServerInfo
	err = codec.DecJSON(data, &servers)
	if err != nil {
		return nil, err
	}

	return &servers, nil
}
