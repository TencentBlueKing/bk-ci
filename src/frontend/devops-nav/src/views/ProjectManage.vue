<template>
    <div
        v-bkloading="{ isLoading: isDataLoading }"
        style="width: 100%"
    >
        <div class="biz-pm biz-pm-index biz-create-pm">
            <template v-if="projectList.length || isDataLoading">
                <div class="biz-pm-header">
                    <div class="title">
                        {{ $t('projectManage') }}
                    </div>
                    <div class="action">
                        <bk-checkbox
                            v-model="isFilterByOffline"
                            name="isFilterByOffline"
                        >
                            {{ $t('showDisableProject') }}
                        </bk-checkbox>
                        <bk-button
                            theme="primary"
                            icon="icon-plus"
                            @click="hasCreatePermission ? togglePMDialog(true) : applyCreatePermission()"
                        >
                            {{ $t('addProject') }}
                        </bk-button>
                        <bk-input
                            v-model="inputValue"
                            class="search-input-row"
                            name="searchInput"
                            :placeholder="$t('searchTips')"
                            right-icon="icon-search"
                            @keyup="filterProjectList(isFilterByOffline)"
                        />
                    </div>
                </div>
                <bk-table
                    v-if="curProjectList.length"
                    class="biz-table"
                    size="medium"
                    :data="formatPageData"
                    :pagination="pageConf"
                    @page-change="pageChange"
                    @page-limit-change="limitChange"
                >
                    <bk-table-column
                        :label="$t('projectName')"
                        prop="logoAddr"
                        width="300"
                    >
                        <template slot-scope="props">
                            <div class="project-name-cell">
                                <span
                                    v-if="props.row.logoAddr"
                                    class="avatar"
                                    @click="modifyLogo(props.row)"
                                >
                                    <img
                                        class="avatar-addr"
                                        :src="props.row.logoAddr"
                                    >
                                    <span class="bg-avatar">{{ $t('editLabel') }}</span>
                                </span>
                                <span
                                    v-else
                                    class="avatar"
                                    :class="['project-avatar', `match-color-${matchForCode(props.row.projectCode)}`]"
                                    @click="modifyLogo(props.row)"
                                >
                                    {{ props.row.projectName.substr(0, 1) }}
                                    <span class="bg-avatar">{{ $t('editLabel') }}</span>
                                </span>
                                <div class="info">
                                    <p class="title">
                                        <template v-if="props.row.approvalStatus !== 2">
                                            <span class="is-disabled">{{ props.row.projectName }}</span>
                                        </template>
                                        <template v-else>
                                            <span
                                                :class="['title-text', { 'is-disabled': !props.row.enabled }]"
                                            >{{ props.row.projectName }}</span>
                                        </template>
                                    </p>
                                    <time class="time">{{ props.row.created_at }}</time>
                                </div>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('projectDesc')"
                        prop="description"
                    />
                    <bk-table-column
                        :label="$t('projectCreator')"
                        prop="creator"
                    />
                    <bk-table-column
                        :label="$t('projectOperation')"
                        width="200"
                    >
                        <template slot-scope="props">
                            <!-- 状态为待审批 -->
                            <template v-if="props.row.approvalStatus === 1">
                                <a
                                    v-bk-tooltips="{ content: $t('waitforReview'), allowHTML: false }"
                                    href="javascript:void(0)"
                                    class="bk-text-button is-disabled"
                                    :title="$t('accessDeny.noOperateAccess')"
                                >{{ $t('editLabel') }}</a>
                                <a
                                    v-bk-tooltips="{ content: $t('waitforReview'), allowHTML: false }"
                                    href="javascript:void(0)"
                                    class="bk-text-button is-disabled"
                                    :title="$t('accessDeny.noOperateAccess')"
                                >{{ $t('enableLabel') }}</a>
                            </template>
                            <!-- 状态为已驳回 -->
                            <template v-else-if="props.row.approvalStatus === 3">
                                <a
                                    href="javascript:void(0)"
                                    :class="['bk-text-button']"
                                    @click.stop.prevent="togglePMDialog(true, props.row)"
                                >{{ $t('editLabel') }}</a>
                                <a
                                    v-bk-tooltips="{ content: $t('accessDeny.noOperateAccessTip'), allowHTML: false }"
                                    href="javascript:void(0)"
                                    class="bk-text-button is-disabled"
                                    :title="$t('accessDeny.noOperateAccess')"
                                >{{ $t("enableLabel") }}</a>
                                
                            </template>

                            <!-- 否则正常显示 -->
                            <template v-else>
                                <a
                                    href="javascript:void(0)"
                                    :class="['bk-text-button', { 'is-disabled': !props.row.enabled }]"
                                    @click.stop.prevent="togglePMDialog(true, props.row)"
                                >{{ $t('editLabel') }}</a>
                                <a
                                    href="javascript:void(0)"
                                    class="bk-text-button"
                                    @click.stop.prevent="toggleProject(props.row)"
                                >{{ props.row.enabled ? $t('disableLabel') : $t('enableLabel') }}</a>
                                
                            </template>
                        </template>
                    </bk-table-column>
                </bk-table>
                <template v-else>
                    <div
                        v-show="!isDataLoading"
                        class="biz-guide-box"
                    >
                        <p
                            v-if="!isFilterByOffline && disableProjectNum"
                            class="title"
                        >
                            {{ $t('disableProjectTips', { disableProjectNum }) }}
                        </p>
                        <p
                            v-else
                            class="title"
                        >
                            {{ $t("emptyData") }}
                        </p>
                    </div>
                </template>
            </template>
            <empty-tips
                v-else
                :show-lock="true"
                :title="$t('notFindProject')"
                :desc="$t('notFindProjectTips')"
            >
                <bk-button
                    icon-left="icon-plus"
                    theme="primary"
                    @click="togglePMDialog(true)"
                >
                    {{ $t('newProject') }}
                </bk-button>
                <a
                    class="empty-btns-item"
                    href="javascript:;"
                    @click="toApplyPermission"
                >
                    <bk-button theme="success">{{ $t('applyProject') }}</bk-button>
                </a>
            </empty-tips>
        </div>
        <logo-dialog
            :show-dialog="showlogoDialog"
            :to-confirm-logo="toConfirmLogo"
            :to-close-dialog="toCloseDialog"
            :file-change="fileChange"
            :selected-url="selectedUrl"
            :is-uploading="isUploading"
        />
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Watch } from 'vue-property-decorator'
    import { Action, Getter, State } from 'vuex-class'
    import logoDialog from '../components/logoDialog/index.vue'

    @Component({
        components: {
            logoDialog
        }
    })
    export default class ProjectManage extends Vue {
        @State projectList
        @Getter disableProjectList
        @State newProject
        @Action toggleProjectDialog
        @Action ajaxUpdatePM
        @Action getProjects
        @Action toggleProjectEnable
        @Action changeProjectLogo
        @Action hasCreateProjectPermission

        isFilterByOffline: boolean = false
        showlogoDialog: boolean = false
        isUploading: boolean = false
        curProjectData: object
        selectedFile: object
        isDataLoading: boolean = false
        selectedUrl: string | ArrayBuffer = ''
        curSelectProject: string = ''
        inputValue: string = ''
        curProjectList: object[] = []
        curPageData: object[] = []
        pageConf: any = {
            totalPage: 1,
            limit: 15,
            current: 1,
            show: false,
            limitList: [10, 15, 20, 25, 30],
            count: 0
        }

        hasCreatePermission: boolean = true
        matchColorList: string[] = [
            'green',
            'yellow',
            'red',
            'blue'
        ]

        get formatPageData (): object[] {
            return this.curPageData.map(item => ({
                ...item
            }))
        }

        get disableProjectNum (): number {
            console.log(this.disableProjectList.length)
            return this.disableProjectList.length
        }

        @Watch('isFilterByOffline')
        watchFilterOffline (isFilterByOffline: boolean): void {
            this.filterProjectList(isFilterByOffline)
        }

        @Watch('projectList', { deep: true })
        watchProjects (val): void {
            this.initList()
            this.reloadCurPage()
        }

        created () {
            this.fetchAllProjects()
            this.checkCreatePermission()
        }

        async checkCreatePermission () {
          try {
            const hasCreatePermission = await this.hasCreateProjectPermission()
            this.hasCreatePermission = hasCreatePermission
          } catch (e) {
            this.hasCreatePermission = false
          }
        }

        async fetchAllProjects () {
            this.isDataLoading = true
            await this.getProjects()
            this.isDataLoading = false
        }

        initList () {
            this.filterProjectList(this.isFilterByOffline)
        }

        filterProjectList (showOfflined) {
            if (showOfflined) {
                this.curProjectList = this.projectList.filter(project => {
                    return project.projectName.indexOf(this.inputValue) !== -1 && project.approvalStatus !== 3
                })
            } else {
                this.curProjectList = this.projectList.filter(project => {
                    return project.enabled && project.projectName.indexOf(this.inputValue) !== -1 && project.approvalStatus !== 3
                })
            }
            this.initPageConf()
            this.pageConf.current = 1
            this.curPageData = this.getDataByPage(this.pageConf.current)
        }

        initPageConf () {
            const total = this.curProjectList.length
            if (total <= this.pageConf.limit) {
                this.pageConf.show = false
            } else {
                this.pageConf.show = true
            }
            this.pageConf.count = total
            this.pageConf.totalPage = Math.ceil(total / this.pageConf.limit)
        }

        reloadCurPage () {
            this.initPageConf()
            if (this.pageConf.current > this.pageConf.totalPage) {
                this.pageConf.current = this.pageConf.totalPage
            }
            this.curPageData = this.getDataByPage(this.pageConf.current)
        }

        getDataByPage (page) {
            let startIndex = (page - 1) * this.pageConf.limit
            let endIndex = page * this.pageConf.limit
            if (startIndex < 0) {
                startIndex = 0
            }
            if (endIndex > this.curProjectList.length) {
                endIndex = this.curProjectList.length
            }
            const data = this.curProjectList.slice(startIndex, endIndex)
            return data
        }

        pageChange (page) {
            this.pageConf.current = page
            const data = this.getDataByPage(page)
            this.curPageData = JSON.parse(JSON.stringify(data))
        }

        limitChange (limit) {
            this.pageConf.limit = limit
            this.pageChange(1)
        }

        togglePMDialog (show: boolean, project = null): void {
            this.toggleProjectDialog({
                showProjectDialog: show,
                project
            })
        }

        // goProject ({ projectCode, enabled }): void {
        //     if (enabled) {
        //         window.open(`${PERM_URL_PREFIX}perm/my-project?project_code=${projectCode}`, '_blank')
        //     }
        // }

        toApplyPermission () {
            this.applyPermission(this.$permissionActionMap.view, this.$permissionResourceMap.project)
        }

        applyCreatePermission () {
            // this.applyPermission(this.$permissionActionMap.create, this.$permissionResourceMap.project)
            this.$showAskPermissionDialog({
                noPermissionList: [{
                    actionId: this.$permissionActionMap.create,
                    resourceId: this.$permissionResourceMap.project,
                    instanceId: []
                }]
            })
        }

        toggleProject (project: any): void {
            const { enabled, projectCode, projectName = '' } = project
            this.curProjectData = JSON.parse(JSON.stringify(project))

            const message = (enabled ? this.$t('disableProjectConfirm') : this.$t('enableProjectConfirm')) + projectName

            this.$bkInfo({
                title: message,
                confirmFn: async () => {
                    let msg = ''
                    let theme = 'error'
                    try {
                        const params = {
                            projectCode,
                            enabled: !enabled
                        }
                        await this.toggleProjectEnable(params)
                        msg = (enabled ? this.$t('disableLabel') : this.$t('enableLabel')) + projectName + this.$t('projectSuccess')
                        theme = 'success'
                        await this.getProjects(true)
                        return true
                    } catch (error) {
                        if (error.code === 403) {
                          this.applyPermission(this.$permissionActionMap.edit, this.$permissionResourceMap.project, [{
                            id: projectCode,
                            type: this.$permissionResourceTypeMap.PROJECT
                          }])
                        } else {
                            msg = error.message || ((enabled ? this.$t('disableLabel') : this.$t('enableLabel')) + projectName + this.$t('projectFail'))
                        }
                        
                        return true
                    } finally {
                        msg && this.$bkMessage({
                            theme,
                            message: msg
                        })
                    }
                }
            })
        }

        matchForCode (projectCode) {
            const event = projectCode.substr(0, 1)
            const key = event.charCodeAt() % 4
            return this.matchColorList[key]
        }

        modifyLogo (project) {
            if (project.logoAddr) {
                this.selectedUrl = project.logoAddr
            } else {
                this.selectedUrl = ''
            }
            this.showlogoDialog = true
            this.isUploading = false
            this.curSelectProject = project.projectCode
        }

        async toConfirmLogo () {
            if (this.selectedUrl && this.selectedFile) {
                this.isUploading = true

                const formData = new FormData()
                formData.append('logo', this.selectedFile[0])

                try {
                    const res = await this.changeProjectLogo({
                        projectCode: this.curSelectProject,
                        formData
                    })

                    if (res) {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('changeLogoSuccessTips')
                        })

                        this.showlogoDialog = false
                        this.projectList.forEach(item => {
                            if (item.projectCode === this.curSelectProject) {
                                item.logoAddr = res.logoAddr
                            }
                        })
                    }
                } catch (e) {
                    this.$bkMessage({
                        message: e.message,
                        theme: 'error'
                    })

                    this.isUploading = false
                } finally {
                    this.selectedFile = undefined
                }
            } else if (!this.selectedUrl) {
                this.$bkMessage({
                    message: this.$t('noLogoTips'),
                    theme: 'error'
                })
            } else {
                this.showlogoDialog = false
            }
            this.resetUploadInput()
        }

        toCloseDialog () {
            this.showlogoDialog = false
            this.selectedFile = undefined
            this.resetUploadInput()
        }

        fileChange (e): void {
            const file = e.target.files[0]
            if (file) {
                if (!(file.type === 'image/jpeg' || file.type === 'image/png')) {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('supportExtTips')
                    })
                } else if (file.size > (2 * 1024 * 1024)) {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('logoSizelimit')
                    })
                } else {
                    const reader = new FileReader()
                    reader.readAsDataURL(file)
                    reader.onload = evts => {
                        this.selectedUrl = reader.result
                    }
                    this.selectedFile = e.target.files
                }
            }
        }

        /**
         * 清空input file的值
         */
        resetUploadInput () {
            this.$nextTick(() => {
                const inputElement: any = document.getElementById('inputfile')
                inputElement.value = ''
            })
        }
    }
</script>

<style lang="scss" scoped>
    @import '../assets/scss/mixins/ellipsis';
    .biz-pm-index {
        width: 1180px;
        margin: 0 auto 0 auto;
    }
    .biz-order {
        padding: 0;
        min-width: 20px;
        text-align: center;
    }
    .biz-pm-page {
        text-align: center;
        margin-top: 30px;
    }
    .biz-pm-index {
        padding-bottom: 75px;
    }
    .biz-pm-header {
        margin: 30px 0 25px 0;
        height: 36px;
        line-height: 36px;
        .title {
            float: left;
            font-size: 18px;
            color: #333948;
            a {
                color: #333948;
            }
        }
        .action {
            float: right;
        }
        .search-input-row {
            float: right;
            margin-left: 45px;
            width: 220px;
        }
    }
    .biz-table {
        font-weight: normal;
        td:first-child {
            display: flex;
            align-items: center;
        }
        .title {
            color: #7b7d8a;
            font-weight: bold;
            white-space: nowrap;
            padding: 0;
            margin: 0 0 5px 0;
            a {
                color: #333948;
                &:hover {
                    color: #3c96ff;
                }
            }
        }
        .action {
            text-align: center;
        }
        .time {
            color: #a3a4ac;
        }
        .disabled {
            color: #c3cdd7;
            .title,
            .time,
            .desc {
                color: #c3cdd7 !important;
            }
        }
        .project-name-cell {
            display: flex;
            .info {
                flex: 1;
                overflow: hidden;
                > .title {
                    @include ellipsis();
                    display: block;
                }
                .title-text {
                    color: #333948;
                }
                .is-disabled {
                    color: #e6e6e6 !important;
                    cursor: not-allowed;
                }
            }

            .avatar,
            .bg-avatar {
                display: inline-block;
                position: relative;
                margin-right: 10px;
                width: 32px;
                height: 32px;
                line-height: 30px;
                border-radius: 16px;
                text-align: center;
                color: #fff;
                font-size: 16px;
                cursor: pointer;
                &:hover {
                    .bg-avatar {
                        display: block;
                    }
                }
            }
            .avatar-addr {
                width: 100%;
                height: 100%;
                border-radius: 16px;
                object-fit: cover;
            }
            .bg-avatar {
                position: absolute;
                top: 0;
                left: 0;
                background: rgba(0, 0, 0, 0.4);
                font-size: 12px;
                display: none;
            }
            .match-color-green {
                background-color: #30D878;
            }
            .match-color-yellow {
                background-color: #FFB400;
            }
            .match-color-red {
                background-color: #FF5656;
            }
            .match-color-blue {
                background-color: #3C96FF;
            }
        }
        
    }
    .biz-pm-form {
        margin: 0 auto 15px auto;
    }
    .bk-form-checkbox {
        margin-right: 35px;
    }
    .desc {
        word-break: break-all;
    }
    .biz-text-bum {
        position: absolute;
        bottom: 8px;
        right: 10px;
        font-size: 12px;
    }

    .create-project-dialog {
        button.disabled {
            background-color: #fafafa;
            border-color: #e6e6e6;
            color: #cccccc;
            cursor: not-allowed;
            &:hover {
                background-color: #fafafa;
                border-color: #e6e6e6;
            }
        }
    }

    .biz-guide-box {
        background-color: #fff;
        padding: 75px 30px;
        border-radius: 4px;
        box-shadow: 0 0 3px rgba(0, 0, 0, .1);
        text-align: center;
        margin-top: 30px;
        .title {
            font-size: 22px;
            color: #333;
        }
    }
</style>

<style lang="scss">
    @import '../assets/scss/conf.scss';
    @import '../assets/scss/mixins/scroller.scss';

    @media screen and (max-width: $mediaWidth) {
        .biz-create-pm .bk-dialog-body {
            max-height: 440px;
            overflow: auto;
            @include scroller(#9e9e9e);
        }
    }
</style>
