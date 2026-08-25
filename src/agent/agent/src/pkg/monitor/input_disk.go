//go:build !loong64
// +build !loong64

package monitor

import (
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v4/disk"
)

// defaultDiskIgnoreFS 与 telegrafConf 中 inputs.disk.ignore_fs 完全一致。
// 这些文件系统要么是虚拟的（tmpfs / devtmpfs / devfs）要么是容器层叠
// （overlay / aufs / squashfs），上报容量没有实际意义。
var defaultDiskIgnoreFS = map[string]struct{}{
	"tmpfs":    {},
	"devtmpfs": {},
	"devfs":    {},
	"iso9660":  {}, // 光驱 / 镜像挂载，对齐 telegraf 默认 ignore_fs
	"overlay":  {},
	"aufs":     {},
	"squashfs": {},
	"nullfs":   {}, // macOS App Translocation / firmlinks 挂载到 /private/var/folders/...
}

// networkFSPrefixes 网络 / 可能因后端不可达而挂死的文件系统前缀。
//
// 关键：这类挂载点一旦后端不可达（NFS server 宕机、网络分区、SMB 掉线），
// 底层 statfs/statvfs syscall 会进入不可中断睡眠（D 状态）永不返回，连
// SIGKILL 都杀不掉——这正是 monitor input goroutine 累积卡死（触发
// inflightHardCap 告警）的根因。Go 层无法打断已进入内核的 syscall，
// 唯一可靠的办法是「根本不对它们发起 statfs」。
//
// 对齐 telegraf「ignore_fs 从源头绕开」的思路；telegraf 靠运维手动配置
// ignore_fs 排除网络盘，我们无配置入口，故在代码里默认跳过。
//
// 用「前缀匹配」而非精确匹配，覆盖 fuse.sshfs / fuse.s3fs 等 fuse 变体
// 以及 nfs / nfs4 等版本后缀。
var networkFSPrefixes = []string{
	"nfs",   // nfs, nfs3, nfs4
	"cifs",  // Windows / Samba 共享
	"smbfs", // macOS SMB
	"smb",   // smb2/smb3 变体
	"afpfs", // macOS AFP
	"afp",
	"fuse",     // fuse.sshfs / fuse.s3fs / fuse.glusterfs 等，后端多为网络
	"9p",       // Plan9 / 虚拟机共享目录
	"glusterfs",
	"ceph",
	"webdav",
	"davfs",
}

// isNetworkFS 判断 fstype 是否属于「可能挂死」的网络文件系统。
// 命中则 Gather 会跳过该挂载点，不发起 statfs。
func isNetworkFS(fstype string) bool {
	fs := strings.ToLower(fstype)
	for _, p := range networkFSPrefixes {
		if strings.HasPrefix(fs, p) {
			return true
		}
	}
	return false
}

// Disk 对齐 telegraf plugins/inputs/disk。每个 physical mountpoint 产出
// 一条 metric，tag 含 device / fstype / path / mode。
type Disk struct {
	partitionsFn func(all bool) ([]disk.PartitionStat, error)
	usageFn      func(path string) (*disk.UsageStat, error)
	nowFn        func() time.Time

	// IgnoreFS 为 nil 时使用 defaultDiskIgnoreFS。
	IgnoreFS map[string]struct{}
}

// NewDisk 返回默认 disk 采集器。
func NewDisk() *Disk {
	return &Disk{
		partitionsFn: disk.Partitions,
		usageFn:      disk.Usage,
		nowFn:        time.Now,
	}
}

// Name 返回 measurement 名 "disk"。
func (d *Disk) Name() string { return MeasurementDisk }

// Gather 遍历所有 partition，跳过 ignore_fs 列表中的文件系统后调用 Usage
// 取容量/inode 信息。
//
// 卡死防护：网络文件系统（nfs/cifs/fuse 等）后端不可达时 statfs 会进入
// 不可中断睡眠永不返回，是 monitor goroutine 累积卡死的根因。因此在调用
// Usage 前先按 fstype 跳过这类挂载点（isNetworkFS），从源头避免阻塞
// syscall —— 对齐 telegraf「ignore_fs 从源头绕开」的思路。
//
// 对于本地盘的 Usage 失败（权限等），记录跳过不影响其他挂载点，对齐
// telegraf 的降级行为。
func (d *Disk) Gather() ([]Metric, error) {
	parts, err := d.partitionsFn(false)
	if err != nil {
		return nil, errors.Wrap(err, "disk: Partitions failed")
	}
	ignore := d.IgnoreFS
	if ignore == nil {
		ignore = defaultDiskIgnoreFS
	}

	now := d.nowFn()
	out := make([]Metric, 0, len(parts))
	for _, p := range parts {
		if _, skip := ignore[p.Fstype]; skip {
			continue
		}
		// 网络文件系统一旦后端挂死，statfs 会永久阻塞（D 状态，SIGKILL
		// 都杀不掉）。绝不对它们发起 Usage，直接跳过。
		if isNetworkFS(p.Fstype) {
			continue
		}
		usage, uerr := d.usageFn(p.Mountpoint)
		if uerr != nil || usage == nil {
			// 本地挂载点不可读（权限等）跳过，不影响其他挂载点上报
			continue
		}
		if usage.Total == 0 {
			continue
		}

		// macOS APFS: gopsutil Statfs 不含 purgeable space（Time Machine 快照、
		// iCloud 缓存等），导致 free 偏低、used_percent 虚高。用 Foundation API
		// 获取与"系统信息"一致的可用空间来覆盖。非 darwin 或 CGO 禁用时为 no-op。
		free := usage.Free
		used := usage.Used
		usedPercent := usage.UsedPercent
		if avail, ok := darwinAvailableCapacity(p.Mountpoint); ok && avail > 0 {
			free = avail
			if usage.Total > avail {
				used = usage.Total - avail
			} else {
				used = 0
			}
			if usage.Total > 0 {
				usedPercent = 100 * float64(used) / float64(usage.Total)
			}
		}

		fields := map[string]interface{}{
			FieldTotal:        usage.Total,
			FieldFree:         free,
			FieldUsed:         used,
			RenamedFieldInUse: usedPercent,
			FieldInodesTotal:  usage.InodesTotal,
			FieldInodesFree:   usage.InodesFree,
			FieldInodesUsed:   usage.InodesUsed,
		}
		if usage.InodesTotal > 0 {
			fields[FieldInodesUsedPercent] = usage.InodesUsedPercent
		}

		tags := map[string]string{
			TagDevice: trimDevicePrefix(p.Device),
			TagFstype: p.Fstype,
			TagPath:   normalizeDiskPathTag(p.Mountpoint),
		}
		if p.Opts != nil {
			// telegraf 把 opts 的第一个元素作为 mode（ro/rw），这里保持同样的风格
			if len(p.Opts) > 0 {
				tags[TagMode] = p.Opts[0]
			}
		}
		out = append(out, Metric{
			Name:      MeasurementDisk,
			Tags:      tags,
			Fields:    fields,
			Timestamp: now,
		})
	}
	return out, nil
}

// trimDevicePrefix 将 gopsutil 返回的 "/dev/sda1" / "/dev/disk3s1s1" 形式
// 归一化为 telegraf inputs.disk 的裸设备名（"sda1" / "disk3s1s1"）。
// 仅去除 "/dev/" 前缀；其它形式（Windows 的 "C:\" / nullfs 挂载的
// "/Applications/xxx.app/Wrapper" / 已经是裸名的）原样返回，避免误伤。
func trimDevicePrefix(dev string) string {
	return strings.TrimPrefix(dev, "/dev/")
}
