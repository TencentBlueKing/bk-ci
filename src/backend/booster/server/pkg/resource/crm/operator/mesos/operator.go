package mesos

import (
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	commonHttp "github.com/Tencent/bk-ci/src/booster/common/http"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpclient"
	commonTypes "github.com/Tencent/bk-ci/src/booster/common/types"
	"github.com/Tencent/bk-ci/src/booster/server/config"
	op "github.com/Tencent/bk-ci/src/booster/server/pkg/resource/crm/operator"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/crm/types"
)

const (
	bcsURLGetResource  = "%s/bcsapi/v4/scheduler/mesos/cluster/resources"
	bcsURLCreateApp    = "%s/bcsapi/v4/scheduler/mesos/namespaces/%s/applications"
	bcsURLScaleApp     = "%s/bcsapi/v4/scheduler/mesos/namespaces/%s/applications/%s/scale/%d"
	bcsURLDeleteApp    = "%s/bcsapi/v4/scheduler/mesos/namespaces/%s/applications/%s?enforce=1"
	bcsURLGetApp       = "%s/bcsapi/v4/storage/query/mesos/dynamic/clusters/%s/application"
	bcsURLGetTaskGroup = "%s/bcsapi/v4/storage/query/mesos/dynamic/clusters/%s/taskgroup"

	templateVarImage           = "__crm_image__"
	templateVarName            = "__crm_name__"
	templateVarNamespace       = "__crm_namespace__"
	templateVarInstance        = "__crm_instance__"
	templateVarCPU             = "__crm_cpu__"
	templateVarMem             = "__crm_mem__"
	templateLimitVarCPU        = "__crm_limit_cpu__"
	templateLimitVarMem        = "__crm_limit_mem__"
	templateVarConstraint      = "__crm_constraint__"
	templateVarConstraintKey   = "__crm_constraint_key__"
	templateVarConstraintValue = "__crm_constraint_value__"
	templateVarEnv             = "__crm_env__"
	templateVarEnvKey          = "__crm_env_key__"
	templateVarEnvValue        = "__crm_env_value__"
	templateVarPorts           = "__crm_ports__"
	templateVarPortsProtocol   = "__crm_ports_protocol__"
	templateVarPortsName       = "__crm_ports_names__"

	templateContentConstraint = "{\"unionData\":[{\"name\":\"__crm_constraint_key__\"," +
		"\"operate\":\"LIKE\",\"type\":3,\"text\":{\"value\":\"__crm_constraint_value__\"}}]}"
	templateContentEnv   = "{\"name\":\"__crm_env_key__\",\"value\":\"__crm_env_value__\"}"
	templateContentPorts = "{\"hostPort\":0,\"name\":\"__crm_ports_names__\"," +
		"\"protocol\":\"__crm_ports_protocol__\"}"
)

// NewOperator get a new operator.
func NewOperator(conf *config.ContainerResourceConfig) (op.Operator, error) {
	data, err := ioutil.ReadFile(conf.BcsAppTemplate)
	if err != nil {
		blog.Errorf("get new operator, read template file failed: %v", err)
		return nil, err
	}
	blog.Infof("crm: load bcs application template: \n%s", string(data))

	o := &operator{
		conf:      conf,
		templates: string(data),
	}
	return o, nil
}

type operator struct {
	conf      *config.ContainerResourceConfig
	templates string
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
	return o.scaleServer(clusterID, namespace, name, instance)
}

// ReleaseServer release the specific service(application).
func (o *operator) ReleaseServer(clusterID, namespace, name string) error {
	return o.releaseServer(clusterID, namespace, name)
}

func (o *operator) getResource(clusterID string) ([]*op.NodeInfo, error) {
	uri := fmt.Sprintf(bcsURLGetResource, o.conf.BcsAPIPool.GetAddress())

	blog.Debugf("crm: get resource clusterID(%s): %s", clusterID, uri)
	resp, _, err := o.query(uri, getHeader(clusterID))
	if err != nil {
		blog.Errorf("crm: get resource failed url(%s) clusterID(%s): %v", uri, clusterID, err)
		return nil, err
	}
	var data []byte
	if err = codec.EncJSON(resp.Data, &data); err != nil {
		blog.Errorf("crm: get resource encode data failed url(%s) clusterID(%s): %v", uri, clusterID, err)
		return nil, err
	}

	var info commonTypes.BcsClusterResource
	if err = codec.DecJSON(data, &info); err != nil {
		blog.Errorf("crm: get resource decode failed url(%s) clusterID(%s): %v", uri, clusterID, err)
		return nil, err
	}

	nodeInfoList := make([]*op.NodeInfo, 0, 1000)
	for _, agent := range info.Agents {
		attributes := make(map[string]string, 10)
		for _, attr := range agent.Attributes {
			if attr.Text != nil {
				attributes[attr.Name] = attr.Text.Value
			}
		}
		attributes[op.AttributeKeyPlatform] = "linux"

		nodeInfoList = append(nodeInfoList, &op.NodeInfo{
			IP:         agent.IP,
			Hostname:   agent.HostName,
			DiskTotal:  agent.DiskTotal,
			MemTotal:   agent.MemTotal,
			CPUTotal:   agent.CPUTotal,
			DiskUsed:   agent.DiskUsed,
			MemUsed:    agent.MemUsed,
			CPUUsed:    agent.CPUUsed,
			Attributes: attributes,

			Disabled: agent.Disabled,
		})
	}

	blog.Debugf("crm: success to get resource clusterID(%s): %s", clusterID, uri)
	return nodeInfoList, nil
}

func (o *operator) getServerStatus(clusterID, namespace, name string) (*op.ServiceInfo, error) {
	info := &op.ServiceInfo{}

	if err := o.getApplication(clusterID, namespace, name, info); err != nil {
		blog.Errorf("get server status, clusterId(%s), ns(%s), name(%s) get application failed: %v",
			clusterID, namespace, name, err)
		return nil, err
	}

	if err := o.getTaskGroup(clusterID, namespace, name, info); err != nil {
		blog.Errorf("get server status,  clusterId(%s), ns(%s), name(%s) get taskGroup failed: %v",
			clusterID, namespace, name, err)
		return nil, err
	}

	return info, nil
}

func (o *operator) getApplication(clusterID, namespace, name string, info *op.ServiceInfo) error {
	param := make(url.Values)
	param.Add("name", name)
	param.Add("namespace", namespace)
	uri := fmt.Sprintf(bcsURLGetApp, o.conf.BcsAPIPool.GetAddress(), clusterID) + "?" + param.Encode()

	blog.Debugf("crm: get application: %s", uri)
	resp, _, err := o.query(uri, getHeader(clusterID))
	if err != nil {
		blog.Errorf("crm: get application failed url(%s): %v", uri, err)
		return err
	}
	var data []byte
	if err = codec.EncJSON(resp.Data, &data); err != nil {
		blog.Errorf("crm: get application encode data failed url(%s): %v", uri, err)
		return err
	}

	var appList []*applicationData
	if err = codec.DecJSON(data, &appList); err != nil {
		blog.Errorf("crm: get application decode failed url(%s): %v", uri, err)
		return err
	}

	if len(appList) == 0 {
		blog.Warnf("crm: get application failed url(%s): application no found Name(%s) Namespace(%s)",
			uri, name, namespace)
		return types.ErrorApplicationNoFound
	}

	app := appList[0].Data
	switch app.Status {
	case commonTypes.RCStaging, commonTypes.RCDeploying, commonTypes.RCOperating:
		info.Status = op.ServiceStatusStaging
	case commonTypes.RCRunning:
		info.Status = op.ServiceStatusRunning
	case commonTypes.RCFinish, commonTypes.RCError:
		info.Status = op.ServiceStatusFailed
	default:
		info.Status = op.ServiceStatusFailed
		blog.Warnf("crm: get application(%s) status: %s, default to FAILED", app.Name, app.Status)
	}

	info.Message = app.Message
	info.RequestInstances = app.Instance

	blog.Debugf("crm: get application successfully, AppName(%s) NS(%s) Status(%s)",
		name, namespace, app.Status)
	return nil
}

func (o *operator) getTaskGroup(clusterID, namespace, name string, info *op.ServiceInfo) error {
	param := make(url.Values)
	param.Add("rcName", name)
	param.Add("namespace", namespace)
	uri := fmt.Sprintf(bcsURLGetTaskGroup, o.conf.BcsAPIPool.GetAddress(), clusterID) + "?" + param.Encode()

	blog.Debugf("crm: get taskGroup: %s", uri)
	resp, _, err := o.query(uri, getHeader(clusterID))
	if err != nil {
		blog.Errorf("crm: get taskGroup failed url(%s): %v", uri, err)
		return err
	}

	var data []byte
	if err = codec.EncJSON(resp.Data, &data); err != nil {
		blog.Errorf("crm: get taskGroup encode data failed url(%s): %v", uri, err)
		return err
	}

	blog.Debugf("crm: get taskGroup(%s): %s", name, string(data))

	var taskGroupList []*taskGroupData
	if err = codec.DecJSON(data, &taskGroupList); err != nil {
		blog.Errorf("crm: get taskGroup decode failed url(%s): %v", uri, err)
		return err
	}

	availableEndpoint := make([]*op.Endpoint, 0, 100)
	for _, taskGroupRaw := range taskGroupList {
		taskGroup := taskGroupRaw.Data

		// In case of info.Status is terminated but some taskgroups are not terminated,
		// then change info.Status to Staging.
		if taskGroup.Status != commonTypes.PodRunning {
			if (info.Status != op.ServiceStatusStaging) &&
				(taskGroup.Status == commonTypes.PodStaging || taskGroup.Status == commonTypes.PodStarting) {

				blog.Warnf(
					"crm: there is still a taskgroup(%s) in status(%s), "+
						"server status will be set to staging by force", taskGroup.Name, taskGroup.Status)
				info.Status = op.ServiceStatusStaging
			}
			continue
		}

		if len(taskGroup.ContainerStatuses) <= 0 {
			continue
		}

		ports := make(map[string]int, 10)
		for _, container := range taskGroup.ContainerStatuses {
			for _, p := range container.Ports {
				if p.HostPort == 0 {
					continue
				}

				ports[p.Name] = p.HostPort
			}
		}

		availableEndpoint = append(availableEndpoint, &op.Endpoint{
			IP:    taskGroup.HostIP,
			Ports: ports,
		})
	}

	// if taskgroup are not all built, just means that the application is staging yet.
	if (info.RequestInstances < len(taskGroupList)) && info.Status != op.ServiceStatusStaging {
		info.Status = op.ServiceStatusStaging
	}

	info.CurrentInstances = len(availableEndpoint)
	info.AvailableEndpoints = availableEndpoint
	return nil
}

func (o *operator) launchServer(clusterID string, param op.BcsLaunchParam) error {
	uri := fmt.Sprintf(bcsURLCreateApp, o.conf.BcsAPIPool.GetAddress(), param.Namespace)
	jsonData, err := o.getJSONFromTemplate(param)
	if err != nil {
		blog.Errorf("crm: launch server get json from template failed: %v", err)
		return err
	}

	blog.Infof("crm: launch application: %s, clusterID(%s)\njson: %s", uri, clusterID, jsonData)
	if _, _, err = o.post(uri, getHeader(clusterID), []byte(jsonData)); err != nil {
		blog.Errorf("crm: launch server failed: %v", err)
		return err
	}
	blog.Infof("crm: success to launch application: %s, clusterID(%s)", uri, clusterID)
	return nil
}

func (o *operator) scaleServer(clusterID, namespace, name string, instance int) error {
	uri := fmt.Sprintf(bcsURLScaleApp, o.conf.BcsAPIPool.GetAddress(), namespace, name, instance)

	blog.Infof("crm: scale server: %s, clusterID(%s)", uri, clusterID)
	if _, _, err := o.put(uri, getHeader(clusterID), nil); err != nil {
		blog.Errorf("crm: scale server failed: %v", err)
		return err
	}
	blog.Infof("crm: success to scale server: %s, clusterID(%s)", uri, clusterID)
	return nil
}

func (o *operator) releaseServer(clusterID, namespace, name string) error {
	uri := fmt.Sprintf(bcsURLDeleteApp, o.conf.BcsAPIPool.GetAddress(), namespace, name)

	blog.Infof("crm: release server: %s, clusterID(%s)", uri, clusterID)
	_, _, err := o.delete(uri, getHeader(clusterID), nil)
	if err != nil {
		blog.Errorf("crm: release server failed: %v", err)
		return nil
	}
	blog.Infof("crm: success to release server: %s, clusterID(%s)", uri, clusterID)
	return nil
}

func (o *operator) query(uri string, header http.Header) (resp *commonHttp.APIResponse, raw []byte, err error) {
	return o.request("GET", uri, header, nil)
}

func (o *operator) post(uri string, header http.Header, data []byte) (
	resp *commonHttp.APIResponse, raw []byte, err error) {
	return o.request("POST", uri, header, data)
}

func (o *operator) put(uri string, header http.Header, data []byte) (
	resp *commonHttp.APIResponse, raw []byte, err error) {
	return o.request("PUT", uri, header, data)
}

func (o *operator) delete(uri string, header http.Header, data []byte) (
	resp *commonHttp.APIResponse, raw []byte, err error) {
	return o.request("DELETE", uri, header, data)
}

func (o *operator) getClient() *httpclient.HTTPClient {
	return httpclient.NewHTTPClient()
}

func (o *operator) request(method, uri string, header http.Header, data []byte) (
	resp *commonHttp.APIResponse, raw []byte, err error) {
	var r *httpclient.HttpResponse

	client := o.getClient()
	before := time.Now().Local()

	// add auth token in header
	if header == nil {
		header = http.Header{}
	}
	header.Set("Authorization", fmt.Sprintf("Bearer %s", o.conf.BcsAPIToken))
	switch strings.ToUpper(method) {
	case "GET":
		if r, err = client.Get(uri, header, data); err != nil {
			return
		}
	case "POST":
		if r, err = client.Post(uri, header, data); err != nil {
			return
		}
	case "PUT":
		if r, err = client.Put(uri, header, data); err != nil {
			return
		}
	case "DELETE":
		if r, err = client.Delete(uri, header, data); err != nil {
			return
		}
	}

	now := time.Now().Local()
	blog.V(5).Infof("crm: operator request [%s] %s for record: %s", method, uri, now.Sub(before).String())
	if before.Add(1 * time.Second).Before(now) {
		blog.Warnf("crm: operator request [%s] %s for too long: %s", method, uri, now.Sub(before).String())
	}

	if r.StatusCode != http.StatusOK {
		err = fmt.Errorf("crm: failed to request, http(%d)%s: %s", r.StatusCode, r.Status, uri)
		return
	}

	if err = codec.DecJSON(r.Reply, &resp); err != nil {
		err = fmt.Errorf("crm: %v: %s", err, string(r.Reply))
		return
	}

	if resp.Code != common.RestSuccess {
		err = fmt.Errorf("crm: failed to request, resp(%d)%s: %s", resp.Code, resp.Message, uri)
		return
	}

	if err = codec.EncJSON(resp.Data, &raw); err != nil {
		return
	}
	return
}

func (o *operator) getJSONFromTemplate(param op.BcsLaunchParam) (string, error) {
	data := o.templates
	data = strings.Replace(data, templateVarImage, param.Image, -1)
	data = strings.Replace(data, templateVarName, param.Name, -1)
	data = strings.Replace(data, templateVarNamespace, param.Namespace, -1)
	data = strings.Replace(data, templateVarInstance, strconv.Itoa(param.Instance), -1)
	varCPU := o.conf.BcsCPUPerInstance
	varMem := o.conf.BcsMemPerInstance
	varLimitCPU := o.conf.BcsCPUPerInstance
	varLimitMem := o.conf.BcsMemPerInstance
	if o.conf.BcsCPULimitPerInstance > 0.0 {
		varLimitCPU = o.conf.BcsCPULimitPerInstance
	}
	if o.conf.BcsMemLimitPerInstance > 0.0 {
		varLimitMem = o.conf.BcsMemLimitPerInstance
	}
	for _, istItem := range o.conf.InstanceType {
		if !param.CheckQueueKey(istItem) {
			continue
		}
		if istItem.CPUPerInstance > 0.0 {
			varCPU = istItem.CPUPerInstance
			varLimitCPU = istItem.CPUPerInstance
		}
		if istItem.MemPerInstance > 0.0 {
			varMem = istItem.MemPerInstance
			varLimitMem = istItem.MemPerInstance
		}
		if istItem.CPULimitPerInstance > 0.0 {
			varLimitCPU = istItem.CPULimitPerInstance
		}
		if istItem.MemLimitPerInstance > 0.0 {
			varLimitMem = istItem.MemLimitPerInstance
		}
		break
	}
	data = strings.Replace(data, templateVarCPU, fmt.Sprintf("%.2f", varCPU), -1)
	data = strings.Replace(data, templateVarMem, fmt.Sprintf("%.2f", varMem), -1)
	data = strings.Replace(data, templateLimitVarCPU, fmt.Sprintf("%.2f", varLimitCPU), -1)
	data = strings.Replace(data, templateLimitVarMem, fmt.Sprintf("%.2f", varLimitMem), -1)

	data = insertConstraint(data, param.AttributeCondition)
	data = insertEnv(data, param.Env)
	data = insertPorts(data, param.Ports)

	return data, nil
}

func insertConstraint(data string, attributeCondition map[string]string) string {
	if _, ok := attributeCondition[op.AttributeKeyPlatform]; ok {
		delete(attributeCondition, op.AttributeKeyPlatform)
	}

	constraintList := make([]string, 0, 10)
	for k, v := range attributeCondition {
		content := templateContentConstraint
		content = strings.Replace(content, templateVarConstraintKey, k, -1)
		content = strings.Replace(content, templateVarConstraintValue, v, -1)

		constraintList = append(constraintList, content)
	}

	return strings.Replace(data, templateVarConstraint, strings.Join(constraintList, ","), -1)
}

func insertEnv(data string, env map[string]string) string {
	envList := make([]string, 0, 10)
	for k, v := range env {
		content := templateContentEnv
		content = strings.Replace(content, templateVarEnvKey, k, -1)
		content = strings.Replace(content, templateVarEnvValue, v, -1)

		envList = append(envList, content)
	}

	return strings.Replace(data, templateVarEnv, strings.Join(envList, ","), -1)
}

func insertPorts(data string, ports map[string]string) string {
	portsList := make([]string, 0, 10)
	for k, v := range ports {
		content := templateContentPorts
		content = strings.Replace(content, templateVarPortsName, k, -1)
		content = strings.Replace(content, templateVarPortsProtocol, v, -1)

		portsList = append(portsList, content)
	}

	return strings.Replace(data, templateVarPorts, strings.Join(portsList, ","), -1)
}

func getHeader(clusterID string) http.Header {
	header := http.Header{}
	header.Set("BCS-ClusterID", clusterID)
	return header
}

type applicationData struct {
	Data *commonTypes.BcsReplicaControllerStatus `json:"data"`
}

type taskGroupData struct {
	Data *commonTypes.BcsPodStatus `json:"data"`
}
