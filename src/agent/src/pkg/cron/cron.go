/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package cron

import (
	"context"
	"fmt"
	"io/ioutil"
	"os"
	"strings"
	"time"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/imagedebug"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/job_docker"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/api/types/container"
	"github.com/docker/docker/client"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
)

func CleanJob() {
	defer func() {
		if err := recover(); err != nil {
			logs.Error("agent clean panic: ", err)
		}
	}()

	intervalInHours := 2
	TryCleanFile()
	for {
		now := time.Now()
		nextTime := now.Add(time.Hour * time.Duration(intervalInHours))
		logs.Info("next clean time: ", util.FormatTime(nextTime))
		t := time.NewTimer(nextTime.Sub(now))
		<-t.C
		TryCleanFile()
	}
}

func TryCleanFile() {
	logs.Info("clean task starts")
	defer func() {
		if err := recover(); err != nil {
			logs.Error("run clean task error: ", err)
		}
	}()

	cleanDumpFile(config.GAgentConfig.LogsKeepHours)
	cleanLogFile(config.GAgentConfig.LogsKeepHours)
}

func cleanDumpFile(timeBeforeInHours int) {
	dumpFileBeforeStr := util.FormatTime(time.Now().Add(time.Hour * time.Duration(timeBeforeInHours*-1)))
	workDir := systemutil.GetWorkDir()
	logs.Info(fmt.Sprintf("clean dump file before %s(%d hours) in %s", dumpFileBeforeStr, timeBeforeInHours, workDir))
	files, err := ioutil.ReadDir(workDir)
	if err != nil {
		logs.Warn("read work dir error: ", err.Error())
		return
	}
	for _, file := range files {
		if file.IsDir() {
			continue
		}
		if strings.HasPrefix(file.Name(), "hs_err_pid") && int(time.Since(file.ModTime()).Hours()) > timeBeforeInHours {
			fileFullName := workDir + "/" + file.Name()
			err = os.Remove(fileFullName)
			if err != nil {
				logs.Warn(fmt.Sprintf("remove file %s failed: ", fileFullName))
			} else {
				logs.Info(fmt.Sprintf("file %s removed", fileFullName))
			}
		}
	}
	logs.Info("clean dump file done")
}

func cleanLogFile(timeBeforeInHours int) {
	logFileBeforeStr := util.FormatTime(time.Now().Add(time.Hour * time.Duration(timeBeforeInHours*-1)))
	logDir := systemutil.GetLogDir()
	logs.Info(fmt.Sprintf("clean log file before %s(%d hours) in %s", logFileBeforeStr, timeBeforeInHours, logDir))
	files, err := ioutil.ReadDir(logDir)
	if err != nil {
		logs.Warn("read log dir error: ", err.Error())
		return
	}
	for _, file := range files {
		if file.IsDir() {
			continue
		}
		if strings.HasSuffix(file.Name(), ".log") && int(time.Since(file.ModTime()).Hours()) > timeBeforeInHours {
			fileFullName := logDir + "/" + file.Name()
			err = os.Remove(fileFullName)
			if err != nil {
				logs.Warn(fmt.Sprintf("remove file %s failed: ", fileFullName))
			} else {
				logs.Info(fmt.Sprintf("file %s removed", fileFullName))
			}
		}
	}
	logs.Info("clean log file done")

	// 清理docker构建记录
	dockerLogDir := job_docker.LocalDockerWorkSpaceDirName + "/logs"
	dockerFiles, err := ioutil.ReadDir(dockerLogDir)
	if err != nil {
		logs.Warn("read docker log dir error: ", err.Error())
		return
	}

	// 因为docker构建机是按照buildId分类存储到文件夹中，所以只需要查看文件夹变更日期之后删除即可
	for _, file := range dockerFiles {
		if !file.IsDir() {
			continue
		}

		if int(time.Since(file.ModTime()).Hours()) > timeBeforeInHours {
			dockerFullName := dockerLogDir + "/" + file.Name()
			err = os.RemoveAll(dockerFullName)
			if err != nil {
				logs.Warn(fmt.Sprintf("remove docker log file %s failed: ", dockerFullName))
			} else {
				logs.Info(fmt.Sprintf("docker log file %s removed", dockerFullName))
			}
		}
	}
}

func CleanDebugContainer() {
	if !systemutil.IsLinux() {
		return
	}

	defer func() {
		if err := recover(); err != nil {
			logs.Error("agent clean debug container panic: ", err)
		}
	}()

	cleanDebugContainer()

	ticker := time.Tick(4 * time.Hour)
	for range ticker {
		cleanDebugContainer()
	}
}

// 清理过期的调试容器
func cleanDebugContainer() {
	if !config.GAgentConfig.EnableDockerBuild {
		return
	}
	cli, err := client.NewClientWithOpts(client.FromEnv, client.WithAPIVersionNegotiation())
	if err != nil {
		logs.WithError(err).Warn("cleanDebugContainer get docker lient error")
		return
	}
	conList, err := cli.ContainerList(context.Background(), types.ContainerListOptions{})
	if err != nil {
		logs.WithError(err).Warn("cleanDebugContainer get docker container list error")
		return
	}

	for _, c := range conList {
		for _, n := range c.Names {
			if strings.Contains(n, imagedebug.DebugContainerHeader+"b-") && time.Now().Sub(time.Unix(c.Created, 0)) > imagedebug.ImageDebugMaxHoldHour*time.Hour {
				logs.Infof("cleanDebugContainer find debug container %s created %s than 24 hour will remove", c.ID, time.Unix(c.Created, 0).String())
				containerStopTimeout := 0
				if err := cli.ContainerStop(context.Background(), c.ID, container.StopOptions{Timeout: &containerStopTimeout}); err != nil {
					logs.WithError(err).Warnf("cleanDebugContainer stop container %s error", c.ID)
				}
				if err = cli.ContainerRemove(context.Background(), c.ID, types.ContainerRemoveOptions{Force: true}); err != nil {
					logs.WithError(err).Warnf("remove container %s error", c.ID)
				}
			}
		}
	}

	return
}
