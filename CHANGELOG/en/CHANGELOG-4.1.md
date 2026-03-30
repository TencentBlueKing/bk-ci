<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v4.1.0-rc.4](#v410-rc4)
   - [Changelog since v4.1.0-rc.3](#changelog-since-v410-rc3)

- [v4.1.0-rc.3](#v410-rc3)
   - [Changelog since v4.1.0-rc.2](#changelog-since-v410-rc2)

- [v4.1.0-rc.2](#v410-rc2)
   - [Changelog since v4.1.0-rc.1](#changelog-since-v410-rc1)

- [v4.1.0-rc.1](#v410-rc1)
   - [Changelog since v4.0.0](#changelog-since-v400)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v4.1.0-rc.4
## 2026-03-30
### Changelog since v4.1.0-rc.3
#### New Features

##### Pipeline
- [New] feat: Support querying blank templates by channel when creating pipelines [Link](http://github.com/TencentBlueKing/bk-ci/issues/12762)
- [New] feat: Support starting execution with specified startup parameter combinations [Link](http://github.com/TencentBlueKing/bk-ci/issues/10798)
- [New] feat: Upgrade template ApiGW interfaces from v1 to v2 [Link](http://github.com/TencentBlueKing/bk-ci/issues/12686)
- [New] feat: Support --network option when running Docker on third-party build machines [Link](http://github.com/TencentBlueKing/bk-ci/issues/12298)
- [New] feat: WeCom bot notification supports sending multimedia messages [Link](http://github.com/TencentBlueKing/bk-ci/issues/12617)
- [New] feat: Support quick-fill for startup parameters [Link](http://github.com/TencentBlueKing/bk-ci/issues/12418)
- [New] Optimize default cron rules for timer triggers [Link](http://github.com/TencentBlueKing/bk-ci/issues/12610)
- [New] feat: Compatibility for unmigrated templates in Store [Link](http://github.com/TencentBlueKing/bk-ci/issues/12588)
- [New] feat: Pipeline variables support "sensitive" attribute [Link](http://github.com/TencentBlueKing/bk-ci/issues/11738)
- [New] feat: Adjust replay event logic on build detail page [Link](http://github.com/TencentBlueKing/bk-ci/issues/12344)
- [New] feat: Pipeline parameters support linkage display on execution preview page [Link](http://github.com/TencentBlueKing/bk-ci/issues/11438)
- [New] feat: [PAC Template] Pipeline templates support PAC feature [Link](http://github.com/TencentBlueKing/bk-ci/issues/11414)
- [New] feat: Support variable input for select and enum-input in dynamic-parameter and dynamic-parameter-simple components [Link](http://github.com/TencentBlueKing/bk-ci/issues/12343)
- [New] feat: Support configurable policies for canceling running pipeline builds [Link](http://github.com/TencentBlueKing/bk-ci/issues/9233)
- [New] feat: UI-to-code conversion supports hexadecimal strings [Link](http://github.com/TencentBlueKing/bk-ci/issues/12451)
- [New] feat: High-spec machine types support code [Link](http://github.com/TencentBlueKing/bk-ci/issues/12353)
- [New] feat: Add CI context variable debug flag to indicate whether the build is a debug or production run [Link](http://github.com/TencentBlueKing/bk-ci/issues/12345)

##### Repository
- [New] feat: BkCode supports Commit Check [Link](http://github.com/TencentBlueKing/bk-ci/issues/12559)
- [New] feat: OpenAPI interface repository_info_list adds scm_code query condition [Link](http://github.com/TencentBlueKing/bk-ci/issues/12537)
- [New] feat: CodeCC code snippet viewing API supports code source platform [Link](http://github.com/TencentBlueKing/bk-ci/issues/12478)
- [New] feat: Support BkCode code source [Link](http://github.com/TencentBlueKing/bk-ci/issues/12411)

##### Store
- [New] feat: Plugin category and label information support filtering by service scope [Link](http://github.com/TencentBlueKing/bk-ci/issues/12755)
- [New] feat: Store images support upgrading historical minor versions [Link](http://github.com/TencentBlueKing/bk-ci/issues/12627)
- [New] feat: Store images support upgrading historical minor versions [Link](http://github.com/TencentBlueKing/bk-ci/issues/12598)
- [New] feat: Plugin release description supports reading recent commit messages from plugin repository [Link](http://github.com/TencentBlueKing/bk-ci/issues/12080)
- [New] feat: Support setting visibility scope when publishing templates [Link](http://github.com/TencentBlueKing/bk-ci/issues/12423)
- [New] feat: Add SHA256 digest for stored package files in existing cloud development packages [Link](http://github.com/TencentBlueKing/bk-ci/issues/12398)
- [New] feat: Store component package integrity verification supports SHA256 algorithm [Link](http://github.com/TencentBlueKing/bk-ci/issues/12362)

##### Environment Management
- [New] Windows listens for environment variable updates [Link](http://github.com/TencentBlueKing/bk-ci/issues/12449)
- [New] feat: Build machine monitoring data collection supports GPU and CPU model [Link](http://github.com/TencentBlueKing/bk-ci/issues/12394)

##### Permission Center
- [New] feat: Provide list of user groups that a user has joined under a project [Link](http://github.com/TencentBlueKing/bk-ci/issues/12516)
- [New] feat: New resource types integrated with Permission Center [Link](http://github.com/TencentBlueKing/bk-ci/issues/12328)

##### Dispatch
- [New] feat: Third-party build machine Docker supports configuring startup user [Link](http://github.com/TencentBlueKing/bk-ci/issues/12703)
- [New] feat: Optimize K8s build cluster solution [Link](http://github.com/TencentBlueKing/bk-ci/issues/10636)

##### Others
- [New] feat: Workflow management [Link](http://github.com/TencentBlueKing/bk-ci/issues/12414)
- [New] feat: Gray-scale strategy supports percentage-based routing [Link](http://github.com/TencentBlueKing/bk-ci/issues/12688)
- [New] feat: Workflow integrated with Permission Center [Link](http://github.com/TencentBlueKing/bk-ci/issues/12493)
- [New] feat: Agent supports MCP [Link](http://github.com/TencentBlueKing/bk-ci/issues/12653)
- [New] feat: New template version names support duplicates [Link](http://github.com/TencentBlueKing/bk-ci/issues/12713)
- [New] feat: Fix excessive Prometheus data from spring-amqp [Link](http://github.com/TencentBlueKing/bk-ci/issues/12462)
- [New] feat: Add ApiGW interface for admin info exempt from project authorization verification [Link](http://github.com/TencentBlueKing/bk-ci/issues/12659)
- [New] feat: Remove unused AUTH_HEADER_DEVOPS_ACCESS_TOKEN logic [Link](http://github.com/TencentBlueKing/bk-ci/issues/12622)
- [New] Develop lightweight OpenAPI pipeline build history query interface [Link](http://github.com/TencentBlueKing/bk-ci/issues/12472)
- [New] feat: OpenAPI environment node interface adds start/stop information [Link](http://github.com/TencentBlueKing/bk-ci/issues/12586)
- [New] feat: Frontend containerization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12445)
- [New] feat: WebSocket logic optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12484)
- [New] feat: AI workflow practice based on SDD [Link](http://github.com/TencentBlueKing/bk-ci/issues/12501)
- [New] feat: RedisLock supports separate Redis instances [Link](http://github.com/TencentBlueKing/bk-ci/issues/12508)
- [New] feat: Provide OpenAPI interface to reset pipeline permission delegation [Link](http://github.com/TencentBlueKing/bk-ci/issues/11258)
- [New] feat: Add CRC64 value to file information [Link](http://github.com/TencentBlueKing/bk-ci/issues/12461)
- [New] feat: Reduce Redis pressure [Link](http://github.com/TencentBlueKing/bk-ci/issues/12482)
- [New] feat: Add additional interfaces [Link](http://github.com/TencentBlueKing/bk-ci/issues/12440)
- [New] feat: Upgrade framework to 1.1.0 [Link](http://github.com/TencentBlueKing/bk-ci/issues/12375)
- [New] Provide OpenAPI interfaces for enabling/disabling private build environment clusters [Link](http://github.com/TencentBlueKing/bk-ci/issues/12406)
- [New] [PAC] Streamline open-source Stream service [Link](http://github.com/TencentBlueKing/bk-ci/issues/12233)

#### Optimization

##### Pipeline
- [Optimization] perf: [PAC Template] Ensure data consistency during template data migration [Link](http://github.com/TencentBlueKing/bk-ci/issues/12675)
- [Optimization] perf: Optimize navigation to template target page [Link](http://github.com/TencentBlueKing/bk-ci/issues/12651)
- [Optimization] pref: Pipeline version quick rollback tool [Link](http://github.com/TencentBlueKing/bk-ci/issues/12631)
- [Optimization] pref: Ensure accuracy of pipeline template data migration [Link](http://github.com/TencentBlueKing/bk-ci/issues/12541)
- [Optimization] perf: Optimize pipeline webhook trigger process [Link](http://github.com/TencentBlueKing/bk-ci/issues/11884)
- [Optimization] perf: New build data no longer written to detail table [Link](http://github.com/TencentBlueKing/bk-ci/issues/12464)
- [Optimization] perf: Optimize pipeline version query SQL content [Link](http://github.com/TencentBlueKing/bk-ci/issues/12424)
- [Optimization] perf: Support MR_ACC event to YAML conversion [Link](http://github.com/TencentBlueKing/bk-ci/issues/12309)
- [Optimization] pref: Permission circuit breaker optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12363)
- [Optimization] perf: Fix issue where constant values could be modified [Link](http://github.com/TencentBlueKing/bk-ci/issues/12313)
- [Optimization] perf: Fix legacy pipelines using plugin output variable namespaces unable to compare differences [Link](http://github.com/TencentBlueKing/bk-ci/issues/12312)

##### Repository
- [Optimization] perf: Support searching by address when associating repositories [Link](http://github.com/TencentBlueKing/bk-ci/issues/12393)

##### Store
- [Optimization] pref: Default service scope set to pipeline for plugin initialization data [Link](http://github.com/TencentBlueKing/bk-ci/issues/12775)
- [Optimization] perf: Plugin pipeline list supports pagination [Link](http://github.com/TencentBlueKing/bk-ci/issues/12434)
- [Optimization] pref: Optimize plugin download logic [Link](http://github.com/TencentBlueKing/bk-ci/issues/12561)
- [Optimization] pref: Enhance regex validation for component version numbers in Store general upload API [Link](http://github.com/TencentBlueKing/bk-ci/issues/12490)

##### Log Service
- [Optimization] perf: Log subTag check logic can get cache outside the loop [Link](http://github.com/TencentBlueKing/bk-ci/issues/12646)
- [Optimization] perf: Increase ES connection timeout and connection count for log service [Link](http://github.com/TencentBlueKing/bk-ci/issues/12609)

##### Dispatch
- [Optimization] perf: Optimize pipeline concurrent quota management [Link](http://github.com/TencentBlueKing/bk-ci/issues/12340)

##### Others
- [Optimization] pref: Pipeline template ApiGW interface compatibility [Link](http://github.com/TencentBlueKing/bk-ci/issues/12743)
- [Optimization] pref: Add IAM group member count retrieval [Link](http://github.com/TencentBlueKing/bk-ci/issues/12731)
- [Optimization] perf: Add namespace to consumer group when customizing binder [Link](http://github.com/TencentBlueKing/bk-ci/issues/12628)
- [Optimization] pref: Streamline skills files to reduce context [Link](http://github.com/TencentBlueKing/bk-ci/issues/12594)
- [Optimization] docs: Cloud desktop documentation update [Link](http://github.com/TencentBlueKing/bk-ci/issues/12582)
- [Optimization] perf: Support custom middleware binder parameters [Link](http://github.com/TencentBlueKing/bk-ci/issues/12443)
- [Optimization] perf: Optimize crontab setting interaction for timer triggers [Link](http://github.com/TencentBlueKing/bk-ci/issues/12205)
- [Optimization] chore: Upgrade api-turbo to 0.0.7-RELEASE [Link](http://github.com/TencentBlueKing/bk-ci/issues/12382)

#### Bug Fixes

##### Pipeline
- [Bug Fix] perf: Compatibility optimization for variables when upgrading legacy templates to new version [Link](http://github.com/TencentBlueKing/bk-ci/issues/12471)
- [Bug Fix] bug: Store template not found when creating pipeline [Link](http://github.com/TencentBlueKing/bk-ci/issues/12749)
- [Bug Fix] bug: Template instantiation lock not released [Link](http://github.com/TencentBlueKing/bk-ci/issues/12721)
- [Bug Fix] feat: Support inserting steps between two plugins [Link](http://github.com/TencentBlueKing/bk-ci/issues/12679)
- [Bug Fix] Pipeline execution page parameter changes not updated in time [Link](http://github.com/TencentBlueKing/bk-ci/issues/12673)
- [Bug Fix] bug: Timer trigger validates whether pipeline has trigger plugin at trigger time [Link](http://github.com/TencentBlueKing/bk-ci/issues/12661)
- [Bug Fix] bug: Page not updated when switching output report to build artifacts [Link](http://github.com/TencentBlueKing/bk-ci/issues/12637)
- [Bug Fix] fix: Remove exit from Windows install script for easier troubleshooting [Link](http://github.com/TencentBlueKing/bk-ci/issues/12512)
- [Bug Fix] bug: Optimize pipeline version saving logic for high-volume event-triggered builds with new pipeline versions [Link](http://github.com/TencentBlueKing/bk-ci/issues/12578)
- [Bug Fix] bug: Review plugin under matrix job cannot correctly parse expressions [Link](http://github.com/TencentBlueKing/bk-ci/issues/12574)
- [Bug Fix] bug: Template timer trigger plugin fails to get parameter list [Link](http://github.com/TencentBlueKing/bk-ci/issues/12564)
- [Bug Fix] bug: Sub-pipeline cross-project calls support cross-cluster invocation [Link](http://github.com/TencentBlueKing/bk-ci/issues/12554)
- [Bug Fix] bug: Reviewer may not match expectation on retry when review plugin reviewer is an expression [Link](http://github.com/TencentBlueKing/bk-ci/issues/12525)
- [Bug Fix] bug: Fix TGit trigger 2.0 version content is empty [Link](http://github.com/TencentBlueKing/bk-ci/issues/12530)
- [Bug Fix] bug: Triggering draft-status new pipeline via service call causes abnormal record data [Link](http://github.com/TencentBlueKing/bk-ci/issues/12513)
- [Bug Fix] bug: Quality gate plugin retry not executed in some cases [Link](http://github.com/TencentBlueKing/bk-ci/issues/12521)
- [Bug Fix] bugfix: Pipeline group queue count set to 0 prevents builds with different group names from starting [Link](http://github.com/TencentBlueKing/bk-ci/issues/12427)
- [Bug Fix] bug: Draft version triggers (repository) should not take effect [Link](http://github.com/TencentBlueKing/bk-ci/issues/11913)
- [Bug Fix] bug: Use asterisks to mask sensitive fields in build detail configuration view [Link](http://github.com/TencentBlueKing/bk-ci/issues/12412)
- [Bug Fix] bug: Incorrect permission scope for execution analysis redirect URL on pipeline page [Link](http://github.com/TencentBlueKing/bk-ci/issues/12408)
- [Bug Fix] bug: Pipeline build history list cannot display elapsed time when viewing stage information [Link](http://github.com/TencentBlueKing/bk-ci/issues/12390)
- [Bug Fix] bugfix: Build node mutex null pointer exception [Link](http://github.com/TencentBlueKing/bk-ci/issues/12376)

##### Repository
- [Bug Fix] bug: Error when resetting repository OAuth authorization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12542)
- [Bug Fix] bug: Fix repository service unable to start [Link](http://github.com/TencentBlueKing/bk-ci/issues/12469)

##### Store
- [Bug Fix] fix: Fix Store unable to install plugins [Link](http://github.com/TencentBlueKing/bk-ci/issues/12500)
- [Bug Fix] bug: Optimize plugin configuration file validation logic during Store plugin submission phase [Link](http://github.com/TencentBlueKing/bk-ci/issues/12355)
- [Bug Fix] bug: Incorrect display of Store plugin-pipeline association information [Link](http://github.com/TencentBlueKing/bk-ci/issues/12307)
- [Bug Fix] bug: Store general upload API may fail when processing complex version log content [Link](http://github.com/TencentBlueKing/bk-ci/issues/12401)

##### Environment Management
- [Bug Fix] bug: Cannot save after importing more than 20 nodes in environment management [Link](http://github.com/TencentBlueKing/bk-ci/issues/12680)
- [Bug Fix] fix: Build machine node referenced pipeline name not updating [Link](http://github.com/TencentBlueKing/bk-ci/issues/12380)

##### Log Service
- [Bug Fix] bug: Log over-limit archiving skipped when job exits due to timeout [Link](http://github.com/TencentBlueKing/bk-ci/issues/11626)

##### Quality Gate
- [Bug Fix] bug: Incorrect threshold calculation for metrics with multiple metadata when quality gate checkpoint is not configured on the corresponding plugin [Link](http://github.com/TencentBlueKing/bk-ci/issues/12738)

##### Permission Center
- [Bug Fix] bug: Fix release-4.0 service cannot start [Link](http://github.com/TencentBlueKing/bk-ci/issues/12457)

##### Dispatch
- [Bug Fix] fix: Docker build specified workspace not taking effect [Link](http://github.com/TencentBlueKing/bk-ci/issues/12495)

##### Others
- [Bug Fix] bug: Fix typos in page prompt messages [Link](http://github.com/TencentBlueKing/bk-ci/issues/12547)
- [Bug Fix] bugfix: Optimize third-party machine restart using systemd [Link](http://github.com/TencentBlueKing/bk-ci/issues/12373)
- [Bug Fix] bugfix: Initialize default image script error [Link](http://github.com/TencentBlueKing/bk-ci/issues/12216)
- [Bug Fix] bug: MacOS install script missing service_name assignment [Link](http://github.com/TencentBlueKing/bk-ci/issues/11651)

# v4.1.0-rc.3
## 2025-10-30
### Changelog since v4.1.0-rc.2
#### New Features

##### Pipeline
- [New] feat: Pipeline list "recent execution" info adds stage progress display [Link](http://github.com/TencentBlueKing/bk-ci/issues/12228)
- [New] feat: Add tip for referencing variables when dropdown variable options are fetched from API [Link](http://github.com/TencentBlueKing/bk-ci/issues/11726)
- [New] feat: Optimize cancellation during review-in-progress status [Link](http://github.com/TencentBlueKing/bk-ci/issues/12300)
- [New] feat: Add copy-remark capability to remark column in execution history list [Link](http://github.com/TencentBlueKing/bk-ci/issues/12138)
- [New] feat: Optimize matrix parsing [Link](http://github.com/TencentBlueKing/bk-ci/issues/12231)

##### Repository
- [New] feat: Code source supports visibility scope [Link](http://github.com/TencentBlueKing/bk-ci/issues/12146)

##### Store
- [New] feat: Add open API for Store component package file download link [Link](http://github.com/TencentBlueKing/bk-ci/issues/12254)

##### Environment Management
- [New] feat: Environment management supports batch modification of node importers [Link](http://github.com/TencentBlueKing/bk-ci/issues/12221)
- [New] feat: Support batch modification of third-party machine max concurrency [Link](http://github.com/TencentBlueKing/bk-ci/issues/12222)

##### Permission Center
- [New] feat: User management related optimizations [Link](http://github.com/TencentBlueKing/bk-ci/issues/12329)

##### Dispatch
- [New] feat: Dispatch adds multiple authentication interfaces referencing build_util.lua [Link](http://github.com/TencentBlueKing/bk-ci/issues/12271)

##### Others
- [New] feat: Plugin startup exception monitoring and self-healing [Link](http://github.com/TencentBlueKing/bk-ci/issues/12299)
- [New] feat: Provide batch export model/code API [Link](http://github.com/TencentBlueKing/bk-ci/issues/12267)
- [New] feat: Optimize Redis dual-write Java thread pool [Link](http://github.com/TencentBlueKing/bk-ci/issues/12263)
- [New] feat: Disable enableServiceLinks to speed up service startup [Link](http://github.com/TencentBlueKing/bk-ci/issues/12260)
- [New] feat: Add report size limit to compressed archive report configuration [Link](http://github.com/TencentBlueKing/bk-ci/issues/12150)

#### Optimization

##### Pipeline
- [Optimization] pref: Optimize deep pagination issue caused by large data volume in build pipeline table during project data migration [Link](http://github.com/TencentBlueKing/bk-ci/issues/12274)
- [Optimization] perf: Deprecate legacy data compatibility on detail page [Link](http://github.com/TencentBlueKing/bk-ci/issues/9522)

##### Credential Management
- [Optimization] chore: Upgrade bcprov-jdk15on to 1.78.1 [Link](http://github.com/TencentBlueKing/bk-ci/issues/11888)

##### Permission Center
- [Optimization] pref: Permission system circuit breaker design - verification [Link](http://github.com/TencentBlueKing/bk-ci/issues/12186)
- [Optimization] pref: Permission renewal optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12293)
- [Optimization] pref: Auth service open-source related script optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12259)
- [Optimization] perf: My authorization - OAuth authorization prompt optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12227)

#### Bug Fixes

##### Pipeline
- [Bug Fix] bug: Optimize sort field for T_PIPELINE_BUILD_RECORD_STAGE table data list query [Link](http://github.com/TencentBlueKing/bk-ci/issues/12331)
- [Bug Fix] bug: PAC pipeline groups not refreshed on publish [Link](http://github.com/TencentBlueKing/bk-ci/issues/11953)
- [Bug Fix] bug: Error when starting next retried build while pipeline is queued [Link](http://github.com/TencentBlueKing/bk-ci/issues/12322)
- [Bug Fix] bug: Incorrect sorting by recent execution time on pipeline list page [Link](http://github.com/TencentBlueKing/bk-ci/issues/11480)
- [Bug Fix] bug: Constant definitions not taking effect during git trigger [Link](http://github.com/TencentBlueKing/bk-ci/issues/12308)
- [Bug Fix] bug: Labels lost when adding pipeline labels, editing pipeline settings, and converting to code [Link](http://github.com/TencentBlueKing/bk-ci/issues/12305)
- [Bug Fix] bug: Labels disappear when editing a copied pipeline with labels [Link](http://github.com/TencentBlueKing/bk-ci/issues/12304)
- [Bug Fix] bug: Pipeline release version execution can only execute the latest release version [Link](http://github.com/TencentBlueKing/bk-ci/issues/12301)
- [Bug Fix] bug: Stage-level retry build fails to lock pipeline group [Link](http://github.com/TencentBlueKing/bk-ci/issues/12294)
- [Bug Fix] bug: Optimize model generation logic for debug builds [Link](http://github.com/TencentBlueKing/bk-ci/issues/12283)
- [Bug Fix] bug: Matrix compatibility with record [Link](http://github.com/TencentBlueKing/bk-ci/issues/12279)
- [Bug Fix] bug: Build may fail when selecting recommended version number [Link](http://github.com/TencentBlueKing/bk-ci/issues/12277)

##### Project Management
- [Bug Fix] bug: Service-level get project list API response adds project admin permission flag [Link](http://github.com/TencentBlueKing/bk-ci/issues/12289)

##### Others
- [Bug Fix] bug: Some legacy pipelines without jobId configured cause steps context syntax parsing errors in plugins [Link](http://github.com/TencentBlueKing/bk-ci/issues/12356)
- [Bug Fix] bug: The name value in stageStatus field of build detail API response does not match expectation [Link](http://github.com/TencentBlueKing/bk-ci/issues/12348)
- [Bug Fix] fix: [Build Machine] Fix Windows 2022 may cause temporary directory creation failure [Link](http://github.com/TencentBlueKing/bk-ci/issues/12333)
- [Bug Fix] bug: Fix pipeline export API [Link](http://github.com/TencentBlueKing/bk-ci/issues/12243)

# v4.1.0-rc.2
## 2025-09-11
### Changelog since v4.1.0-rc.1
#### New Features

##### Pipeline
- [New] feat: Manual trigger supports configuring default "build information" [Link](http://github.com/TencentBlueKing/bk-ci/issues/12215)

##### Repository
- [New] feat: githubService adds extension interfaces [Link](http://github.com/TencentBlueKing/bk-ci/issues/12246)
- [New] feat: Gitee type repository supports Check-Run [Link](http://github.com/TencentBlueKing/bk-ci/issues/12092)

##### Others
- [New] feat: Add filter conditions to APP-level OpenAPI interface for getting project list [Link](http://github.com/TencentBlueKing/bk-ci/issues/12232)
- [New] feat: Pass traceId to bkrepo [Link](http://github.com/TencentBlueKing/bk-ci/issues/12223)

#### Optimization

##### Pipeline
- [Optimization] pref: Sharding logic supports excluding data sources by specified sequence number [Link](http://github.com/TencentBlueKing/bk-ci/issues/12212)

##### Store
- [Optimization] pref: Optimize plugin detail query logic when plugin repository is deleted [Link](http://github.com/TencentBlueKing/bk-ci/issues/12206)

##### Credential Management
- [Optimization] chore: Upgrade bcprov-jdk15on to 1.78.1 [Link](http://github.com/TencentBlueKing/bk-ci/issues/11888)

##### Permission Center
- [Optimization] perf: My authorization - OAuth authorization prompt optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12227)

#### Bug Fixes

##### Pipeline
- [Bug Fix] bug: Duplicate name exception when saving pipeline draft with constraints [Link](http://github.com/TencentBlueKing/bk-ci/issues/12234)
- [Bug Fix] bug: PAC pipeline permission center resource not released after name change [Link](http://github.com/TencentBlueKing/bk-ci/issues/12078)

##### Store
- [Bug Fix] bug: Latest flag set repeatedly when republishing after multiple version cancellations [Link](http://github.com/TencentBlueKing/bk-ci/issues/12248)

##### Others
- [Bug Fix] bug: Framework upgrade causes Jersey's getAnnotation method unable to retrieve method annotations [Link](http://github.com/TencentBlueKing/bk-ci/issues/12237)
- [Bug Fix] bugfix: Initialize default image script error [Link](http://github.com/TencentBlueKing/bk-ci/issues/12216)
- [Bug Fix] bug: Pipeline event reporting handles abnormal data [Link](http://github.com/TencentBlueKing/bk-ci/issues/12210)

# v4.1.0-rc.1
## 2025-09-01
### Changelog since v4.0.0
#### New Features

##### Pipeline
- [New] pref: Optimize template instance asynchronous update logic [Link](http://github.com/TencentBlueKing/bk-ci/issues/12131)
- [New] feat: Timer trigger supports binding TGIT repository [Link](http://github.com/TencentBlueKing/bk-ci/issues/12073)
- [New] feat: Optimize countGroupByBuildId method [Link](http://github.com/TencentBlueKing/bk-ci/issues/12136)
- [New] feat: Error when pipeline parameter value is too long [Link](http://github.com/TencentBlueKing/bk-ci/issues/10665)
- [New] feat: Variable conditional display supports Code definition [Link](http://github.com/TencentBlueKing/bk-ci/issues/12110)
- [New] Plugin configuration supports field linkage [Link](http://github.com/TencentBlueKing/bk-ci/issues/11251)
- [New] feat: Skip concurrency group check during stage review [Link](http://github.com/TencentBlueKing/bk-ci/issues/12121)
- [New] feat: Plugin "pause before execution" supports Code definition [Link](http://github.com/TencentBlueKing/bk-ci/issues/12113)

##### Environment Management
- [New] feat: Add build machine label query API [Link](http://github.com/TencentBlueKing/bk-ci/issues/12058)

##### Quality Gate
- [New] feat: Quality gate metrics only check at their designated checkpoint [Link](http://github.com/TencentBlueKing/bk-ci/issues/12137)
- [New] feat: Custom bash quality gate metrics support prompt information [Link](http://github.com/TencentBlueKing/bk-ci/issues/12162)

##### Permission Center
- [New] pref: Permission system circuit breaker design - permission cache optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12173)

##### Others
- [New] feat: Event monitoring data collection optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12196)
- [New] feat: Modify OpenAPI ES configuration [Link](http://github.com/TencentBlueKing/bk-ci/issues/11516)
- [New] feat: One-click configure BlueKing gateway to connect with OpenAPI service [Link](http://github.com/TencentBlueKing/bk-ci/issues/12142)
- [New] feat: [OpenAPI] Support enabling/disabling pipelines [Link](http://github.com/TencentBlueKing/bk-ci/issues/12129)
- [New] SQL doc documentation update [Link](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [New] feat: Support inter-service JWT verification [Link](http://github.com/TencentBlueKing/bk-ci/issues/12067)

#### Optimization

##### Pipeline
- [Optimization] pref: Optimize project data migration logic [Link](http://github.com/TencentBlueKing/bk-ci/issues/12117)
- [Optimization] pref: Optimize artifact quality display [Link](http://github.com/TencentBlueKing/bk-ci/issues/12191)

##### Permission Center
- [Optimization] pref: Optimize department info retrieval API [Link](http://github.com/TencentBlueKing/bk-ci/issues/12190)

##### Project Management
- [Optimization] pref: Optimize create/update project process [Link](http://github.com/TencentBlueKing/bk-ci/issues/12152)

##### Others
- [Optimization] Bug: Third-party build machine Docker image pull strategy default value is empty [Link](http://github.com/TencentBlueKing/bk-ci/issues/12198)
- [Optimization] pref: Optimize project plugin info ingestion for metrics service [Link](http://github.com/TencentBlueKing/bk-ci/issues/12139)
- [Optimization] pref: Message queue configuration optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11878)

#### Bug Fixes

##### Repository
- [Bug Fix] bug: Fix PAC pipeline publish exception [Link](http://github.com/TencentBlueKing/bk-ci/issues/12168)
- [Bug Fix] bug: Adjust build API OAuth info retrieval validation logic [Link](http://github.com/TencentBlueKing/bk-ci/issues/12160)

##### Store
- [Bug Fix] fix: Fix incorrect default plugin major version when adding plugin to pipeline [Link](http://github.com/TencentBlueKing/bk-ci/issues/12181)

##### Agent
- [Bug Fix] bugfix: Agent upgrade related bug fixes [Link](http://github.com/TencentBlueKing/bk-ci/issues/12157)
