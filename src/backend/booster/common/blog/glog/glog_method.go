/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package glog

import (
	"strconv"
	"sync"
)

// SetV set logs output level in runtime.
func SetV(level Level) {
	_ = logging.verbosity.Set(strconv.Itoa(int(level)))
}

// SetStderrLevel 设置高于等于level的内容会被打到stderr中
func SetStderrLevel(level int32) {
	logging.toStderrLevel = severity(level)
}

var once sync.Once

// InitLogs init glog from commandline params
func InitLogs(
	toStderr, alsoToStderr, asyncFlush bool,
	verbose, toStderrLevel int32,
	stdErrThreshold, vModule, traceLocation, dir string,
	maxSize uint64,
	maxNum int) {

	once.Do(func() {
		logging.toStderr = toStderr
		logging.toStderrLevel = severity(toStderrLevel)
		logging.asyncFlush = asyncFlush
		logging.alsoToStderr = alsoToStderr
		_ = logging.verbosity.Set(strconv.Itoa(int(verbose)))
		_ = logging.stderrThreshold.Set(stdErrThreshold)
		_ = logging.vmodule.Set(vModule)
		_ = logging.traceLocation.Set(traceLocation)

		logMaxNum = maxNum
		logMaxSize = maxSize * 1024 * 1024
		logDir = dir
	})
}
