package monitor

import "testing"

func TestShouldSkipDiskIO(t *testing.T) {
	cases := []struct {
		name string
		skip bool
	}{
		// Linux 物理盘 / 分区 → 保留
		{"sda", false},
		{"sda1", false},
		{"sdb2", false},
		{"nvme0n1", false},
		{"nvme0n1p1", false},
		{"vda", false},
		{"vda1", false},
		{"vdb", false},
		// Linux 需过滤
		{"loop0", true},
		{"loop123", true},
		{"ram0", true},
		{"sr0", true},
		{"md0", true},
		{"md127", true},
		{"fd0", true},
		{"nbd0", true},
		{"zram0", true},
		{"dm-0", true},
		{"dm-123", true},
		// macOS
		{"disk0", false},
		// Windows PDH 实例名不含任何黑名单前缀
		{"0 C:", false},
		{"1 D:", false},
		{"_Total", false},
	}
	for _, tc := range cases {
		tc := tc
		t.Run(tc.name, func(t *testing.T) {
			if got := shouldSkipDiskIO(tc.name); got != tc.skip {
				t.Errorf("shouldSkipDiskIO(%q) = %v, want %v", tc.name, got, tc.skip)
			}
		})
	}
}
