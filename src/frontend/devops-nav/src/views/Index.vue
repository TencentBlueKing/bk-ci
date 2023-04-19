<template>
    <div
        v-bkloading="loadingOption"
        class="devops-index"
    >
        <div class="user-prompt" v-if="showAnnounce">
            <!-- <p><i class="bk-icon icon-info-circle-shape"></i>{{currentNotice.noticeContent}}</p> -->
            <p v-html="currentNotice.noticeContent"></p>
        </div>
        <template v-if="projectList">
            <Header />
            <main>
                <template v-if="hasProjectList">
                    <empty-tips
                        v-if="!hasProject"
                        :title="$t('accessDeny.title')"
                        :desc="$t('accessDeny.desc')"
                    >
                        <bk-button
                            v-if="curProjectCode"
                            theme="primary"
                            @click="handleApplyJoin"
                        >
                            {{ $t('accessDeny.applyJoin') }}
                        </bk-button>
                        <bk-button
                            @click="switchProject"
                        >
                            {{ $t('accessDeny.switchProject') }}
                        </bk-button>
                    </empty-tips>
                    <empty-tips
                        v-else-if="isDisableProject"
                        :title="$t('accessDeny.projectBan')"
                        :desc="$t('accessDeny.projectBanDesc')"
                    >
                        <bk-button
                            theme="primary"
                            @click="switchProject"
                        >
                            {{ $t('accessDeny.switchProject') }}
                        </bk-button>
                        <a
                            target="_blank"
                            class="empty-btns-item"
                            href="/console/pm"
                        ><bk-button theme="success">{{ $t("projectManage") }}</bk-button></a>
                    </empty-tips>

                    <!--<empty-tips v-else-if='isApprovalingProject' title='无法访问该项目' desc='你正在访问的项目正在处于审核中，禁止访问'>
                        <bk-button type='primary' @click='switchProject'>切换项目</bk-button>
                    </empty-tips>-->
                </template>
                <template v-if="projectApprovalStatus === 1">
                    <section class="devops-empty-tips">
                        <bk-exception
                            class="exception-wrap-item exception-part"
                            :type="403"
                            scene="part"
                        >
                            <span class="bk-exception-title">{{ $t('projectCreatingTitle') }}</span>
                            <div class="bk-exception-description">{{ $t('projectCreatingDesc', [curProject.projectName]) }}</div>
                        </bk-exception>
                    </section>
                </template>
                <router-view v-else-if="!hasProjectList || isOnlineProject || isApprovalingProject" />
            </main>
        </template>

        <apply-project-dialog ref="applyProjectDialog" :project-code="curProjectCode" />
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import Header from '../components/Header/index.vue'
    import ApplyProjectDialog from '../components/ApplyProjectDialog/index.vue'
    import { Component, Watch } from 'vue-property-decorator'
    import { State, Getter } from 'vuex-class'
    import eventBus from '../utils/eventBus'

    @Component({
        components: {
            Header,
            ApplyProjectDialog
        }
    })
    export default class Index extends Vue {
        @State currentNotice
        @State projectList
        @State headerConfig
        @Getter showAnnounce
        @Getter enableProjectList
        @Getter disableProjectList
        @Getter approvalingProjectList

        get loadingOption (): object {
            return {
                isLoading: this.projectList === null
            }
        }

        get hasProject (): boolean {
            return this.projectList.some(project => project.projectCode === this.curProjectCode)
        }

        get curProject (): any {
            return this.projectList.find(project => project.projectCode === this.curProjectCode)
        }

        get projectApprovalStatus () : number {
            if (this.curProject) {
                return this.curProject.approvalStatus
            }
            return -1
        }

        get isDisableProject (): boolean {
            const project = this.disableProjectList.find(project => project.projectCode === this.curProjectCode)
            return project ? !project.enabled : false
        }

        get isApprovalingProject (): boolean {
            return !!this.approvalingProjectList.find(project => project.projectCode === this.curProjectCode)
        }

        get isOnlineProject (): boolean {
            return !!this.enableProjectList.find(project => project.projectCode === this.curProjectCode)
        }

        get hasProjectList (): boolean {
            return this.headerConfig.showProjectList
        }

        get curProjectCode (): string {
            return this.$route.params.projectId
        }

        switchProject () {
            this.iframeUtil.toggleProjectMenu(true)
        }

        @Watch('hasProject', {
            immediate: true
        })
        wacthHasProject (val: boolean) {
            if (!val) {
                this.handleApplyJoin()
            }
        }

        handleApplyJoin () {
            const { restPath } = this.$route.params
            const hasPipelineId = restPath && restPath.startsWith('p-')
            const pipelineId = restPath && restPath.split('/')[0]
            const resourceType = hasPipelineId ? 'pipeline' : 'project'
            const resourceCode = hasPipelineId ? pipelineId : this.curProjectCode
            if (resourceCode) {
                this.handleNoPermission({
                    projectId: this.curProjectCode,
                    resourceType,
                    resourceCode
                })
            }
        }

        created () {
            eventBus.$on('update-project-id', projectId => {
                this.$router.replace({
                    params: {
                        projectId
                    }
                })
            })
        }
    }
</script>

<style lang="scss" scoped>
    @import '../assets/scss/conf';
    .devops-index {
        height: 100%;
        display: flex;
        flex: 1;
        flex-direction: column;
        background-color: $bgHoverColor;
        > main {
            display: flex;
            flex: 1;
            overflow: auto;
        }
        .user-prompt {
            display: flex;
            justify-content: center;
            padding: 0 24px;
            line-height: 32px;
            background-color: #FF9600;
            color: #fff;
            max-height: 32px;
        }
    }
    ::v-deep .bk-exception-img {
        width: 480px !important;
        height: 240px !important;
    }
    .bk-exception-title {
        margin-top: 18px;
        font-size: 24px;
        line-height: 32px;
        color: #313238;
    }
    .bk-exception-description {
        margin-top: 16px;
        font-size: 14px;
        line-height: 22px;
        color: #63656e;
    }
</style>
