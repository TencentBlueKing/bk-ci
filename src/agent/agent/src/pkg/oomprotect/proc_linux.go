//go:build linux

package oomprotect

import (
	"fmt"
	"os"
	"strconv"
	"strings"
)

// Score 读取内核实际值，不能仅凭写入没有报错就认为保护已经生效。
func Score(pid int) (int, error) {
	b, err := os.ReadFile(fmt.Sprintf("/proc/%d/oom_score_adj", pid))
	if err != nil {
		return 0, err
	}
	return strconv.Atoi(strings.TrimSpace(string(b)))
}

func SetScore(pid, score int) error {
	if pid <= 0 || score < -1000 || score > 1000 {
		return fmt.Errorf("invalid OOM score target")
	}
	if err := os.WriteFile(fmt.Sprintf("/proc/%d/oom_score_adj", pid), []byte(strconv.Itoa(score)), 0600); err != nil {
		return fmt.Errorf("set OOM score for pid %d: %w (root/CAP_SYS_RESOURCE required to lower score)", pid, err)
	}
	actual, err := Score(pid)
	if err != nil {
		return err
	}
	if actual != score {
		return fmt.Errorf("pid %d OOM score: want %d, got %d", pid, score, actual)
	}
	return nil
}

func syncDir(path string) error {
	f, err := os.Open(path)
	if err != nil {
		return err
	}
	defer f.Close()
	return f.Sync()
}
