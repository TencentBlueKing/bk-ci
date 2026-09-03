package authz

import (
	"regexp"
	"strings"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

var invalidLabelChar = regexp.MustCompile(`[^A-Za-z0-9._-]`)

// AuthorizeObject 校验调用方是否对目标对象具备属主/租户权限。默认拒绝无属主对象。
// 仅用于 debug/terminal 与 namespace 资源（deployment/secret）硬拒绝面。
func AuthorizeObject(caller Caller, owner Owner) error {
	if caller.IsEmpty() {
		return ErrMissingIdentity
	}
	if owner.IsEmpty() {
		return ErrObjectUnowned
	}
	if owner.TenantID != "" && caller.TenantID != "" && owner.TenantID != caller.TenantID {
		return ErrForbidden
	}
	if owner.ProjectID != "" && caller.ProjectID != "" && owner.ProjectID != caller.ProjectID {
		return ErrForbidden
	}
	matched := (owner.TenantID != "" && caller.TenantID == owner.TenantID) ||
		(owner.ProjectID != "" && caller.ProjectID == owner.ProjectID) ||
		(owner.UserID != "" && caller.UserID == owner.UserID)
	if !matched {
		return ErrForbidden
	}
	return nil
}

// AuthorizeDebugIssue 签发校验：自称必须同时带 user+project（dispatch N4 必填）。
// 匹配走 AuthorizeObject（同项目即可），以支持非创建者发起登录调试。
// 伪造头由 HMAC 身份签名在 CallerFromHeader 丢弃，不再靠 user AND 挡 PoC6。
func AuthorizeDebugIssue(claimed Caller, owner Owner) error {
	if claimed.UserID == "" || claimed.ProjectID == "" {
		return ErrMissingIdentity
	}
	if owner.IsEmpty() {
		return ErrObjectUnowned
	}
	return AuthorizeObject(claimed, owner)
}

// AuthorizeBuilderObserve 用于 status：判定轴是对象有无属主。
// 无属主放行（存量构建）；已属主但无身份也放行（只读探活）；有身份则必须匹配。
func AuthorizeBuilderObserve(caller Caller, owner Owner) error {
	if owner.IsEmpty() {
		return nil
	}
	if caller.IsEmpty() {
		return nil
	}
	return AuthorizeObject(caller, owner)
}

// AuthorizeBuilderMutate 用于 stop/delete：无属主放行；已属主则必须带匹配身份。
// 不发身份头不得对他人已打标构建机做破坏操作。
func AuthorizeBuilderMutate(caller Caller, owner Owner) error {
	if owner.IsEmpty() {
		return nil
	}
	if caller.IsEmpty() {
		return ErrMissingIdentity
	}
	return AuthorizeObject(caller, owner)
}

func OwnerFromMetadata(meta metav1.ObjectMeta) Owner {
	return OwnerFromMap(meta.Labels).Fill(OwnerFromMap(meta.Annotations))
}

func OwnerFromMap(m map[string]string) Owner {
	if m == nil {
		return Owner{}
	}
	return Owner{
		UserID:    firstNonEmpty(m[LabelUserID], m["userId"], m["user-id"]),
		ProjectID: firstNonEmpty(m[LabelProjectID], m["projectId"], m["project-id"]),
		TenantID:  firstNonEmpty(m[LabelTenantID], m["tenantId"], m["tenant-id"]),
	}
}

func OwnerFromEnvMap(env map[string]string) Owner {
	if env == nil {
		return Owner{}
	}
	return Owner{
		UserID:    firstNonEmpty(env[EnvUserID]),
		ProjectID: firstNonEmpty(env[EnvProjectID]),
		TenantID:  firstNonEmpty(env[EnvTenantID]),
	}
}

func OwnerFromPod(pod *corev1.Pod) Owner {
	if pod == nil {
		return Owner{}
	}
	owner := OwnerFromMetadata(pod.ObjectMeta)
	for _, c := range pod.Spec.Containers {
		owner = owner.Fill(ownerFromEnvVars(c.Env))
	}
	return owner
}

func ownerFromEnvVars(env []corev1.EnvVar) Owner {
	m := map[string]string{}
	for _, e := range env {
		if e.Value != "" {
			m[e.Name] = e.Value
		}
	}
	return OwnerFromEnvMap(m)
}

func ApplyOwnerLabels(dst map[string]string, owner Owner) map[string]string {
	if dst == nil {
		dst = map[string]string{}
	}
	if owner.UserID != "" {
		dst[LabelUserID] = SanitizeLabelValue(owner.UserID)
	}
	if owner.ProjectID != "" {
		dst[LabelProjectID] = SanitizeLabelValue(owner.ProjectID)
	}
	if owner.TenantID != "" {
		dst[LabelTenantID] = SanitizeLabelValue(owner.TenantID)
	}
	return dst
}

func ApplyOwnerAnnotations(dst map[string]string, owner Owner) map[string]string {
	if dst == nil {
		dst = map[string]string{}
	}
	if owner.UserID != "" {
		dst[LabelUserID] = owner.UserID
	}
	if owner.ProjectID != "" {
		dst[LabelProjectID] = owner.ProjectID
	}
	if owner.TenantID != "" {
		dst[LabelTenantID] = owner.TenantID
	}
	return dst
}

// SanitizeLabelValue 将属主字段收敛为合法 Kubernetes label value。
func SanitizeLabelValue(v string) string {
	v = strings.TrimSpace(v)
	v = invalidLabelChar.ReplaceAllString(v, "-")
	v = strings.Trim(v, "-._")
	if len(v) > 63 {
		v = v[:63]
		v = strings.Trim(v, "-._")
	}
	return v
}
