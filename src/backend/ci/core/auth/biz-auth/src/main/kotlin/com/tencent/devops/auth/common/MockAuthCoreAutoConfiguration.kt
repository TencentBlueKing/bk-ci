package com.tencent.devops.auth.common

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.service.impl.GrantServiceImpl
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
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.auth.service.iam.PermissionResourceValidateService
import com.tencent.devops.auth.service.iam.PermissionRoleMemberService
import com.tencent.devops.auth.service.iam.PermissionRoleService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.auth.service.iam.PermissionUrlService
import com.tencent.devops.auth.service.sample.SampleAuthPermissionProjectService
import com.tencent.devops.auth.service.sample.SampleAuthPermissionService
import com.tencent.devops.auth.service.sample.SampleGrantPermissionServiceImpl
import com.tencent.devops.auth.service.sample.SampleLocalManagerServiceImpl
import com.tencent.devops.auth.service.sample.SampleOrganizationService
import com.tencent.devops.auth.service.sample.SamplePermissionApplyService
import com.tencent.devops.auth.service.sample.SamplePermissionExtService
import com.tencent.devops.auth.service.sample.SamplePermissionGradeService
import com.tencent.devops.auth.service.sample.SamplePermissionItsmCallbackService
import com.tencent.devops.auth.service.sample.SamplePermissionMigrateService
import com.tencent.devops.auth.service.sample.SamplePermissionResourceGroupService
import com.tencent.devops.auth.service.sample.SamplePermissionResourceService
import com.tencent.devops.auth.service.sample.SamplePermissionResourceValidateService
import com.tencent.devops.auth.service.sample.SamplePermissionRoleMemberService
import com.tencent.devops.auth.service.sample.SamplePermissionRoleService
import com.tencent.devops.auth.service.sample.SamplePermissionSuperManagerService
import com.tencent.devops.auth.service.sample.SamplePermissionUrlServiceImpl
import com.tencent.devops.common.client.Client
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

/**
 * spring bean先加载自定义的配置类,然后再加载AutoConfiguration
 * 这个类应该在最后加载,所以放到AutoConfiguration中初始化
 */
@Suppress("ALL")
class MockAuthCoreAutoConfiguration {
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
    fun sampleGrantPermissionServiceImpl(
        grantServiceImpl: GrantServiceImpl,
        iamConfiguration: IamConfiguration,
        client: Client
    ) = SampleGrantPermissionServiceImpl(grantServiceImpl, iamConfiguration, client)

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
}
