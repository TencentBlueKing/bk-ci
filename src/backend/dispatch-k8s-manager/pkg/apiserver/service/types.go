package service

import "disaptch-k8s-manager/pkg/types"

type CommonWorkLoad struct {
	Name          string                 `json:"name"  binding:"required,max=36"` // 唯一名称
	Image         string                 `json:"image" binding:"required"`        // 镜像
	ImageRegistry *types.Registry        `json:"registry"`                        // 镜像凭证
	Resource      CommonWorkLoadResource `json:"resource"  binding:"required"`    // 工作负载资源
	Env           map[string]string      `json:"env"`                             // 环境变量
	Command       []string               `json:"command"`                         // 启动命令
	NFSs          []types.NFS            `json:"nfs"`                             // nfs配置
}

type CommonWorkLoadResource struct {
	RequestCPU    string `json:"requestCPU" binding:"required"`    // 最小CPU
	RequestMemory string `json:"requestMem" binding:"required"`    // 最小内存
	RequestDisk   string `json:"requestDisk" binding:"required"`   // 最小磁盘容量
	RequestDiskIO string `json:"requestDiskIO" binding:"required"` // 最小磁盘IO
	LimitCPU      string `json:"limitCpu" binding:"required"`      // 最大CPU
	LimitMemory   string `json:"limitMem" binding:"required"`      // 最大内存
	LimitDisk     string `json:"limitDisk" binding:"required"`     // 最大磁盘容量
	LimitDiskIO   string `json:"limitDiskIO" binding:"required"`   // 最大磁盘容量
}
