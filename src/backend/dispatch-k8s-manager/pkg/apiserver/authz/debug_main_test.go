package authz

import (
	"os"
	"testing"
)

func TestMain(m *testing.M) {
	SetDebugTicketSecretForTest([]byte(UnitTestDebugTicketSecret))
	os.Exit(m.Run())
}
