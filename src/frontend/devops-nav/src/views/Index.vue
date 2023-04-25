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
                        :show-lock="true"
                        :title="$t('accessDeny.title')"
                        :desc="$t('accessDeny.desc')"
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
                            :href="`/console/perm/apply-join-project${$route.params.projectId ? `?project_code=${$route.params.projectId}` : ''}`"
                        >
                            <bk-button theme="success">{{ $t('apply') }}</bk-button>
                        </a>
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
                <router-view v-if="!hasProjectList || isOnlineProject || isApprovalingProject" />
            </main>
        </template>

        <ask-permission-dialog />
        <extension-aside-panel />
        <extension-dialog />
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import Header from '../components/Header/index.vue'
    import AskPermissionDialog from '../components/AskPermissionDialog/AskPermissionDialog.vue'
    import ExtensionAsidePanel from '../components/ExtensionAsidePanel/index.vue'
    import ExtensionDialog from '../components/ExtensionDialog/index.vue'
    import { Component } from 'vue-property-decorator'
    import { State, Getter, Action } from 'vuex-class'
    import eventBus from '../utils/eventBus'

    Component.registerHooks([
        'beforeRouteEnter',
        'beforeRouteLeave',
        'beforeRouteUpdate'
    ])

    @Component({
        components: {
            Header,
            AskPermissionDialog,
            ExtensionAsidePanel,
            ExtensionDialog
        }
    })
    export default class Index extends Vue {
        @State currentNotice
        @State currentPage
        @State projectList
        @State headerConfig
        @Getter showAnnounce
        @Getter enableProjectList
        @Getter disableProjectList
        @Getter approvalingProjectList
        @Action fetchServiceHooks

        get loadingOption (): object {
            return {
                isLoading: this.projectList === null
            }
        }

        get hasProject (): boolean {
            return this.projectList.some(project => project.projectCode === this.$route.params.projectId)
        }

        get isDisableProject (): boolean {
            const project = this.disableProjectList.find(project => project.projectCode === this.$route.params.projectId)
            return project ? !project.enabled : false
        }

        get isApprovalingProject (): boolean {
            return !!this.approvalingProjectList.find(project => project.projectCode === this.$route.params.projectId)
        }

        get isOnlineProject (): boolean {
            return !!this.enableProjectList.find(project => project.projectCode === this.$route.params.projectId)
        }

        get hasProjectList (): boolean {
            return this.headerConfig.showProjectList
        }

        switchProject () {
            this.iframeUtil.toggleProjectMenu(true)
        }

        created () {
            eventBus.$on('update-project-id', projectId => {
                this.$router.replace({
                    params: {
                        projectId
                    }
                })
            })

            eventBus.$on('change-extension-route', options => {
              this.$router.push(options.url)
            })

            if (this.currentPage) {
                this.fetchServiceHooks({
                    serviceId: this.currentPage.id
                })
            }
        }
    }
</script>

<style lang="scss">
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
</style>
