/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package constant

import "os"

const (
	DaemonExitCode = 88

	// DevopsAgentEnableNewConsole 如果设为true 则windows启动进程时使用 newConsole
	DevopsAgentEnableNewConsole = "DEVOPS_AGENT_ENABLE_NEW_CONSOLE"
	// DevopsAgentEnableExitGroup 启动Agent杀掉构建进程组的兜底逻辑
	DevopsAgentEnableExitGroup = "DEVOPS_AGENT_ENABLE_EXIT_GROUP"

	// CommonFileModePerm 公共文件权限
	CommonFileModePerm os.FileMode = 0644

	// WinCommandNewConsole windwos启动进程时打开新的console窗口
	WinCommandNewConsole = 0x00000010

	// DevopsAgentTimeoutExitTime 设置一个次数，达到超时次数 Agent 进程退出
	DevopsAgentTimeoutExitTime = "DEVOPS_AGENT_TIMEOUT_EXIT_TIME"
)
