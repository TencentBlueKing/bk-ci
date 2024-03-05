package com.tencent.devops.auth.provider.sample.config

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.service.impl.GrantServiceImpl
import com.tencent.devops.auth.provider.sample.service.SampleAuthAuthorizationScopesService
import com.tencent.devops.auth.provider.sample.service.SampleAuthMonitorSpaceService
import com.tencent.devops.auth.provider.sample.service.SampleAuthPermissionProjectService
import com.tencent.devops.auth.provider.sample.service.SampleAuthPermissionService
import com.tencent.devops.auth.provider.sample.service.SampleGrantPermissionServiceImpl
import com.tencent.devops.auth.provider.sample.service.SampleLocalManagerServiceImpl
import com.tencent.devops.auth.provider.sample.service.SampleOrganizationService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionApplyService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionExtService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionGradeService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionItsmCallbackService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionMigrateService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionResourceGroupService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionResourceMemberService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionResourceService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionResourceValidateService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionRoleMemberService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionRoleService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionSuperManagerService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionUrlServiceImpl
import com.tencent.devops.auth.service.AuthAuthorizationScopesService
import com.tencent.devops.auth.service.AuthMonitorSpaceService
import com.tencent.devops.auth.service.DefaultDeptServiceImpl
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.LocalManagerService
import com.tencent.devops.auth.service.OrganizationService
import com.tencent.devops.auth.service.PermissionSuperManagerService
import com.tencent.devops.auth.service.iam.PermissionApplyService
import com.tencent.devops.auth.service.iam.PermissionExtService
import com.tencent.devops.auth.service.iam.PermissionGradeService
import com.tencent.devops.auth.service.iam.PermissionGrantService
import com.tencent.devops.auth.service.iam.PermissionItsmCallbackService
import com.tencent.devops.auth.service.iam.PermissionMigrateService
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.auth.service.iam.PermissionResourceValidateService
import com.tencent.devops.auth.service.iam.PermissionRoleMemberService
import com.tencent.devops.auth.service.iam.PermissionRoleService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.auth.service.iam.PermissionUrlService
import com.tencent.devops.common.client.Client
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

/**
 * auth兜底bean,当在有的权限模型下，不存在的bean则使用默认的bean配置,当前配置必须在其他配置之后加载
 *
 * spring bean先加载自定义的配置类,然后再加载AutoConfiguration
 * 这个类应该在最后加载,所以放到AutoConfiguration中初始化
 */
@Suppress("ALL")
class MockAuthAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(DeptService::class)
    fun defaultDeptServiceImpl() = DefaultDeptServiceImpl()

    @Bean
    @ConditionalOnMissingBean(PermissionExtService::class)
    fun permissionExtService() = SamplePermissionExtService()

    @Bean
    @ConditionalOnMissingBean(PermissionUrlService::class)
    fun permissionUrlService() = SamplePermissionUrlServiceImpl()

    @Bean
    @ConditionalOnMissingBean(PermissionProjectService::class)
    fun sampleAuthPermissionProjectService() = SampleAuthPermissionProjectService()

    @Bean
    @ConditionalOnMissingBean(PermissionService::class)
    fun sampleAuthPermissionService() = SampleAuthPermissionService()

    @Bean
    @ConditionalOnMissingBean(PermissionGrantService::class)
    fun sampleGrantPermissionServiceImpl() = SampleGrantPermissionServiceImpl()

    @Bean
    @ConditionalOnMissingBean(LocalManagerService::class)
    fun sampleLocalManagerServiceImpl() = SampleLocalManagerServiceImpl()

    @Bean
    @ConditionalOnMissingBean(PermissionGradeService::class)
    fun samplePermissionGradeService() = SamplePermissionGradeService()

    @Bean
    @ConditionalOnMissingBean(PermissionRoleMemberService::class)
    fun samplePermissionRoleMemberService() = SamplePermissionRoleMemberService()

    @Bean
    @ConditionalOnMissingBean(PermissionRoleService::class)
    fun samplePermissionRoleService() = SamplePermissionRoleService()

    @Bean
    @ConditionalOnMissingBean(OrganizationService::class)
    fun sampleOrganizationService() = SampleOrganizationService()

    @Bean
    @ConditionalOnMissingBean(PermissionResourceService::class)
    fun samplePermissionResourceService() = SamplePermissionResourceService()

    @Bean
    @ConditionalOnMissingBean(PermissionResourceGroupService::class)
    fun samplePermissionResourceGroupService() = SamplePermissionResourceGroupService()

    @Bean
    @ConditionalOnMissingBean(PermissionResourceMemberService::class)
    fun samplePermissionResourceMemberService() = SamplePermissionResourceMemberService()

    @Bean
    @ConditionalOnMissingBean(PermissionApplyService::class)
    fun samplePermissionApplyService() = SamplePermissionApplyService()

    @Bean
    @ConditionalOnMissingBean(PermissionItsmCallbackService::class)
    fun samplePermissionItsmCallbackService() = SamplePermissionItsmCallbackService()

    @Bean
    @ConditionalOnMissingBean(PermissionResourceValidateService::class)
    fun samplePermissionResourceValidateService() = SamplePermissionResourceValidateService()

    @Bean
    @ConditionalOnMissingBean(PermissionSuperManagerService::class)
    fun samplePermissionSuperManagerService() = SamplePermissionSuperManagerService()

    @Bean
    @ConditionalOnMissingBean(PermissionMigrateService::class)
    fun samplePermissionMigrateService() = SamplePermissionMigrateService()

    @Bean
    @ConditionalOnMissingBean(AuthAuthorizationScopesService::class)
    fun sampleAuthAuthorizationScopesService() = SampleAuthAuthorizationScopesService()

    @Bean
    @ConditionalOnMissingBean(AuthMonitorSpaceService::class)
    fun sampleAuthMonitorSpaceService() = SampleAuthMonitorSpaceService()
}
