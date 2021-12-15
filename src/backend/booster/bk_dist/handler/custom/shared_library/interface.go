/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package sharedlibrary

// TransLayer
type TransLayer interface {
	// InitExtra receive the extra data from project settings and init the handle extra data
	InitExtra(extra []byte)

	InitSandbox(sandbox []byte)

	// PreWork define the work before all executors run.
	PreWork(config []byte) (err []byte)

	// PostWork define the work after all executors end.
	PostWork(config []byte) (err []byte)

	// RenderArgs return the actual executing args with the origin args and config
	RenderArgs(config []byte, originArgs []byte) (err []byte)

	// GetPreloadConfig define the preload config from settings.
	GetPreloadConfig(config []byte) (preloadConfig []byte, err []byte)

	// GetFilterRules return files which will be used by multi-executor, send only once
	GetFilterRules() (rules []byte, err []byte)

	// PreExecute will be called before task is distributed
	PreExecute(command []byte) (distCommand []byte, err []byte)

	// PreExecuteNeedLock decide whether executor should lock before pre execution
	PreExecuteNeedLock(command []byte) (need bool)

	// LocalExecuteNeed decide whether executor should execute local command
	LocalExecuteNeed(command []byte) (need bool)

	// LocalExecute will execute this command by handler
	LocalExecute(command []byte) (exitCode int, err []byte)

	// PostExecute will be called after task is distributed and executed
	PostExecute(result []byte) (err []byte)

	// PostExecuteNeedLock decide whether executor should lock before post execution
	PostExecuteNeedLock(result []byte) (need bool)

	// FinalExecute chance to finalize for handler, must be safe to call in goroutines
	FinalExecute(command []byte)
}
