//go:build !loong64
// +build !loong64

package monitor

// names.go 统一维护所有 measurement / field / tag 名称常量，避免在 input、
// rename、reporter 等多处散落 "cpu"、"used_percent" 之类的魔法字符串。
//
// 命名约定：
//   - Measurement*    —— input 直接产出的 measurement 名（与 telegraf plugin 对齐）
//   - Field*          —— input 直接产出的字段名（与 gopsutil / telegraf 字段对齐）
//   - Tag*            —— metric tag 键（host、interface、cpu、device、path 等）
//   - Renamed*Measurement / Renamed*Field —— 经 rename 层处理后的最终名，也就
//     是实际上报给 BK-CI 后端的名字
//
// 修改任一常量前，请同步确认：
//   1. rename.go 的映射表
//   2. collector/telegrafConf 里 telegraf 对应配置
// 两者必须一起变，否则 collector 与 monitor 上报会出现同义字段双份。

// ------------------------------------------------------------
// Measurement 名（telegraf plugin 原生 / input 直接产出）
// ------------------------------------------------------------

const (
	MeasurementCPU       = "cpu"
	MeasurementMem       = "mem"
	MeasurementDisk      = "disk"
	MeasurementDiskIO    = "diskio"
	MeasurementNet       = "net"
	MeasurementNetstat   = "netstat"
	MeasurementSwap      = "swap"
	MeasurementSystem    = "system"
	MeasurementKernel    = "kernel"
	MeasurementProcesses = "processes"
)

// ------------------------------------------------------------
// Measurement 重命名后的名字（reporter 最终输出）
// ------------------------------------------------------------

const (
	RenamedCPUDetail = "cpu_detail"
	RenamedIO        = "io"
	RenamedLoad      = "load"
	RenamedEnv       = "env"
)

// ------------------------------------------------------------
// Field 名（input 产出的原生字段，对齐 gopsutil / telegraf）
// ------------------------------------------------------------

const (
	// cpu
	FieldUsageUser      = "usage_user"
	FieldUsageSystem    = "usage_system"
	FieldUsageIdle      = "usage_idle"
	FieldUsageIowait    = "usage_iowait"
	FieldUsageNice      = "usage_nice"
	FieldUsageIrq       = "usage_irq"
	FieldUsageSoftirq   = "usage_softirq"
	FieldUsageSteal     = "usage_steal"
	FieldUsageGuest     = "usage_guest"
	FieldUsageGuestNice = "usage_guest_nice"

	// mem / swap
	FieldTotal            = "total"
	FieldAvailable        = "available"
	FieldUsed             = "used"
	FieldFree             = "free"
	FieldUsedPercent      = "used_percent"
	FieldBuffered         = "buffered"
	FieldCached           = "cached"
	FieldActive           = "active"
	FieldInactive         = "inactive"
	FieldSlab             = "slab"
	FieldWired            = "wired"
	FieldShared           = "shared"
	FieldAvailablePercent = "available_percent"
	FieldSwapIn           = "in"
	FieldSwapOut          = "out"

	// disk
	FieldInodesTotal       = "inodes_total"
	FieldInodesUsed        = "inodes_used"
	FieldInodesFree        = "inodes_free"
	FieldInodesUsedPercent = "inodes_used_percent"

	// diskio
	FieldReadBytes       = "read_bytes"
	FieldWriteBytes      = "write_bytes"
	FieldReads           = "reads"
	FieldWrites          = "writes"
	FieldReadTime        = "read_time"
	FieldWriteTime       = "write_time"
	FieldIOTime          = "io_time"
	FieldWeightedIOTime  = "weighted_io_time"
	FieldIOPSInProgress  = "iops_in_progress"
	FieldMergedReads     = "merged_reads"
	FieldMergedWrites    = "merged_writes"

	// net
	FieldBytesSent   = "bytes_sent"
	FieldBytesRecv   = "bytes_recv"
	FieldPacketsSent = "packets_sent"
	FieldPacketsRecv = "packets_recv"
	FieldErrIn       = "err_in"
	FieldErrOut      = "err_out"
	FieldDropIn      = "drop_in"
	FieldDropOut     = "drop_out"

	// netstat
	FieldTCPEstablished = "tcp_established"
	FieldTCPSynSent     = "tcp_syn_sent"
	FieldTCPSynRecv     = "tcp_syn_recv"
	FieldTCPFinWait1    = "tcp_fin_wait1"
	FieldTCPFinWait2    = "tcp_fin_wait2"
	FieldTCPTimeWait    = "tcp_time_wait"
	FieldTCPClose       = "tcp_close"
	FieldTCPCloseWait   = "tcp_close_wait"
	FieldTCPLastAck     = "tcp_last_ack"
	FieldTCPListen      = "tcp_listen"
	FieldTCPClosing     = "tcp_closing"
	FieldTCPNone        = "tcp_none"
	FieldUDPSocket      = "udp_socket"

	// system
	FieldLoad1        = "load1"
	FieldLoad5        = "load5"
	FieldLoad15       = "load15"
	FieldNCPUs        = "n_cpus"
	FieldNUsers       = "n_users"
	FieldUptime       = "uptime"        // system 下用 uptime（也是 kernel.boot_time 改名后的目标）
	FieldUptimeFormat = "uptime_format"

	// kernel
	FieldBootTime        = "boot_time"
	FieldProcessesForked = "processes_forked"
	FieldInterrupts      = "interrupts"
	FieldContextSwitches = "context_switches"
	FieldDiskPagesIn     = "disk_pages_in"
	FieldDiskPagesOut    = "disk_pages_out"

	// processes
	FieldRunning      = "running"
	FieldSleeping     = "sleeping"
	FieldStopped      = "stopped"
	FieldZombies      = "zombies"
	FieldTotalThreads = "total_threads"
)

// ------------------------------------------------------------
// 重命名后的 Field（reporter 最终输出）
// ------------------------------------------------------------

const (
	// cpu (measurement=cpu_detail)
	RenamedFieldUser   = "user"
	RenamedFieldSystem = "system"
	RenamedFieldIdle   = "idle"
	RenamedFieldIowait = "iowait"

	// net
	RenamedFieldSpeedRecv         = "speed_recv"
	RenamedFieldSpeedSent         = "speed_sent"
	RenamedFieldSpeedPacketsRecv  = "speed_packets_recv"
	RenamedFieldSpeedPacketsSent  = "speed_packets_sent"

	// mem
	RenamedFieldPctUsed = "pct_used"

	// disk 特例：used_percent -> in_use
	RenamedFieldInUse = "in_use"

	// diskio (measurement=io)
	RenamedFieldRkbS = "rkb_s"
	RenamedFieldWkbS = "wkb_s"

	// netstat
	RenamedFieldCurTCPCloseWait = "cur_tcp_closewait"
	RenamedFieldCurTCPTimeWait  = "cur_tcp_timewait"
	RenamedFieldCurTCPClosed    = "cur_tcp_closed"
	RenamedFieldCurTCPClosing   = "cur_tcp_closing"
	RenamedFieldCurTCPEstab     = "cur_tcp_estab"
	RenamedFieldCurTCPFinWait1  = "cur_tcp_finwait1"
	RenamedFieldCurTCPFinWait2  = "cur_tcp_finwait2"
	RenamedFieldCurTCPLastAck   = "cur_tcp_lastack"
	RenamedFieldCurTCPListen    = "cur_tcp_listen"
	RenamedFieldCurTCPSynRecv   = "cur_tcp_syn_recv"
	RenamedFieldCurTCPSynSent   = "cur_tcp_syn_sent"

	// kernel (measurement=env)
	RenamedFieldProcs = "procs"
	// FieldUptime 已在上面定义，给 kernel.boot_time 也用同一名（rename 目标）
)

// ------------------------------------------------------------
// Windows PDH 计数器字段名（与 gopsutil / telegraf win_perf_counters 对齐）
// ------------------------------------------------------------

const (
	WinFieldPercentUserTime       = "Percent_User_Time"
	WinFieldPercentPrivilegedTime = "Percent_Privileged_Time"
	WinFieldPercentIdleTime       = "Percent_Idle_Time"
	WinFieldBytesReceivedPerSec   = "Bytes_Received_persec"
	WinFieldBytesSentPerSec       = "Bytes_Sent_persec"
	WinFieldPacketsReceivedPerSec = "Packets_Received_persec"
	WinFieldPacketsSentPerSec     = "Packets_Sent_persec"
	WinFieldDiskReadBytesPerSec   = "Disk_Read_Bytes_persec"
	WinFieldDiskWriteBytesPerSec  = "Disk_Write_Bytes_persec"
)

// ------------------------------------------------------------
// Tag 键
// ------------------------------------------------------------

const (
	TagHost      = "host"
	TagCPU       = "cpu"       // cpu measurement: cpu0 / cpu-total
	TagInterface = "interface" // net measurement
	TagDevice    = "device"    // disk measurement
	TagName      = "name"      // diskio measurement（对齐 telegraf inputs.diskio 默认 tag）
	TagFstype    = "fstype"
	TagMode      = "mode"
	TagPath      = "path"
	TagSource    = "source" // monitor / collector 区分上报来源

	// 全局 tag（由 reporter 从 config 注入）
	TagProjectID   = "projectId"
	TagAgentID     = "agentId"
	TagAgentSecret = "agentSecret"
	TagHostName    = "hostName"
	TagHostIP      = "hostIp"

	// tag 值
	TagValueCPUTotal = "cpu-total"
	TagSourceMonitor = "monitor"
)

// ------------------------------------------------------------
// 上报路径 / HTTP header
// ------------------------------------------------------------

const (
	// CI 项目 JSON 上报路径（POST）
	ReportPathMetrics = "/ms/environment/api/buildAgent/agent/thirdPartyAgent/agents/metrics"
	// Stream 项目 InfluxDB line protocol 上报路径（POST）
	ReportPathMetrix = "/ms/environment/api/buildAgent/agent/thirdPartyAgent/agents/metrix"

	// stream 项目判定：projectId 前缀
	StreamProjectPrefix = "git_"

	ContentTypeJSON         = "application/json; charset=utf-8"
	ContentTypeLineProtocol = "text/plain; charset=utf-8"

	HeaderBuildType = "X-DEVOPS-BUILD-TYPE"
	HeaderProjectID = "X-DEVOPS-PROJECT-ID"
	HeaderAgentID   = "X-DEVOPS-AGENT-ID"
	HeaderSecretKey = "X-DEVOPS-AGENT-SECRET-KEY"
)
