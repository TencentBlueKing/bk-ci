//go:build linux && !loong64
// +build linux,!loong64

package monitor

import (
	"bytes"
	"os"
	"strconv"
	"strings"

	"github.com/pkg/errors"
)

// input_kernel_linux.go 承担 telegraf plugins/inputs/kernel/kernel_linux.go
// 的等价职责：读取 /proc/stat 与 /proc/sys/kernel/random/entropy_avail，
// 产出 interrupts / context_switches / processes_forked / boot_time /
// disk_pages_in / disk_pages_out / entropy_avail 字段。
//
// 字段名一一对应 telegraf kernel_linux.go 中的 fields[...] key，因此后端
// InfluxDB 看板字段可无缝复用。
//
// 文件路径通过包级变量暴露，单测可覆盖以注入 fake /proc 内容（不需要改
// defaultLinuxExtraFn 的签名）。

var (
	procStatPath         = "/proc/stat"
	procEntropyAvailPath = "/proc/sys/kernel/random/entropy_avail"

	// 对齐 telegraf kernel_linux.go 的 line prefix
	kStatPrefixIntr     = []byte("intr")
	kStatPrefixCtxt     = []byte("ctxt")
	kStatPrefixProcs    = []byte("processes")
	kStatPrefixPage     = []byte("page")
	kStatPrefixBootTime = []byte("btime")
)

// defaultLinuxExtraFn 是默认 Linux 实现；非 Linux 平台同名变量为 nil
// （见 input_kernel_other.go）。
var defaultLinuxExtraFn = readProcStatAndEntropy

// readProcStatAndEntropy 解析 /proc/stat + entropy_avail，返回待合并到 env
// measurement 的 fields。任一文件读取失败直接返回错误；调用方 Kernel.Gather
// 会把 error 降级为 "丢弃 Linux 专属字段、仅保留 uptime"。
func readProcStatAndEntropy() (map[string]interface{}, error) {
	statData, err := os.ReadFile(procStatPath)
	if err != nil {
		return nil, errors.Wrap(err, "kernel: read /proc/stat")
	}
	entropyRaw, err := os.ReadFile(procEntropyAvailPath)
	if err != nil {
		return nil, errors.Wrap(err, "kernel: read entropy_avail")
	}
	entropy, err := strconv.ParseInt(strings.TrimSpace(string(entropyRaw)), 10, 64)
	if err != nil {
		return nil, errors.Wrap(err, "kernel: parse entropy_avail")
	}

	fields := map[string]interface{}{
		FieldEntropyAvail: entropy,
	}

	// /proc/stat 每行按空白分割；我们只关心 5 个前缀。解析失败时跳过该前缀
	// 而非整体报错——/proc/stat 在不同内核版本字段数可能微调，个别行解析
	// 失败不应影响其他有效字段。
	tokens := bytes.Fields(statData)
	for i, tok := range tokens {
		switch {
		case bytes.Equal(tok, kStatPrefixIntr):
			if v, ok := parseInt64At(tokens, i+1); ok {
				fields[FieldInterrupts] = v
			}
		case bytes.Equal(tok, kStatPrefixCtxt):
			if v, ok := parseInt64At(tokens, i+1); ok {
				fields[FieldContextSwitches] = v
			}
		case bytes.Equal(tok, kStatPrefixProcs):
			if v, ok := parseInt64At(tokens, i+1); ok {
				fields[RenamedFieldProcs] = v
			}
		case bytes.Equal(tok, kStatPrefixBootTime):
			if v, ok := parseInt64At(tokens, i+1); ok {
				fields[FieldBootTime] = v
			}
		case bytes.Equal(tok, kStatPrefixPage):
			if v, ok := parseInt64At(tokens, i+1); ok {
				fields[FieldDiskPagesIn] = v
			}
			if v, ok := parseInt64At(tokens, i+2); ok {
				fields[FieldDiskPagesOut] = v
			}
		}
	}
	return fields, nil
}

func parseInt64At(tokens [][]byte, i int) (int64, bool) {
	if i < 0 || i >= len(tokens) {
		return 0, false
	}
	v, err := strconv.ParseInt(string(tokens[i]), 10, 64)
	if err != nil {
		return 0, false
	}
	return v, true
}
