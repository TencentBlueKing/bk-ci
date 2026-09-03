package authz

import (
	"os"
	"testing"
)

func TestMain(m *testing.M) {
	SetDebugTicketSecretForTest([]byte(UnitTestDebugTicketSecret))
	SetIdentitySigningKeyForTest([]byte(UnitTestIdentitySigningKey))
	os.Exit(m.Run())
}
