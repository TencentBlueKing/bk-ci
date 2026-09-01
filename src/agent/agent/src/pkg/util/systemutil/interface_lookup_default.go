//go:build !windows
// +build !windows

package systemutil

func loadAgentInterfaceLookup() (agentInterfaceLookup, error) {
	return agentInterfaceLookup{
		byIndex: map[int]agentInterfaceDetail{},
		byName:  map[string]agentInterfaceDetail{},
	}, nil
}
