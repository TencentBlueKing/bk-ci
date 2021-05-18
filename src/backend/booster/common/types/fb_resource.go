package types

type FBClusterResource struct {
	DiskTotal float64              `json:"disktotal"`
	MemTotal  float64              `json:"memtotal"`
	CPUTotal  float64              `json:"cputotal"`
	DiskUsed  float64              `json:"diskused"`
	MemUsed   float64              `json:"memused"`
	CPUUsed   float64              `json:"cpuused"`
	Agents    []FBClusterAgentInfo `json:"agents"`
}

type FBClusterAgentInfo struct {
	HostName  string  `json:"hostname"`
	IP        string  `json:"ip"`
	DiskTotal float64 `json:"disktotal"`
	MemTotal  float64 `json:"memtotal"`
	CPUTotal  float64 `json:"cputotal"`
	DiskUsed  float64 `json:"diskused"`
	MemUsed   float64 `json:"memused"`
	CPUUsed   float64 `json:"cpuused"`

	Disabled   bool               `json:"disabled"`
	Attributes []FBAgentAttribute `json:"attributes"`
}

type FBAgentAttribute struct {
	Name  string `json:"name"`
	Value string `json:"value"`
}
