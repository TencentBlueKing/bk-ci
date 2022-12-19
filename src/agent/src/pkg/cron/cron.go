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
	"fmt"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/config"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/job"
	"io/ioutil"
	"os"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/agent/src/pkg/logs"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/systemutil"
)

func CleanJob() {
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
	dockerLogDir := job.LocalDockerWorkSpaceDirName + "/logs"
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
