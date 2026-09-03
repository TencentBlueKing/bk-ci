package service

import (
	"os"
	"testing"

	"disaptch-k8s-manager/pkg/apiserver/authz"
)

func TestMain(m *testing.M) {
	authz.SetDebugTicketSecretForTest([]byte(authz.UnitTestDebugTicketSecret))
	authz.SetIdentitySigningKeyForTest([]byte(authz.UnitTestIdentitySigningKey))
	os.Exit(m.Run())
}
