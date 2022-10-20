package config

// 常量
const (
	// ManagerLog api服务器日志地址
	ManagerLog = "manager.log"

	// AccessLog api服务器access日志地址
	AccessLog = "access_log.log"

	// BuilderConfigVolumeName 挂载构建机脚本的 configMap的volume的名称
	BuilderConfigVolumeName = "builder-config-volume"

	// DataVolumeName 挂载数据盘的volume的名称
	DataVolumeName = "data-volume"

	// LogsVolumeName 挂载日志盘的volume的名称
	LogsVolumeName = "logs-volume"

	// NfsVolumeNamePrefix 挂载nfs的volume的名称
	NfsVolumeNamePrefix = "nfs-volume"

	// CfsVolumeName 挂载cfs的volume的名称(使用hostPath挂载)
	CfsVolumeName = "cfs-volume"
)

// 一些附加功能的常量
const (
	// BuilderNodeHisSize 构建机历史节点长度
	BuilderNodeHisSize = 3

	// BuilderRealResourceHisSize 构建机真实资源历史长度
	BuilderRealResourceHisSize = 10

	// BuilderRealResourceScheduleSize 构建机真实资源可调度长度
	BuilderRealResourceScheduleSize = 5
)
