package types

// 自己定义枚举类型方便转换
type AgentLanguage string

const (
	Chinese AgentLanguage = "zh-CN"
	Englist AgentLanguage = "zh-US"
)

var SupportAgentLanguage = []AgentLanguage{Chinese, Englist}

func (a AgentLanguage) String() string {
	return string(a)
}
