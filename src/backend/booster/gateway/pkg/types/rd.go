package types

import (
	"fmt"

	"build-booster/common/types"
)

type RoleType string

const (
	ServerMaster  RoleType = "master"
	ServerSlave   RoleType = "slave"
	ServerUnknown RoleType = "unknown"
)

// Provides the master-change event notifications.
type RoleChangeEvent <-chan RoleType

type ServerInfo struct {
	types.ServerInfo
}

func (di *ServerInfo) GetURI() string {
	return fmt.Sprintf("%s://%s:%d", di.Scheme, di.IP, di.Port)
}

const (
	ServerElectionPath = "server"
)
