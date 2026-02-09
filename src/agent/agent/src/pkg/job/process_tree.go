//go:build linux || darwin
// +build linux darwin

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

package job

import (
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/shirou/gopsutil/v3/process"
)

const processTreeLogInterval = 30 * time.Second

// logProcessTree 定时采集构建进程的进程树并上报到后台日志（DEBUG级别），直到 ctx 被取消
func logProcessTree(ctx context.Context, buildInfo *api.ThirdPartyBuildInfo, pid int) {
	logs.Infof("build[%s] start process tree monitor for pid %d, interval %s", buildInfo.BuildId, pid, processTreeLogInterval)
	ticker := time.NewTicker(processTreeLogInterval)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			logs.Infof("build[%s] process tree monitor stopped for pid %d", buildInfo.BuildId, pid)
			return
		case <-ticker.C:
			tree := getProcessTree(int32(pid), 0)
			if tree == "" {
				continue
			}
			message := fmt.Sprintf("process tree for pid %d:\n%s", pid, tree)
			postProcessTreeLog(message, buildInfo)
		}
	}
}

// postProcessTreeLog 将进程树信息以 DEBUG 级别上报到后台日志
func postProcessTreeLog(message string, buildInfo *api.ThirdPartyBuildInfo) {
	taskId := "startVM-" + buildInfo.VmSeqId
	logMessage := &api.LogMessage{
		Message:      message,
		Timestamp:    time.Now().UnixMilli(),
		Tag:          taskId,
		JobId:        buildInfo.ContainerHashId,
		LogType:      api.LogtypeDebug,
		ExecuteCount: buildInfo.ExecuteCount,
		SubTag:       nil,
	}
	if _, err := api.AddLogLine(buildInfo.BuildId, logMessage, buildInfo.VmSeqId); err != nil {
		logs.Warnf("build[%s] post process tree log error: %v", buildInfo.BuildId, err)
	}
}

// GetProcessTreeText 递归获取指定进程及其子进程的进程树文本（导出供 MCP 等模块调用）
func GetProcessTreeText(pid int32, indent int) string {
	return getProcessTree(pid, indent)
}

// getProcessTree 递归获取指定进程及其子进程的进程树
func getProcessTree(pid int32, indent int) string {
	p, err := process.NewProcess(pid)
	if err != nil {
		return ""
	}

	var sb strings.Builder
	prefix := strings.Repeat("  ", indent)

	name, _ := p.Name()
	cmdline, _ := p.Cmdline()
	status, _ := p.Status()
	createTime, _ := p.CreateTime()

	var runDuration string
	if createTime > 0 {
		dur := time.Since(time.UnixMilli(createTime))
		runDuration = formatDuration(dur)
	}

	statusStr := strings.Join(status, ",")

	sb.WriteString(fmt.Sprintf("%s├─ pid=%d name=%s status=[%s] running=%s\n",
		prefix, pid, name, statusStr, runDuration))
	if cmdline != "" {
		if len(cmdline) > 200 {
			cmdline = cmdline[:200] + "..."
		}
		sb.WriteString(fmt.Sprintf("%s│  cmd: %s\n", prefix, cmdline))
	}

	children, err := p.Children()
	if err != nil {
		return sb.String()
	}
	for _, child := range children {
		childTree := getProcessTree(child.Pid, indent+1)
		if childTree != "" {
			sb.WriteString(childTree)
		}
	}

	return sb.String()
}

// formatDuration 将 Duration 格式化为易读的字符串
func formatDuration(d time.Duration) string {
	if d < time.Minute {
		return fmt.Sprintf("%ds", int(d.Seconds()))
	}
	if d < time.Hour {
		return fmt.Sprintf("%dm%ds", int(d.Minutes()), int(d.Seconds())%60)
	}
	return fmt.Sprintf("%dh%dm%ds", int(d.Hours()), int(d.Minutes())%60, int(d.Seconds())%60)
}
