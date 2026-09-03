package kubeclient

import (
	"context"
	"disaptch-k8s-manager/pkg/apiserver/authz"

	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func GetNamespace(namespace string) (*corev1.Namespace, error) {
	if kubeClient == nil {
		return nil, errors.New("kube client not initialized")
	}
	return kubeClient.CoreV1().Namespaces().Get(context.TODO(), namespace, metav1.GetOptions{})
}

func LoadNamespaceOwner(namespace string) (authz.Owner, bool) {
	ns, err := GetNamespace(namespace)
	if err != nil || ns == nil {
		return authz.Owner{}, false
	}
	owner := authz.OwnerFromMetadata(ns.ObjectMeta)
	if owner.IsEmpty() {
		return authz.Owner{}, false
	}
	return owner, true
}

func PersistNamespaceOwner(namespace string, owner authz.Owner) error {
	if kubeClient == nil {
		return nil
	}
	ns, err := GetNamespace(namespace)
	if err != nil {
		return err
	}
	if ns.Labels == nil {
		ns.Labels = map[string]string{}
	}
	if ns.Annotations == nil {
		ns.Annotations = map[string]string{}
	}
	ns.Labels = authz.ApplyOwnerLabels(ns.Labels, owner)
	ns.Annotations = authz.ApplyOwnerAnnotations(ns.Annotations, owner)
	_, err = kubeClient.CoreV1().Namespaces().Update(context.TODO(), ns, metav1.UpdateOptions{})
	return err
}
