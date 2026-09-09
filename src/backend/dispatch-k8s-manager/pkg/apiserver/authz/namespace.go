package authz

import (
	"strings"
	"sync"
)

var systemNamespaces = map[string]struct{}{
	"kube-system":      {},
	"kube-public":      {},
	"kube-node-lease":  {},
	"kube-flannel":     {},
	"default":          {},
	"kubernetes":       {},
	"istio-system":     {},
	"istio-operator":   {},
	"ingress-nginx":    {},
	"cert-manager":     {},
	"monitoring":       {},
	"logging":          {},
	"observability":    {},
	"cattle-system":    {},
	"knative-serving":  {},
	"knative-eventing": {},
}

var protectedPrefixes = []string{
	"kube-", "istio-", "openshift-", "cattle-", "tkg-", "knative-",
}

// NamespaceOwnerStore 记录 namespace 属主。内存是缓存，权威来源是 K8s label/annotation。
type NamespaceOwnerStore struct {
	mu      sync.RWMutex
	owners  map[string]Owner
	load    func(namespace string) (Owner, bool)
	persist func(namespace string, owner Owner) error
}

func NewNamespaceOwnerStore() *NamespaceOwnerStore {
	return &NamespaceOwnerStore{owners: map[string]Owner{}}
}

var DefaultNamespaceOwners = NewNamespaceOwnerStore()

func (s *NamespaceOwnerStore) SetLoader(fn func(namespace string) (Owner, bool)) {
	if s == nil {
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	s.load = fn
}

func (s *NamespaceOwnerStore) SetPersister(fn func(namespace string, owner Owner) error) {
	if s == nil {
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	s.persist = fn
}

func (s *NamespaceOwnerStore) Get(namespace string) (Owner, bool) {
	if s == nil {
		return Owner{}, false
	}
	s.mu.RLock()
	defer s.mu.RUnlock()
	owner, ok := s.owners[namespace]
	return owner, ok
}

// Resolve 先读内存，未命中再从持久化来源加载（重启/多副本一致）。
func (s *NamespaceOwnerStore) Resolve(namespace string) (Owner, bool) {
	if owner, ok := s.Get(namespace); ok {
		return owner, true
	}
	s.mu.RLock()
	load := s.load
	s.mu.RUnlock()
	if load == nil {
		return Owner{}, false
	}
	owner, ok := load(namespace)
	if !ok || owner.IsEmpty() {
		return Owner{}, false
	}
	s.Bind(namespace, owner)
	return owner, true
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

func (s *NamespaceOwnerStore) BindAndPersist(namespace string, owner Owner) error {
	s.mu.RLock()
	persist := s.persist
	s.mu.RUnlock()
	if persist != nil {
		if err := persist(namespace, owner); err != nil {
			return err
		}
	}
	s.Bind(namespace, owner)
	return nil
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
	if _, ok := systemNamespaces[ns]; ok {
		return true
	}
	lower := strings.ToLower(ns)
	for _, prefix := range protectedPrefixes {
		if strings.HasPrefix(lower, prefix) {
			return true
		}
	}
	return false
}

func requireProjectCaller(caller Caller) error {
	if caller.UserID == "" || !caller.HasTenantScope() {
		return ErrMissingIdentity
	}
	return nil
}

// AuthorizeNamespace 校验调用方对目标 namespace 的属主权限。
// resourceOwner 来自 Deployment/Secret 自身的 label；nsOwner 来自属主登记。
func AuthorizeNamespace(caller Caller, namespace string, resourceOwner Owner, nsOwner Owner, hasNsOwner bool) error {
	if err := requireProjectCaller(caller); err != nil {
		return err
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

// AuthorizeNamespaceWrite 写：受保护 ns 拒绝；已绑定必须匹配；未绑定须有项目/租户身份才可登记并持久化。
func AuthorizeNamespaceWrite(store *NamespaceOwnerStore, caller Caller, namespace string, resourceOwner Owner) error {
	if err := requireProjectCaller(caller); err != nil {
		return err
	}
	if IsSystemNamespace(namespace) {
		return ErrNamespaceDenied
	}
	nsOwner, hasNsOwner := store.Resolve(namespace)
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
	// 未绑定：不再对任意自称 default-allow。必须带项目/租户，并落到持久化来源。
	if err := store.BindAndPersist(namespace, caller.Owner()); err != nil {
		return err
	}
	return nil
}

// AuthorizeNamespaceRead 读/删：必须命中 namespace 或资源属主，默认拒绝。
func AuthorizeNamespaceRead(store *NamespaceOwnerStore, caller Caller, namespace string, resourceOwner Owner) error {
	nsOwner, hasNsOwner := store.Resolve(namespace)
	return AuthorizeNamespace(caller, namespace, resourceOwner, nsOwner, hasNsOwner)
}
