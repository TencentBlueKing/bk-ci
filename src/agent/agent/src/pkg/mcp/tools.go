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
	"bufio"
	"context"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/job"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"

	"github.com/ThinkInAIXYZ/go-mcp/protocol"
	"github.com/ThinkInAIXYZ/go-mcp/server"
)

const (
	serverName    = "bk-ci-agent"
	serverVersion = "1.0.0"
)

// serverInfo 返回 MCP Server 元信息
func serverInfo() protocol.Implementation {
	return protocol.Implementation{
		Name:    serverName,
		Version: serverVersion,
	}
}

// registerAllTools 注册所有 MCP 工具到 server
func registerAllTools(s *server.Server) {
	// Tool 1: list_running_builds
	listBuildsTool := protocol.NewToolWithRawSchema(
		"list_running_builds",
		"获取当前 Agent 上所有正在运行的构建任务信息，用于排查进程阻塞、资源占用等问题",
		[]byte(`{"type":"object","properties":{}}`),
	)
	s.RegisterTool(listBuildsTool, handleListRunningBuilds)

	// Tool 2: get_recent_error_logs
	getLogsTool := protocol.NewToolWithRawSchema(
		"get_recent_error_logs",
		"获取 Agent 近期的错误日志（ERROR/WARN级别），用于分析 Agent 运行异常、构建失败等问题",
		[]byte(`{"type":"object","properties":{"lines":{"type":"integer","description":"返回的最大日志行数，默认100"},"level":{"type":"string","description":"日志级别过滤: error 只返回 ERROR, warn 返回 ERROR+WARN, all 返回所有。默认 error","enum":["error","warn","all"]}}}`),
	)
	s.RegisterTool(getLogsTool, handleGetRecentErrorLogs)

	logs.Infof("mcp server registered 2 tools")
}

// handleListRunningBuilds 处理 list_running_builds 工具调用
func handleListRunningBuilds(_ context.Context, req *protocol.CallToolRequest) (*protocol.CallToolResult, error) {
	builds := job.GBuildManager.GetInstancesWithPid()
	dockerBuilds := job.GBuildDockerManager.GetInstances()

	if len(builds) == 0 && len(dockerBuilds) == 0 {
		return newTextResult("当前没有正在运行的构建任务"), nil
	}

	var sb strings.Builder
	sb.WriteString(fmt.Sprintf("=== 正在运行的构建任务 (%d 个普通构建, %d 个Docker构建) ===\n\n",
		len(builds), len(dockerBuilds)))

	for pid, info := range builds {
		sb.WriteString(fmt.Sprintf("--- 构建 %s ---\n", info.BuildId))
		sb.WriteString(fmt.Sprintf("  项目: %s\n", info.ProjectId))
		sb.WriteString(fmt.Sprintf("  流水线: %s\n", info.PipelineId))
		sb.WriteString(fmt.Sprintf("  VmSeqId: %s\n", info.VmSeqId))
		sb.WriteString(fmt.Sprintf("  PID: %d\n", pid))
		sb.WriteString("\n")
	}

	for _, d := range dockerBuilds {
		sb.WriteString(fmt.Sprintf("--- Docker 构建 %s ---\n", d.BuildId))
		sb.WriteString(fmt.Sprintf("  项目: %s\n", d.ProjectId))
		sb.WriteString(fmt.Sprintf("  VmSeqId: %s\n", d.VmSeqId))
		sb.WriteString("\n")
	}

	return newTextResult(sb.String()), nil
}

// handleGetRecentErrorLogs 处理 get_recent_error_logs 工具调用
func handleGetRecentErrorLogs(_ context.Context, req *protocol.CallToolRequest) (*protocol.CallToolResult, error) {
	maxLines := 100
	level := "error"

	if req.Arguments != nil {
		if v, ok := req.Arguments["lines"]; ok {
			if f, ok := v.(float64); ok {
				maxLines = int(f)
			}
		}
		if v, ok := req.Arguments["level"]; ok {
			if s, ok := v.(string); ok && s != "" {
				level = s
			}
		}
	}

	logFile := filepath.Join(systemutil.GetWorkDir(), "logs", "devopsAgent.log")
	lines, err := tailFile(logFile, maxLines*10)
	if err != nil {
		return newTextResult(fmt.Sprintf("读取日志文件失败: %v", err)), nil
	}

	var filtered []string
	for _, line := range lines {
		if matchLogLevel(line, level) {
			filtered = append(filtered, line)
		}
	}

	if len(filtered) > maxLines {
		filtered = filtered[len(filtered)-maxLines:]
	}

	if len(filtered) == 0 {
		return newTextResult(fmt.Sprintf("近期没有匹配的日志（级别: %s）", level)), nil
	}

	var sb strings.Builder
	sb.WriteString(fmt.Sprintf("=== Agent 近期日志 (级别: %s, 共 %d 条) ===\n", level, len(filtered)))
	sb.WriteString(fmt.Sprintf("日志文件: %s\n", logFile))
	sb.WriteString(fmt.Sprintf("查询时间: %s\n\n", time.Now().Format("2006-01-02 15:04:05")))
	for _, line := range filtered {
		sb.WriteString(line)
		sb.WriteString("\n")
	}
	return newTextResult(sb.String()), nil
}

// newTextResult 创建文本类型的 MCP 工具结果
func newTextResult(text string) *protocol.CallToolResult {
	return &protocol.CallToolResult{
		Content: []protocol.Content{
			&protocol.TextContent{
				Type: "text",
				Text: text,
			},
		},
	}
}

// matchLogLevel 判断日志行是否匹配指定级别
func matchLogLevel(line string, level string) bool {
	switch level {
	case "error":
		return strings.Contains(line, "|error|") || strings.Contains(line, "|fatal|")
	case "warn":
		return strings.Contains(line, "|error|") || strings.Contains(line, "|fatal|") ||
			strings.Contains(line, "|warning|")
	case "all":
		return true
	default:
		return strings.Contains(line, "|error|") || strings.Contains(line, "|fatal|")
	}
}

// tailFile 读取文件最后 n 行
func tailFile(filePath string, n int) ([]string, error) {
	f, err := os.Open(filePath)
	if err != nil {
		return nil, err
	}
	defer f.Close()

	var lines []string
	scanner := bufio.NewScanner(f)
	buf := make([]byte, 0, 64*1024)
	scanner.Buffer(buf, 1024*1024)

	for scanner.Scan() {
		lines = append(lines, scanner.Text())
	}

	if len(lines) > n {
		lines = lines[len(lines)-n:]
	}
	return lines, scanner.Err()
}

// RunningBuildInfo 供 MCP tool 使用的构建信息视图
type RunningBuildInfo struct {
	BuildId    string
	ProjectId  string
	PipelineId string
	VmSeqId    string
	Pid        int
}

// toRunningBuildInfo 从 API 类型转换
func toRunningBuildInfo(pid int, info *api.ThirdPartyBuildInfo) RunningBuildInfo {
	return RunningBuildInfo{
		BuildId:    info.BuildId,
		ProjectId:  info.ProjectId,
		PipelineId: info.PipelineId,
		VmSeqId:    info.VmSeqId,
		Pid:        pid,
	}
}
