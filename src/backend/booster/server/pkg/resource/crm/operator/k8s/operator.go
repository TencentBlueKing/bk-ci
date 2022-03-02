/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package k8s

import (
	"context"
	"crypto/tls"
	"fmt"
	"io/ioutil"
	"net"
	"net/http"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/server/config"
	op "github.com/Tencent/bk-ci/src/booster/server/pkg/resource/crm/operator"

	"github.com/ghodss/yaml"
	appsV1 "k8s.io/api/apps/v1"
	coreV1 "k8s.io/api/core/v1"
	metaV1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/fields"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
)

var (
	EnableBCSApiGw = ""
)

// define const vars
const (
	bcsAPIK8SBaseURI   = "%s/tunnels/clusters/%s/"
	bcsAPIGWK8SBaseURI = "%s/clusters/%s/"

	disableLabel = "tbs/disabled"
	appLabel     = "tbs/name"

	specificPort = 31000

	envKeyHostPort = "HOST_PORT_"
	envKeyRandPort = "RAND_PORT_"

	templateVarImage            = "__crm_image__"
	templateVarName             = "__crm_name__"
	templateVarNamespace        = "__crm_namespace__"
	templateVarInstance         = "__crm_instance__"
	templateVarCPU              = "__crm_cpu__"
	templateVarMem              = "__crm_mem__"
	templateRequestVarCPU       = "__crm_request_cpu__"
	templateRequestVarMem       = "__crm_request_mem__"
	templateVarEnv              = "__crm_env__"
	templateVarEnvKey           = "__crm_env_key__"
	templateVarEnvValue         = "__crm_env_value__"
	templateVarPorts            = "__crm_ports__"
	templateVarPortsName        = "__crm_ports_name__"
	templateVarPortsContainer   = "__crm_ports_container__"
	templateVarPortsHost        = "__crm_ports_host__"
	templateVarPlatform         = "__crm_platform__"
	templateVarPlatformKey      = "__crm_platform_key__"
	templateVarCity             = "__crm_city__"
	templateVarCityKey          = "__crm_city_key__"
	templateVarVolumeMounts     = "__crm_volume_mounts__"
	templateVarVolumes          = "__crm_volumes__"
	templateVarVolumeMountsName = "__crm_volume_mounts_name__"
	templateVarVolumeMountsPath = "__crm_volume_mounts_path__"
	templateVarVolumeHostPath   = "__crm_volume_host_path__"
	templateVarRandPortNames    = "__crm_rand_port_names__"
	templateVarHostNetwork      = "__crm_host_network__"

	templateContentEnv = "" +
		"        - name: __crm_env_key__\n" +
		"          value: __crm_env_value__"

	templateContentPorts = "" +
		"        - name: __crm_ports_name__\n" +
		"          containerPort: __crm_ports_container__\n" +
		"          hostPort: __crm_ports_host__"

	templateContentVolumeMounts = "" +
		"        - mountPath: __crm_volume_mounts_path__\n" +
		"          name: __crm_volume_mounts_name__"

	templateContentVolumes = "" +
		"      - name: __crm_volume_mounts_name__\n" +
		"        hostPath:\n" +
		"          path: __crm_volume_host_path__\n" +
		"          type: DirectoryOrCreate"
)

// NewOperator get a new operator.
// TODO: For now, k8s operator do not support to deploy multi instances in one node(all pods with some host port).
//  So the request_cpu must big enough to occupy whole resource in one node. This should be solved later, and handle
//  the ports managements.
func NewOperator(conf *config.ContainerResourceConfig) (op.Operator, error) {
	data, err := ioutil.ReadFile(conf.BcsAppTemplate)
	if err != nil {
		blog.Errorf("get new operator, read template file failed: %v", err)
		return nil, err
	}
	blog.Infof("crm: load bcs application template: \n%s", string(data))

	o := &operator{
		conf:               conf,
		templates:          string(data),
		clusterClientCache: make(map[string]*clusterClientSet),
		clusterCacheLock:   make(map[string]*sync.Mutex),
		disableWinHostNW:   conf.BcsDisableWinHostNW,
	}
	o.cityLabelKey = o.getCityLabelKey()
	o.platformLabelKey = o.getPlatformLabelKeyLabelKey()
	return o, nil
}

// k8s operator for bcs operations
type operator struct {
	conf      *config.ContainerResourceConfig
	templates string

	clusterClientCache map[string]*clusterClientSet
	cacheLock          sync.RWMutex
	clusterCacheLock   map[string]*sync.Mutex

	cityLabelKey     string
	platformLabelKey string
	disableWinHostNW bool
}

type clusterClientSet struct {
	clientSet   *kubernetes.Clientset
	timeoutTime time.Time
}

// GetResource get specific cluster's resources.
func (o *operator) GetResource(clusterID string) ([]*op.NodeInfo, error) {
	return o.getResource(clusterID)
}

// GetServerStatus get the specific service(application and its taskgroup) status.
func (o *operator) GetServerStatus(clusterID, namespace, name string) (*op.ServiceInfo, error) {
	return o.getServerStatus(clusterID, namespace, name)
}

// LaunchServer launch a new service with given bcsLaunchParam.
func (o *operator) LaunchServer(clusterID string, param op.BcsLaunchParam) error {
	return o.launchServer(clusterID, param)
}

// ScaleServer scale worker instances of a existing service.
func (o *operator) ScaleServer(clusterID string, namespace, name string, instance int) error {
	return nil
}

// ReleaseServer release the specific service(application).
func (o *operator) ReleaseServer(clusterID, namespace, name string) error {
	return o.releaseServer(clusterID, namespace, name)
}

func (o *operator) getResource(clusterID string) ([]*op.NodeInfo, error) {
	blog.Debugf("k8s-operator: get resource for clusterID(%s)", clusterID)
	client, err := o.getClientSet(clusterID)
	if err != nil {
		blog.Errorf("k8s-operator: try to get resource from clusterID(%s) and get client set failed: %v",
			clusterID, err)
		return nil, err
	}
	nodeList, err := client.clientSet.CoreV1().Nodes().List(context.TODO(), metaV1.ListOptions{})
	if err != nil {
		blog.Errorf("k8s-operator: get node resource from k8s failed clusterID(%s): %v", clusterID, err)
		return nil, err
	}

	fieldSelector, err := fields.ParseSelector(
		"status.phase!=" + string(coreV1.PodSucceeded) + ",status.phase!=" + string(coreV1.PodFailed))
	if err != nil {
		blog.Errorf("k8s-operator: generate field selector for k8s nodes failed: %v", err)
		return nil, err
	}
	nodeNonTerminatedPodsList, err := client.clientSet.CoreV1().Pods("").
		List(context.TODO(), metaV1.ListOptions{FieldSelector: fieldSelector.String()})

	nodeInfoList := make([]*op.NodeInfo, 0, 1000)
	for _, node := range nodeList.Items {

		// get internal ip from status
		ip := ""
		for _, addr := range node.Status.Addresses {
			if addr.Type == coreV1.NodeInternalIP {
				ip = addr.Address
				break
			}
		}
		if ip == "" {
			blog.Errorf("k8s-operator: get node(%s) address internal ip empty, clusterID(%s)",
				node.Name, clusterID)
			continue
		}

		allocatedResource := getPodsTotalRequests(node.Name, nodeNonTerminatedPodsList)

		// get disable information from labels
		dl, _ := node.Labels[disableLabel]
		disabled := dl == "true"

		// use city-label-key value and platform-label-key to overwrite the city and platform
		node.Labels[op.AttributeKeyCity], _ = node.Labels[o.cityLabelKey]
		node.Labels[op.AttributeKeyPlatform], _ = node.Labels[o.platformLabelKey]
		nodeInfoList = append(nodeInfoList, &op.NodeInfo{
			IP:         ip,
			Hostname:   node.Name,
			DiskTotal:  float64(node.Status.Capacity.StorageEphemeral().Value()),
			MemTotal:   float64(node.Status.Capacity.Memory().Value()) / 1024 / 1024,
			CPUTotal:   float64(node.Status.Capacity.Cpu().Value()),
			DiskUsed:   float64(allocatedResource.StorageEphemeral().Value()),
			MemUsed:    float64(allocatedResource.Memory().Value()) / 1024 / 1024,
			CPUUsed:    float64(allocatedResource.Cpu().Value()),
			Attributes: node.Labels,

			Disabled: disabled,
		})
	}

	blog.Debugf("k8s-operator: success to get resource clusterID(%s)", clusterID)
	return nodeInfoList, nil
}

func (o *operator) getServerStatus(clusterID, namespace, name string) (*op.ServiceInfo, error) {
	info := &op.ServiceInfo{}

	if err := o.getDeployments(clusterID, namespace, name, info); err != nil {
		blog.Errorf("k8s-operator: get server status, get deployments failed: %v", err)
		return nil, err
	}

	if err := o.getPods(clusterID, namespace, name, info); err != nil {
		blog.Errorf("k8s-operator: get server status, get pods failed: %v", err)
		return nil, err
	}

	return info, nil
}

func (o *operator) getDeployments(clusterID, namespace, name string, info *op.ServiceInfo) error {
	client, err := o.getClientSet(clusterID)
	if err != nil {
		blog.Errorf("k8s-operator: try to get deployment clusterID(%s) namespace(%s) name(%s) "+
			"and get client set failed: %v", clusterID, namespace, name, err)
		return err
	}

	deploy, err := client.clientSet.AppsV1().Deployments(namespace).Get(context.TODO(), name, metaV1.GetOptions{})
	if err != nil {
		blog.Errorf("k8s-operator: get deployment clusterID(%s) namespace(%s) name(%s) failed: %v",
			clusterID, namespace, name, err)
		return err
	}

	info.Status = op.ServiceStatusRunning
	info.RequestInstances = int(deploy.Status.Replicas)
	if deploy.Status.UnavailableReplicas > 0 {
		info.Status = op.ServiceStatusStaging
	}

	blog.Debugf("k8s-operator: get deployment successfully, AppName(%s) NS(%s)",
		name, namespace)
	return nil
}

func (o *operator) getPods(clusterID, namespace, name string, info *op.ServiceInfo) error {
	client, err := o.getClientSet(clusterID)
	if err != nil {
		blog.Errorf("k8s-operator: try to get deployment clusterID(%s) namespace(%s) name(%s) "+
			"and get client set failed: %v", clusterID, namespace, name, err)
		return err
	}

	podList, err := client.clientSet.CoreV1().Pods(namespace).List(context.TODO(), metaV1.ListOptions{
		LabelSelector: fmt.Sprintf("%s=%s", appLabel, name),
	})

	availableEndpoint := make([]*op.Endpoint, 0, 100)
	for _, pod := range podList.Items {
		if pod.Status.Phase != coreV1.PodRunning {
			if (info.Status != op.ServiceStatusStaging) && (pod.Status.Phase == coreV1.PodPending) {
				blog.Warnf("k8s-operator: there is still a pod(%s) in status(%s), "+
					"server status will be set to staging by force", pod.Name, pod.Status.Phase)
				info.Status = op.ServiceStatusStaging
			}
			continue
		}

		if len(pod.Status.ContainerStatuses) <= 0 || len(pod.Spec.Containers) <= 0 {
			continue
		}

		ports := make(map[string]int)
		for _, port := range pod.Spec.Containers[0].Ports {
			ports[k8sPort2EnginePort(port.Name)] = int(port.HostPort)
		}

		availableEndpoint = append(availableEndpoint, &op.Endpoint{
			IP:    pod.Status.HostIP,
			Ports: ports,
		})
	}

	// if taskgroup are not all built, just means that the application is staging yet.
	if (info.RequestInstances < len(podList.Items)) && info.Status != op.ServiceStatusStaging {
		info.Status = op.ServiceStatusStaging
	}

	info.CurrentInstances = len(availableEndpoint)
	info.AvailableEndpoints = availableEndpoint
	return nil
}

func (o *operator) launchServer(clusterID string, param op.BcsLaunchParam) error {
	yamlData, err := o.getYAMLFromTemplate(param)
	if err != nil {
		blog.Errorf("k8s-operator: launch server for clusterID(%s) namespace(%s) name(%s) "+
			"get json from template failed: %v", clusterID, param.Namespace, param.Name, err)
		return err
	}

	blog.Infof("k8s-operator: launch deployment, clusterID(%s) namespace(%s) name(%s), "+
		"yaml:\n %s", clusterID, param.Namespace, param.Name, yamlData)
	client, err := o.getClientSet(clusterID)
	if err != nil {
		blog.Errorf("k8s-operator: try to launch server for clusterID(%s) namespace(%s) name(%s) "+
			"and get client set failed: %v", clusterID, param.Namespace, param.Name, err)
		return err
	}

	var deployment appsV1.Deployment
	if err = yaml.Unmarshal([]byte(yamlData), &deployment); err != nil {
		blog.Errorf("k8s-operator: create deployment namespace(%s) name(%s) from clusterID(%s), "+
			"decode from data failed: %v",
			param.Namespace, param.Name, clusterID, err)
		return err
	}

	if _, err = client.clientSet.AppsV1().Deployments(param.Namespace).
		Create(context.TODO(), &deployment, metaV1.CreateOptions{}); err != nil {
		blog.Errorf("k8s-operator: create deployment namespace(%s) name(%s) from clusterID(%s) failed: %v",
			param.Namespace, param.Name, clusterID, err)
		return err
	}

	blog.Infof("k8s-operator: success to create deployment namespace(%s) name(%s) from clusterID(%s)",
		param.Namespace, param.Name, clusterID)
	return nil
}

func (o *operator) scaleServer(clusterID, namespace, name string, instance int) error {
	return nil
}

func (o *operator) releaseServer(clusterID, namespace, name string) error {
	blog.Infof("k8s-operator: release server: clusterID(%s) namespace(%s) name(%s)",
		clusterID, namespace, name)
	client, err := o.getClientSet(clusterID)
	if err != nil {
		blog.Errorf("k8s-operator: try to release server for clusterID(%s) namespace(%s) name(%s) "+
			"and get client set failed: %v", clusterID, namespace, name, err)
		return err
	}

	var gracePeriodSeconds int64 = 0
	propagationPolicy := metaV1.DeletePropagationBackground
	if err = client.clientSet.AppsV1().Deployments(namespace).
		Delete(
			context.TODO(),
			name,
			metaV1.DeleteOptions{GracePeriodSeconds: &gracePeriodSeconds, PropagationPolicy: &propagationPolicy},
		); err != nil {
		if strings.Contains(err.Error(), "not found") {
			blog.Warnf("k8s-operator: release server clusterID(%s) namespace(%s) name(%s) not found, "+
				"regarded as released: %v", clusterID, namespace, name, err)
			return nil
		}

		blog.Errorf("k8s-operator: release server for clusterID(%s) namespace(%s) name(%s) failed: %v",
			clusterID, namespace, name, err)
		return err
	}
	blog.Infof("k8s-operator: success to release server: clusterID(%s) namespace(%s) name(%s)",
		clusterID, namespace, name)
	return nil
}

type portsMap struct {
	protocol string
	port     int
}

func (o *operator) getYAMLFromTemplate(param op.BcsLaunchParam) (string, error) {
	// add host port to env
	index := 0
	pm := make(map[string]portsMap)
	randPortsNames := make([]string, 0, 10)
	for port := range param.Ports {
		portNum := specificPort + index

		param.Env[envKeyHostPort+port] = fmt.Sprintf("%d", portNum)
		param.Env[envKeyRandPort+port] = fmt.Sprintf("%d", portNum)
		pm[port] = portsMap{
			protocol: param.Ports[port],
			port:     portNum,
		}
		index += 1

		randPortsNames = append(randPortsNames, enginePort2K8SPort(port))
	}

	data := o.templates
	data = strings.ReplaceAll(data, templateVarImage, param.Image)
	data = strings.ReplaceAll(data, templateVarName, param.Name)
	data = strings.ReplaceAll(data, templateVarNamespace, param.Namespace)
	data = strings.ReplaceAll(data, templateVarInstance, strconv.Itoa(param.Instance))
	data = strings.ReplaceAll(data, templateVarRandPortNames, strings.Join(randPortsNames, ","))
	data = insertYamlPorts(data, pm)
	data = insertYamlEnv(data, param.Env)
	data = insertYamlVolumes(data, param.Volumes)

	// set platform
	platform := "linux"
	networkValue := ""
	if v, ok := param.AttributeCondition[op.AttributeKeyPlatform]; ok {
		switch v {
		case "windows", "WINDOWS", "win", "WIN":
			platform = "windows"
			if !o.disableWinHostNW {
				networkValue = "hostNetwork: true"
			}
		}
	}

	// handle host network settings for k8s-windows need it, but linux not.
	data = strings.ReplaceAll(data, templateVarHostNetwork, networkValue)
	data = strings.ReplaceAll(data, templateVarPlatform, platform)
	data = strings.ReplaceAll(data, templateVarPlatformKey, o.platformLabelKey)

	// set city
	if _, ok := param.AttributeCondition[op.AttributeKeyCity]; !ok {
		return "", fmt.Errorf("unknown city for yaml")
	}
	city := param.AttributeCondition[op.AttributeKeyCity]
	data = strings.ReplaceAll(data, templateVarCity, city)
	data = strings.ReplaceAll(data, templateVarCityKey, o.cityLabelKey)

	varCPU := o.conf.BcsCPUPerInstance
	varMem := o.conf.BcsMemPerInstance
	varRequestCPU := o.conf.BcsCPUPerInstance
	varRequestMem := o.conf.BcsMemPerInstance
	for _, istItem := range o.conf.InstanceType {
		if !param.CheckQueueKey(istItem) {
			continue
		}
		if istItem.CPUPerInstance > 0.0 {
			varCPU = istItem.CPUPerInstance
			varRequestCPU = istItem.CPUPerInstance
		}
		if istItem.MemPerInstance > 0.0 {
			varMem = istItem.MemPerInstance
			varRequestMem = istItem.MemPerInstance
		}
		if istItem.CPURequestPerInstance > 0.0 {
			varRequestCPU = istItem.CPURequestPerInstance
		}
		if istItem.MemRequestPerInstance > 0.0 {
			varRequestMem = istItem.MemRequestPerInstance
		}
		break
	}
	data = strings.ReplaceAll(data, templateVarCPU, fmt.Sprintf("%.2f", varCPU*1000))
	data = strings.ReplaceAll(data, templateVarMem, fmt.Sprintf("%.2f", varMem))
	data = strings.ReplaceAll(data, templateRequestVarCPU, fmt.Sprintf("%.2f", varRequestCPU*1000))
	data = strings.ReplaceAll(data, templateRequestVarMem, fmt.Sprintf("%.2f", varRequestMem))
	return data, nil
}

func (o *operator) getClientSet(clusterID string) (*clusterClientSet, error) {
	// check if the client-set of this clusterID exists
	cs, ok := o.getClientSetFromCache(clusterID)
	if ok {
		return cs, nil
	}

	// make sure the cluster-cache-lock exist for this clusterID
	o.cacheLock.Lock()
	if _, ok = o.clusterCacheLock[clusterID]; !ok {
		o.clusterCacheLock[clusterID] = new(sync.Mutex)
	}
	cacheLock := o.clusterCacheLock[clusterID]
	o.cacheLock.Unlock()

	// lock cluster-cache-lock of this clusterID, and then check client-set again
	// else go generate new client.
	cacheLock.Lock()
	defer cacheLock.Unlock()
	cs, ok = o.getClientSetFromCache(clusterID)

	if ok {
		return cs, nil
	}
	return o.generateClient(clusterID)
}

func (o *operator) getClientSetFromCache(clusterID string) (*clusterClientSet, bool) {
	o.cacheLock.RLock()

	defer o.cacheLock.RUnlock()
	cs, ok := o.clusterClientCache[clusterID]

	if ok && cs.timeoutTime.Before(time.Now().Local()) {
		blog.Debugf("k8s-operator: the client from cache is out of date since(%s), should be regenerated",
			cs.timeoutTime.String())
		return nil, false
	}
	return cs, ok
}

func (o *operator) generateClient(clusterID string) (*clusterClientSet, error) {
	address := o.conf.BcsAPIPool.GetAddress()
	host := fmt.Sprintf(getBcsK8SBaseUri(), address, clusterID)

	blog.Infof("k8s-operator: try generate client with host(%s) token(%s)", host, o.conf.BcsAPIToken)
	// get client set by real api-server address
	c := &rest.Config{
		Host:        host,
		BearerToken: o.conf.BcsAPIToken,
		QPS:         1e6,
		Burst:       1e6,
		Transport: &http.Transport{
			TLSHandshakeTimeout: 5 * time.Second,
			DialContext: (&net.Dialer{
				Timeout:   5 * time.Second,
				KeepAlive: 30 * time.Second,
			}).DialContext,
			ResponseHeaderTimeout: 30 * time.Second,
			TLSClientConfig:       &tls.Config{InsecureSkipVerify: true},
		},
	}

	blog.Infof("k8s-operator: get client set, create new client set for cluster(%s), config: %v",
		clusterID, c)
	clientSet, err := kubernetes.NewForConfig(c)
	if err != nil {
		blog.Errorf("k8s-operator: get client set(%s), create new client set failed: %v", clusterID, err)
		return nil, err
	}

	cs := &clusterClientSet{
		clientSet:   clientSet,
		timeoutTime: time.Now().Local().Add(1 * time.Minute),
	}
	o.cacheLock.Lock()
	o.clusterClientCache[clusterID] = cs
	o.cacheLock.Unlock()
	return cs, nil
}

func (o *operator) getCityLabelKey() string {
	if o.conf.BcsGroupLabelKey != "" {
		return o.conf.BcsGroupLabelKey
	}

	return op.AttributeKeyCity
}

func (o *operator) getPlatformLabelKeyLabelKey() string {
	if o.conf.BcsPlatformLabelKey != "" {
		return o.conf.BcsPlatformLabelKey
	}

	return "kubernetes.io/os"
}

func insertYamlPorts(data string, ports map[string]portsMap) string {
	portsYaml := ""
	index := 0

	for name, port := range ports {
		// for k8s storage rule.
		portName := enginePort2K8SPort(name)

		content := templateContentPorts
		content = strings.ReplaceAll(content, templateVarPortsName, portName)
		content = strings.ReplaceAll(content, templateVarPortsContainer, fmt.Sprintf("%d", port.port))
		content = strings.ReplaceAll(content, templateVarPortsHost, fmt.Sprintf("%d", port.port))

		portsYaml += "\n" + content

		index += 1
	}

	data = strings.ReplaceAll(data, templateVarPorts, portsYaml)
	return data
}

func insertYamlEnv(data string, env map[string]string) string {
	envYaml := ""

	for k, v := range env {
		content := templateContentEnv
		content = strings.ReplaceAll(content, templateVarEnvKey, k)
		content = strings.ReplaceAll(content, templateVarEnvValue, v)
		envYaml += "\n" + content
	}

	return strings.ReplaceAll(data, templateVarEnv, envYaml)
}

func insertYamlVolumes(data string, volumes map[string]op.BcsVolume) string {
	volumeMountsYaml := ""

	for k, v := range volumes {
		content := templateContentVolumeMounts
		content = strings.ReplaceAll(content, templateVarVolumeMountsPath, v.ContainerDir)
		content = strings.ReplaceAll(content, templateVarVolumeMountsName, k)
		volumeMountsYaml += "\n" + content
	}

	volumesYaml := ""

	for k, v := range volumes {
		content := templateContentVolumes
		content = strings.ReplaceAll(content, templateVarVolumeMountsName, k)
		content = strings.ReplaceAll(content, templateVarVolumeHostPath, v.HostDir)
		volumesYaml += "\n" + content
	}

	data = strings.ReplaceAll(data, templateVarVolumeMounts, volumeMountsYaml)
	data = strings.ReplaceAll(data, templateVarVolumes, volumesYaml)
	return data
}

func enginePort2K8SPort(name string) string {
	return strings.ReplaceAll(strings.ToLower(name), "_", "-")
}

func k8sPort2EnginePort(name string) string {
	return strings.ReplaceAll(strings.ToUpper(name), "-", "_")
}

func getPodsTotalRequests(nodeName string, podList *coreV1.PodList) coreV1.ResourceList {
	requests := make(coreV1.ResourceList)

	for _, pod := range podList.Items {
		if pod.Spec.NodeName != nodeName {
			continue
		}

		podRequests := podRequests(&pod)

		for podName, podRequestValue := range podRequests {
			if value, ok := requests[podName]; !ok {
				requests[podName] = podRequestValue.DeepCopy()
			} else {
				value.Add(podRequestValue)
				requests[podName] = value
			}
		}
	}

	return requests
}

func podRequests(pod *coreV1.Pod) coreV1.ResourceList {
	requests := coreV1.ResourceList{}
	for _, container := range pod.Spec.Containers {
		addResourceList(requests, container.Resources.Requests)
	}
	// init containers define the minimum of any resource
	for _, container := range pod.Spec.InitContainers {
		maxResourceList(requests, container.Resources.Requests)
	}

	return requests
}

// addResourceList adds the resources in newList to list
func addResourceList(list, new coreV1.ResourceList) {
	for name, quantity := range new {
		if value, ok := list[name]; !ok {
			list[name] = quantity.DeepCopy()
		} else {
			value.Add(quantity)
			list[name] = value
		}
	}
}

// maxResourceList sets list to the greater of list/newList for every resource
// either list
func maxResourceList(list, new coreV1.ResourceList) {
	for name, quantity := range new {
		if value, ok := list[name]; !ok {
			list[name] = quantity.DeepCopy()
			continue
		} else {
			if quantity.Cmp(value) > 0 {
				list[name] = quantity.DeepCopy()
			}
		}
	}
}

func getBcsK8SBaseUri() string {
	if len(EnableBCSApiGw) > 0 {
		return bcsAPIGWK8SBaseURI
	}

	return bcsAPIK8SBaseURI
}
