package sshproxy

import "github.com/ci-plugins/crypto-go/ssh"

type SSHError struct {
	shortName   string
	description string
	err         error
}

func (e SSHError) Error() string {
	return e.description
}

func (e SSHError) ShortName() string {
	return e.shortName
}
func (e SSHError) Unwrap() error {
	return e.err
}

var (
	ErrWorkspaceNotFound = NewSSHErrorWithReject("WS_NOTFOUND", "not found workspace")
	ErrAuthFailed        = NewSSHError("AUTH_FAILED", "auth failed")
)

func NewSSHError(shortName string, description string) SSHError {
	return SSHError{shortName: shortName, description: description}
}

func NewSSHErrorWithReject(shortName string, description string) SSHError {
	return SSHError{shortName: shortName, description: description, err: ssh.ErrDenied}
}
