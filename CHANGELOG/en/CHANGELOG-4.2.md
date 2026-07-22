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
