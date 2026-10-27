//go:build linux

package oomprotect

import (
	"fmt"
	"os"
	"os/exec"
	"strings"
	"testing"
)

// 在独立测试子进程中改变分数，不给 go test 主进程或机器上的其他进程加免杀保护。
// 无 CAP_SYS_RESOURCE 时跳过真实负分数测试，其余配置测试照常运行。
func TestLinuxProtectionInheritance(t *testing.T) {
	if os.Getenv("BK_CI_OOM_TEST_CHILD") == "1" {
		if err := SetScore(os.Getpid(), -1000); err != nil {
			fmt.Fprintln(os.Stderr, err)
			os.Exit(77)
		}
		// 旧 worker 无降级逻辑，允许整个用户进程树继承 -1000。
		out, err := exec.Command("/bin/sh", "-c", "cat /proc/self/oom_score_adj; /bin/sh -c 'cat /proc/self/oom_score_adj'").CombinedOutput()
		if err != nil || strings.TrimSpace(string(out)) != "-1000\n-1000" {
			os.Exit(2)
		}
		// 与 JVM 启动器同样只在 fork 后的子进程解除保护，孙进程继续继承 0。
		state.enabled = true
		program, args := UserCommand("/bin/sh", []string{"-c", "cat /proc/self/oom_score_adj"})
		out, err = exec.Command(program, args...).CombinedOutput()
		if err != nil || strings.TrimSpace(string(out)) != "0" {
			os.Exit(3)
		}
		score, err := Score(os.Getpid())
		if err != nil || score != -1000 {
			os.Exit(4)
		}
		os.Exit(0)
	}
	before, err := Score(os.Getpid())
	if err != nil {
		t.Fatal(err)
	}
	cmd := exec.Command(os.Args[0], "-test.run=^TestLinuxProtectionInheritance$")
	cmd.Env = append(os.Environ(), "BK_CI_OOM_TEST_CHILD=1")
	out, err := cmd.CombinedOutput()
	if exit, ok := err.(*exec.ExitError); ok && exit.ExitCode() == 77 {
		t.Skipf("CAP_SYS_RESOURCE unavailable: %s", out)
	}
	if err != nil {
		t.Fatalf("inheritance check: %v: %s", err, out)
	}
	after, err := Score(os.Getpid())
	if err != nil || after != before {
		t.Fatalf("parent score changed: %d -> %d (%v)", before, after, err)
	}
}
