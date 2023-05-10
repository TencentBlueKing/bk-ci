package proxy

import (
	"common/devops"
	"common/logs"
	"context"
	"encoding/base64"
	"encoding/json"
	"wsproxy/pkg/config"
	"wsproxy/pkg/constant"

	"github.com/allegro/bigcache/v3"
	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"
	kerrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/tools/cache"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/builder"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/predicate"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

// WorkspaceInfoProvider 提供工作空间信息
type WorkspaceInfoProvider interface {
	WorkspaceInfo(workspaceID string) *WorkspaceInfo
}

// WorkspaceInfo proxy需要的工作空间信息
type WorkspaceInfo struct {
	WorkspaceID string `json:"workspaceId"`
	// 工作空间具体IP
	IPAddress string `json:"ipAddress"`
	// SSHPublicKeys 用户上传的公钥
	SSHPublicKeys []string `json:"sshPubKey"`
}

const (
	workspaceIndex = "workspaceIndex"
)

// RemoteWorkspaceInfoProvider kube原生接口
type RemoteWorkspaceInfoProvider struct {
	client.Client
	Scheme *runtime.Scheme

	store cache.ThreadSafeStore
}

func NewRemoteWorkspaceInfoProvider(client client.Client, scheme *runtime.Scheme) *RemoteWorkspaceInfoProvider {
	indexers := cache.Indexers{
		workspaceIndex: func(obj interface{}) ([]string, error) {
			if workspaceInfo, ok := obj.(*WorkspaceInfo); ok {
				return []string{workspaceInfo.WorkspaceID}, nil
			}

			return nil, errors.Errorf("object is not a WorkspaceInfo")
		},
	}

	return &RemoteWorkspaceInfoProvider{
		Client: client,
		Scheme: scheme,

		store: cache.NewThreadSafeStore(indexers, cache.Indices{}),
	}
}

func (r *RemoteWorkspaceInfoProvider) WorkspaceInfo(workspaceID string) *WorkspaceInfo {
	workspaces, err := r.store.ByIndex(workspaceIndex, workspaceID)
	if err != nil {
		return nil
	}

	if len(workspaces) == 1 {
		return workspaces[0].(*WorkspaceInfo)
	}

	return nil
}

func (r *RemoteWorkspaceInfoProvider) SetupWithManager(mgr ctrl.Manager) error {
	podWorkspaceSelector, err := predicate.LabelSelectorPredicate(metav1.LabelSelector{
		MatchLabels: map[string]string{
			constant.KubernetesCoreLabelName: constant.KubernetesCoreLabelNameValue,
		},
	})
	if err != nil {
		return err
	}

	return ctrl.NewControllerManagedBy(mgr).
		Named("pod").
		WithEventFilter(predicate.ResourceVersionChangedPredicate{}).
		For(
			&corev1.Pod{},
			builder.WithPredicates(podWorkspaceSelector),
		).
		Complete(r)
}

func (r *RemoteWorkspaceInfoProvider) Reconcile(ctx context.Context, req ctrl.Request) (ctrl.Result, error) {
	var pod corev1.Pod
	err := r.Client.Get(context.Background(), req.NamespacedName, &pod)
	if kerrors.IsNotFound(err) {
		// pod is gone - that's ok
		r.store.Delete(req.Name)
		logs.WithField("workspace", req.Name).Debug("removing workspace from store")

		return reconcile.Result{}, nil
	}

	// extract workspace details from pod and store
	workspaceInfo := mapPodToWorkspaceInfo(&pod)
	r.store.Update(req.Name, workspaceInfo)
	logs.WithField("workspace", req.Name).WithField("details", workspaceInfo).Debug("adding/updating workspace details")

	return ctrl.Result{}, nil
}

func mapPodToWorkspaceInfo(pod *corev1.Pod) *WorkspaceInfo {
	return &WorkspaceInfo{
		WorkspaceID:   pod.Labels[constant.KubernetesWorkspaceIDLabel],
		IPAddress:     pod.Status.PodIP,
		SSHPublicKeys: extractUserSSHPublicKeysByPod(pod),
	}
}

type SSHPublicKeys struct {
	Keys []string `json:"keys,omitempty"`
}

func extractUserSSHPublicKeysByPod(pod *corev1.Pod) []string {
	if data, ok := pod.Annotations[constant.KubernetesWorkspaceSSHPublicKeys]; ok && len(data) != 0 {
		return extractUserSSHPublicKeys(data)
	}
	return nil
}

func extractUserSSHPublicKeys(base64SSHKey string) []string {
	if base64SSHKey == "" {
		return nil
	}

	specJ, err := base64.StdEncoding.DecodeString(base64SSHKey)
	if err != nil {
		return nil
	}
	var spec SSHPublicKeys
	err = json.Unmarshal(specJ, &spec)
	if err != nil {
		return nil
	}
	return spec.Keys
}

// BackendWorkspaceInfoProvider 后台接口
type BackendWorkspaceInfoProvider struct {
	client *devops.RemoteDevClient

	cache *bigcache.BigCache
}

func NewBackendWorkspaceInfoProvider(cfg *config.WsPorxyConfig) (*BackendWorkspaceInfoProvider, error) {
	client := devops.NewRemoteDevClient(cfg.DevRemotingBackend.HostName, cfg.DevRemotingBackend.SHA1Key)

	// cache, err := bigcache.New(context.Background(), bigcache.DefaultConfig(30*time.Minute))
	// if err != nil {
	// 	return nil, errors.Wrap(err, "create big cache error")
	// }

	return &BackendWorkspaceInfoProvider{
		client: client,
		cache:  nil,
	}, nil
}

func (b *BackendWorkspaceInfoProvider) WorkspaceInfo(workspaceId string) *WorkspaceInfo {
	// var wsInfo = &WorkspaceInfo{}
	// wsInfoCache, err := b.cache.Get(workspaceId)
	// if err != nil {
	// 	if err != bigcache.ErrEntryNotFound {
	// 		logs.WithField("workspaceId", workspaceId).WithError(err).Error("bigcache get value error")
	// 	}
	// } else {
	// 	err = json.Unmarshal(wsInfoCache, wsInfo)
	// 	if err != nil {
	// 		logs.WithField("workspaceId", workspaceId).WithError(err).Error("bigcache get value json Unmarshal error")
	// 	} else {
	// 		return wsInfo
	// 	}
	// }

	info, err := b.client.GetWorkspaceDetail(context.Background(), workspaceId)
	if err != nil {
		logs.WithField("workspaceId", workspaceId).WithError(err).Error("get backend workspace info error")
		return nil
	}
	logs.WithField("workspaceId", workspaceId).WithField("details", info).Debug("get backend workspace details")

	if info.PodIp == "" {
		logs.WithField("workspaceId", workspaceId).Error("wsinfo podIp is null")
	}

	wsInfo := &WorkspaceInfo{
		WorkspaceID:   workspaceId,
		IPAddress:     info.PodIp,
		SSHPublicKeys: extractUserSSHPublicKeys(info.SSHKey),
	}

	// wsInfoData, err := json.Marshal(wsInfo)
	// if err != nil {
	// 	logs.WithField("workspaceId", workspaceId).WithError(err).Error("bigcache get value json Marshal error")
	// 	return wsInfo
	// }

	// b.cache.Set(workspaceId, wsInfoData)

	return wsInfo
}
