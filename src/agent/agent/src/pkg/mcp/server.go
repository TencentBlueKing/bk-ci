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

// Package mcp 实现了 MCP (Model Context Protocol) server，使用 JSON-RPC 2.0 over stdio 进行通信。
// 该 server 提供两个工具：查看运行中的构建任务及其进程树、获取近期错误日志。
//
// MCP Server 作为 agent 主进程的一个协程运行，随 agent 启动。
// 通过环境变量 DEVOPS_AGENT_ENABLE_MCP=true 开启。
// 开启后 stdio 专属 MCP 通信，agent 日志全部走文件。
//
// 不依赖任何第三方库，纯标准库实现 JSON-RPC 2.0 over stdio，保持 go 1.19 兼容。
package mcp

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io"
	"strconv"
	"strings"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

const (
	mcpProtocolVersion = "2024-11-05"
	serverName         = "bk-ci-agent"
	serverVersion      = "1.0.0"
)

// jsonRPCRequest JSON-RPC 2.0 请求
type jsonRPCRequest struct {
	JSONRPC string          `json:"jsonrpc"`
	ID      json.RawMessage `json:"id,omitempty"`
	Method  string          `json:"method"`
	Params  json.RawMessage `json:"params,omitempty"`
}

// jsonRPCResponse JSON-RPC 2.0 响应
type jsonRPCResponse struct {
	JSONRPC string          `json:"jsonrpc"`
	ID      json.RawMessage `json:"id,omitempty"`
	Result  interface{}     `json:"result,omitempty"`
	Error   *jsonRPCError   `json:"error,omitempty"`
}

type jsonRPCError struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
}

// ToolDefinition MCP Tool 定义
type ToolDefinition struct {
	Name        string                 `json:"name"`
	Description string                 `json:"description"`
	InputSchema map[string]interface{} `json:"inputSchema"`
}

// ToolHandler 工具处理函数
type ToolHandler func(args map[string]interface{}) (string, error)

// Server MCP Server，通过 stdio 进行 JSON-RPC 2.0 交互
type Server struct {
	tools    []ToolDefinition
	handlers map[string]ToolHandler
	reader   *bufio.Reader
	writer   io.Writer
}

// NewServer 创建 MCP Server
func NewServer(reader io.Reader, writer io.Writer) *Server {
	return &Server{
		tools:    make([]ToolDefinition, 0),
		handlers: make(map[string]ToolHandler),
		reader:   bufio.NewReader(reader),
		writer:   writer,
	}
}

// RegisterTool 注册工具
func (s *Server) RegisterTool(def ToolDefinition, handler ToolHandler) {
	s.tools = append(s.tools, def)
	s.handlers[def.Name] = handler
}

// Serve 启动 MCP Server 主循环（阻塞），逐行读取 stdin 并处理 JSON-RPC 请求
func (s *Server) Serve() {
	logs.Info("mcp server started, waiting for requests on stdio")
	for {
		line, err := s.reader.ReadString('\n')
		if err != nil {
			if err == io.EOF {
				logs.Info("mcp server: stdin closed, exiting")
				return
			}
			logs.Warnf("mcp server: read error: %v", err)
			return
		}

		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}

		var req jsonRPCRequest
		if err := json.Unmarshal([]byte(line), &req); err != nil {
			logs.Warnf("mcp server: invalid json: %v", err)
			s.sendError(nil, -32700, "Parse error")
			continue
		}

		s.handleRequest(&req)
	}
}

func (s *Server) handleRequest(req *jsonRPCRequest) {
	switch req.Method {
	case "initialize":
		s.handleInitialize(req)
	case "notifications/initialized":
		// 客户端初始化完成通知，无需回复
	case "tools/list":
		s.handleToolsList(req)
	case "tools/call":
		s.handleToolsCall(req)
	case "ping":
		s.sendResult(req.ID, map[string]interface{}{})
	default:
		// 如果是通知（无 ID），直接忽略
		if req.ID == nil || string(req.ID) == "null" {
			return
		}
		s.sendError(req.ID, -32601, fmt.Sprintf("Method not found: %s", req.Method))
	}
}

func (s *Server) handleInitialize(req *jsonRPCRequest) {
	result := map[string]interface{}{
		"protocolVersion": mcpProtocolVersion,
		"capabilities": map[string]interface{}{
			"tools": map[string]interface{}{},
		},
		"serverInfo": map[string]interface{}{
			"name":    serverName,
			"version": serverVersion,
		},
	}
	s.sendResult(req.ID, result)
}

func (s *Server) handleToolsList(req *jsonRPCRequest) {
	result := map[string]interface{}{
		"tools": s.tools,
	}
	s.sendResult(req.ID, result)
}

func (s *Server) handleToolsCall(req *jsonRPCRequest) {
	var params struct {
		Name      string                 `json:"name"`
		Arguments map[string]interface{} `json:"arguments"`
	}
	if req.Params != nil {
		if err := json.Unmarshal(req.Params, &params); err != nil {
			s.sendError(req.ID, -32602, "Invalid params")
			return
		}
	}

	handler, ok := s.handlers[params.Name]
	if !ok {
		s.sendToolResult(req.ID, fmt.Sprintf("Unknown tool: %s", params.Name), true)
		return
	}

	text, err := handler(params.Arguments)
	if err != nil {
		s.sendToolResult(req.ID, fmt.Sprintf("Error: %v", err), true)
		return
	}

	s.sendToolResult(req.ID, text, false)
}

func (s *Server) sendToolResult(id json.RawMessage, text string, isError bool) {
	result := map[string]interface{}{
		"content": []map[string]interface{}{
			{
				"type": "text",
				"text": text,
			},
		},
		"isError": isError,
	}
	s.sendResult(id, result)
}

func (s *Server) sendResult(id json.RawMessage, result interface{}) {
	resp := jsonRPCResponse{
		JSONRPC: "2.0",
		ID:      id,
		Result:  result,
	}
	s.writeResponse(resp)
}

func (s *Server) sendError(id json.RawMessage, code int, message string) {
	resp := jsonRPCResponse{
		JSONRPC: "2.0",
		ID:      id,
		Error: &jsonRPCError{
			Code:    code,
			Message: message,
		},
	}
	s.writeResponse(resp)
}

func (s *Server) writeResponse(resp jsonRPCResponse) {
	data, err := json.Marshal(resp)
	if err != nil {
		logs.Errorf("mcp server: marshal response error: %v", err)
		return
	}
	line := string(data) + "\n"
	if _, err := io.WriteString(s.writer, line); err != nil {
		logs.Errorf("mcp server: write response error: %v", err)
	}
}

// parseIntArg 从参数 map 中解析 int 值
func parseIntArg(args map[string]interface{}, key string, defaultVal int) int {
	v, ok := args[key]
	if !ok {
		return defaultVal
	}
	switch val := v.(type) {
	case float64:
		return int(val)
	case string:
		i, err := strconv.Atoi(val)
		if err != nil {
			return defaultVal
		}
		return i
	default:
		return defaultVal
	}
}
