//go:build windows
// +build windows

package monitor

import "testing"

// TestComputeRate_Basic 覆盖 computeRate 的正常速率 / counter reset / 非法 dt
// 三个关键分支。对齐 plan 6.1 的 TestComputeRate_Basic 条目。
func TestComputeRate_Basic(t *testing.T) {
	cases := []struct {
		name    string
		cur     uint64
		prev    uint64
		dtSec   float64
		wantVal float64
		wantOK  bool
	}{
		{"normal 60KB/s", 61000, 1000, 1.0, 60000.0, true},
		{"one-second MB/s", 1_000_000 + 500_000, 1_000_000, 1.0, 500_000.0, true},
		{"fractional dt", 2000, 1000, 0.5, 2000.0, true},
		{"zero delta", 1000, 1000, 1.0, 0.0, true},
		{"counter reset cur<prev", 500, 1000, 1.0, 0, false},
		{"dt zero", 2000, 1000, 0, 0, false},
		{"dt negative", 2000, 1000, -0.1, 0, false},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			got, ok := computeRate(tc.cur, tc.prev, tc.dtSec)
			if ok != tc.wantOK {
				t.Fatalf("ok = %v, want %v", ok, tc.wantOK)
			}
			if ok && got != tc.wantVal {
				t.Fatalf("rate = %v, want %v", got, tc.wantVal)
			}
		})
	}
}

// TestRateSampleInterval_MatchesPDH 守护常量，避免后续重构误把 1s 改成
// Gather 周期或其他值——plan 2.1 明确要求分母对齐 PDH 默认 1s。
func TestRateSampleInterval_MatchesPDH(t *testing.T) {
	if rateSampleInterval.Seconds() != 1.0 {
		t.Fatalf("rateSampleInterval = %v, want 1s (aligned with PDH CounterRefreshInterval)", rateSampleInterval)
	}
}
