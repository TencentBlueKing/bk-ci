package authz

import (
	"strings"
	"sync"
)

var systemNamespaces = map[string]struct{}{
	"kube-system":     {},
	"kube-public":     {},
	"kube-node-lease": {},
	"default":         {},
	"kubernetes":      {},
}

// NamespaceOwnerStore 记录 namespace 属主。共享 token 本身不能区分用户，必须按租户/项目绑定。
type NamespaceOwnerStore struct {
	mu     sync.RWMutex
	owners map[string]Owner
}

func NewNamespaceOwnerStore() *NamespaceOwnerStore {
	return &NamespaceOwnerStore{owners: map[string]Owner{}}
}

var DefaultNamespaceOwners = NewNamespaceOwnerStore()

func (s *NamespaceOwnerStore) Get(namespace string) (Owner, bool) {
	if s == nil {
		return Owner{}, false
	}
	s.mu.RLock()
	defer s.mu.RUnlock()
	owner, ok := s.owners[namespace]
	return owner, ok
}

func (s *NamespaceOwnerStore) Bind(namespace string, owner Owner) {
	if s == nil || namespace == "" || owner.IsEmpty() {
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	if _, exists := s.owners[namespace]; exists {
		return
	}
	s.owners[namespace] = owner
}

func (s *NamespaceOwnerStore) Reset() {
	if s == nil {
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	s.owners = map[string]Owner{}
}

func IsSystemNamespace(namespace string) bool {
	ns := strings.TrimSpace(namespace)
	if ns == "" {
		return true
	}
	_, ok := systemNamespaces[ns]
	return ok
}

// AuthorizeNamespace 校验调用方对目标 namespace 的属主权限。
// resourceOwner 来自 Deployment/Secret 自身的 label；nsOwner 来自属主登记。
func AuthorizeNamespace(caller Caller, namespace string, resourceOwner Owner, nsOwner Owner, hasNsOwner bool) error {
	if caller.UserID == "" {
		return ErrMissingIdentity
	}
	if IsSystemNamespace(namespace) {
		return ErrNamespaceDenied
	}
	if hasNsOwner {
		if err := AuthorizeObject(caller, nsOwner); err != nil {
			return ErrNamespaceDenied
		}
	}
	if !resourceOwner.IsEmpty() {
		if err := AuthorizeObject(caller, resourceOwner); err != nil {
			return ErrNamespaceDenied
		}
		return nil
	}
	if hasNsOwner {
		return nil
	}
	return ErrNamespaceDenied
}

// AuthorizeNamespaceWrite 写操作：已绑定则必须匹配；未绑定则绑定给当前调用方。
func AuthorizeNamespaceWrite(store *NamespaceOwnerStore, caller Caller, namespace string, resourceOwner Owner) error {
	if caller.UserID == "" {
		return ErrMissingIdentity
	}
	if IsSystemNamespace(namespace) {
		return ErrNamespaceDenied
	}
	nsOwner, hasNsOwner := store.Get(namespace)
	if !resourceOwner.IsEmpty() {
		if err := AuthorizeObject(caller, resourceOwner); err != nil {
			return ErrNamespaceDenied
		}
	}
	if hasNsOwner {
		if err := AuthorizeObject(caller, nsOwner); err != nil {
			return ErrNamespaceDenied
		}
		return nil
	}
	store.Bind(namespace, caller.Owner())
	return nil
}

// AuthorizeNamespaceRead 读/删：必须命中 namespace 或资源属主，默认拒绝。
func AuthorizeNamespaceRead(store *NamespaceOwnerStore, caller Caller, namespace string, resourceOwner Owner) error {
	nsOwner, hasNsOwner := store.Get(namespace)
	return AuthorizeNamespace(caller, namespace, resourceOwner, nsOwner, hasNsOwner)
}
