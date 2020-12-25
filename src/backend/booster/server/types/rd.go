package types

import (
	"fmt"

	"build-booster/common/types"
)

// RoleType: string of role
type RoleType string

// define role strings
const (
	ServerMaster  RoleType = "master"
	ServerSlave   RoleType = "slave"
	ServerUnknown RoleType = "unknown"
)

// RoleChangeEvent: Provides the master-change event notifications.
type RoleChangeEvent <-chan RoleType

type ServerInfo struct {
	types.ServerInfo
}

// GetURI: return uri of server
func (di *ServerInfo) GetURI() string {
	return fmt.Sprintf("%s://%s:%d", di.Scheme, di.IP, di.Port)
}

// define ServerElectionPath to "server"
const (
	ServerElectionPath = "server"
)
