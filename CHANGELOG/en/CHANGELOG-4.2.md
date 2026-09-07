<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v4.2.0-rc.4](#v420-rc4)
   - [Changelog since v4.2.0-rc.3](#changelog-since-v420-rc3)

- [v4.2.0-rc.3](#v420-rc3)
   - [Changelog since v4.2.0-rc.2](#changelog-since-v420-rc2)

- [v4.2.0-rc.2](#v420-rc2)
   - [Changelog since v4.2.0-rc.1](#changelog-since-v420-rc1)

- [v4.2.0-rc.1](#v420-rc1)
   - [Changelog since v4.1.0](#changelog-since-v410)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v4.2.0-rc.6
## 2026-08-18
### Changelog since v4.2.0-rc.5
### Summary
Key changes in this release:

**Features**
- Creation Flow: support copying to personal projects by following the manifest, add start-node keywords to scheduled-trigger YAML, and optimize OpenAPI routing to no longer distinguish by project
- Matrix jobs: support manual retry of failed steps, and when retrying a failed job, only rerun the failed job instead of the entire matrix
- Log Service: add multiple query interfaces
- Repository: support user-mode interfaces to get branch and tag lists
- Store: plugin external task links support SDK reporting and canvas navigation
- Support GitHub issue assignee and label events
- Version changelog supports separation from the frontend image, with independent mounting across multiple clusters
- Build history supports hiding the actions column

**Bug Fixes**
- Fixed cancelled jobs during a retried build leaving a residual CANCELED status that wrongly marked the final build status as cancelled
- Fixed "pipeline build does not exist" error when cancelling and restarting a build (buildRestart) that replayed an already-retried build
- Fixed retries mistakenly running prior matrix jobs
- Fixed pipeline-level callbacks not being cleaned up after the start events were cleared

#### New Features

##### Pipeline
- [New] feat: steps under a matrix job support manual retry on failure [Link](http://github.com/TencentBlueKing/bk-ci/issues/10780)

##### Repository
- [New] feat: support user-mode interfaces to get repository branch list / tag list [Link](http://github.com/TencentBlueKing/bk-ci/issues/13013)

##### Store
- [New] feat: plugin external task link SDK reporting and canvas navigation [Link](http://github.com/TencentBlueKing/bk-ci/issues/13312)

##### Log Service
- [New] feat: Log Service adds multiple query interfaces [Link](http://github.com/TencentBlueKing/bk-ci/issues/13230)

##### Others
- [New] bug: support GitHub issue assignee and label events [Link](http://github.com/TencentBlueKing/bk-ci/issues/13404)
- [New] feat: Creation Flow supports copying to personal projects by following the manifest [Link](http://github.com/TencentBlueKing/bk-ci/issues/13432)
- [New] feat: add an OP interface to modify the maximum number of pipelines under a project [Link](http://github.com/TencentBlueKing/bk-ci/issues/13423)
- [New] feat: when retrying a failed job, if it is a matrix job, support rerunning only the failed job instead of the whole matrix [Link](http://github.com/TencentBlueKing/bk-ci/issues/10799)
- [New] feat: Creation Flow scheduled-trigger YAML adds start-node keywords [Link](http://github.com/TencentBlueKing/bk-ci/issues/13309)
- [New] Support hiding the actions column in build history table settings [Link](http://github.com/TencentBlueKing/bk-ci/issues/13376)
- [New] feat: version changelog supports separation from the frontend image, enabling independent mounting across multiple clusters [Link](http://github.com/TencentBlueKing/bk-ci/issues/13363)

#### Improvements

##### Pipeline
- [Improved] perf: [PAC] use the user-entered description for the MR title when publishing pipelines/templates [Link](http://github.com/TencentBlueKing/bk-ci/issues/13444)

##### Others
- [Improved] pref: Creation Flow OpenAPI requests no longer route to different clusters by project [Link](http://github.com/TencentBlueKing/bk-ci/issues/13410)
- [Improved] chore: upgrade devopsScm to 1.1.10 [Link](http://github.com/TencentBlueKing/bk-ci/issues/13373)

#### Bug Fixes

##### Pipeline
- [Fixed] bug: draftVersion duplication under concurrent/same-second draft saves causes primary key conflict in T_PIPELINE_RESOURCE_DRAFT_VERSION [Link](http://github.com/TencentBlueKing/bk-ci/issues/13459)
- [Fixed] bug: TAPD API-created requirements/bugs lack the ci.event_url variable [Link](http://github.com/TencentBlueKing/bk-ci/issues/13405)
- [Fixed] bug: when a PAC template instantiates a pipeline triggered from the repository, checkbox/multi-select dropdown default values are broken into [] [Link](http://github.com/TencentBlueKing/bk-ci/issues/13447)
- [Fixed] bug: remove sub-pipeline branch version validation when saving pipelines/templates [Link](http://github.com/TencentBlueKing/bk-ci/issues/13412)
- [Fixed] bug: [PAC] branch version execution should use the branch version settings [Link](http://github.com/TencentBlueKing/bk-ci/issues/12697)
- [Fixed] bug: optimize text when trigger event has no permission [Link](http://github.com/TencentBlueKing/bk-ci/issues/13103)
- [Fixed] bug: pipeline-level callback is not cleaned up after the start events are cleared [Link](http://github.com/TencentBlueKing/bk-ci/issues/13313)

##### Others
- [Fixed] bug: fix draft save validation error showing function source code [Link](http://github.com/TencentBlueKing/bk-ci/issues/13454)
- [Fixed] Fix real-time updates in the Creative Stream embedded view [Link](http://github.com/TencentBlueKing/bk-ci/issues/13426)
- [Fixed] bug: cancel API interception of non-formal pipeline versions [Link](http://github.com/TencentBlueKing/bk-ci/issues/13402)
- [Fixed] bug: resource lock errors when converting YAML to UI with variables [Link](http://github.com/TencentBlueKing/bk-ci/issues/13421)
- [Fixed] fix: cancelling and restarting a build (buildRestart) that replays an already-retried build reports "pipeline build [xxx] does not exist" [Link](http://github.com/TencentBlueKing/bk-ci/issues/13430)
- [Fixed] bug: publishing a template with the same name fails when the branch version/version is behind [Link](http://github.com/TencentBlueKing/bk-ci/issues/13427)
- [Fixed] bug: retries mistakenly run prior matrix jobs [Link](http://github.com/TencentBlueKing/bk-ci/issues/13084)
- [Fixed] fix: refresh timer trigger start-parameter options after flow variable changes [Link](http://github.com/TencentBlueKing/bk-ci/issues/13413)
- [Fixed] bugfix: environment management start/stop function overridden, dynamic environments not started/stopped [Link](http://github.com/TencentBlueKing/bk-ci/issues/13398)
- [Fixed] bug: a cancelled Job during a retried build leaves a residual CANCELED status, causing the final build status to be wrongly marked as cancelled [Link](http://github.com/TencentBlueKing/bk-ci/issues/13407)
- [Fixed] Fix boolean pipeline variable default value handling [Link](http://github.com/TencentBlueKing/bk-ci/issues/13329)
- [Fixed] bug: optimize Creation Flow plugin query by specified OS [Link](http://github.com/TencentBlueKing/bk-ci/issues/13396)
- [Fixed] bug: draft overwrite warning popup fetches an incorrect unpublished draft version number [Link](http://github.com/TencentBlueKing/bk-ci/issues/13387)
- [Fixed] bug: adjust yaml schema $.concurrency.queue-length default length to 200 [Link](http://github.com/TencentBlueKing/bk-ci/issues/13292)
- [Fixed] bug: after adding a variable (set runtime read-only) to a template, the instance cannot modify the variable value on update [Link](http://github.com/TencentBlueKing/bk-ci/issues/13369)
- [Fixed] Optimize artifact download preparation text spacing [Link](http://github.com/TencentBlueKing/bk-ci/issues/13328)

# v4.2.0-rc.5
## 2026-08-04
### Changelog since v4.2.0-rc.4
### Summary
Key changes in this release:

**Features**
- Creation Flow: support Creation Flow management; Creation Flow environments and creation environments support system attributes and specified OS
- Support storage and query of pipeline metadata for image artifacts
- Pipeline group management: provide Apigw interfaces for pipeline group management and optimize pipeline group display
- Repository trigger flow changed from serial to parallel pipeline execution
- Review plugin supports sending reminders to specific users
- Optimize concurrent draft editing interaction
- Provide some cloud-dev OpenAPI interfaces
- GONGFENGSCAN (Gongfeng Commit Check) uses the CodeCC result URL

**Bug Fixes**
- Fixed timed task deletion failure causing Quartz RAMJobStore memory leak
- Fixed template instance upgrade with parameter type changes triggering infinite updates and crashing the page
- Fixed webhook build parameter overflow discarding the entire payload (now only drops oversized environment variables)
- Fixed PAC-instantiated YAML pipelines showing abnormal status when deleted or when the YAML format is invalid

#### New Features

##### Pipeline
- [New] feat: optimize concurrent draft editing interaction [Link](http://github.com/TencentBlueKing/bk-ci/issues/10553)
- [New] feat: support storage and query of pipeline metadata for image artifacts [Link](http://github.com/TencentBlueKing/bk-ci/issues/13160)
- [New] feat: GONGFENGSCAN (Gongfeng Commit Check) uses the CodeCC result URL [Link](http://github.com/TencentBlueKing/bk-ci/issues/13253)
- [New] feat: optimize repository trigger flow: serial to parallel pipeline execution [Link](http://github.com/TencentBlueKing/bk-ci/issues/13186)

##### Environment Management
- [New] feat: Creation Flow environment supports system attributes [Link](http://github.com/TencentBlueKing/bk-ci/issues/13282)
- [New] feat: optimize task list for environments/nodes under environment management [Link](http://github.com/TencentBlueKing/bk-ci/issues/13269)
- [New] feat: third-party build machine Docker supports no-mount keywords [Link](http://github.com/TencentBlueKing/bk-ci/issues/13251)

##### Others
- [New] feat: review plugin supports sending reminders to specific users [Link](http://github.com/TencentBlueKing/bk-ci/issues/13382)
- [New] feat: creation environment supports specified OS [Link](http://github.com/TencentBlueKing/bk-ci/issues/13324)
- [New] feat: Creation Flow management [Link](http://github.com/TencentBlueKing/bk-ci/issues/12414)
- [New] feat: provide Apigw interfaces for pipeline group management [Link](http://github.com/TencentBlueKing/bk-ci/issues/13289)
- [New] feat: provide some cloud-dev OpenAPI interfaces [Link](http://github.com/TencentBlueKing/bk-ci/issues/13279)

#### Improvements
- [Improved] pref: improve store upload filename validation [Link](http://github.com/TencentBlueKing/bk-ci/issues/13027)
- [Improved] pref: CodeCC rule set id conversion [Link](http://github.com/TencentBlueKing/bk-ci/issues/13354)
- [Improved] pref: add per-candidate total execution timeout to AI model failover [Link](http://github.com/TencentBlueKing/bk-ci/issues/13255)
- [Improved] pref: strengthen AI's ability to locate sub-pipeline builds [Link](http://github.com/TencentBlueKing/bk-ci/issues/13262)
- [Improved] perf: optimize pipeline group display [Link](http://github.com/TencentBlueKing/bk-ci/issues/12943)
- [Improved] perf: optimize required-field check strategy when setting matrix jobs via UI [Link](http://github.com/TencentBlueKing/bk-ci/issues/12957)

#### Bug Fixes

##### Pipeline
- [Fixed] bug: fix webhook build parameter overflow handling - drop oversized environment variables instead of the whole payload [Link](http://github.com/TencentBlueKing/bk-ci/issues/13356)
- [Fixed] bug: timed task deletion failure causing Quartz RAMJobStore memory leak [Link](http://github.com/TencentBlueKing/bk-ci/issues/13352)
- [Fixed] bug: optimize template instantiation message when scheduled trigger repository does not exist [Link](http://github.com/TencentBlueKing/bk-ci/issues/13346)
- [Fixed] bug: repository branch parameter type not repopulated from previous build parameters [Link](http://github.com/TencentBlueKing/bk-ci/issues/13336)
- [Fixed] bug: [PAC] deleted YAML pipelines should be removed from the YAML pipeline group [Link](http://github.com/TencentBlueKing/bk-ci/issues/12870)
- [Fixed] bug: PAC-instantiated pipelines stay in "upgrading" status when YAML format is invalid [Link](http://github.com/TencentBlueKing/bk-ci/issues/13261)
- [Fixed] bug: template instance upgrade with parameter type changes triggers infinite updates and crashes the page [Link](http://github.com/TencentBlueKing/bk-ci/issues/13294)
- [Fixed] bug: [PAC] finding the pipeline version for the default branch YAML should not require checking whether the version is active [Link](http://github.com/TencentBlueKing/bk-ci/issues/13295)
- [Fixed] bug: clicking artifacts whose name contains '#' on the build artifact page has no effect [Link](http://github.com/TencentBlueKing/bk-ci/issues/13270)

##### Repository
- [Fixed] bug: platform-managed GitHub config not taking effect [Link](http://github.com/TencentBlueKing/bk-ci/issues/13231)

##### Environment Management
- [Fixed] bugfix: add node compatibility logic fix [Link](http://github.com/TencentBlueKing/bk-ci/issues/13355)
- [Fixed] bug: job execution page scrollbar disappears [Link](http://github.com/TencentBlueKing/bk-ci/issues/13254)

##### Log Service
- [Fixed] bug: exception handling for sudden traffic spikes in the log module [Link](http://github.com/TencentBlueKing/bk-ci/issues/13327)

##### Project Management
- [Fixed] bug: the total service count shown in the homepage menu does not match the actual displayed count [Link](http://github.com/TencentBlueKing/bk-ci/issues/13322)
- [Fixed] bug: project route publish batch interface supports passing project blacklist and project enable params [Link](http://github.com/TencentBlueKing/bk-ci/issues/13319)

##### Dispatch
- [Fixed] bugfix: third-party build redirect link issue [Link](http://github.com/TencentBlueKing/bk-ci/issues/13301)

##### Others
- [Fixed] bug: optimize GitApi trigger judgment [Link](http://github.com/TencentBlueKing/bk-ci/issues/13392)
- [Fixed] bug: Creation Flow draft save occasionally reverts to old orchestration; version delete confirmation dialog blocked by sidebar [Link](http://github.com/TencentBlueKing/bk-ci/issues/13386)
- [Fixed] bug: fix v4_user_repository_get and v4_app_repository_get APIs cannot query repository aliases [Link](http://github.com/TencentBlueKing/bk-ci/issues/13308)
- [Fixed] bugfix: fix start/stop node openapi [Link](http://github.com/TencentBlueKing/bk-ci/issues/13287)
- [Fixed] bug: the "Add Variable" panel field should be named "Variable Name" instead of "Variable ID" [Link](http://github.com/TencentBlueKing/bk-ci/issues/13246)

# v4.2.0-rc.4
## 2026-07-16
### Changelog since v4.2.0-rc.3
### Summary
Key changes in this release:

**Features**
- Creation Flow phase 1: management, visibility scope, workspace, YAML bidirectional conversion, repository event triggers, and third-party build agents
- Pipelines support TAPD event triggers (phase 1)
- Support copying pipelines across projects
- Support pipeline public variable management
- Support custom parameter types and list parameter types for complex inputs
- Store supports project-level visibility scope and version changelog display
- Environment management refactor: node enable/disable supports reason notes and operation logs

**Bug Fixes**
- Fixed deleting early pipeline versions that could remove a version still used by a stuck build
- Fixed build environment auto-retry not releasing the reuse lock
- Fixed failed plugins that could not be retried or skipped when a later plugin runs only if previous plugins failed

#### New Features

##### Pipeline
- [New] feat: Support phase 1 of TAPD event triggering [Link](http://github.com/TencentBlueKing/bk-ci/issues/12959)
- [New] feat: Generate AI summaries when publishing regular pipelines [Link](http://github.com/TencentBlueKing/bk-ci/issues/13100)
- [New] feat: Support copying pipelines across projects [Link](http://github.com/TencentBlueKing/bk-ci/issues/12918)
- [New] feat: Support more operators for variable condition display [Link](http://github.com/TencentBlueKing/bk-ci/issues/12335)
- [New] feat: Python plugin supports running in an isolated virtual environment when compiling and executing [Link](http://github.com/TencentBlueKing/bk-ci/issues/12656)
- [New] feat: Support retrieving sub-pipeline startup parameters by channel [Link](http://github.com/TencentBlueKing/bk-ci/issues/13155)
- [New] feat: Refactor the build environment [Link](http://github.com/TencentBlueKing/bk-ci/issues/13050)
- [New] feat: Support repository event triggering for creation flows [Link](http://github.com/TencentBlueKing/bk-ci/issues/12842)
- [New] feat: Provide custom parameter types and list parameter types to facilitate entering complex parameters [Link](http://github.com/TencentBlueKing/bk-ci/issues/12689)
- [New] feat: Support retrieving the list of pipelines the user has permission for by channel [Link](http://github.com/TencentBlueKing/bk-ci/issues/13115)
- [New] feat: Validate circular dependency of sub-pipelines when saving a pipeline [Link](http://github.com/TencentBlueKing/bk-ci/issues/10479)
- [New] feat: Add node IP response fields to the build history API [Link](http://github.com/TencentBlueKing/bk-ci/issues/13009)
- [New] feat: Support starting via the API by specifying a trigger plugin [Link](http://github.com/TencentBlueKing/bk-ci/issues/12828)
- [New] feat: Platform management - register events [Link](http://github.com/TencentBlueKing/bk-ci/issues/12379)
- [New] feat: Pipeline public variable management [Link](http://github.com/TencentBlueKing/bk-ci/issues/12010)

##### Repository
- [New] feat: Associated repositories can adjust the repository URL within their original configuration [Link](http://github.com/TencentBlueKing/bk-ci/issues/13130)

##### Store
- [New] feat: Unified switch of Store comment-related APIs to public APIs [Link](http://github.com/TencentBlueKing/bk-ci/issues/13242)
- [New] feat: Store - support setting visibility scope by BlueKing project [Link](http://github.com/TencentBlueKing/bk-ci/issues/13080)
- [New] feat: Store - support application installation paths and installation methods [Link](http://github.com/TencentBlueKing/bk-ci/issues/13105)
- [New] Store: Support displaying version logs [Link](http://github.com/TencentBlueKing/bk-ci/issues/1761)
- [New] feat: Store plugin homepage API supports filtering data by service scope [Link](http://github.com/TencentBlueKing/bk-ci/issues/12785)
- [New] feat: OP supports modifying a plugin's "Applicable Scope" [Link](http://github.com/TencentBlueKing/bk-ci/issues/12803)

##### Environment Management
- [New] feat: Add reason descriptions and operation logs when disabling/enabling nodes in the build environment [Link](http://github.com/TencentBlueKing/bk-ci/issues/12764)
- [New] Add skip policies for build nodes and deployment nodes [Link](http://github.com/TencentBlueKing/bk-ci/issues/13145)
- [New] feat: Node filtering in environment management supports filtering by node status [Link](http://github.com/TencentBlueKing/bk-ci/issues/12851)
- [New] feat: Add the node list returned by the creation flow trigger environment [Link](http://github.com/TencentBlueKing/bk-ci/issues/13064)
- [New] feat: Create environment/node management [Link](http://github.com/TencentBlueKing/bk-ci/issues/12389)
- [New] feat: Team creation flow third-party machine related [Link](http://github.com/TencentBlueKing/bk-ci/issues/13004)
- [New] feat: Refactor environment management [Link](http://github.com/TencentBlueKing/bk-ci/issues/12416)

##### Log Service
- [New] feat: Add various query interfaces to the log service [Link](http://github.com/TencentBlueKing/bk-ci/issues/13230)

##### Quality Red Line
- [New] feat: The CodeCC metric jump link in CodeCC comments on Gongfeng MRs prioritizes the configured logPrompt [Link](http://github.com/TencentBlueKing/bk-ci/issues/13137)

##### Permission Center
- [New] feat: Support migrating project-level user groups and members to other projects [Link](http://github.com/TencentBlueKing/bk-ci/issues/13109)

##### Others
- [New] feat: Optimize task list of environments/nodes under environment management [Link](http://github.com/TencentBlueKing/bk-ci/issues/13269)
- [New] feat: Creation flow management [Link](http://github.com/TencentBlueKing/bk-ci/issues/12414)
- [New] feat: Creation flows support workspaces [Link](http://github.com/TencentBlueKing/bk-ci/issues/13208)
- [New] feat: Creation flows support YAML conversion [Link](http://github.com/TencentBlueKing/bk-ci/issues/12862)
- [New] feat: Add permission checks for the trigger user and machine when starting a team creation flow [Link](http://github.com/TencentBlueKing/bk-ci/issues/13139)
- [New] Return the corresponding host for the report URL based on the request source [Link](http://github.com/TencentBlueKing/bk-ci/issues/13135)
- [New] feat: Add an API Gateway interface for querying project members by condition [Link](http://github.com/TencentBlueKing/bk-ci/issues/13094)
- [New] feat: Add a session ID system variable when starting a creation flow [Link](http://github.com/TencentBlueKing/bk-ci/issues/13014)
- [New] feat: Provide an OpenAPI interface for retrieving plugin YAML files [Link](http://github.com/TencentBlueKing/bk-ci/issues/12977)
- [New] feat: Add a visibility scope to creation flows [Link](http://github.com/TencentBlueKing/bk-ci/issues/12823)
- [New] feat: Creation flow phase 1 [Link](http://github.com/TencentBlueKing/bk-ci/issues/12400)
- [New] feat: Automatically add AI descriptions when publishing creation flows [Link](http://github.com/TencentBlueKing/bk-ci/issues/12825)
- [New] feat: Creation flow - third-party machines [Link](http://github.com/TencentBlueKing/bk-ci/issues/12354)

#### Improvements

##### Pipeline
- [Improved] perf: Add cleanup reminders to the recycle bin [Link](http://github.com/TencentBlueKing/bk-ci/issues/13171)
- [Improved] perf: Reduce access frequency for Redis hot keys [Link](http://github.com/TencentBlueKing/bk-ci/issues/12488)

##### Store
- [Improved] perf: Optimize the plugin feature whitelist cache mechanism [Link](http://github.com/TencentBlueKing/bk-ci/issues/13244)
- [Improved] perf: Auto-update fixes stale plugin run info cache [Link](http://github.com/TencentBlueKing/bk-ci/issues/13213)
- [Improved] perf: Adjust creation flow plugin category information [Link](http://github.com/TencentBlueKing/bk-ci/issues/12819)

##### Permission Center
- [Improved] perf: Optimize department validation after removing users from a project [Link](http://github.com/TencentBlueKing/bk-ci/issues/13164)

##### Others
- [Improved] perf: Click to batch display batch task history [Link](http://github.com/TencentBlueKing/bk-ci/issues/13267)
- [Improved] perf: Optimize the BK-CI intelligent assistant's analysis of pipeline error scenarios [Link](http://github.com/TencentBlueKing/bk-ci/issues/13219)
- [Improved] perf: Optimize plugin permission checks under creation flows [Link](http://github.com/TencentBlueKing/bk-ci/issues/13190)
- [Improved] perf: Enhance the BK-CI intelligent assistant's capability to analyze user permissions [Link](http://github.com/TencentBlueKing/bk-ci/issues/13117)
- [Improved] perf: Support routing some CodeCC interfaces to corresponding clusters by specified tag [Link](http://github.com/TencentBlueKing/bk-ci/issues/13129)
- [Improved] perf: Support a switch to control whether to write monitoring data to InfluxDB [Link](http://github.com/TencentBlueKing/bk-ci/issues/13077)

#### Bug Fixes

##### Pipeline
- [Fixed] bug: Incorrect format validation for the trigger-conf.variables parameter in scheduled trigger YAML configuration [Link](http://github.com/TencentBlueKing/bk-ci/issues/13192)
- [Fixed] Bug: Pipeline Code (YAML) edit mode: a blank panel at the bottom obscures the horizontal scrollbar when a code line is long, preventing horizontal scrolling/selection [Link](http://github.com/TencentBlueKing/bk-ci/issues/13232)
- [Fixed] bug: Deleting early pipeline version records might delete the version currently stuck in a build [Link](http://github.com/TencentBlueKing/bk-ci/issues/13218)
- [Fixed] bugfix: Build environment auto-retry failed to release the reuse lock [Link](http://github.com/TencentBlueKing/bk-ci/issues/13214)
- [Fixed] feat: Optimize sub-pipeline access [Link](http://github.com/TencentBlueKing/bk-ci/issues/13003)
- [Fixed] Fix missing add trigger action for imported pipeline drafts [Link](http://github.com/TencentBlueKing/bk-ci/issues/13202)
- [Fixed] bug: A plugin whose run condition is "Run only when a preceding plugin fails" prevents the preceding failed plugin from being retried or skipped [Link](http://github.com/TencentBlueKing/bk-ci/issues/13167)
- [Fixed] bug: Re-validate plugin visibility when instantiating Store templates [Link](http://github.com/TencentBlueKing/bk-ci/issues/13170)
- [Fixed] bug: The baseline version of a template draft may not be the latest official version [Link](http://github.com/TencentBlueKing/bk-ci/issues/13125)
- [Fixed] bug: Editing a template via code mode may turn other variables into input parameters [Link](http://github.com/TencentBlueKing/bk-ci/issues/13126)
- [Fixed] bug: Modifying as-instance-input via template code mode did not take effect [Link](http://github.com/TencentBlueKing/bk-ci/issues/13113)
- [Fixed] fix: Display issue with the duration of a running step on the build detail page [Link](http://github.com/TencentBlueKing/bk-ci/issues/9705)

##### Store
- [Fixed] bug: When searching by keyword on the plugin selection page in a pipeline, if the result set is large, some plugins may fail to display properly [Link](http://github.com/TencentBlueKing/bk-ci/issues/12808)
- [Fixed] bug: Old configurations were not fully cleaned when reducing the service scope while modifying plugin basic information [Link](http://github.com/TencentBlueKing/bk-ci/issues/13182)

##### Environment Management
- [Fixed] bugfix: Fix creation flow environment related bugs [Link](http://github.com/TencentBlueKing/bk-ci/issues/13225)
- [Fixed] bugfix: Global agent acquisition failure caused tasks to be un-cancelable [Link](http://github.com/TencentBlueKing/bk-ci/issues/13193)
- [Fixed] fix: Incorrect node count in the environment management node list [Link](http://github.com/TencentBlueKing/bk-ci/issues/13151)

##### Others
- [Fixed] bug: Job execution page scrollbar disappeared [Link](http://github.com/TencentBlueKing/bk-ci/issues/13254)
- [Fixed] bug: Fix creation flow trigger bug [Link](http://github.com/TencentBlueKing/bk-ci/issues/13226)
- [Fixed] bug: When viewing plugin details, if under the creation flow service, clicking through to the Store should go to the creation flow plugin [Link](http://github.com/TencentBlueKing/bk-ci/issues/13228)
- [Fixed] bug: Handle permission logic that was missed for channel-based validation in creation flows [Link](http://github.com/TencentBlueKing/bk-ci/issues/13239)
- [Fixed] feat: Support specifying a custom workspace for creation-flow Job [Link](http://github.com/TencentBlueKing/bk-ci/issues/13209)
- [Fixed] bug: Node information of creation flows triggered by WEB_HOOK type was not persisted [Link](http://github.com/TencentBlueKing/bk-ci/issues/13187)

# v4.2.0-rc.3
## 2026-06-08
### Changelog since v4.2.0-rc.2
#### New Features

##### Pipeline
- [New] feat: PAC pipelines support specifying branches [Link](http://github.com/TencentBlueKing/bk-ci/issues/12635)

##### Environment Management
- [New] feat: Refactor environment management [Link](http://github.com/TencentBlueKing/bk-ci/issues/12416)
- [New] feat: Create environment/node management [Link](http://github.com/TencentBlueKing/bk-ci/issues/12389)

##### Permission Center
- [New] feat: Add AI companion APIs for permission member governance, issue #13019 [Link](http://github.com/TencentBlueKing/bk-ci/issues/13019)

##### Project Management
- [New] feat: Support personal projects [Link](http://github.com/TencentBlueKing/bk-ci/issues/12852)

##### Others
- [New] feat: Provide a build API for plugins to retrieve the raw configuration of the current step [Link](http://github.com/TencentBlueKing/bk-ci/issues/12953)
- [New] feat: BK-CI intelligent assistant supports multi-channel LLM configuration, user model configuration, and failover [Link](http://github.com/TencentBlueKing/bk-ci/issues/12958)
- [New] feat: Integrate CodeCC rule sets with the permission center [Link](http://github.com/TencentBlueKing/bk-ci/issues/12981)
- [New] feat: Customize scheduling priority for third-party build machine clusters [Link](http://github.com/TencentBlueKing/bk-ci/issues/2680)
- [New] feat: Optimize dependency package version upgrades [Link](http://github.com/TencentBlueKing/bk-ci/issues/12951)
- [New] fix: Fix startup failure caused by missing interface implementation in the project module [Link](http://github.com/TencentBlueKing/bk-ci/issues/1285)

#### Improvements

##### Permission Center
- [Improved] perf: Optimize permission application and handover scenarios [Link](http://github.com/TencentBlueKing/bk-ci/issues/13026)

##### Others
- [Improved] docs: Link the CodeCC repository [Link](http://github.com/TencentBlueKing/bk-ci/issues/13075)
- [Improved] perf: Optimize gap lock handling for the T_PIPELINE_WEBHOOK_QUEUE table [Link](http://github.com/TencentBlueKing/bk-ci/issues/13052)
- [Improved] perf: Optimize metrics data reporting [Link](http://github.com/TencentBlueKing/bk-ci/issues/13000)
- [Improved] perf: Optimize copyright notices [Link](http://github.com/TencentBlueKing/bk-ci/issues/12988)
- [Improved] perf: Optimize Skill documents with progressive disclosure to reduce context usage and unify structure [Link](http://github.com/TencentBlueKing/bk-ci/issues/12948)
- [Improved] perf: Optimize the ZIP decompression utility [Link](http://github.com/TencentBlueKing/bk-ci/issues/12960)

#### Bug Fixes

##### Pipeline
- [Fixed] bug: Incorrect latest template version when exporting templates [Link](http://github.com/TencentBlueKing/bk-ci/issues/13049)
- [Fixed] bug: Remove constant parameters when validating required fields during template instantiation [Link](http://github.com/TencentBlueKing/bk-ci/issues/13039)
- [Fixed] bug: Trigger event descriptions are assembled on the frontend [Link](http://github.com/TencentBlueKing/bk-ci/issues/12969)

##### Credential Management
- [Fixed] bug: Configure secrets using placeholders [Link](http://github.com/TencentBlueKing/bk-ci/issues/12971)

##### Others
- [Fixed] bug: Fix 401 error when publishing PAC without permission [Link](http://github.com/TencentBlueKing/bk-ci/issues/13020)
- [Fixed] bug: Fix agent log singleton output task ID confusion and frontend after API issues [Link](http://github.com/TencentBlueKing/bk-ci/issues/12970)
- [Fixed] bug: AI chat AG-UI stream occasionally missing RUN_FINISHED, causing frontend sessions to remain in running state [Link](http://github.com/TencentBlueKing/bk-ci/issues/12994)
- [Fixed] bug: URLs were not converted to hyperlinks when pipeline build notifications with group messages were converted to Markdown [Link](http://github.com/TencentBlueKing/bk-ci/issues/13044)
- [Fixed] bugfix: Fix some issues in the new third-party machine mode [Link](http://github.com/TencentBlueKing/bk-ci/issues/12945)
- [Fixed] fix: Upgrade frontend dependency versions [Link](http://github.com/TencentBlueKing/bk-ci/issues/12965)
- [Fixed] bug: Optimize the HTTP utility for callback calls [Link](http://github.com/TencentBlueKing/bk-ci/issues/12961)

# v4.2.0-rc.2
## 2026-05-15
### Changelog since v4.2.0-rc.1
#### New Features

##### Pipeline
- [New] feat: Support retrieving pipeline failure details [Link](http://github.com/TencentBlueKing/bk-ci/issues/12873)
- [New] feat: Fix encryption issue when sensitive fields are not set [Link](http://github.com/TencentBlueKing/bk-ci/issues/12871)
- [New] feat: Automatically install plugins for constraint-mode templates triggered by source template upgrades [Link](http://github.com/TencentBlueKing/bk-ci/issues/12896)
- [New] feature: Add default images for pipeline jobs, issue #1108 [Link](http://github.com/TencentBlueKing/bk-ci/issues/1265)

##### Store
- [New] feat: Support sending Store plugin comment and review notifications to groups [Link](http://github.com/TencentBlueKing/bk-ci/issues/12655)
- [New] Store: Support displaying version logs [Link](http://github.com/TencentBlueKing/bk-ci/issues/1761)

##### Permission Center
- [New] feat: Add labels and prompts for groups that cannot be proactively joined when applying for permissions [Link](http://github.com/TencentBlueKing/bk-ci/issues/12436)

##### Project Management
- [New] feat: Add an Ops API for setting the system default cluster [Link](http://github.com/TencentBlueKing/bk-ci/issues/12804)
- [New] feat: Optimize project ownership information entry [Link](http://github.com/TencentBlueKing/bk-ci/issues/12527)

##### Agent
- [New] feat: Agent reports concurrency metric data [Link](http://github.com/TencentBlueKing/bk-ci/issues/12526)
- [New] feat: Support `--network` and `--user` for third-party build machines using the Docker runtime [Link](http://github.com/TencentBlueKing/bk-ci/issues/12832)
- [New] feat: Support nohead for macOS agents [Link](http://github.com/TencentBlueKing/bk-ci/issues/12809)
- [New] feat: Support MCP for agents [Link](http://github.com/TencentBlueKing/bk-ci/issues/12653)

##### Others
- [New] feat: Remove the telegraf dependency from third-party build machines [Link](http://github.com/TencentBlueKing/bk-ci/issues/12895)
- [New] feat: Harden login debugging authentication [Link](http://github.com/TencentBlueKing/bk-ci/issues/12924)
- [New] feat: Support using user sessions under Windows services [Link](http://github.com/TencentBlueKing/bk-ci/issues/12765)
- [New] feat: Support BK-CI intelligent assistant [Link](http://github.com/TencentBlueKing/bk-ci/issues/12737)
- [New] BlueKing security governance: SAST scan fixes [Link](http://github.com/TencentBlueKing/bk-ci/issues/12884)
- [New] feat: Pipeline real-time monitoring overview page [Link](http://github.com/TencentBlueKing/bk-ci/issues/12497)
- [New] feat: Replace the Docker SDK with Docker CLI for agents to avoid dependency version issues caused by daemon upgrades [Link](http://github.com/TencentBlueKing/bk-ci/issues/12791)
- [New] Fix compilation errors, issue #171 [Link](http://github.com/TencentBlueKing/bk-ci/issues/176)

#### Improvements

##### Pipeline
- [Improved] perf: Reduce access frequency for Redis hot keys [Link](http://github.com/TencentBlueKing/bk-ci/issues/12488)

##### Store
- [Improved] perf: Redownload and overwrite damaged plugin cache files on build machines from the repository [Link](http://github.com/TencentBlueKing/bk-ci/issues/12447)

##### Permission Center
- [Improved] perf: Optimize the agent permission renewal application API [Link](http://github.com/TencentBlueKing/bk-ci/issues/12929)
- [Improved] perf: Stop sending permission renewal reminders for disabled projects [Link](http://github.com/TencentBlueKing/bk-ci/issues/12475)

##### Others
- [Improved] perf: Optimize pipeline build data cleanup [Link](http://github.com/TencentBlueKing/bk-ci/issues/12797)

#### Bug Fixes

##### Pipeline
- [Fixed] bug: PAC scheduled trigger creation reported that the PAC listener scheduled trigger configuration was invalid because PAC was not enabled for the current pipeline [Link](http://github.com/TencentBlueKing/bk-ci/issues/12946)
- [Fixed] bug: Publishing PAC pipelines should submit repository changes as the publisher [Link](http://github.com/TencentBlueKing/bk-ci/issues/12846)
- [Fixed] Parameters were not saved after modifying them and switching instances during batch template instance upgrades [Link](http://github.com/TencentBlueKing/bk-ci/issues/12939)
- [Fixed] bug: Regular pipelines could be changed to constraint-mode pipelines [Link](http://github.com/TencentBlueKing/bk-ci/issues/12933)
- [Fixed] bug: Templates copied from instantiated pipelines could not be edited after being copied as template instances [Link](http://github.com/TencentBlueKing/bk-ci/issues/12899)
- [Fixed] bug: Automatic retry did not take effect after the manual review plugin timed out [Link](http://github.com/TencentBlueKing/bk-ci/issues/11661)
- [Fixed] bug: Renaming a PAC YAML file should not delete the old pipeline and create a new one [Link](http://github.com/TencentBlueKing/bk-ci/issues/12658)
- [Fixed] bug: Webhook registration failed in SELF mode for PAC pipelines [Link](http://github.com/TencentBlueKing/bk-ci/issues/12874)
- [Fixed] bug: Editing instantiated pipelines incorrectly prompted required-value errors for constants or other variables [Link](http://github.com/TencentBlueKing/bk-ci/issues/12921)
- [Fixed] bug: Permission check details for child pipeline plugins were not surfaced during template instantiation [Link](http://github.com/TencentBlueKing/bk-ci/issues/12891)

##### Repository
- [Fixed] bug: Change the ExternalCodeccRepoResource API to service mode [Link](http://github.com/TencentBlueKing/bk-ci/issues/12892)

##### Others
- [Fixed] bugfix: Fix some issues in the new third-party build machine mode [Link](http://github.com/TencentBlueKing/bk-ci/issues/12945)

# v4.2.0-rc.1
## 2026-04-21
### Changelog since v4.1.0
#### New Features

##### Pipeline
- [New] feat: Tag event triggers support action filtering [Link](http://github.com/TencentBlueKing/bk-ci/issues/12670)
- [New] feat: The review comment in the manual review plugin can now be configured as required [Link](http://github.com/TencentBlueKing/bk-ci/issues/12810)

##### Store
- [New] feat: Support signatures for Go plugin installation packages [Link](http://github.com/TencentBlueKing/bk-ci/issues/12694)

##### Project Management
- [New] feat: Support hiding project properties [Link](http://github.com/TencentBlueKing/bk-ci/issues/12796)
- [New] feat: Support enabling/disabling shared artifact capability at the project level [Link](http://github.com/TencentBlueKing/bk-ci/issues/12592)

##### Others
- [New] feat: RedisLock supports a dedicated Redis instance [Link](http://github.com/TencentBlueKing/bk-ci/issues/12508)

#### Improvements

##### Pipeline
- [Improved] perf: Adjust the built-in pipeline parameter list [Link](http://github.com/TencentBlueKing/bk-ci/issues/12859)

##### Store
- [Improved] perf: Optimize Store template installation [Link](http://github.com/TencentBlueKing/bk-ci/issues/12837)
- [Improved] perf: Optimize Store template installation [Link](http://github.com/TencentBlueKing/bk-ci/issues/12771)

##### Permission Center
- [Improved] perf: Optimize the display of member and organization statistics in user groups, and improve long user group name display [Link](http://github.com/TencentBlueKing/bk-ci/issues/12798)

##### Others
- [Improved] bug: OpenAPI lightweight pipeline build history retrieval did not sort query results [Link](http://github.com/TencentBlueKing/bk-ci/issues/12830)

#### Bug Fixes

##### Pipeline
- [Fixed] bug: Instance update could cause labels to be lost [Link](http://github.com/TencentBlueKing/bk-ci/issues/12863)
- [Fixed] bug: Deleting pipeline labels did not update dynamic pipeline groups [Link](http://github.com/TencentBlueKing/bk-ci/issues/12831)
- [Fixed] bug: Some partitioned table SQL queries lacked partition key conditions [Link](http://github.com/TencentBlueKing/bk-ci/issues/12841)
- [Fixed] bug: After deleting pipeline notifications, they could still be viewed in code mode [Link](http://github.com/TencentBlueKing/bk-ci/issues/12788)
- [Fixed] bug: The `manualCommand` field was lost for the run plugin in code mode [Link](http://github.com/TencentBlueKing/bk-ci/issues/12789)
- [Fixed] bug: Matrix-split review based on the number of reviewers caused errors when clicking approve in the manual review plugin [Link](http://github.com/TencentBlueKing/bk-ci/issues/12783)
- [Fixed] bug: When a pipeline job timed out, the plugin that failed at that time was not written to the `BK_CI_BUILD_FAIL_TASKS` variable [Link](http://github.com/TencentBlueKing/bk-ci/issues/12741)
- [Fixed] bug: After retrying the manual review plugin, variable values referenced in notification content still used the previous execution's values [Link](http://github.com/TencentBlueKing/bk-ci/issues/12723)

##### Repository
- [Fixed] bug: Compatible with 400 and 404 error codes when retrieving repository directory lists [Link](http://github.com/TencentBlueKing/bk-ci/issues/12853)

##### Dispatch
- [Fixed] bugfix: Remove the specified workspace for third-party build machine containers [Link](http://github.com/TencentBlueKing/bk-ci/issues/12814)

##### Others
- [Fixed] bug: Super administrator permission verification bug [Link](http://github.com/TencentBlueKing/bk-ci/issues/12849)
- [Fixed] bugfix: The pipeline view API could not return favorited pipelines and pipelines created by the current user [Link](http://github.com/TencentBlueKing/bk-ci/issues/12817)
- [Fixed] bug: APK packages uploaded in chunks could not be downloaded from Experience [Link](http://github.com/TencentBlueKing/bk-ci/issues/12821)
