package cron

import (
	"fmt"
	"os"
	"strings"
	"time"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"
	"github.com/TencentBlueKing/bk-ci/agentslim/pkg/config"
)

func CleanLog() {
	ticker := time.Tick(4 * time.Hour)
	for range ticker {
		cleanDumpFile(3 * 24)
		cleanLogFile(3 * 24)
	}
}

func cleanDumpFile(timeBeforeInHours int) {
	dumpFileBeforeStr := time.Now().Add(time.Hour * time.Duration(timeBeforeInHours*-1)).Format("2006-01-02 15:04:05")
	workDir := config.Config.WorkDir
	logs.Info(fmt.Sprintf("clean dump file before %s(%d hours) in %s", dumpFileBeforeStr, timeBeforeInHours, workDir))
	files, err := os.ReadDir(workDir)
	if err != nil {
		logs.Warn("read work dir error: ", err.Error())
		return
	}
	for _, file := range files {
		if file.IsDir() {
			continue
		}

		info, err := file.Info()
		if err != nil {
			logs.WithError(err).Warn("cleanDumpFile error")
			continue
		}

		if strings.HasPrefix(file.Name(), "hs_err_pid") && int(time.Since(info.ModTime()).Hours()) > timeBeforeInHours {
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
	logFileBeforeStr := time.Now().Add(time.Hour * time.Duration(timeBeforeInHours*-1)).Format("2006-01-02 15:04:05")
	logDir := config.Config.LogDir
	logs.Info(fmt.Sprintf("clean log file before %s(%d hours) in %s", logFileBeforeStr, timeBeforeInHours, logDir))
	files, err := os.ReadDir(logDir)
	if err != nil {
		logs.Warn("read log dir error: ", err.Error())
		return
	}
	for _, file := range files {
		if file.IsDir() {
			continue
		}

		info, err := file.Info()
		if err != nil {
			logs.WithError(err).Warn("cleanLogFile error")
			continue
		}

		if strings.HasSuffix(file.Name(), ".log") && int(time.Since(info.ModTime()).Hours()) > timeBeforeInHours {
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
}
