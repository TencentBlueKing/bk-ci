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

// Package mcp 实现了 MCP (Model Context Protocol) server，使用 Streamable HTTP 传输进行通信。
// 该 server 提供工具：查看运行中的构建任务、获取近期错误日志。
//
// MCP Server 作为 agent 主进程的一个协程运行，支持通过环境变量 DEVOPS_AGENT_ENABLE_MCP 动态启停。
// 开启后在 127.0.0.1 上监听 Streamable HTTP，端口号持久化到 .agent.properties 的 devops.mcp.server.port。
//
// 使用第三方库 github.com/ThinkInAIXYZ/go-mcp 实现 MCP 协议。
package mcp

import (
	"context"
	"fmt"
	"net"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/ThinkInAIXYZ/go-mcp/server"
	"github.com/ThinkInAIXYZ/go-mcp/transport"
)

const (
	mcpEndpoint = "/mcp"
)

// resolvePort 确定 MCP Server 监听端口。
// 如果配置中已有端口号且可用，则直接使用；否则随机分配一个新端口并写回配置。
func resolvePort() (int, error) {
	cfgPort := config.GAgentConfig.McpServerPort

	if cfgPort <= 0 {
		// 配置中无端口，随机分配并持久化
		listener, err := net.Listen("tcp", "127.0.0.1:0")
		if err != nil {
			return 0, fmt.Errorf("listen on random port failed: %v", err)
		}
		port := listener.Addr().(*net.TCPAddr).Port
		listener.Close()

		config.GAgentConfig.McpServerPort = port
		if err := config.GAgentConfig.SaveConfig(); err != nil {
			logs.Warnf("mcp server save port to config failed: %v", err)
		}
		logs.Infof("mcp server allocated random port %d, saved to .agent.properties", port)
		return port, nil
	}

	// 配置中已有端口，检查是否可用
	addr := fmt.Sprintf("127.0.0.1:%d", cfgPort)
	listener, err := net.Listen("tcp", addr)
	if err != nil {
		return 0, fmt.Errorf("mcp server configured port %d unavailable: %v", cfgPort, err)
	}
	listener.Close()
	logs.Infof("mcp server reusing configured port %d", cfgPort)
	return cfgPort, nil
}

// startServer 创建并启动 MCP Server，使用 Streamable HTTP 传输。
// 端口从 .agent.properties 读取（首次自动分配），端口不可用时返回错误不做重试。
// 通过 entry.go 中的 stopFunc 注入 Shutdown 回调，支持外部停止。
// 该函数阻塞直到 server 退出。
func startServer() error {
	port, err := resolvePort()
	if err != nil {
		return err
	}

	addr := fmt.Sprintf("127.0.0.1:%d", port)

	// 创建 Streamable HTTP 传输
	t := transport.NewStreamableHTTPServerTransport(addr,
		transport.WithStreamableHTTPServerTransportOptionEndpoint(mcpEndpoint),
	)

	// 创建 MCP Server
	mcpServer, err := server.NewServer(t,
		server.WithServerInfo(serverInfo()),
	)
	if err != nil {
		return fmt.Errorf("create mcp server failed: %v", err)
	}

	// 注册 Shutdown 回调，供 SyncState 调用以停止 server
	mu.Lock()
	stopFunc = func() {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		if err := mcpServer.Shutdown(ctx); err != nil {
			logs.Errorf("mcp server shutdown error: %v", err)
		}
	}
	mu.Unlock()

	// 注册所有工具
	registerAllTools(mcpServer)

	logs.Infof("mcp server starting on http://%s%s", addr, mcpEndpoint)

	// 阻塞运行
	if err := mcpServer.Run(); err != nil {
		return fmt.Errorf("mcp server run failed: %v", err)
	}

	return nil
}
