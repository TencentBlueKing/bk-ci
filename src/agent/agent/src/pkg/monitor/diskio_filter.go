package monitor

import "strings"

// diskio_filter.go 按前缀黑名单过滤不反映真实物理磁盘 IO 的设备。
//
// 设计取舍与 net_filter.go 一致：硬编码黑名单、前缀匹配、跨平台共享。
//
//   - Linux：loop*（loopback 镜像挂载，例如 snap 应用每个 snap 一个 loop
//     设备，IO 数据反映的是 squashfs 只读镜像不具备监控价值）；ram*
//     （ramdisk）；sr*（光驱）；md*（软 RAID，由后端按物理盘汇总更准确）；
//     fd*（软驱）；nbd*（Network Block Device）；zram*（压缩内存盘）；
//     dm-*（device mapper，常见于 LVM，数值与底层物理磁盘重复）
//   - macOS：gopsutil 默认仅返回 disk0（物理盘），无需额外过滤
//   - Windows：PhysicalDisk 实例通常只有 "0 C:"、"1 D:"，无需过滤
//
// 保留 sda/sdb/nvme0n1/vda/vdb 等物理盘与分区（vda1/sda2 等），telegraf 默
// 认行为同样报告分区。
var defaultDiskIOSkipPrefixes = []string{
	"loop", // loop0 / loop1 ... snap 挂载镜像
	"ram",  // ram0 / ram1 ramdisk
	"sr",   // sr0 CD/DVD
	"md",   // mdX 软 RAID（由底层物理盘指标覆盖）
	"fd",   // fd0 floppy
	"nbd",  // nbd0 Network Block Device
	"zram", // zram0 压缩 swap
	"dm-",  // dm-N device mapper / LVM / dm-crypt（与底层磁盘重复）
}

// shouldSkipDiskIO 返回 true 表示该磁盘名应从 diskio 采集中剔除。
// 大小写敏感——Linux 的块设备名本身就是小写，Windows 的 PDH 实例名
// 如 "0 C:" 也不会命中任何前缀。
func shouldSkipDiskIO(name string) bool {
	lower := strings.ToLower(name)
	for _, p := range defaultDiskIOSkipPrefixes {
		if strings.HasPrefix(lower, p) {
			return true
		}
	}
	return false
}
