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

package mcp

import (
	"sync"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/envs"
)

var (
	// mu 保护 running 状态和 stopFunc 的并发访问
	mu      sync.Mutex
	running bool
	// stopFunc 用于停止当前运行的 MCP Server，由 startServer 设置
	stopFunc func()
)

// SyncState 根据当前环境变量状态动态启停 MCP Server。
// 每次心跳更新环境变量后应调用此函数，它会检测 DEVOPS_AGENT_ENABLE_MCP 的变化并相应启停服务。
// 该函数是并发安全的。
func SyncState() {
	enabled := envs.FetchEnvAndCheck(constant.DevopsAgentEnableMCP, "true")

	mu.Lock()
	defer mu.Unlock()

	if enabled && !running {
		// 需要启动
		logs.Info("mcp server enabled, starting in background goroutine")
		running = true
		go func() {
			defer func() {
				if r := recover(); r != nil {
					logs.Errorf("mcp server panic recovered: %v", r)
				}
				mu.Lock()
				running = false
				stopFunc = nil
				mu.Unlock()
			}()

			if err := startServer(); err != nil {
				logs.Errorf("mcp server start failed: %v", err)
				return
			}
			logs.Info("mcp server exited")
		}()
	} else if !enabled && running {
		// 需要停止
		logs.Info("mcp server disabled, stopping")
		if stopFunc != nil {
			stopFunc()
		}
		// running 会在 goroutine 的 defer 中被设为 false
	}
}
