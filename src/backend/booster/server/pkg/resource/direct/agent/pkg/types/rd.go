package types

import (
	"fmt"

	"build-booster/common/types"
)

// RoleType : string of role
type RoleType string

// define role strings
const (
	DistCCServerMaster  RoleType = "master"
	DistCCServerSlave   RoleType = "slave"
	DistCCServerUnknown RoleType = "unknown"
)

// RoleChangeEvent : Provides the master-change event notifications.
type RoleChangeEvent <-chan RoleType

// DistCCServerInfo : types.ServerInfo
type DistCCServerInfo struct {
	types.ServerInfo
}

// GetURI : return uri of server
func (di *DistCCServerInfo) GetURI() string {
	return fmt.Sprintf("%s://%s:%d", di.Scheme, di.IP, di.Port)
}

// define ServerElectionPath to "server"
const (
	ServerElectionPath = "server"
)
