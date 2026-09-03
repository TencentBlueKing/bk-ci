package authz

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestSystemNamespaceDenied(t *testing.T) {
	assert.True(t, IsSystemNamespace("kube-system"))
	assert.True(t, IsSystemNamespace("default"))
	assert.True(t, IsSystemNamespace(""))
	assert.True(t, IsSystemNamespace("istio-system"))
	assert.True(t, IsSystemNamespace("istio-operator"))
	assert.True(t, IsSystemNamespace("cattle-system"))
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

	assert.ErrorIs(t, AuthorizeNamespaceWrite(store, alice, "istio-system", Owner{}), ErrNamespaceDenied)
	assert.ErrorIs(t, AuthorizeNamespaceWrite(store, Caller{UserID: "eve"}, "proj-b-ns", Owner{}), ErrMissingIdentity)

	carol := Caller{UserID: "carol", ProjectID: "proj-a"}
	assert.NoError(t, AuthorizeNamespaceWrite(store, carol, "proj-a-ns", Owner{}))
}

func TestNamespaceOwnerReloadedAfterRestart(t *testing.T) {
	persisted := map[string]Owner{}
	store := NewNamespaceOwnerStore()
	store.SetLoader(func(namespace string) (Owner, bool) {
		o, ok := persisted[namespace]
		return o, ok
	})
	store.SetPersister(func(namespace string, owner Owner) error {
		persisted[namespace] = owner
		return nil
	})

	alice := Caller{UserID: "alice", ProjectID: "proj-a"}
	assert.NoError(t, AuthorizeNamespaceWrite(store, alice, "proj-a-ns", Owner{}))

	store.Reset()
	assert.NoError(t, AuthorizeNamespaceRead(store, alice, "proj-a-ns", Owner{}))
	assert.NoError(t, AuthorizeNamespaceRead(store, Caller{UserID: "dave", ProjectID: "proj-a"}, "proj-a-ns", Owner{}))
}
