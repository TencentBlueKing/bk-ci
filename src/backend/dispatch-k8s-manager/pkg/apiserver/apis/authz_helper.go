package apis

import (
	"disaptch-k8s-manager/pkg/apiserver/authz"
	"disaptch-k8s-manager/pkg/kubeclient"
	"net/http"

	"github.com/gin-gonic/gin"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

var lookupNamespace = kubeclient.GetNamespace

func requireTenantCaller(c *gin.Context) (authz.Caller, bool) {
	caller, err := authz.RequireTenantCaller(c)
	if err != nil {
		fail(c, http.StatusForbidden, err)
		return authz.Caller{}, false
	}
	return caller, true
}

func requireNamespaceCaller(c *gin.Context) (authz.Caller, bool) {
	if authz.HasQueryIdentity(c) {
		fail(c, http.StatusForbidden, authz.ErrUntrustedIdentity)
		return authz.Caller{}, false
	}
	caller := authz.CallerFromRequest(c)
	if caller.UserID == "" || !caller.HasTenantScope() {
		fail(c, http.StatusForbidden, authz.ErrMissingIdentity)
		return authz.Caller{}, false
	}
	return caller, true
}

func denySystemNamespace(c *gin.Context, namespace string) bool {
	if authz.IsSystemNamespace(namespace) {
		fail(c, http.StatusForbidden, authz.ErrNamespaceDenied)
		return true
	}
	return false
}

func namespaceOwnerFromStore(namespace string) (authz.Owner, bool) {
	if owner, ok := authz.DefaultNamespaceOwners.Get(namespace); ok {
		return owner, true
	}
	ns, err := lookupNamespace(namespace)
	if err != nil || ns == nil {
		return authz.Owner{}, false
	}
	owner := authz.OwnerFromMetadata(ns.ObjectMeta)
	if owner.IsEmpty() {
		return authz.Owner{}, false
	}
	authz.DefaultNamespaceOwners.Bind(namespace, owner)
	return owner, true
}

func authorizeNamespaceRead(c *gin.Context, namespace string, meta metav1.ObjectMeta) bool {
	caller, ok := requireNamespaceCaller(c)
	if !ok {
		return false
	}
	resourceOwner := authz.OwnerFromMetadata(meta)
	if err := authz.AuthorizeNamespaceRead(authz.DefaultNamespaceOwners, caller, namespace, resourceOwner); err != nil {
		// store 可能尚未缓存 k8s namespace 属主，补一次后再判
		if _, found := namespaceOwnerFromStore(namespace); found {
			if err = authz.AuthorizeNamespaceRead(authz.DefaultNamespaceOwners, caller, namespace, resourceOwner); err == nil {
				return true
			}
		}
		fail(c, http.StatusForbidden, err)
		return false
	}
	return true
}

func authorizeNamespaceWrite(c *gin.Context, namespace string, meta metav1.ObjectMeta) (authz.Caller, bool) {
	caller, ok := requireNamespaceCaller(c)
	if !ok {
		return authz.Caller{}, false
	}
	_, _ = namespaceOwnerFromStore(namespace)
	resourceOwner := authz.OwnerFromMetadata(meta)
	if err := authz.AuthorizeNamespaceWrite(authz.DefaultNamespaceOwners, caller, namespace, resourceOwner); err != nil {
		fail(c, http.StatusForbidden, err)
		return authz.Caller{}, false
	}
	return caller, true
}

func stampObjectOwner(meta *metav1.ObjectMeta, owner authz.Owner) {
	if meta == nil {
		return
	}
	meta.Labels = authz.ApplyOwnerLabels(meta.Labels, owner)
	meta.Annotations = authz.ApplyOwnerAnnotations(meta.Annotations, owner)
}
