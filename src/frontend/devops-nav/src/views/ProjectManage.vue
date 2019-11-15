<template>
    <div
        v-bkloading="{ isLoading: isDataLoading }"
        style="width: 100%"
    >
        <div class="biz-pm biz-pm-index biz-create-pm">
            <template v-if="projectList.length || isDataLoading">
                <div class="biz-pm-header">
                    <div class="title">
                        项目管理
                    </div>
                    <div class="action">
                        <bk-checkbox
                            v-model="isFilterByOffline"
                            name="isFilterByOffline"
                        >
                            显示已停用项目
                        </bk-checkbox>
                        <bk-button
                            theme="primary"
                            icon-right="icon-plus"
                            @click="togglePMDialog(true)"
                        >
                            新建项目
                        </bk-button>
                        <bk-input
                            v-model="inputValue"
                            class="search-input-row"
                            name="searchInput"
                            placeholder="搜索"
                            right-icon="bk-icon icon-search"
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
                        label="项目名称"
                        prop="logo_addr"
                        width="200"
                    >
                        <template slot-scope="props">
                            <div class="project-name-cell">
                                <span
                                    v-if="props.row.logo_addr"
                                    class="avatar"
                                    @click="modifyLogo(props.row)"
                                >
                                    <img
                                        class="avatar-addr"
                                        :src="props.row.logo_addr"
                                    >
                                    <span class="bg-avatar">编辑</span>
                                </span>
                                <span
                                    v-else
                                    class="avatar"
                                    :class="['project-avatar', `match-color-${matchForCode(props.row.project_code)}`]"
                                    @click="modifyLogo(props.row)"
                                >
                                    {{ props.row.project_name.substr(0, 1) }}
                                    <span class="bg-avatar">编辑</span>
                                </span>
                                <div class="info">
                                    <p class="title">
                                        <template v-if="props.row.approval_status !== 2">
                                            <a
                                                v-bk-tooltips="{ content: '没有操作权限' }"
                                                href="javascript:void(0)"
                                                class="bk-text-button is-disabled"
                                                title="没有操作权限"
                                            >{{ props.row.project_name }}</a>
                                        </template>
                                        <template v-else>
                                            <a
                                                v-if="!props.row.isOfflined"
                                                href="javascript:void(0)"
                                                :class="['bk-text-button', { 'is-disabled': props.row.is_offlined }]"
                                                @click.stop.prevent="goProject(props.row.projectCode)"
                                            >{{ props.row.projectName }}</a>
                                            <a
                                                v-else
                                                href="javascript:void(0)"
                                                :class="['bk-text-button', { 'is-disabled': props.row.is_offlined }]"
                                            >{{ props.row.project_name }}</a>
                                        </template>
                                    </p>
                                    <time class="time">{{ props.row.created_at }}</time>
                                </div>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        label="项目说明"
                        prop="description"
                    />
                    <bk-table-column
                        label="创建者"
                        prop="creator"
                    />
                    <bk-table-column
                        label="操作"
                        width="200"
                    >
                        <template slot-scope="props">
                            <!-- 状态为待审批 -->
                            <template v-if="props.row.approval_status === 1">
                                <a
                                    v-bk-tooltips="{ content: '待审批，没有操作权限' }"
                                    href="javascript:void(0)"
                                    class="bk-text-button is-disabled"
                                    title="没有操作权限"
                                >编辑</a>
                                <a
                                    v-bk-tooltips="{ content: '待审批，没有操作权限' }"
                                    href="javascript:void(0)"
                                    class="bk-text-button is-disabled"
                                    title="没有操作权限"
                                >启用</a>
                                <a
                                    v-bk-tooltips="{ content: '待审批，没有操作权限' }"
                                    class="bk-text-button is-disabled"
                                    title="没有操作权限"
                                >用户管理</a>
                            </template>
                            <!-- 状态为已驳回 -->
                            <template v-else-if="props.row.approval_status === 3">
                                <a
                                    href="javascript:void(0)"
                                    :class="['bk-text-button']"
                                    @click.stop.prevent="togglePMDialog(true, props.row)"
                                >编辑</a>
                                <a
                                    v-bk-tooltips="{ content: '已驳回，没有操作权限' }"
                                    href="javascript:void(0)"
                                    class="bk-text-button is-disabled"
                                    title="没有操作权限"
                                >启用</a>
                                <a
                                    v-bk-tooltips="{ content: '已驳回，没有操作权限' }"
                                    href="javascript:void(0)"
                                    class="bk-text-button is-disabled"
                                    title="没有操作权限"
                                >用户管理</a>
                            </template>

                            <!-- 否则正常显示 -->
                            <template v-else>
                                <a
                                    href="javascript:void(0)"
                                    :class="['bk-text-button', { 'is-disabled': props.row.isOfflined }]"
                                    @click.stop.prevent="togglePMDialog(true, props.row)"
                                >编辑</a>
                                <template v-if="props.row.isOfflined">
                                    <a
                                        href="javascript:void(0)"
                                        class="bk-text-button"
                                        @click.stop.prevent="offlineProject(props.row)"
                                    >启用</a>
                                </template>
                                <template v-else>
                                    <a
                                        v-bk-tooltips="{ content: '此功能暂未开放' }"
                                        href="javascript:void(0)"
                                        style="margin: 0 15px;"
                                        class="bk-text-button is-disabled"
                                    >停用</a>
                                </template>
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
                            v-if="!isFilterByOffline && offlineProjectNum"
                            class="title"
                        >
                            您有{{ offlineProjectNum }}个项目已经停用，请点击右上角“显示已停用项目”或新建项目
                        </p>
                        <p
                            v-else
                            class="title"
                        >
                            暂时没有数据！
                        </p>
                    </div>
                </template>
            </template>
            <empty-tips
                v-else
                title="未找到您参与的项目"
                desc="您可以创建自己的项目，然后针对自己的项目进行相应用户管理"
            >
                <bk-button
                    icon-left="bk-icon icon-plus"
                    theme="primary"
                    @click="togglePMDialog(true)"
                >
                    新建项目
                </bk-button>
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
    import { State, Action } from 'vuex-class'
    import logoDialog from '../components/logoDialog/index.vue'

    @Component({
      components: {
        logoDialog
      }
    })
    export default class ProjectManage extends Vue {
        @State projectList
        @State newProject
        @Action toggleProjectDialog
        @Action ajaxUpdatePM
        @Action getProjects
        @Action changeProjectLogo

        isFilterByOffline: boolean = false
        showlogoDialog: boolean = false
        isUploading: boolean = false
        curProjectData: object
        selectedFile: object
        isDataLoading: boolean = false
        selectedUrl: string | ArrayBuffer = ''
        curSelectProject: string = ''
        inputValue: string = ''
        offlineProjectNum: number
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
        matchColorList: string[] = [
          'green',
          'yellow',
          'red',
          'blue'
        ]

        get formatPageData (): object[] {
          return this.curPageData.map(item => ({
            ...item,
            isOfflined: item['is_offlined'],
            projectCode: item['project_code'],
            projectName: item['project_name']
          }))
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
          this.initList()
          this.initPageConf()
        }

        initList () {
          this.isDataLoading = true
          this.filterProjectList(this.isFilterByOffline)
          this.isDataLoading = false
        }

        filterProjectList (showOfflined) {
          const offlineList = this.projectList.filter((project) => {
            return project['is_offlined'] === true
          })
          this.offlineProjectNum = offlineList.length

          if (showOfflined) {
            this.curProjectList = this.projectList.filter((project) => {
              return project['project_name'].indexOf(this.inputValue) !== -1 && project['approval_status'] !== 3
            })
          } else {
            this.curProjectList = this.projectList.filter((project) => {
              return project['is_offlined'] === false && project['project_name'].indexOf(this.inputValue) !== -1 && project['approval_status'] !== 3
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

        offlineProject (project: any): void {
          const _this = this
          const { is_offlined: isOfflined, project_id: projectId } = project
          this.curProjectData = JSON.parse(JSON.stringify(project))

          const message = isOfflined ? '确定要启用？' : '确定要停用？'

          this.$bkInfo({
            title: message,
            confirmFn () {
              const params = {
                id: projectId,
                data: {
                  'is_offlined': !isOfflined
                }
              }
              _this.updateProject(params)
              return true
            }
          })
        }

        matchForCode (projectCode) {
          const event = projectCode.substr(0, 1)
          const key = event.charCodeAt() % 4
          return this.matchColorList[key]
        }

        modifyLogo (project) {
          if (project.logo_addr) {
            this.selectedUrl = project.logo_addr
          } else {
            this.selectedUrl = ''
          }
          this.showlogoDialog = true
          this.isUploading = false
          this.curSelectProject = project.project_id
        }

        async toConfirmLogo () {
          if (this.selectedUrl && this.selectedFile) {
            this.isUploading = true

            const formData = new FormData()
            formData.append('logo', this.selectedFile[0])

            try {
              const res = await this.changeProjectLogo({
                projectId: this.curSelectProject,
                formData
              })

              if (res) {
                this.$bkMessage({
                  theme: 'success',
                  message: 'LOGO修改成功！'
                })

                this.showlogoDialog = false
                this.projectList.forEach(item => {
                  if (item.project_id === this.curSelectProject) {
                    item.logo_addr = res.logo_addr
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
              message: '请选择要上传的图片',
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
                message: '请上传png、jpg格式的图片'
              })
            } else if (file.size > (2 * 1024 * 1024)) {
              this.$bkMessage({
                theme: 'error',
                message: '请上传大小不超过2M的图片'
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

        async updateProject (project: any) {
          try {
            await this.ajaxUpdatePM(project)

            this.$bkMessage({
              theme: 'success',
              message: '项目修改成功！'
            })
            this.togglePMDialog(false)
            this.getProjects()
          } catch (e) {
            this.$bkMessage({
              message: e.message,
              theme: 'error'
            })
          }
        }
    }
</script>

<style lang="scss" scoped>
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
