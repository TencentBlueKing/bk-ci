//go:build windows
// +build windows

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

package main

import (
	"os"
	"testing"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"golang.org/x/sys/windows"
)

func TestMain(m *testing.M) {
	logs.UNTestDebugInit()
	os.Exit(m.Run())
}

// isRunningAsSystem reports whether the current process token belongs to
// the Local System account (S-1-5-18). Most WTS token APIs require this.
func isRunningAsSystem() bool {
	var token windows.Token
	err := windows.OpenProcessToken(windows.CurrentProcess(), windows.TOKEN_QUERY, &token)
	if err != nil {
		return false
	}
	defer token.Close()

	user, err := token.GetTokenUser()
	if err != nil {
		return false
	}

	systemSid, err := windows.StringToSid("S-1-5-18")
	if err != nil {
		return false
	}

	return windows.EqualSid(user.User.Sid, systemSid)
}

func TestEnumerateSessions(t *testing.T) {
	sessions, err := enumerateSessions()
	if err != nil {
		t.Fatalf("enumerateSessions: %v", err)
	}
	if len(sessions) == 0 {
		t.Fatal("expected at least one WTS session")
	}

	t.Logf("found %d session(s):", len(sessions))
	for i, s := range sessions {
		t.Logf("  [%d] SessionID=%d State=%d", i, s.SessionID, s.State)
	}
}

func TestEnumerateSessions_ContainsListenOrActive(t *testing.T) {
	sessions, err := enumerateSessions()
	if err != nil {
		t.Fatalf("enumerateSessions: %v", err)
	}

	hasListenOrActive := false
	for _, s := range sessions {
		if s.State == wtsActive || s.State == wtsListen || s.State == wtsConnected {
			hasListenOrActive = true
			break
		}
	}
	if !hasListenOrActive {
		t.Log("WARNING: no Active/Connected/Listen session found; this may be expected in headless CI")
	}
}

func TestGetActiveSessionID(t *testing.T) {
	sessionID, err := GetActiveSessionID()
	if err != nil {
		t.Fatalf("GetActiveSessionID: %v", err)
	}
	t.Logf("active session ID: %d", sessionID)
}

func TestSessionProcessInfoClose_ZeroHandles(t *testing.T) {
	info := &SessionProcessInfo{
		PID:           12345,
		ProcessHandle: 0,
		ThreadHandle:  0,
	}
	info.Close()
	if info.ThreadHandle != 0 {
		t.Fatal("ThreadHandle should remain 0 after Close on zero-valued handle")
	}
}

func TestSessionProcessInfoClose_RealHandle(t *testing.T) {
	handle, err := windows.CreateEvent(nil, 0, 0, nil)
	if err != nil {
		t.Fatalf("CreateEvent: %v", err)
	}

	info := &SessionProcessInfo{
		PID:           0,
		ProcessHandle: 0,
		ThreadHandle:  handle,
	}
	info.Close()

	if info.ThreadHandle != 0 {
		t.Fatal("ThreadHandle should be zeroed after Close")
	}
}

func TestSessionProcessInfoClose_Idempotent(t *testing.T) {
	handle, err := windows.CreateEvent(nil, 0, 0, nil)
	if err != nil {
		t.Fatalf("CreateEvent: %v", err)
	}

	info := &SessionProcessInfo{ThreadHandle: handle}
	info.Close()
	info.Close()

	if info.ThreadHandle != 0 {
		t.Fatal("ThreadHandle should be 0 after double Close")
	}
}

func TestDuplicateUserToken(t *testing.T) {
	if !isRunningAsSystem() {
		t.Skip("requires SYSTEM privileges (run as Windows service)")
	}

	sessionID, err := GetActiveSessionID()
	if err != nil {
		t.Fatalf("GetActiveSessionID: %v", err)
	}

	token, err := duplicateUserToken(sessionID)
	if err != nil {
		t.Fatalf("duplicateUserToken(session=%d): %v", sessionID, err)
	}
	defer token.Close()

	t.Logf("duplicated primary token for session %d", sessionID)
}

func TestStartProcessAsUser(t *testing.T) {
	if !isRunningAsSystem() {
		t.Skip("requires SYSTEM privileges (run as Windows service)")
	}

	appPath := `C:\Windows\System32\cmd.exe`
	cmdLine := `"C:\Windows\System32\cmd.exe" /c whoami`
	workDir := `C:\Windows\System32`

	proc, err := StartProcessAsUser(appPath, cmdLine, workDir)
	if err != nil {
		t.Fatalf("StartProcessAsUser: %v", err)
	}
	defer func() {
		windows.CloseHandle(proc.ProcessHandle)
		proc.Close()
	}()

	if proc.PID == 0 {
		t.Fatal("expected non-zero PID")
	}
	t.Logf("launched process PID=%d", proc.PID)

	event, err := windows.WaitForSingleObject(proc.ProcessHandle, 10_000)
	if err != nil {
		t.Fatalf("WaitForSingleObject: %v", err)
	}
	if event != windows.WAIT_OBJECT_0 {
		windows.TerminateProcess(proc.ProcessHandle, 1)
		t.Fatalf("process did not exit within 10s, wait result=%d", event)
	}

	var exitCode uint32
	if err := windows.GetExitCodeProcess(proc.ProcessHandle, &exitCode); err != nil {
		t.Fatalf("GetExitCodeProcess: %v", err)
	}
	t.Logf("process exited with code %d", exitCode)
	if exitCode != 0 {
		t.Errorf("expected exit code 0, got %d", exitCode)
	}
}

func TestStartProcessAsUser_NoPrivileges(t *testing.T) {
	if isRunningAsSystem() {
		t.Skip("this test verifies graceful failure without SYSTEM privileges")
	}

	appPath := `C:\Windows\System32\cmd.exe`
	cmdLine := `"C:\Windows\System32\cmd.exe" /c echo hello`
	workDir := `C:\Windows\System32`

	_, err := StartProcessAsUser(appPath, cmdLine, workDir)
	if err == nil {
		t.Fatal("expected error when not running as SYSTEM, but got nil")
	}
	t.Logf("expected error: %v", err)
}

func TestStartProcessAsUser_LongRunningProcess(t *testing.T) {
	if !isRunningAsSystem() {
		t.Skip("requires SYSTEM privileges (run as Windows service)")
	}

	appPath := `C:\Windows\System32\cmd.exe`
	cmdLine := `"C:\Windows\System32\cmd.exe" /c timeout /t 2 /nobreak >NUL`
	workDir := `C:\Windows\System32`

	proc, err := StartProcessAsUser(appPath, cmdLine, workDir)
	if err != nil {
		t.Fatalf("StartProcessAsUser: %v", err)
	}
	defer func() {
		windows.CloseHandle(proc.ProcessHandle)
		proc.Close()
	}()

	t.Logf("launched long-running process PID=%d", proc.PID)

	event, err := windows.WaitForSingleObject(proc.ProcessHandle, 30_000)
	if err != nil {
		t.Fatalf("WaitForSingleObject: %v", err)
	}
	if event != windows.WAIT_OBJECT_0 {
		windows.TerminateProcess(proc.ProcessHandle, 1)
		t.Fatalf("process did not exit within 30s, wait result=%d", event)
	}

	var exitCode uint32
	if err := windows.GetExitCodeProcess(proc.ProcessHandle, &exitCode); err != nil {
		t.Fatalf("GetExitCodeProcess: %v", err)
	}
	t.Logf("long-running process exited with code %d", exitCode)
}

func TestStartProcessAsUser_InvalidPath(t *testing.T) {
	if !isRunningAsSystem() {
		t.Skip("requires SYSTEM privileges (run as Windows service)")
	}

	_, err := StartProcessAsUser(
		`C:\NonExistent\fake.exe`,
		`"C:\NonExistent\fake.exe"`,
		`C:\Windows\System32`,
	)
	if err == nil {
		t.Fatal("expected error for invalid executable path, got nil")
	}
	t.Logf("expected error for invalid path: %v", err)
}
