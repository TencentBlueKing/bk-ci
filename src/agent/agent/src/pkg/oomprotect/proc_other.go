//go:build !linux

package oomprotect

import "fmt"

// 其他平台保留编译入口，但显式拒绝把 Linux OOM 功能误判为可用。
func Score(pid int) (int, error)    { return 0, fmt.Errorf("OOM protection requires Linux") }
func SetScore(pid, score int) error { return fmt.Errorf("OOM protection requires Linux") }
func syncDir(path string) error     { return nil }
