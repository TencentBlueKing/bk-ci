package kubeclient

import (
	"context"
	"disaptch-k8s-manager/pkg/config"
	"flag"
	"github.com/pkg/errors"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/watch"
	"k8s.io/client-go/informers"
	"k8s.io/client-go/kubernetes"
	appsv1Listers "k8s.io/client-go/listers/apps/v1"
	corev1Listers "k8s.io/client-go/listers/core/v1"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/cache"
	"k8s.io/client-go/tools/clientcmd"
	"os"
	"time"
)

var kubeClient *kubernetes.Clientset

var kubeRestConfig *rest.Config

type kubeinformers struct {
	pod        corev1Listers.PodLister
	deployment appsv1Listers.DeploymentLister
}

var infs = &kubeinformers{}

func InitKubeClient(kubeConfigFile string, informerStopCh <-chan struct{}) error {
	// 没有指定配置则使用默认集群配置
	_, err := os.Stat(kubeConfigFile)
	if err != nil && os.IsNotExist(err) {
		kubeRestConfig, err = rest.InClusterConfig()
		if err != nil {
			return err
		}
	} else {
		kubeConfig := flag.String(
			"kubeconfig",
			kubeConfigFile,
			"absolute path to the kubeconfig file",
		)

		kubeRestConfig, err = clientcmd.BuildConfigFromFlags("", *kubeConfig)
		if err != nil {
			return err
		}
	}

	clientset, err := kubernetes.NewForConfig(kubeRestConfig)
	if err != nil {
		return err
	}

	kubeClient = clientset

	// 初始化 informer
	factory := informers.NewSharedInformerFactory(clientset, 0)

	podInformer, podLister := initPodInformerAndLister(factory)
	deploymentInformer, deploymentLister := initDeploymentInformerAndLister(factory)

	go factory.Start(informerStopCh)

	// 从 apiserver 同步资源，即 list
	if !cache.WaitForCacheSync(informerStopCh, podInformer.HasSynced) {
		return errors.Wrapf(err, "sync pod informer cache error")
	}
	if !cache.WaitForCacheSync(informerStopCh, deploymentInformer.HasSynced) {
		return errors.Wrapf(err, "sync deployment informer cache error")
	}

	infs.pod = podLister
	infs.deployment = deploymentLister

	return nil
}

func initPodInformerAndLister(f informers.SharedInformerFactory) (cache.SharedIndexInformer, corev1Listers.PodLister) {
	podInformer := f.InformerFor(
		&corev1.Pod{},
		func(client kubernetes.Interface, duration time.Duration) cache.SharedIndexInformer {
			return cache.NewSharedIndexInformer(
				&cache.ListWatch{
					ListFunc: func(options metav1.ListOptions) (runtime.Object, error) {
						options.LabelSelector = config.Config.Dispatch.Label
						return client.CoreV1().Pods(config.Config.Kubernetes.NameSpace).List(context.TODO(), options)
					},
					WatchFunc: func(options metav1.ListOptions) (watch.Interface, error) {
						options.LabelSelector = config.Config.Dispatch.Label
						return client.CoreV1().Pods(config.Config.Kubernetes.NameSpace).Watch(context.TODO(), options)
					},
				},
				&corev1.Pod{},
				0,
				cache.Indexers{cache.NamespaceIndex: cache.MetaNamespaceIndexFunc},
			)
		},
	)

	podLister := corev1Listers.NewPodLister(podInformer.GetIndexer())

	return podInformer, podLister
}

func initDeploymentInformerAndLister(f informers.SharedInformerFactory) (cache.SharedIndexInformer, appsv1Listers.DeploymentLister) {
	informer := f.InformerFor(
		&appsv1.Deployment{},
		func(client kubernetes.Interface, duration time.Duration) cache.SharedIndexInformer {
			return cache.NewSharedIndexInformer(
				&cache.ListWatch{
					ListFunc: func(options metav1.ListOptions) (runtime.Object, error) {
						options.LabelSelector = config.Config.Dispatch.Label
						return client.AppsV1().Deployments(config.Config.Kubernetes.NameSpace).List(context.TODO(), options)
					},
					WatchFunc: func(options metav1.ListOptions) (watch.Interface, error) {
						options.LabelSelector = config.Config.Dispatch.Label
						return client.AppsV1().Deployments(config.Config.Kubernetes.NameSpace).Watch(context.TODO(), options)
					},
				},
				&appsv1.Deployment{},
				0,
				cache.Indexers{cache.NamespaceIndex: cache.MetaNamespaceIndexFunc},
			)
		},
	)

	lister := appsv1Listers.NewDeploymentLister(informer.GetIndexer())

	return informer, lister
}
