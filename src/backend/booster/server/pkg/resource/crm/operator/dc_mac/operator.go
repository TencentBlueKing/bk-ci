package dcmac

import (
	"crypto/md5"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpclient"
	"github.com/Tencent/bk-ci/src/booster/common/util"
	"github.com/Tencent/bk-ci/src/booster/server/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/rd"
	op "github.com/Tencent/bk-ci/src/booster/server/pkg/resource/crm/operator"
)

const (
	// 随机字符串长度, 用于请求头校验
	randLen = 8

	// devcloud 成功的action code
	okActionCode = 200

	// API uri
	createAPI = "%s/api/mac/vm/create"
	deleteAPI = "%s/api/mac/vm/delete"
	queryAPI  = "%s/api/mac/task/result/%s"

	portsService = "SERVICE_PORT"
	portsStats   = "STATS_PORT"

	dataPathPrefix = "dc_mac"
)

// NewOperator get a new operator
func NewOperator(conf *config.ContainerResourceConfig, rdClient rd.RegisterDiscover) (op.Operator, error) {
	var token Token
	if err := codec.DecJSON([]byte(conf.BcsAPIToken), &token); err != nil {
		blog.Errorf("dc_mac operator: decode token from bcs_api_token(%s) failed: %v", conf.BcsAPIToken, err)
		return nil, fmt.Errorf("decode token from bcs_api_token failed")
	}

	blog.Infof("dc_mac operator: success parse id(%s) token(%s) smart_gate_token(%s) uri(%s) for preparing",
		token.ID, token.Token, token.SmartGateToken, conf.BcsAPIAddress)
	return &operator{
		conf:     conf,
		token:    token,
		rdClient: rdClient,
	}, nil
}

// Token 用于访问devcloud接口
type Token struct {
	ID             string `json:"id"`
	Token          string `json:"token"`
	SmartGateToken string `json:"smart_gate_token"`
}

type operator struct {
	conf  *config.ContainerResourceConfig
	token Token

	rdClient rd.RegisterDiscover

	infoMap map[string]*op.ServiceInfo
}

// GetResource get specific cluster's resources.
func (o *operator) GetResource(clusterID string) ([]*op.NodeInfo, error) {
	return o.getResource(clusterID)
}

// GetServerStatus get the specific service(application and its taskgroup) status.
func (o *operator) GetServerStatus(clusterID, namespace, name string) (*op.ServiceInfo, error) {
	return o.getBatchServerStatus(clusterID, namespace, name)
}

// LaunchServer launch a new service with given bcsLaunchParam.
func (o *operator) LaunchServer(clusterID string, param op.BcsLaunchParam) error {
	return o.launchBatchServer(clusterID, param)
}

// ScaleServer scale worker instances of a existing service.
func (o *operator) ScaleServer(_ string, _, _ string, _ int) error {
	return nil
}

// ReleaseServer release the specific service(application).
func (o *operator) ReleaseServer(clusterID, namespace, name string) error {
	return o.releaseServer(clusterID, namespace, name)
}

func (o *operator) getResource(_ string) ([]*op.NodeInfo, error) {
	return []*op.NodeInfo{{
		IP:        "",
		Hostname:  "",
		DiskTotal: 9999999,
		MemTotal:  9999999,
		CPUTotal:  9999999,
		DiskUsed:  0,
		MemUsed:   0,
		CPUUsed:   0,
		Attributes: map[string]string{
			op.AttributeKeyCity:     "generic",
			op.AttributeKeyPlatform: "darwin",
		},
		Disabled: false,
	}}, nil
}

func (o *operator) getBatchServerStatus(clusterID, namespace, name string) (*op.ServiceInfo, error) {
	path := getDataPath(clusterID, namespace, name)
	r, err := o.rdClient.Get(path)
	if err != nil {
		blog.Errorf("dc_mac operator: get serverStatus from rd %s failed: %v", path, err)
		return nil, err
	}

	var resp CachedCreateData
	if err = codec.DecJSON(r, &resp); err != nil {
		blog.Errorf("dc_mac operator: decode from get serverStatus result(%s) failed: %v", string(r), err)
		return nil, err
	}

	var wg sync.WaitGroup
	results := make([]*DetailData, len(resp.CreateData))
	for index, request := range resp.CreateData {
		// check whether task has succeed
		tasksucceed := false
		for _, detail := range resp.DetailData {
			if detail != nil && detail.Status == DetailStatusSucceeded && detail.TaskID == request.TaskID {
				tasksucceed = true
				results[index] = detail
				blog.Infof("dc_mac operator: vm %d has succeed for clusterID(%s) "+
					"namespace(%s) name(%s), do not query again", detail.Id, clusterID, namespace, name)
			}
		}
		if tasksucceed {
			continue
		}

		wg.Add(1)
		go func(idx int, taskid string) {
			defer wg.Done()
			rs, err := o.getServerStatus(taskid)
			if err != nil {
				blog.Errorf("dc_mac operator: query batch server failed: %v", err)
				return
			}

			results[idx] = rs.Data
			if rs.Data != nil && rs.Data.Status == DetailStatusSucceeded {
				blog.Infof("dc_mac operator: success to launch vm %d for clusterID(%s) "+
					"namespace(%s) name(%s) data(%+v)", rs.Data.Id, clusterID, namespace, name, *rs.Data)
			} else if rs.Data != nil && rs.Data.Status == DetailStatusFailed {
				blog.Infof("dc_mac operator: failed to launch vm %d for clusterID(%s) "+
					"namespace(%s) name(%s) data(%+v)", rs.Data.Id, clusterID, namespace, name, *rs.Data)
			}
		}(index, request.TaskID)
	}
	wg.Wait()

	info := &op.ServiceInfo{
		Status: op.ServiceStatusRunning,
	}
	gotallresult := true
	hasstage := false
	for _, result := range results {
		if result == nil {
			gotallresult = false
			continue
		}

		switch result.Status {
		case DetailStatusSucceeded:
			info.AvailableEndpoints = append(info.AvailableEndpoints, &op.Endpoint{
				IP: result.IP,
				Ports: map[string]int{
					portsService: 30821,
					portsStats:   30822,
				},
			})
		case DetailStatusFailed:
			info.Status = op.ServiceStatusFailed
		default:
			hasstage = true
		}
	}

	// wait untail got all result, to avoid resource leak
	if !gotallresult || hasstage {
		info.Status = op.ServiceStatusStaging
	}

	resp.DetailData = results
	var tmp []byte
	_ = codec.EncJSON(resp, &tmp)
	if err = o.rdClient.Put(path, tmp); err != nil {
		blog.Errorf("dc_mac operator: put serverStatus to rd %s failed: %v", path, err)
		return nil, err
	}

	return info, nil
}

func (o *operator) getServerStatus(taskid string) (*DetailResult, error) {
	uri := fmt.Sprintf(queryAPI, o.conf.BcsAPIAddress, taskid)
	header := getHeader(o.token)

	result, err := o.request("GET", uri, header, nil)
	if err != nil {
		blog.Errorf("dc_mac operator: get server status %s taskId(%s), token(%v) failed: %v",
			uri, taskid, o.token, err)
		return nil, err
	}

	blog.Infof("dc_mac operator: get server status(%s)", string(result))
	var data DetailResult
	if err = codec.DecJSON(result, &data); err != nil {
		blog.Errorf("dc_mac operator: decode from get serverStatus result(%s) failed: %v", string(result), err)
		return nil, err
	}

	if data.ActionCode != okActionCode {
		blog.Errorf("dc_mac operator: query server with header(%v) data(%s) action failed: %d %s",
			header, string(result), data.ActionCode, data.ActionMessage)
		return nil, fmt.Errorf("launch action failed: %d %s", data.ActionCode, data.ActionMessage)
	}

	return &data, nil
}

func (o *operator) getClient() *httpclient.HTTPClient {
	return httpclient.NewHTTPClient()
}

func (o *operator) request(method, uri string, requestHeader http.Header, data []byte) (raw []byte, err error) {
	var r *httpclient.HttpResponse

	client := o.getClient()
	before := time.Now().Local()

	// add auth token in header
	header := http.Header{}
	if requestHeader != nil {
		for k := range requestHeader {
			header.Set(k, requestHeader.Get(k))
		}
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
	raw = r.Reply

	now := time.Now().Local()
	blog.V(5).Infof("crm: operator request [%s] %s for record: %s", method, uri, now.Sub(before).String())
	if before.Add(1 * time.Second).Before(now) {
		blog.Warnf("crm: operator request [%s] %s for too long: %s", method, uri, now.Sub(before).String())
	}

	if r.StatusCode != http.StatusOK {
		err = fmt.Errorf("crm: failed to request, http(%d)%s: %s", r.StatusCode, r.Status, uri)
		return
	}

	return
}

// CreateParam 提供创建mac-vm的请求参数
type CreateParam struct {
	// project 项目名称, 目前测试环境下固定为"devcloud"
	Project string `json:"project"`

	// 用于区分不同的job, 目前固定为-1
	VMSeqID string `json:"vmSeqId"`

	// source 固定为"landun"
	Source string `json:"source"`

	// os 为需要的快照名称, 在bcs中可以考虑用image来表示, 会指定不同版本的快照
	OS string `json:"os"`

	// cpu 为要创建的单台vm的cpu
	CPU string `json:"cpu"`

	// mem 为要创建的单台vm的mem
	Mem string `json:"memory"`

	// disk 为要创建的单台vm的disk
	Disk string `json:"disk"`

	// quantity 为创建台数
	Quantity int `json:"quantity"`

	// 以下参数不需要用到, 忽略
	PipelineID string `json:"pipelineId"`
	BuildID    string `json:"buildId"`
}

// CreateResult 描述创建mac-vm的返回信息
type CreateResult struct {
	ActionCode    int         `json:"actionCode"`
	ActionMessage string      `json:"actionMessage"`
	Data          *CreateData `json:"data"`
}

// CreateData 描述创建mac-vm返回信息中的data字段
type CreateData struct {
	TaskID string `json:"taskId"`
}

// CachedCreateData 描述存储在etcd中的创建信息
type CachedCreateData struct {
	CreateData []*CreateData `json:"createData"`
	DetailData []*DetailData `json:"detailData"`
}

// DetailResult 描述查询mac-vm的当前状态的返回信息
type DetailResult struct {
	ActionCode    int         `json:"actionCode"`
	ActionMessage string      `json:"actionMessage"`
	Data          *DetailData `json:"data"`
}

// DetailData 描述查询mac-vm的当前状态的返回信息中的data字段
type DetailData struct {
	Status   DetailStatus `json:"status"`
	IP       string       `json:"ip"`
	User     string       `json:"user"`
	Password string       `json:"password"`
	CPU      string       `json:"cpu"`
	Mem      string       `json:"mem"`
	Disk     string       `json:"disk"`
	Id       int          `json:"id"`
	CreateAt string       `json:"createAt"`
	AssetID  string       `json:"assetId"`
	OS       string       `json:"os"`
	Creator  string       `json:"creator"`
	Name     string       `json:"name"`
	TaskID   string       `json:"taskId"`
}

type DetailStatus string

const (
	DetailStatusWaiting   = "waiting"
	DetailStatusRunning   = "running"
	DetailStatusFailed    = "failed"
	DetailStatusCanceled  = "canceled"
	DetailStatusSucceeded = "succeeded"
)

// CreateItem 描述创建的单个mac-vm信息
type CreateItem struct {
	ID      int    `json:"id"`
	IP      string `json:"ip"`
	AssetID string `json:"assetId"`
	Name    string `json:"name"`

	Creator  string `json:"creator"`
	User     string `json:"user"`
	Password string `json:"password"`

	CPU  string `json:"cpu"`
	Mem  string `json:"memory"`
	Disk string `json:"disk"`
	OS   string `json:"os"`

	CreatedAt string `json:"createdAt"`
}

func (o *operator) launchBatchServer(clusterID string, param op.BcsLaunchParam) error {
	var wg sync.WaitGroup
	results := make([]*CreateResult, param.Instance)
	for index := 0; index < param.Instance; index++ {
		wg.Add(1)
		go func(idx int) {
			defer wg.Done()

			r, err := o.launchServer(clusterID, param)
			if err != nil {
				blog.Errorf("dc_mac operator: launch batch server failed: %v", err)
				return
			}
			results[idx] = r
		}(index)
	}
	wg.Wait()

	cacheData := &CachedCreateData{}
	for _, r := range results {
		if r == nil {
			continue
		}

		cacheData.CreateData = append(cacheData.CreateData, r.Data)
	}

	if len(cacheData.CreateData) == 0 {
		return fmt.Errorf("launch server error")
	}
	var data []byte
	_ = codec.EncJSON(cacheData, &data)

	path := getDataPath(clusterID, param.Namespace, param.Name)
	if err := o.rdClient.Put(path, data); err != nil {
		blog.Errorf("dc_mac operator: put rd to path %s data(%s) failed: %v", path, string(data), err)
		return err
	}

	blog.Infof("dc_mac operator: success to launch server clusterID(%s) namespace(%s) name(%s)",
		clusterID, param.Namespace, param.Name)
	return nil
}

func (o *operator) launchServer(clusterID string, param op.BcsLaunchParam) (*CreateResult, error) {
	cp := &CreateParam{
		Project:    "bktbs-devcloud-mac",
		VMSeqID:    "-1",
		Source:     "landun",
		OS:         param.Image,
		CPU:        fmt.Sprintf("%.f", o.conf.BcsCPUPerInstance),
		Mem:        fmt.Sprintf("%.f", o.conf.BcsMemPerInstance),
		Disk:       "200",
		Quantity:   1,
		BuildID:    param.Name,
		PipelineID: "-1",
	}
	var data []byte
	_ = codec.EncJSON(cp, &data)

	uri := fmt.Sprintf(createAPI, o.conf.BcsAPIAddress)
	header := getHeader(o.token)
	blog.Infof("dc_mac operator: going to launch server with clusterID(%s) "+
		"namespace(%s) name(%s) header(%v) data(%s)", clusterID, param.Namespace, param.Name, header, string(data))
	r, err := o.request("POST", uri, header, data)
	blog.Infof("dc_mac operator: get launch result: %s", string(r))
	if err != nil {
		blog.Errorf("dc_mac operator: launch server clusterID(%s) param(%v), token(%v) failed: %v",
			clusterID, param, o.token, err)
		return nil, err
	}

	var resp CreateResult
	if err = codec.DecJSON(r, &resp); err != nil {
		blog.Errorf("dc_mac operator: decode from launch result(%s) failed: %v", string(r), err)
		return nil, err
	}

	if resp.ActionCode != okActionCode {
		blog.Errorf("dc_mac operator: launch server with header(%v) data(%s) action failed: %d %s",
			header, string(data), resp.ActionCode, resp.ActionMessage)
		return nil, fmt.Errorf("launch action failed: %d %s", resp.ActionCode, resp.ActionMessage)
	}

	return &resp, nil
}

// DeleteParam 提供删除mac-vm的请求参数
type DeleteParam struct {
	// project 项目名称, 目前测试环境下固定为"devcloud"
	Project string `json:"project"`

	// 用于区分不同的job, 目前固定为-1
	VMSeqID string `json:"vmSeqId"`

	// 要删除的vm的id, 在创建的时候会返回
	// 接口没有提供批量删除的办法, 对于create出来的vm, 需要一台一台delete
	ID string `json:"id"`

	// 以下参数不需要用到, 忽略
	PipelineID string `json:"pipelineId"`
	BuildID    string `json:"buildId"`
}

// DeleteResult 描述了删除mac-vm的结果信息
type DeleteResult struct {
	ActionCode    int    `json:"actionCode"`
	ActionMessage string `json:"actionMessage"`
}

func (o *operator) releaseServer(clusterID, namespace, name string) error {
	_, _ = o.getBatchServerStatus(clusterID, namespace, name)

	path := getDataPath(clusterID, namespace, name)
	r, err := o.rdClient.Get(path)
	if err != nil {
		blog.Errorf("dc_mac operator: release server get data from rd %s failed: %v", path, err)
		if strings.Contains(err.Error(), "data path no found") {
			blog.Warnf("dc_mac operator: release server no longer trace the unknown data path target, just end")
			return nil
		}
		return err
	}

	var resp CachedCreateData
	if err = codec.DecJSON(r, &resp); err != nil {
		blog.Errorf("dc_mac operator: release server decode from data(%s) failed: %v", string(r), err)
		return err
	}

	blog.Infof("dc_mac operator: going to release server clusterID(%s) namespace(%s) name(%s)",
		clusterID, namespace, name)

	success := true
	uri := fmt.Sprintf(deleteAPI, o.conf.BcsAPIAddress)
	header := getHeader(o.token)
	var wg sync.WaitGroup
	for _, item := range resp.DetailData {
		if item.Status != DetailStatusSucceeded && item.Status != DetailStatusFailed {
			blog.Errorf("dc_mac operator: release clusterID(%s) namespace(%s) name(%s) with status %s !!",
				clusterID, namespace, name, string(item.Status))
			success = false
			continue
		}

		targetID := strconv.Itoa(item.Id)
		if targetID == "" || targetID == "0" {
			blog.Warnf("dc_mac operator: release clusterID(%s) namespace(%s) name(%s) get invalid id, skip",
				clusterID, namespace, name)
			continue
		}

		dp := &DeleteParam{
			Project:    "bktbs-devcloud-mac",
			VMSeqID:    "-1",
			ID:         targetID,
			BuildID:    name,
			PipelineID: "-1",
		}

		var data []byte
		_ = codec.EncJSON(dp, &data)

		wg.Add(1)
		go func(id int, d []byte) {
			defer wg.Done()

			blog.Infof("dc_mac operator: going to release vm %d for clusterID(%s) namespace(%s) name(%s) "+
				"header(%v) data(%s)", id, clusterID, namespace, name, header, string(d))
			r, err := o.request("POST", uri, header, d)
			if err != nil {
				blog.Errorf("dc_mac operator: release server clusterID(%s) namespace(%s) name(%s), "+
					"token(%v) failed: %v", clusterID, namespace, name, o.token, err)
				success = false
				return
			}

			var resp DeleteResult
			if err = codec.DecJSON(r, &resp); err != nil {
				blog.Errorf("dc_mac operator: decode from release result(%s) failed: %v", string(r), err)
				success = false
				return
			}

			if resp.ActionCode != okActionCode {
				blog.Errorf("dc_mac operator: release server with header(%v) data(%s) action failed: %d %s",
					header, string(d), resp.ActionCode, resp.ActionMessage)
				success = false
				return
			}
			blog.Infof("dc_mac operator: success to release vm %d for clusterID(%s) namespace(%s) name(%s) "+
				"data(%s)", id, clusterID, namespace, name, string(r))
		}(item.Id, data)
	}

	wg.Wait()
	if !success {
		blog.Errorf("dc_mac operator: release server clusterID(%s) namespace(%s) name(%s) failed",
			clusterID, namespace, name)
		return fmt.Errorf("release server failed")
	}

	blog.Infof("dc_mac operator: going to del path %s from rd", path)
	if err = o.rdClient.Del(path); err != nil {
		blog.Warnf("dc_mac operator: del path %s from rd failed: %v", path, err)
	}
	blog.Infof("dc_mac operator: success to release server clusterID(%s) namespace(%s) name(%s)",
		clusterID, namespace, name)
	return nil
}

// 访问dev-cloud的请求头里, 需要带上校验信息
// APPID	token的id，服务端统一申请分发
// RANDOM	8位随机字符串
// TIMESTP	当前Unix时间戳，精确到秒，用于判断签名的时效性
// ENCKEY	签名凭据，通过md5(TOKEN+TIMESTP+RANDOM)得到
func getHeader(token Token) http.Header {
	random := util.RandomString(randLen)
	timestamp := strconv.Itoa(int(time.Now().Local().Unix()))
	sum := md5.Sum([]byte(token.Token + timestamp + random))
	encKey := hex.EncodeToString(sum[:])

	header := http.Header{}
	header.Add("APPID", token.ID)
	header.Add("RANDOM", random)
	header.Add("TIMESTP", timestamp)
	header.Add("ENCKEY", encKey)

	rioSeq := "tbs_" + random
	staffID := ""
	staffName := "anonymous"
	shaSum := sha256.Sum256(
		[]byte(timestamp + token.SmartGateToken + rioSeq + "," + staffID + "," + staffName + "," + timestamp))
	signature := hex.EncodeToString(shaSum[:])
	header.Add("X-RIO-SEQ", rioSeq)
	header.Add("STAFFID", staffID)
	header.Add("STAFFNAME", staffName)
	header.Add("TIMESTAMP", timestamp)
	header.Add("SIGNATURE", signature)

	return header
}

func getDataPath(clusterID, namespace, name string) string {
	return dataPathPrefix + "/" + clusterID + "/" + namespace + "/" + name
}
