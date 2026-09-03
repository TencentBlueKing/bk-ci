package authz

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestSystemNamespaceDenied(t *testing.T) {
	assert.True(t, IsSystemNamespace("kube-system"))
	assert.True(t, IsSystemNamespace("default"))
	assert.True(t, IsSystemNamespace(""))
	assert.False(t, IsSystemNamespace("proj-a-ns"))
}

func TestNamespaceReadWriteIsolation(t *testing.T) {
	store := NewNamespaceOwnerStore()
	alice := Caller{UserID: "alice", ProjectID: "proj-a", TenantID: "t-a"}
	bob := Caller{UserID: "bob", ProjectID: "proj-b", TenantID: "t-b"}

	assert.ErrorIs(t, AuthorizeNamespaceWrite(store, Caller{}, "proj-a-ns", Owner{}), ErrMissingIdentity)
	assert.ErrorIs(t, AuthorizeNamespaceWrite(store, alice, "kube-system", Owner{}), ErrNamespaceDenied)
	assert.NoError(t, AuthorizeNamespaceWrite(store, alice, "proj-a-ns", Owner{}))

	assert.NoError(t, AuthorizeNamespaceRead(store, alice, "proj-a-ns", Owner{}))
	assert.ErrorIs(t, AuthorizeNamespaceRead(store, bob, "proj-a-ns", Owner{}), ErrNamespaceDenied)
	assert.ErrorIs(t, AuthorizeNamespaceWrite(store, bob, "proj-a-ns", Owner{}), ErrNamespaceDenied)

	assert.ErrorIs(t, AuthorizeNamespaceRead(store, alice, "unknown-ns", Owner{}), ErrNamespaceDenied)
	assert.NoError(t, AuthorizeNamespaceRead(store, alice, "unknown-ns", Owner{ProjectID: "proj-a", UserID: "alice"}))
	assert.ErrorIs(t, AuthorizeNamespaceRead(store, bob, "unknown-ns", Owner{ProjectID: "proj-a", UserID: "alice"}), ErrNamespaceDenied)
}
