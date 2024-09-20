package com.tencent.devops.auth.provider.sample.config

import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupPermissionDao
import com.tencent.devops.auth.provider.rbac.service.AuthResourceCodeConverter
import com.tencent.devops.auth.provider.rbac.service.RbacCacheService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionResourceGroupPermissionService
import com.tencent.devops.auth.provider.rbac.service.migrate.MigrateResourceAuthorizationService
import com.tencent.devops.auth.provider.sample.service.SampleAuthAuthorizationScopesService
import com.tencent.devops.auth.provider.sample.service.SampleAuthMonitorSpaceService
import com.tencent.devops.auth.provider.sample.service.SampleAuthPermissionProjectService
import com.tencent.devops.auth.provider.sample.service.SampleAuthPermissionService
import com.tencent.devops.auth.provider.sample.service.SampleGrantPermissionServiceImpl
import com.tencent.devops.auth.provider.sample.service.SampleOrganizationService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionApplyService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionExtService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionGradeService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionItsmCallbackService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionMigrateService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionResourceGroupPermissionService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionResourceGroupService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionResourceGroupSyncService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionResourceMemberService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionResourceService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionResourceValidateService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionRoleMemberService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionRoleService
import com.tencent.devops.auth.provider.sample.service.SamplePermissionUrlServiceImpl
import com.tencent.devops.auth.provider.sample.service.SampleSuperManagerServiceImpl
import com.tencent.devops.auth.service.AuthAuthorizationScopesService
import com.tencent.devops.auth.service.AuthMonitorSpaceService
import com.tencent.devops.auth.service.DefaultDeptServiceImpl
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.OrganizationService
import com.tencent.devops.auth.service.PermissionAuthorizationService
import com.tencent.devops.auth.service.SuperManagerService
import com.tencent.devops.auth.service.iam.PermissionApplyService
import com.tencent.devops.auth.service.iam.PermissionExtService
import com.tencent.devops.auth.service.iam.PermissionGradeService
import com.tencent.devops.auth.service.iam.PermissionGrantService
import com.tencent.devops.auth.service.iam.PermissionItsmCallbackService
import com.tencent.devops.auth.service.iam.PermissionMigrateService
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupSyncService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.auth.service.iam.PermissionResourceValidateService
import com.tencent.devops.auth.service.iam.PermissionRoleMemberService
import com.tencent.devops.auth.service.iam.PermissionRoleService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.auth.service.iam.PermissionUrlService
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

/**
 * auth兜底bean,当在有的权限模型下，不存在的bean则使用默认的bean配置,当前配置必须在其他配置之后加载
 *
 * spring bean先加载自定义的配置类,然后再加载AutoConfiguration
 * 这个类应该在最后加载,所以放到AutoConfiguration中初始化
 */
@Suppress("ALL")
class MockAuthConfiguration {
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
    @ConditionalOnMissingBean(SuperManagerService::class)
    fun sampleSuperManagerServiceImpl() = SampleSuperManagerServiceImpl()

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
    fun samplePermissionResourceService(
        permissionAuthorizationService: PermissionAuthorizationService
    ) = SamplePermissionResourceService(
        permissionAuthorizationService = permissionAuthorizationService
    )

    @Bean
    @ConditionalOnMissingBean(PermissionResourceGroupService::class)
    fun samplePermissionResourceGroupService() = SamplePermissionResourceGroupService()

    @Bean
    @ConditionalOnMissingBean(PermissionResourceGroupPermissionService::class)
    fun samplePermissionResourceGroupPermissionService() = SamplePermissionResourceGroupPermissionService()

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
    @ConditionalOnMissingBean(PermissionMigrateService::class)
    fun samplePermissionMigrateService(
        migrateResourceAuthorizationService: MigrateResourceAuthorizationService
    ) = SamplePermissionMigrateService(
        migrateResourceAuthorizationService = migrateResourceAuthorizationService
    )

    @Bean
    @ConditionalOnMissingBean(AuthAuthorizationScopesService::class)
    fun sampleAuthAuthorizationScopesService() = SampleAuthAuthorizationScopesService()

    @Bean
    @ConditionalOnMissingBean(AuthMonitorSpaceService::class)
    fun sampleAuthMonitorSpaceService() = SampleAuthMonitorSpaceService()

    @Bean
    @ConditionalOnMissingBean(PermissionResourceGroupSyncService::class)
    fun samplePermissionResourceGroupSyncService() = SamplePermissionResourceGroupSyncService()
}
