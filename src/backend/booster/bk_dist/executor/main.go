/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package main

import (
	"os"

	executor "github.com/Tencent/bk-ci/src/booster/bk_dist/executor/pkg"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/conf"
)

// bk-dist-executor 远程执行进程子任务，例如: bk-dist-executor gcc a.c -o a.o
// bk_booster方式运行进程之后，先根据参数申请worker资源，然后hook进程的命令调用子命令，
// 当bk_booster 捕捉到进程调用子命令时，调用bk-dist-executor执行该子命令
func main() {
	// make blog logs into stderr
	blog.InitLogs(conf.LogConfig{
		ToStdErr: true,
	})

	exitCode, _, _ := executor.NewDistExecutor().Run()
	os.Exit(exitCode)
}
