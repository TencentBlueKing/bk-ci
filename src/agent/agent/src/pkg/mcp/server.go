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
// 该 server 提供工具：查看运行中的构建任务及其进程树、获取近期错误日志。
//
// MCP Server 作为 agent 主进程的一个协程运行，随 agent 启动。
// 通过环境变量 DEVOPS_AGENT_ENABLE_MCP=true 开启。
// 开启后在 127.0.0.1 上随机端口监听 Streamable HTTP，端口号写入 .mcp_port 文件。
//
// 使用第三方库 github.com/ThinkInAIXYZ/go-mcp 实现 MCP 协议。
package mcp

import (
	"fmt"
	"net"
	"os"
	"path/filepath"
	"strconv"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/ThinkInAIXYZ/go-mcp/server"
	"github.com/ThinkInAIXYZ/go-mcp/transport"
)

const (
	mcpPortFileName = ".mcp_port"
	mcpEndpoint     = "/mcp"
)

// startServer 创建并启动 MCP Server，使用 Streamable HTTP 传输。
// 在 127.0.0.1 上随机分配端口监听，端口号写入工作目录下的 .mcp_port 文件。
// 该函数阻塞直到 server 退出。
func startServer() error {
	// 随机分配可用端口
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		return fmt.Errorf("listen on random port failed: %v", err)
	}
	port := listener.Addr().(*net.TCPAddr).Port
	// 立即关闭 listener，让 go-mcp transport 自己绑定这个端口
	listener.Close()

	addr := fmt.Sprintf("127.0.0.1:%d", port)

	// 将端口号写入文件，供 MCP 客户端读取
	portFilePath := filepath.Join(systemutil.GetWorkDir(), mcpPortFileName)
	if err := os.WriteFile(portFilePath, []byte(strconv.Itoa(port)), 0644); err != nil {
		return fmt.Errorf("write mcp port file failed: %v", err)
	}
	logs.Infof("mcp server port %d written to %s", port, portFilePath)

	// 创建 Streamable HTTP 传输
	t := transport.NewStreamableHTTPServerTransport(addr,
		transport.WithStreamableHTTPServerTransportOptionEndpoint(mcpEndpoint),
	)

	// 创建 MCP Server
	mcpServer, err := server.NewServer(t,
		server.WithServerInfo(serverInfo()),
	)
	if err != nil {
		os.Remove(portFilePath)
		return fmt.Errorf("create mcp server failed: %v", err)
	}

	// 注册所有工具
	registerAllTools(mcpServer)

	logs.Infof("mcp server starting on http://%s%s", addr, mcpEndpoint)

	// 阻塞运行
	if err := mcpServer.Run(); err != nil {
		os.Remove(portFilePath)
		return fmt.Errorf("mcp server run failed: %v", err)
	}

	// server 退出后清理端口文件
	os.Remove(portFilePath)
	return nil
}
