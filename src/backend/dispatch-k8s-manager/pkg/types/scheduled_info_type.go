package types

type ScheduledInfo struct {
	BuilderName     string                   `json:"builderName"`
	NodeHistory     []string                 `json:"nodeHistory"`
	ResourceHistory []ContainerResourceUsage `json:"resourceHistory"`
}

// ContainerResourceUsage 容器资源使用量
// Cpu cpu核数，单位是毫核 100m = 0.1
// Memory 内存，单位是Mi
type ContainerResourceUsage struct {
	Cpu    string `json:"cpu"`
	Memory string `json:"memory"`
}
