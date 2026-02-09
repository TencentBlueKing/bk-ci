<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v4.0.1](#v401)
   - [Changelog since v4.0.0](#changelog-since-v400)

- [v4.0.0](#v400)
   - [Changelog since v3.2.0](#changelog-since-v320)

- [v4.0.0-rc.7](#v400-rc7)
   - [Changelog since v4.0.0-rc.6](#changelog-since-v400-rc6)

- [v4.0.0-rc.6](#v400-rc6)
   - [Changelog since v4.0.0-rc.5](#changelog-since-v400-rc5)

- [v4.0.0-rc.5](#v400-rc5)
   - [Changelog since v4.0.0-rc.4](#changelog-since-v400-rc4)

- [v4.0.0-rc.4](#v400-rc4)
   - [Changelog since v4.0.0-rc.3](#changelog-since-v400-rc3)

- [v4.0.0-rc.3](#v400-rc3)
   - [Changelog since v4.0.0-rc.2](#changelog-since-v400-rc2)

- [v4.0.0-rc.2](#v400-rc2)
   - [Changelog since v4.0.0-rc.1](#changelog-since-v400-rc1)

- [v4.0.0-rc.1](#v400-rc1)
   - [Changelog since v3.2.0](#changelog-since-v320)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v4.0.1
## 2025-12-30
### Changelog since v4.0.0
#### Optimization
- [Optimization] chore: Upgrade api-turbo to 0.0.7-RELEASE [Link](http://github.com/TencentBlueKing/bk-ci/issues/12382)

#### Bug Fixes

##### Pipeline
- [Bug Fix] bug: Fix tgit trigger 2.0 version content is empty [Link](http://github.com/TencentBlueKing/bk-ci/issues/12530)

##### Permission Center
- [Bug Fix] bug: Fix release-4.0 service cannot start [Link](http://github.com/TencentBlueKing/bk-ci/issues/12457)

##### Others
- [Bug Fix] bugfix: Initialize default image script error [Link](http://github.com/TencentBlueKing/bk-ci/issues/12216)

# v4.0.0
## 2025-08-15
### Changelog since v3.2.0
### Change Overview
The main changes in this version include:
- Upgrade to JDK17
- Support for code source management, facilitating collaborators to integrate new code sources
- Support for archiving pipelines that don't need execution but need to be retained for audit purposes
- Pipeline variables support grouping, conditional display, and field linkage
- Build summary supports displaying artifact quality
- Third-party build machines support tag management
- Permissions: Support users to actively exit projects and batch transfer permissions; Support administrators to batch remove users
- Internationalization support for Japanese version

### Detailed Changes
#### New Features

##### Pipeline
- [New] feat: Register callback to restrict high-risk ports [Link](http://github.com/TencentBlueKing/bk-ci/issues/11967)
- [New] feat: Support displaying artifact automated test results [Link](http://github.com/TencentBlueKing/bk-ci/issues/11462)
- [New] feat: Adjust sub-pipeline interface parameter names [Link](http://github.com/TencentBlueKing/bk-ci/issues/11979)
- [New] feat: GIT trigger distinguishes between pre and post MR merge checks [Link](http://github.com/TencentBlueKing/bk-ci/issues/11966)
- [New] feat: Add pipeline monitoring events [Link](http://github.com/TencentBlueKing/bk-ci/issues/11874)
- [New] Plugin output supports isSensitive attribute [Link](http://github.com/TencentBlueKing/bk-ci/issues/5534)
- [New] Plugin configuration supports field linkage [Link](http://github.com/TencentBlueKing/bk-ci/issues/11251)
- [New] feat: TGIT event trigger needs to support review event listening [Link](http://github.com/TencentBlueKing/bk-ci/issues/11827)
- [New] feat: Manual review plugin parameter retrieval supports matrix [Link](http://github.com/TencentBlueKing/bk-ci/issues/11933)
- [New] feat: Modify pipeline group name value strategy when code repository enables PAC [Link](http://github.com/TencentBlueKing/bk-ci/issues/11741)
- [New] feat: Sub-pipeline call plugin supports specifying branches when sub-pipeline enables PAC [Link](http://github.com/TencentBlueKing/bk-ci/issues/11768)
- [New] feat: Support UI interface for archiving and managing pipelines [Link](http://github.com/TencentBlueKing/bk-ci/issues/10803)
- [New] feat: PAC Code detects whether pipeline uses namespace [Link](http://github.com/TencentBlueKing/bk-ci/issues/11879)
- [New] feat: New version timer trigger compatible with old version template parameters [Link](http://github.com/TencentBlueKing/bk-ci/issues/11803)
- [New] feat: Optimize performance of sub-pipeline circular dependency detection when saving/releasing pipelines [Link](http://github.com/TencentBlueKing/bk-ci/issues/11753)
- [New] feat: Detect irregular usage between stage review parameters and input parameters [Link](http://github.com/TencentBlueKing/bk-ci/issues/11853)
- [New] Hope mac public build machine Xcode version selection box can fill variables for control [Link](http://github.com/TencentBlueKing/bk-ci/issues/11855)
- [New] feat: System built-in context display missing /job/step level variables [Link](http://github.com/TencentBlueKing/bk-ci/issues/10845)
- [New] feat: Pipeline parameter display in execution preview interface supports linkage [Link](http://github.com/TencentBlueKing/bk-ci/issues/11438)
- [New] feat: GIT event trigger supports outputting TAG description information and TAPD ticket numbers [Link](http://github.com/TencentBlueKing/bk-ci/issues/11721)
- [New] feat: Stage review/manual review notification links add positioning [Link](http://github.com/TencentBlueKing/bk-ci/issues/11704)
- [New] feat: Script plugin supports user-defined error codes and error messages [Link](http://github.com/TencentBlueKing/bk-ci/issues/11747)
- [New] feat: CODE supports SVN_TAG type [Link](http://github.com/TencentBlueKing/bk-ci/issues/11781)
- [New] Get third-party build machine list API under project adds return host name [Link](http://github.com/TencentBlueKing/bk-ci/issues/11673)
- [New] feat: Timer trigger supports setting startup variables [Link](http://github.com/TencentBlueKing/bk-ci/issues/10617)
- [New] feat: Manual review plugin review interface needs reasonable parameter validation [Link](http://github.com/TencentBlueKing/bk-ci/issues/11770)
- [New] feat: Pipeline plugin configuration auto-retry displays timeout failure logs [Link](http://github.com/TencentBlueKing/bk-ci/issues/11755)
- [New] feat: Sub-pipeline plugin supports passing recommended version number variables [Link](http://github.com/TencentBlueKing/bk-ci/issues/11684)
- [New] feat: Add expression usage documentation guidance [Link](http://github.com/TencentBlueKing/bk-ci/issues/11723)
- [New] feat: Support configuring whether to report error and terminate execution when pipeline variables are too long [Link](http://github.com/TencentBlueKing/bk-ci/issues/11592)
- [New] feat: Variable grouping supports Code definition [Link](http://github.com/TencentBlueKing/bk-ci/issues/11698)
- [New] feat: Github event trigger supports branch filtering [Link](http://github.com/TencentBlueKing/bk-ci/issues/11682)
- [New] feat: Running retry display optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/10483)
- [New] feat: Execution time display specific resources supports Code setting [Link](http://github.com/TencentBlueKing/bk-ci/issues/11588)
- [New] feat: Pipeline view and build detail view configuration interface sensitive field display optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11019)
- [New] Plugin report function can display latest report by default [Link](http://github.com/TencentBlueKing/bk-ci/issues/11638)
- [New] feat: Sub-pipeline call plugin input parameter type as text box, frontend should be textarea component [Link](http://github.com/TencentBlueKing/bk-ci/issues/11605)
- [New] feat: Matrix job include/exclude syntax optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11519)
- [New] feat: Support build replay events [Link](http://github.com/TencentBlueKing/bk-ci/issues/11232)
- [New] feat: File type variable uploaded files support version management [Link](http://github.com/TencentBlueKing/bk-ci/issues/11467)

##### Code Repository
- [New] feat: When associating code repository, if authorization method is OAUTH, support filtering code repositories by authorization account [Link](http://github.com/TencentBlueKing/bk-ci/issues/11483)
- [New] feat: Add code source universal webhook trigger [Link](http://github.com/TencentBlueKing/bk-ci/issues/11611)
- [New] feat: Platform management - Code source management [Link](http://github.com/TencentBlueKing/bk-ci/issues/11379)
- [New] perf: Refactor code repository service code structure [Link](http://github.com/TencentBlueKing/bk-ci/issues/9952)
- [New] feat: Code source management and code repository service associated code repository linkage [Link](http://github.com/TencentBlueKing/bk-ci/issues/11498)

##### R&D Store
- [New] feat: Plugin application for get_credential interface automatically approved [Link](http://github.com/TencentBlueKing/bk-ci/issues/12039)
- [New] feat: R&D store plugin notification administrator review text adds review button link [Link](http://github.com/TencentBlueKing/bk-ci/issues/11938)
- [New] feat: Add component creation post-processing [Link](http://github.com/TencentBlueKing/bk-ci/issues/11823)
- [New] feat: R&D store plugin listing enters review stage to notify administrator approval [Link](http://github.com/TencentBlueKing/bk-ci/issues/11897)
- [New] [R&D Store] Support displaying version logs [Link](http://github.com/TencentBlueKing/bk-ci/issues/1761)
- [New] feat: R&D store components support one-click publishing [Link](http://github.com/TencentBlueKing/bk-ci/issues/11543)
- [New] bua: Fix publisher information synchronization layering may cause ID setting error when getting ID by organization name with same name organizations [Link](http://github.com/TencentBlueKing/bk-ci/issues/11643)

##### Environment Management
- [New] feat: Third-party build machine tags support batch editing [Link](http://github.com/TencentBlueKing/bk-ci/issues/11902)
- [New] feat: Support getting and modifying third-party build machine information [Link](http://github.com/TencentBlueKing/bk-ci/issues/12072)
- [New] feat: Import third-party build machine supports automatic switching startup user [Link](http://github.com/TencentBlueKing/bk-ci/issues/11945)
- [New] feat: Third-party build machine nodes support tag management [Link](http://github.com/TencentBlueKing/bk-ci/issues/11881)
- [New] feat: Environment management supports search [Link](http://github.com/TencentBlueKing/bk-ci/issues/11844)
- [New] feat: Support multi-parameter getting third-party build machine related interfaces [Link](http://github.com/TencentBlueKing/bk-ci/issues/11759)
- [New] feat: node_third_part_detail interface gets correct node data [Link](http://github.com/TencentBlueKing/bk-ci/issues/11607)
- [New] Optimize environment management build machine Agent scheduled maintenance tasks [Link](http://github.com/TencentBlueKing/bk-ci/issues/11579)

##### Permission Center
- [New] feat: Permission system circuit breaker design data accuracy verification [Link](http://github.com/TencentBlueKing/bk-ci/issues/11964)
- [New] feat: Permission model optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11832)
- [New] feat: Support administrator batch remove users [Link](http://github.com/TencentBlueKing/bk-ci/issues/11200)
- [New] feat: Permission system circuit breaker design prerequisite data preparation [Link](http://github.com/TencentBlueKing/bk-ci/issues/11789)

##### Project Management
- [New] feat: v4_app_project_list interface supports channel query and pagination parameters [Link](http://github.com/TencentBlueKing/bk-ci/issues/11751)
- [New] feat: project_list interface adds product_id filter condition [Link](http://github.com/TencentBlueKing/bk-ci/issues/11610)

##### Stream
- [New] feat: Reference push trigger to add stream cross-repository trigger branch deletion scenario trigger parameters [Link](http://github.com/TencentBlueKing/bk-ci/issues/11634)

##### Scheduling
- [New] feat: Non-compilation build machine polling task logic optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12046)
- [New] feat: Non-compilation build machine task claiming adds container status double verification [Link](http://github.com/TencentBlueKing/bk-ci/issues/11904)

##### Agent
- [New] Private build machines using Docker to run build tasks can also get custom environment variables [Link](http://github.com/TencentBlueKing/bk-ci/issues/11955)
- [New] Fix mac agent disconnection reconnection long-term failure [Link](http://github.com/TencentBlueKing/bk-ci/issues/11918)
- [New] feat: Support plugin output passing pipeline artifact metadata [Link](http://github.com/TencentBlueKing/bk-ci/issues/11940)
- [New] Agent IP change update mechanism optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11774)
- [New] Build process ends later than next build retry [Link](http://github.com/TencentBlueKing/bk-ci/issues/11873)
- [New] Agent dependency upgrade [Link](http://github.com/TencentBlueKing/bk-ci/issues/11599)
- [New] Agent error exception throwing [Link](http://github.com/TencentBlueKing/bk-ci/issues/11573)

##### Others
- [New] feat: Maven repository publishing migrated from oss to central [Link](http://github.com/TencentBlueKing/bk-ci/issues/11817)
- [New] feat: Support inter-service jwt verification [Link](http://github.com/TencentBlueKing/bk-ci/issues/12067)
- [New] feat: Remove proxy cross-network zone code [Link](http://github.com/TencentBlueKing/bk-ci/issues/12091)
- [New] feat: Upgrade turbo version [Link](http://github.com/TencentBlueKing/bk-ci/issues/12055)
- [New] feat: Report supports compressed preview [Link](http://github.com/TencentBlueKing/bk-ci/issues/11923)
- [New] feat: BlueKing internationalization supports Japanese version [Link](http://github.com/TencentBlueKing/bk-ci/issues/11877)
- [New] feat: okhttp client adds dns configuration [Link](http://github.com/TencentBlueKing/bk-ci/issues/12026)
- [New] Tencent open source License update [Link](http://github.com/TencentBlueKing/bk-ci/issues/11919)
- [New] feat: Message notification supports platform-level "communication blacklist" settings [Link](http://github.com/TencentBlueKing/bk-ci/issues/11885)
- [New] feat: Hope openapi provides batch query pipeline build task interface [Link](http://github.com/TencentBlueKing/bk-ci/issues/11889)
- [New] feat: Query service jump adds pipeline dimension parameters [Link](http://github.com/TencentBlueKing/bk-ci/issues/11765)
- [New] sql doc documentation update [Link](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [New] feat: Add some openapi [Link](http://github.com/TencentBlueKing/bk-ci/issues/11655)
- [New] feat: [PAC Template] Pipeline template supports PAC features [Link](http://github.com/TencentBlueKing/bk-ci/issues/11414)
- [New] feat: Upgrade JDK17 [Link](http://github.com/TencentBlueKing/bk-ci/issues/10593)
- [New] feat: Let mq initialize normally when program starts [Link](http://github.com/TencentBlueKing/bk-ci/issues/11584)

#### Optimizations

##### Pipeline
- [Optimization] pref: Optimize project creation/modification validation [Link](http://github.com/TencentBlueKing/bk-ci/issues/12052)
- [Optimization] perf: Pipeline group text optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12056)
- [Optimization] perf: Optimize scheduled task locking rules [Link](http://github.com/TencentBlueKing/bk-ci/issues/12041)
- [Optimization] perf: Record operation logs when archiving pipelines [Link](http://github.com/TencentBlueKing/bk-ci/issues/11922)
- [Optimization] perf: Archived pipeline view page, export and delete operations unavailable [Link](http://github.com/TencentBlueKing/bk-ci/issues/12006)
- [Optimization] perf: Pipeline governance optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11968)
- [Optimization] perf: Pipeline release description required optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11843)
- [Optimization] perf: Display optimization when comparing with draft version [Link](http://github.com/TencentBlueKing/bk-ci/issues/11969)
- [Optimization] pref: Support variable grouping [Link](http://github.com/TencentBlueKing/bk-ci/issues/11590)
- [Optimization] pref: Pipeline version reference identifier refresh interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11829)
- [Optimization] perf: Pipeline running, retry failed steps queue full problem follow-up and optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11807)
- [Optimization] pref: Log archive download link supports automatic domain switching [Link](http://github.com/TencentBlueKing/bk-ci/issues/11582)

##### Code Repository
- [Optimization] perf: Optimize code source webhook parsing process [Link](http://github.com/TencentBlueKing/bk-ci/issues/11694)

##### R&D Store
- [Optimization] pref: Optimize R&D store metrics associated published component query [Link](http://github.com/TencentBlueKing/bk-ci/issues/12099)
- [Optimization] perf: Optimize R&D store component query [Link](http://github.com/TencentBlueKing/bk-ci/issues/12027)
- [Optimization] pref: Plugin runtime package file download timeout optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11954)
- [Optimization] pref: R&D store component version number specification adjustment [Link](http://github.com/TencentBlueKing/bk-ci/issues/11780)
- [Optimization] pref: R&D store homepage component query by visible range filter optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11676)
- [Optimization] pref: Improve R&D store component configuration file parameter validation [Link](http://github.com/TencentBlueKing/bk-ci/issues/11269)
- [Optimization] pref: R&D store component package signing process optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11572)
- [Optimization] pref: Download plugins in testing status execution packages not from artifact repository cache [Link](http://github.com/TencentBlueKing/bk-ci/issues/11615)

##### Log Service
- [Optimization] perf: Log module data cleanup does not support cross-cluster [Link](http://github.com/TencentBlueKing/bk-ci/issues/11814)

##### Permission Center
- [Optimization] pref: Get user group member interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11891)
- [Optimization] pref: Group member synchronization optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11864)
- [Optimization] pref: No permission jump application optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11769)
- [Optimization] pref: User management interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11816)
- [Optimization] pref: Batch transfer interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11725)
- [Optimization] pref: Permission center open interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11465)
- [Optimization] pref: User management related interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11687)
- [Optimization] pref: User group add user interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11678)

##### Project Management
- [Optimization] pref: db sharding rule save optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11811)
- [Optimization] pref: Enable disable project interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11763)
- [Optimization] pref: db sharding rule save update optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11732)

##### Others
- [Optimization] docs: third party notices update [Link](http://github.com/TencentBlueKing/bk-ci/issues/12122)
- [Optimization] pref: Super admin get resource logic optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12103)
- [Optimization] pref: process database migration data cleanup logic optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12064)
- [Optimization] pref: Artifact quality display optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12096)
- [Optimization] pref: metrics service some interfaces improve permission verification [Link](http://github.com/TencentBlueKing/bk-ci/issues/11620)
- [Optimization] pref: metrics build data reporting interface may cause thread blocking when build concurrency is high [Link](http://github.com/TencentBlueKing/bk-ci/issues/12043)
- [Optimization] pref: Message queue configuration optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11878)
- [Optimization] pref: metrics service data reporting interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12014)
- [Optimization] pref: Adjust metrics service data reporting interface distributed lock timeout [Link](http://github.com/TencentBlueKing/bk-ci/issues/12005)
- [Optimization] pref: Get artifact repository file download link interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11999)
- [Optimization] pref: metrics build data reporting interface may cause thread blocking when same pipeline build concurrency is high [Link](http://github.com/TencentBlueKing/bk-ci/issues/11921)
- [Optimization] docs: Update JDK production documentation [Link](http://github.com/TencentBlueKing/bk-ci/issues/11937)
- [Optimization] pref: Add build machine interface for worker to determine current environment type [Link](http://github.com/TencentBlueKing/bk-ci/issues/11636)

#### Bug Fixes

##### Pipeline
- [Bug Fix] bug: Template instance update, display condition field not assigned [Link](http://github.com/TencentBlueKing/bk-ci/issues/12087)
- [Bug Fix] Static pipeline group search pipeline, add pipeline save after, next operation will cause entire page unresponsive [Link](http://github.com/TencentBlueKing/bk-ci/issues/12035)
- [Bug Fix] bug: Have project administrator permission, but cannot operate delete pipeline group [Link](http://github.com/TencentBlueKing/bk-ci/issues/12031)
- [Bug Fix] fix: Condition judgment process log description error [Link](http://github.com/TencentBlueKing/bk-ci/issues/12008)
- [Bug Fix] mac public build machine cannot select xcode16 version configuration [Link](http://github.com/TencentBlueKing/bk-ci/issues/11980)
- [Bug Fix] bug: Pipeline owner cannot export pipeline [Link](http://github.com/TencentBlueKing/bk-ci/issues/11959)
- [Bug Fix] bug: PAC pipeline submission failure reason refinement [Link](http://github.com/TencentBlueKing/bk-ci/issues/11740)
- [Bug Fix] bug: PAC mode pipeline deletion, pipeline group still exists needs optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11742)
- [Bug Fix] bug: PAC create pipeline group, initialize pipeline group very slow [Link](http://github.com/TencentBlueKing/bk-ci/issues/11686)
- [Bug Fix] bug: Running retry, retry plugin stage status must be running [Link](http://github.com/TencentBlueKing/bk-ci/issues/11802)
- [Bug Fix] bug: Scheduled trigger task cannot be removed normally [Link](http://github.com/TencentBlueKing/bk-ci/issues/11833)
- [Bug Fix] bug: Trigger compatible with GitLab SVN repository name [Link](http://github.com/TencentBlueKing/bk-ci/issues/11799)
- [Bug Fix] bug: Fix scheduled task not triggered when project ID contains _ [Link](http://github.com/TencentBlueKing/bk-ci/issues/11800)
- [Bug Fix] bug: Pipeline artifact download 10Gb limit fix [Link](http://github.com/TencentBlueKing/bk-ci/issues/11793)
- [Bug Fix] bug: Dependent Job single step retry success but not execute subsequent Job fix [Link](http://github.com/TencentBlueKing/bk-ci/issues/11412)
- [Bug Fix] Pipeline template instantiation checkbox default value not automatically filled [Link](http://github.com/TencentBlueKing/bk-ci/issues/11761)
- [Bug Fix] bug: Copy pipeline page, belonging dynamic pipeline group logic error [Link](http://github.com/TencentBlueKing/bk-ci/issues/11734)
- [Bug Fix] Sub-pipeline call plugin delete parameter causes parameter value to become default value fix [Link](http://github.com/TencentBlueKing/bk-ci/issues/11709)
- [Bug Fix] Pipeline group management permission page error [Link](http://github.com/TencentBlueKing/bk-ci/issues/11714)
- [Bug Fix] bug: Fix variable type checkbox one-click copy cannot click [Link](http://github.com/TencentBlueKing/bk-ci/issues/11705)
- [Bug Fix] bug: step shorthand context invalid [Link](http://github.com/TencentBlueKing/bk-ci/issues/11688)
- [Bug Fix] bug: Template release, check template image published logic fix [Link](http://github.com/TencentBlueKing/bk-ci/issues/11600)
- [Bug Fix] bug: Recycle bin restored pipeline name will have extra string of numbers needs optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11632)
- [Bug Fix] Edit template pipeline, matchRuleList interface parameter missing [Link](http://github.com/TencentBlueKing/bk-ci/issues/11613)
- [Bug Fix] bug: Peak period engine printed service internal build logs trigger circuit breaker [Link](http://github.com/TencentBlueKing/bk-ci/issues/11589)
- [Bug Fix] bug: Pipeline template version sort list abnormal, latest update version not at top [Link](http://github.com/TencentBlueKing/bk-ci/issues/11495)

##### Code Repository
- [Bug Fix] bug: Enable PAC repository member validation failure [Link](http://github.com/TencentBlueKing/bk-ci/issues/12085)
- [Bug Fix] bug: Create code repository and enable PAC, scmCode field empty [Link](http://github.com/TencentBlueKing/bk-ci/issues/11820)
- [Bug Fix] bug: OAUTH authorization interface no need to verify platform management permission [Link](http://github.com/TencentBlueKing/bk-ci/issues/11748)

##### R&D Store
- [Bug Fix] bug: Built-in plugin target parameter needs to support nullable [Link](http://github.com/TencentBlueKing/bk-ci/issues/11936)
- [Bug Fix] bug: R&D store unpublished components in testing no need to filter by visible range [Link](http://github.com/TencentBlueKing/bk-ci/issues/11863)
- [Bug Fix] bug: Plugin unzip needs to remove root directory [Link](http://github.com/TencentBlueKing/bk-ci/issues/11825)
- [Bug Fix] bug: Image market homepage query by recommended use invalid [Link](http://github.com/TencentBlueKing/bk-ci/issues/11675)
- [Bug Fix] bug: Get R&D store component upgrade version interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11669)
- [Bug Fix] bug: Fix store homepage hidden application query [Link](http://github.com/TencentBlueKing/bk-ci/issues/11648)

##### Environment Management
- [Bug Fix] Environment management node function top right no import button, cannot download agent [Link](http://github.com/TencentBlueKing/bk-ci/issues/11950)
- [Bug Fix] bug: Environment management search CMDB node returns empty [Link](http://github.com/TencentBlueKing/bk-ci/issues/11645)

##### Permission Center
- [Bug Fix] bug: Fix user interface authentication occasional stuck [Link](http://github.com/TencentBlueKing/bk-ci/issues/12111)

##### Project Management
- [Bug Fix] bug: Add db sharding rule snapshot read may cause duplicate data insertion [Link](http://github.com/TencentBlueKing/bk-ci/issues/11860)

##### Agent
- [Bug Fix] bugfix: Third-party build machine cancelled retry task [Link](http://github.com/TencentBlueKing/bk-ci/issues/11268)

##### Others
- [Bug Fix] Configuration no default value artifactory service startup failure [Link](http://github.com/TencentBlueKing/bk-ci/issues/12097)
- [Bug Fix] bug: Remove interface redundant userId parameter [Link](http://github.com/TencentBlueKing/bk-ci/issues/12069)
- [Bug Fix] bug: User interface add project access permission verification [Link](http://github.com/TencentBlueKing/bk-ci/issues/11971)
- [Bug Fix] bug: publish-plugin plugin does not support Nexus Pro 3 [Link](http://github.com/TencentBlueKing/bk-ci/issues/11944)
- [Bug Fix] bug: Solve deadlock problem caused by recording user operation data [Link](http://github.com/TencentBlueKing/bk-ci/issues/11776)
- [Bug Fix] bug: ThreadPoolUtil submitAction method continuously creates thread pools [Link](http://github.com/TencentBlueKing/bk-ci/issues/11702)
- [Bug Fix] bug: Interface parameter variables starting with is serialization exception [Link](http://github.com/TencentBlueKing/bk-ci/issues/11689)

# v4.0.0-rc.7
## 2025-08-15
### Changelog since v4.0.0-rc.6
#### New Features

##### Pipeline
- [New] feat: Register callback to restrict high-risk ports [Link](http://github.com/TencentBlueKing/bk-ci/issues/11967)
- [New] feat: Support displaying artifact automated test results [Link](http://github.com/TencentBlueKing/bk-ci/issues/11462)

##### Code Repository
- [New] feat: When associating code repository, if authorization method is OAUTH, support filtering code repositories by authorization account [Link](http://github.com/TencentBlueKing/bk-ci/issues/11483)
- [New] feat: Add code source universal webhook trigger [Link](http://github.com/TencentBlueKing/bk-ci/issues/11611)

##### Environment Management
- [New] feat: Third-party build machine tags support batch editing [Link](http://github.com/TencentBlueKing/bk-ci/issues/11902)
- [New] feat: Support getting and modifying third-party build machine information [Link](http://github.com/TencentBlueKing/bk-ci/issues/12072)
- [New] feat: Import third-party build machine supports automatic switching startup user [Link](http://github.com/TencentBlueKing/bk-ci/issues/11945)
- [New] feat: Environment management supports search [Link](http://github.com/TencentBlueKing/bk-ci/issues/11844)

##### Permission Center
- [New] feat: Permission system circuit breaker design data accuracy verification [Link](http://github.com/TencentBlueKing/bk-ci/issues/11964)

##### Others
- [New] feat: Maven repository publishing migrated from oss to central [Link](http://github.com/TencentBlueKing/bk-ci/issues/11817)
- [New] feat: Support inter-service jwt verification [Link](http://github.com/TencentBlueKing/bk-ci/issues/12067)
- [New] feat: Remove proxy cross-network zone code [Link](http://github.com/TencentBlueKing/bk-ci/issues/12091)
- [New] feat: Upgrade turbo version [Link](http://github.com/TencentBlueKing/bk-ci/issues/12055)

#### Optimizations

##### R&D Store
- [Optimization] pref: Optimize R&D store metrics associated published component query [Link](http://github.com/TencentBlueKing/bk-ci/issues/12099)

##### Others
- [Optimization] docs: third party notices update [Link](http://github.com/TencentBlueKing/bk-ci/issues/12122)
- [Optimization] pref: Super admin get resource logic optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12103)
- [Optimization] pref: process database migration data cleanup logic optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12064)
- [Optimization] pref: Artifact quality display optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12096)

#### Bug Fixes

##### Pipeline
- [Bug Fix] bug: Template instance update, display condition field not assigned [Link](http://github.com/TencentBlueKing/bk-ci/issues/12087)

##### Code Repository
- [Bug Fix] bug: Enable PAC repository member validation failure [Link](http://github.com/TencentBlueKing/bk-ci/issues/12085)

##### Permission Center
- [Bug Fix] bug: Fix user interface authentication occasional stuck [Link](http://github.com/TencentBlueKing/bk-ci/issues/12111)

##### Others
- [Bug Fix] Configuration no default value artifactory service startup failure [Link](http://github.com/TencentBlueKing/bk-ci/issues/12097)
- [Bug Fix] bug: User interface add project access permission verification [Link](http://github.com/TencentBlueKing/bk-ci/issues/11971)

# v4.0.0-rc.6
## 2025-08-01
### Changelog since v4.0.0-rc.5
#### New Features

##### Pipeline
- [New] feat: Support displaying artifact automated test results [Link](http://github.com/TencentBlueKing/bk-ci/issues/11462)
- [New] feat: Adjust sub-pipeline interface parameter names [Link](http://github.com/TencentBlueKing/bk-ci/issues/11979)
- [New] feat: GIT trigger distinguishes between pre and post MR merge checks [Link](http://github.com/TencentBlueKing/bk-ci/issues/11966)

##### Code Repository
- [New] feat: Platform management - Code source management [Link](http://github.com/TencentBlueKing/bk-ci/issues/11379)

##### R&D Store
- [New] feat: Plugin application for get_credential interface automatically approved [Link](http://github.com/TencentBlueKing/bk-ci/issues/12039)

##### Environment Management
- [New] feat: Import third-party build machine supports automatic switching startup user [Link](http://github.com/TencentBlueKing/bk-ci/issues/11945)

##### Permission Center
- [New] feat: Permission model optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11832)
- [New] feat: Permission system circuit breaker design data accuracy verification [Link](http://github.com/TencentBlueKing/bk-ci/issues/11964)

##### Scheduling
- [New] feat: Non-compilation build machine polling task logic optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12046)

##### Agent
- [New] feat: Support plugin output passing pipeline artifact metadata [Link](http://github.com/TencentBlueKing/bk-ci/issues/11940)

##### Others
- [New] feat: Report supports compressed preview [Link](http://github.com/TencentBlueKing/bk-ci/issues/11923)
- [New] feat: BlueKing internationalization supports Japanese version [Link](http://github.com/TencentBlueKing/bk-ci/issues/11877)
- [New] feat: okhttp client adds dns configuration [Link](http://github.com/TencentBlueKing/bk-ci/issues/12026)

#### Optimizations

##### Pipeline
- [Optimization] pref: Optimize project creation/modification validation [Link](http://github.com/TencentBlueKing/bk-ci/issues/12052)
- [Optimization] perf: Pipeline group text optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/12056)
- [Optimization] perf: Optimize scheduled task locking rules [Link](http://github.com/TencentBlueKing/bk-ci/issues/12041)
- [Optimization] perf: Record operation logs when archiving pipelines [Link](http://github.com/TencentBlueKing/bk-ci/issues/11922)
- [Optimization] perf: Archived pipeline view page, export and delete operations unavailable [Link](http://github.com/TencentBlueKing/bk-ci/issues/12006)

##### R&D Store
- [Optimization] perf: Optimize R&D store component query [Link](http://github.com/TencentBlueKing/bk-ci/issues/12027)

##### Others
- [Optimization] pref: metrics service some interfaces improve permission verification [Link](http://github.com/TencentBlueKing/bk-ci/issues/11620)
- [Optimization] pref: metrics build data reporting interface may cause thread blocking when build concurrency is high [Link](http://github.com/TencentBlueKing/bk-ci/issues/12043)
- [Optimization] pref: Message queue configuration optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11878)
- [Optimization] pref: Add build machine interface for worker to determine current environment type [Link](http://github.com/TencentBlueKing/bk-ci/issues/11636)

#### Bug Fixes

##### Pipeline
- [Bug Fix] Static pipeline group search pipeline, add pipeline save after, next operation will cause entire page unresponsive [Link](http://github.com/TencentBlueKing/bk-ci/issues/12035)
- [Bug Fix] bug: Have project administrator permission, but cannot operate delete pipeline group [Link](http://github.com/TencentBlueKing/bk-ci/issues/12031)

##### Others
- [Bug Fix] bug: Remove interface redundant userId parameter [Link](http://github.com/TencentBlueKing/bk-ci/issues/12069)
- [Bug Fix] bug: User interface add project access permission verification [Link](http://github.com/TencentBlueKing/bk-ci/issues/11971)

# v4.0.0-rc.5
## 2025-07-18
### Changelog since v4.0.0-rc.4
#### New Features
- [New] feat: Add pipeline monitoring events [Link](http://github.com/TencentBlueKing/bk-ci/issues/11874)
- [New] Plugin output supports isSensitive attribute [Link](http://github.com/TencentBlueKing/bk-ci/issues/5534)
- [New] Plugin configuration supports field linkage [Link](http://github.com/TencentBlueKing/bk-ci/issues/11251)
- [New] feat: TGIT event trigger supports review event listening [Link](http://github.com/TencentBlueKing/bk-ci/issues/11827)
- [New] feat: Manual review plugin parameter retrieval supports matrix [Link](http://github.com/TencentBlueKing/bk-ci/issues/11933)
- [New] feat: Third-party build machine nodes support tag management [Link](http://github.com/TencentBlueKing/bk-ci/issues/11881)
- [New] Private build machines using Docker can get custom environment variables [Link](http://github.com/TencentBlueKing/bk-ci/issues/11955)
- [New] Fix mac agent disconnection reconnection long-term failure [Link](http://github.com/TencentBlueKing/bk-ci/issues/11918)
- [New] feat: BlueKing internationalization supports Japanese version [Link](http://github.com/TencentBlueKing/bk-ci/issues/11877)
- [New] Tencent open source License update [Link](http://github.com/TencentBlueKing/bk-ci/issues/11919)

#### Optimizations
- [Optimization] perf: Record operation logs when archiving pipelines [Link](http://github.com/TencentBlueKing/bk-ci/issues/11922)
- [Optimization] perf: Pipeline governance optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11968)
- [Optimization] perf: Pipeline release description required optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11843)
- [Optimization] perf: Display optimization when comparing with draft version [Link](http://github.com/TencentBlueKing/bk-ci/issues/11969)
- [Optimization] pref: Plugin runtime package file download timeout optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11954)

#### Bug Fixes
- [Bug Fix] fix: Condition judgment process log description error [Link](http://github.com/TencentBlueKing/bk-ci/issues/12008)
- [Bug Fix] mac public build machine cannot select xcode16 version configuration [Link](http://github.com/TencentBlueKing/bk-ci/issues/11980)
- [Bug Fix] bug: Pipeline owner cannot export pipeline [Link](http://github.com/TencentBlueKing/bk-ci/issues/11959)
- [Bug Fix] bug: Built-in plugin target parameter needs to support nullable [Link](http://github.com/TencentBlueKing/bk-ci/issues/11936)
- [Bug Fix] Environment management node function no import button, cannot download agent [Link](http://github.com/TencentBlueKing/bk-ci/issues/11950)
- [Bug Fix] bug: publish-plugin plugin does not support Nexus Pro 3 [Link](http://github.com/TencentBlueKing/bk-ci/issues/11944)

# v4.0.0-rc.4
## 2025-07-04
### Changelog since v4.0.0-rc.3
#### New Features
- [New] feat: Sub-pipeline call plugin supports specifying branches when PAC enabled [Link](http://github.com/TencentBlueKing/bk-ci/issues/11768)
- [New] feat: Support UI interface for archiving and managing pipelines [Link](http://github.com/TencentBlueKing/bk-ci/issues/10803)
- [New] feat: PAC Code detects whether pipeline uses namespace [Link](http://github.com/TencentBlueKing/bk-ci/issues/11879)
- [New] feat: New version timer trigger compatible with old template parameters [Link](http://github.com/TencentBlueKing/bk-ci/issues/11803)
- [New] feat: Optimize sub-pipeline circular dependency detection performance [Link](http://github.com/TencentBlueKing/bk-ci/issues/11753)
- [New] feat: Detect stage review parameters and input parameters irregular usage [Link](http://github.com/TencentBlueKing/bk-ci/issues/11853)
- [New] Hope mac public build machine Xcode version selection supports variables [Link](http://github.com/TencentBlueKing/bk-ci/issues/11855)
- [New] feat: System built-in context display missing /job/step level variables [Link](http://github.com/TencentBlueKing/bk-ci/issues/10845)
- [New] feat: Pipeline parameter display in execution preview supports linkage [Link](http://github.com/TencentBlueKing/bk-ci/issues/11438)
- [New] feat: GIT event trigger supports outputting TAG description and TAPD ticket numbers [Link](http://github.com/TencentBlueKing/bk-ci/issues/11721)
- [New] feat: Stage review/manual review notification links add positioning [Link](http://github.com/TencentBlueKing/bk-ci/issues/11704)
- [New] feat: Script plugin supports user-defined error codes and messages [Link](http://github.com/TencentBlueKing/bk-ci/issues/11747)
- [New] feat: Manual review plugin review interface needs reasonable parameter validation [Link](http://github.com/TencentBlueKing/bk-ci/issues/11770)
- [New] feat: Pipeline plugin configuration auto-retry displays timeout failure logs [Link](http://github.com/TencentBlueKing/bk-ci/issues/11755)
- [New] feat: Sub-pipeline plugin supports passing recommended version number variables [Link](http://github.com/TencentBlueKing/bk-ci/issues/11684)
- [New] feat: R&D store plugin listing enters review stage to notify administrator [Link](http://github.com/TencentBlueKing/bk-ci/issues/11897)
- [New] feat: Support multi-parameter getting third-party build machine interfaces [Link](http://github.com/TencentBlueKing/bk-ci/issues/11759)
- [New] feat: Support administrator batch remove users [Link](http://github.com/TencentBlueKing/bk-ci/issues/11200)
- [New] feat: Permission system circuit breaker design prerequisite data preparation [Link](http://github.com/TencentBlueKing/bk-ci/issues/11789)
- [New] feat: v4_app_project_list interface supports channel query and pagination [Link](http://github.com/TencentBlueKing/bk-ci/issues/11751)
- [New] Agent IP change update mechanism optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11774)
- [New] Build process ends later than next build retry [Link](http://github.com/TencentBlueKing/bk-ci/issues/11873)
- [New] feat: Message notification supports platform-level communication blacklist settings [Link](http://github.com/TencentBlueKing/bk-ci/issues/11885)
- [New] feat: Hope openapi provides batch query pipeline build task interface [Link](http://github.com/TencentBlueKing/bk-ci/issues/11889)
- [New] feat: Maven repository publishing migrated from oss to central [Link](http://github.com/TencentBlueKing/bk-ci/issues/11817)

#### Optimizations
- [Optimization] pref: Support variable grouping [Link](http://github.com/TencentBlueKing/bk-ci/issues/11590)
- [Optimization] pref: Pipeline version reference identifier refresh interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11829)
- [Optimization] pref: R&D store component version number specification adjustment [Link](http://github.com/TencentBlueKing/bk-ci/issues/11780)
- [Optimization] pref: Get user group member interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11891)
- [Optimization] pref: Group member synchronization optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11864)
- [Optimization] pref: No permission jump application optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11769)
- [Optimization] pref: User management interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11816)
- [Optimization] pref: db sharding rule save optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11811)

#### Bug Fixes
- [Bug Fix] bug: PAC pipeline submission failure reason refinement [Link](http://github.com/TencentBlueKing/bk-ci/issues/11740)
- [Bug Fix] bug: PAC mode pipeline deletion, pipeline group still exists [Link](http://github.com/TencentBlueKing/bk-ci/issues/11742)
- [Bug Fix] bug: PAC create pipeline group, initialize pipeline group very slow [Link](http://github.com/TencentBlueKing/bk-ci/issues/11686)
- [Bug Fix] bug: Running retry, retry plugin stage status must be running [Link](http://github.com/TencentBlueKing/bk-ci/issues/11802)
- [Bug Fix] bug: Scheduled trigger task cannot be removed normally [Link](http://github.com/TencentBlueKing/bk-ci/issues/11833)
- [Bug Fix] bug: Trigger compatible with GitLab SVN repository name [Link](http://github.com/TencentBlueKing/bk-ci/issues/11799)
- [Bug Fix] bug: Pipeline artifact download 10Gb limit fix [Link](http://github.com/TencentBlueKing/bk-ci/issues/11793)
- [Bug Fix] bug: Create code repository and enable PAC, scmCode field empty [Link](http://github.com/TencentBlueKing/bk-ci/issues/11820)
- [Bug Fix] bug: R&D store unpublished components in testing no need to filter by visible range [Link](http://github.com/TencentBlueKing/bk-ci/issues/11863)
- [Bug Fix] bug: Plugin unzip needs to remove root directory [Link](http://github.com/TencentBlueKing/bk-ci/issues/11825)
- [Bug Fix] bug: Image market homepage query by recommended use invalid [Link](http://github.com/TencentBlueKing/bk-ci/issues/11675)
- [Bug Fix] bug: Add db sharding rule snapshot read may cause duplicate data insertion [Link](http://github.com/TencentBlueKing/bk-ci/issues/11860)
- [Bug Fix] bug: Solve deadlock problem caused by recording user operation data [Link](http://github.com/TencentBlueKing/bk-ci/issues/11776)

# v4.0.0-rc.3
## 2025-06-10
### Changelog since v4.0.0-rc.2
#### New Features
- [New] feat: CODE supports SVN_TAG type [Link](http://github.com/TencentBlueKing/bk-ci/issues/11781)
- [New] feat: Query service jump adds pipeline dimension parameters [Link](http://github.com/TencentBlueKing/bk-ci/issues/11765)

#### Optimizations
- [Optimization] perf: Pipeline running retry failed steps queue full problem optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11807)
- [Optimization] perf: Log module data cleanup does not support cross-cluster [Link](http://github.com/TencentBlueKing/bk-ci/issues/11814)
- [Optimization] pref: db sharding rule save optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11811)
- [Optimization] pref: Enable disable project interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11763)

#### Bug Fixes
- [Bug Fix] bug: Running retry, retry plugin stage status must be running [Link](http://github.com/TencentBlueKing/bk-ci/issues/11802)
- [Bug Fix] bug: Fix scheduled task not triggered when project ID contains _ [Link](http://github.com/TencentBlueKing/bk-ci/issues/11800)

# v4.0.0-rc.2
## 2025-05-29
### Changelog since v4.0.0-rc.1
#### New Features
- [New] Get third-party build machine list API adds return host name [Link](http://github.com/TencentBlueKing/bk-ci/issues/11673)
- [New] feat: Timer trigger supports setting startup variables [Link](http://github.com/TencentBlueKing/bk-ci/issues/10617)
- [New] feat: Add expression usage documentation guidance [Link](http://github.com/TencentBlueKing/bk-ci/issues/11723)
- [New] feat: Support configuring pipeline variable length error termination [Link](http://github.com/TencentBlueKing/bk-ci/issues/11592)
- [New] feat: Variable grouping supports Code definition [Link](http://github.com/TencentBlueKing/bk-ci/issues/11698)
- [New] feat: Github event trigger supports branch filtering [Link](http://github.com/TencentBlueKing/bk-ci/issues/11682)
- [New] Agent dependency upgrade [Link](http://github.com/TencentBlueKing/bk-ci/issues/11599)
- [New] Agent error exception throwing [Link](http://github.com/TencentBlueKing/bk-ci/issues/11573)

#### Optimizations
- [Optimization] perf: Optimize code source webhook parsing process [Link](http://github.com/TencentBlueKing/bk-ci/issues/11694)
- [Optimization] pref: R&D store homepage component query by visible range filter optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11676)
- [Optimization] pref: Batch transfer interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11725)
- [Optimization] pref: Permission center open interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11465)
- [Optimization] pref: User management related interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11687)
- [Optimization] pref: db sharding rule save update optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11732)

#### Bug Fixes
- [Bug Fix] bug: Dependent Job single step retry success but not execute subsequent Job [Link](http://github.com/TencentBlueKing/bk-ci/issues/11412)
- [Bug Fix] Pipeline template instantiation checkbox default value not automatically filled [Link](http://github.com/TencentBlueKing/bk-ci/issues/11761)
- [Bug Fix] bug: Copy pipeline page, belonging dynamic pipeline group logic error [Link](http://github.com/TencentBlueKing/bk-ci/issues/11734)
- [Bug Fix] Sub-pipeline call plugin delete parameter causes parameter value to become default [Link](http://github.com/TencentBlueKing/bk-ci/issues/11709)
- [Bug Fix] Pipeline group management permission page error [Link](http://github.com/TencentBlueKing/bk-ci/issues/11714)
- [Bug Fix] bug: Fix variable type checkbox one-click copy cannot click [Link](http://github.com/TencentBlueKing/bk-ci/issues/11705)
- [Bug Fix] bug: OAUTH authorization interface no need to verify platform management permission [Link](http://github.com/TencentBlueKing/bk-ci/issues/11748)
- [Bug Fix] bugfix: Third-party build machine cancelled retry task [Link](http://github.com/TencentBlueKing/bk-ci/issues/11268)
- [Bug Fix] bug: ThreadPoolUtil submitAction method continuously creates thread pools [Link](http://github.com/TencentBlueKing/bk-ci/issues/11702)

# v4.0.0-rc.1
## 2025-05-09
### Changelog since v3.2.0
#### New Features
- [New] feat: Running retry display optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/10483)
- [New] feat: Execution time display specific resources supports Code setting [Link](http://github.com/TencentBlueKing/bk-ci/issues/11588)
- [New] feat: Pipeline view and build detail view sensitive field display optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11019)
- [New] Plugin report function can display latest report by default [Link](http://github.com/TencentBlueKing/bk-ci/issues/11638)
- [New] feat: Sub-pipeline call plugin input parameter type as text box, frontend should be textarea [Link](http://github.com/TencentBlueKing/bk-ci/issues/11605)
- [New] feat: Matrix job include/exclude syntax optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11519)
- [New] feat: Support build replay events [Link](http://github.com/TencentBlueKing/bk-ci/issues/11232)
- [New] feat: File type variable uploaded files support version management [Link](http://github.com/TencentBlueKing/bk-ci/issues/11467)
- [New] perf: Refactor code repository service code structure [Link](http://github.com/TencentBlueKing/bk-ci/issues/9952)
- [New] feat: Platform management - Code source management [Link](http://github.com/TencentBlueKing/bk-ci/issues/11379)
- [New] feat: Code source management and code repository service linkage [Link](http://github.com/TencentBlueKing/bk-ci/issues/11498)
- [New] [R&D Store] Support displaying version logs [Link](http://github.com/TencentBlueKing/bk-ci/issues/1761)
- [New] feat: R&D store components support one-click publishing [Link](http://github.com/TencentBlueKing/bk-ci/issues/11543)
- [New] bua: Fix publisher information synchronization layering organization name ID setting error [Link](http://github.com/TencentBlueKing/bk-ci/issues/11643)
- [New] feat: node_third_part_detail interface gets correct node data [Link](http://github.com/TencentBlueKing/bk-ci/issues/11607)
- [New] Optimize environment management build machine Agent scheduled maintenance tasks [Link](http://github.com/TencentBlueKing/bk-ci/issues/11579)
- [New] feat: project_list interface adds product_id filter condition [Link](http://github.com/TencentBlueKing/bk-ci/issues/11610)
- [New] feat: Reference push trigger to add stream cross-repository trigger branch deletion scenario [Link](http://github.com/TencentBlueKing/bk-ci/issues/11634)
- [New] sql doc documentation update [Link](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [New] feat: Add some openapi [Link](http://github.com/TencentBlueKing/bk-ci/issues/11655)
- [New] feat: [PAC Template] Pipeline template supports PAC features [Link](http://github.com/TencentBlueKing/bk-ci/issues/11414)
- [New] feat: Upgrade JDK17 [Link](http://github.com/TencentBlueKing/bk-ci/issues/10593)
- [New] feat: Let mq initialize normally when program starts [Link](http://github.com/TencentBlueKing/bk-ci/issues/11584)

#### Optimizations
- [Optimization] pref: Support variable grouping [Link](http://github.com/TencentBlueKing/bk-ci/issues/11590)
- [Optimization] pref: Log archive download link supports automatic domain switching [Link](http://github.com/TencentBlueKing/bk-ci/issues/11582)
- [Optimization] pref: Improve R&D store component configuration file parameter validation [Link](http://github.com/TencentBlueKing/bk-ci/issues/11269)
- [Optimization] pref: R&D store component package signing process optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11572)
- [Optimization] pref: Download plugins in testing status execution packages not from artifact repository cache [Link](http://github.com/TencentBlueKing/bk-ci/issues/11615)
- [Optimization] pref: User group add user interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11678)

#### Bug Fixes
- [Bug Fix] bug: Template release, check template image published logic fix [Link](http://github.com/TencentBlueKing/bk-ci/issues/11600)
- [Bug Fix] bug: Recycle bin restored pipeline name will have extra string of numbers [Link](http://github.com/TencentBlueKing/bk-ci/issues/11632)
- [Bug Fix] Edit template pipeline, matchRuleList interface parameter missing [Link](http://github.com/TencentBlueKing/bk-ci/issues/11613)
- [Bug Fix] bug: Peak period engine printed service internal build logs trigger circuit breaker [Link](http://github.com/TencentBlueKing/bk-ci/issues/11589)
- [Bug Fix] bug: Pipeline template version sort list abnormal, latest update version not at top [Link](http://github.com/TencentBlueKing/bk-ci/issues/11495)
- [Bug Fix] bug: Get R&D store component upgrade version interface optimization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11669)
- [Bug Fix] bug: Fix store homepage hidden application query [Link](http://github.com/TencentBlueKing/bk-ci/issues/11648)
- [Bug Fix] bug: Environment management search CMDB node returns empty [Link](http://github.com/TencentBlueKing/bk-ci/issues/11645)
