<template>
    <bk-dialog
        v-model="showDialog"
        class="devops-project-dialog"
        :show-footer="true"
        :width="width"
        :quick-close="false"
        :close-icon="false"
        @after-leave="resetNewProject"
    >
        <h3
            slot="header"
            class="project-dialog-header"
        >
            {{ title }}
        </h3>
        <bk-form
            class="biz-pm-form"
            :model="newProject"
        >
            <devops-form-item
                :label="$t('projectName')"
                :required="true"
                :is-error="errors.has('project_name')"
            >
                <bk-input
                    v-model="newProject.project_name"
                    v-validate="{ required: true, min: 1, max: 12, projectNameUnique: [newProject.project_id] }"
                    maxlength="12"
                    name="project_name"
                    :placeholder="$t('projectNamePlaceholder')"
                />
                <div
                    v-if="errors.has('project_name')"
                    slot="error-tips"
                    class="project-dialog-error-tips"
                >
                    {{ errors.first('project_name') }}
                    <span v-if="isErrorsRule(errors, 'projectNameUnique')">
                        {{ $t('questionTips') }}
                        <a
                            class="text-link"
                            href="wxwork://message/?username=DevOps"
                        >
                            {{ $t('quickStart.blueShieldAssistant') }}
                        </a>
                    </span>
                </div>
            </devops-form-item>
            <devops-form-item
                :label="$t('englishName')"
                :required="true"
                :rules="[]"
                property="english_name"
                :is-error="errors.has('english_name')"
                :error-msg="errors.first('english_name')"
            >
                <bk-input
                    v-model="newProject.english_name"
                    v-validate="{ required: true, min: 2, max: 32, projectEnglishNameReg: true, projectEnglishNameUnique: isNew }"
                    :placeholder="$t('projectEnglishNamePlaceholder')"
                    name="english_name"
                    maxlength="32"
                    :disabled="!isNew"
                />
            </devops-form-item>
            <devops-form-item
                :label="$t('projectDesc')"
                :required="true"
                property="description"
                :is-error="errors.has('description')"
            >
                <bk-input
                    v-model="newProject.description"
                    v-validate="{ required: true }"
                    type="textarea"
                    maxlength="100"
                    :placeholder="$t('projectDescPlaceholder')"
                    name="description"
                />
            </devops-form-item>
            <bk-form-item
                :label="$t('centerInfo')"
                :required="true"
            >
                <div class="bk-dropdown-box">
                    <bk-select
                        v-model="newProject.bg_id"
                        :placeholder="$t('BGLabel')"
                        name="bg"
                        :loading="deptLoading.bg"
                        searchable
                        @selected="setBgName"
                    >
                        <bk-option
                            v-for="bg in curDepartmentInfo.bg"
                            :id="bg.id"
                            :key="bg.id"
                            :name="bg.name"
                        />
                    </bk-select>
                </div>
                <div class="bk-dropdown-box">
                    <bk-select
                        v-model="newProject.dept_id"
                        :placeholder="$t('departmentLabel')"
                        name="dept"
                        :loading="deptLoading.dept"
                        searchable
                        @selected="setDeptName"
                    >
                        <bk-option
                            v-for="bg in curDepartmentInfo.dept"
                            :id="bg.id"
                            :key="bg.id"
                            :name="bg.name"
                        />
                    </bk-select>
                </div>
                <div class="bk-dropdown-box">
                    <bk-select
                        v-model="newProject.center_id"
                        :placeholder="$t('centerLabel')"
                        name="center"
                        :loading="deptLoading.center"
                        searchable
                        @selected="setCenterName"
                    >
                        <bk-option
                            v-for="center in curDepartmentInfo.center"
                            :id="center.id"
                            :key="center.id"
                            :name="center.name"
                        />
                    </bk-select>
                </div>
            </bk-form-item>
            <bk-form-item
                :label="$t('projectTypeLabel')"
                :required="true"
                property="project_type"
            >
                <bk-select
                    v-model="newProject.project_type"
                    :placeholder="$t('selectProjectTypePlaceholder')"
                    name="center"
                    searchable
                >
                    <bk-option
                        v-for="type in projectTypeList"
                        :id="type.id"
                        :key="type.id"
                        :name="type.name"
                    />
                </bk-select>
            </bk-form-item>
        </bk-form>
                        
        <template slot="footer">
            <div class="bk-dialog-outer">
                <bk-button
                    theme="primary"
                    class="bk-dialog-btn bk-dialog-btn-confirm"
                    :disabled="isCreating"
                    :loading="isCreating"
                    @click="saveProject"
                >
                    {{ $t("okLabel") }}
                </bk-button>
                <bk-button
                    class="bk-dialog-btn bk-dialog-btn-cancel"
                    :disabled="isCreating"
                    @click="cancelProject"
                >
                    {{ $t("cancelLabel") }}
                </bk-button>
            </div>
        </template>
    </bk-dialog>
</template>

<script lang='ts'>
    import Vue from 'vue'
    import { Component, Prop, Watch } from 'vue-property-decorator'
    import { State, Action, Getter } from 'vuex-class'
    import eventBus from '../../utils/eventBus'

    @Component
    export default class ProjectDialog extends Vue {
        @Prop({ default: false })
        initShowDialog: boolean

        @Prop({ default: 860 })
        width: number | string

        descriptionLength: number = 100
        validate: object = {}
        curDepartmentInfo: any = {
            'bg': [],
            'dept': [],
            'center': []
        }
        isNew: boolean = true
        isCreating: boolean = false
        deptLoading: any = {
            bg: false,
            dept: false,
            center: false
        }

        @State newProject
        @State showProjectDialog
        @Getter isEmptyProject
        @Action updateNewProject
        @Action checkProjectField
        @Action toggleProjectDialog
        @Action getDepartmentInfo
        @Action ajaxUpdatePM
        @Action ajaxAddPM
        @Action getProjects
        @Action resetNewProject
        @Action getMyDepartmentInfo

        handleProjectChange (e): void {
            const { name, value, type, checked } = e.target
            const isCheckbox = type === 'checkbox'

            this.updateNewProject({
                [name]: isCheckbox ? checked : value
            })
        }

        get projectTypeList (): object {
            return [
                {
                    id: 1,
                    name: this.$t('mobileGame')
                },
                {
                    id: 2,
                    name: this.$t('pcGame')
                },
                {
                    id: 3,
                    name: this.$t('webGame')
                },
                {
                    id: 4,
                    name: this.$t('platformProduct')
                },
                {
                    id: 5,
                    name: this.$t('supportProduct')
                }
            ]
        }

        get showDialog (): boolean {
            return this.showProjectDialog
        }

        set showDialog (showProjectDialog: boolean) {
            this.toggleProjectDialog({
                showProjectDialog
            })
        }

        get title () {
            return this.isEmptyProject(this.newProject) ? this.$t('newProject') : this.$t('editProject')
        }

        @Watch('showDialog')
        async watchDialog (show: boolean) {
            if (show) {
                this.$validator.reset()
                if (this.newProject.project_name) {
                    this.isNew = false
                } else {
                    this.isNew = true
                }
                this.getDepartment('bg', 0)
                
                if (this.isEmptyProject(this.newProject)) {
                    this.deptLoading['bg'] = true
                    this.deptLoading['dept'] = true
                    this.deptLoading['center'] = true
                    const res = await this.getMyDepartmentInfo()
                    if (res) {
                        this.newProject.bg_id = res.bg_id
                        this.newProject.bg_name = res.bg_name
                        this.newProject.dept_id = res.dept_id
                        this.newProject.dept_name = res.dept_name
                        this.newProject.center_id = res.center_id
                        this.newProject.center_name = res.center_name
                    }
                }
            }
        }

        @Watch('newProject.bg_id')
        watchBg (bgId: number): void {
            this.curDepartmentInfo['dept'] = []
            this.curDepartmentInfo['center'] = []
            bgId && this.getDepartment('dept', this.newProject.bg_id)
        }

        @Watch('newProject.dept_id')
        watchDept (deptId: number): void {
            this.curDepartmentInfo['center'] = []
            deptId && this.getDepartment('center', this.newProject.dept_id)
        }

        isErrorsRule (errors, rule): boolean {
            try {
                return errors.items.find(item => item.rule === rule)
            } catch (e) {
                console.error(e)
                return false
            }
        }

        async getDepartment (type: string, id: number) {
            this.deptLoading[type] = true
            try {
                const res = await this.getDepartmentInfo({
                    type,
                    id
                })
                this.curDepartmentInfo[type] = res
                this.curDepartmentInfo[type].splice(0, this.curDepartmentInfo[type].length, ...res)
            } catch (e) {
                this.curDepartmentInfo[type] = []
            }
            this.deptLoading[type] = false
        }

        setBgName (id) {
            const data = this.curDepartmentInfo.bg.find(bg => bg.id === id)
            if (data) {
                this.newProject.bg_name = data.name
            }
        }

        setDeptName (id) {
            const data = this.curDepartmentInfo.dept.find(dept => dept.id === id)
            if (data) {
                this.newProject.dept_name = data.name
            }
        }

        setCenterName (id) {
            const data = this.curDepartmentInfo.center.find(center => center.id === id)
            if (data) {
                this.newProject.center_name = data.name
            }
        }

        closeDialog () {
            this.showDialog = false
        }

        async addProject () {
            const data = this.newProject
            try {
                const res = await this.ajaxAddPM(data)

                if (typeof res === 'boolean' && res) {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('addProjectSuccuess')
                    })
                    this.closeDialog()
                    await this.getProjects()
                    eventBus.$emit('addNewProject', data)
                } else {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('exception.apiError')
                    })
                }
                setTimeout(() => {
                    this.isCreating = false
                }, 100)
            } catch (err) {
                this.$bkMessage({
                    theme: 'error',
                    message: err.message || this.$t('exception.apiError')
                })
                setTimeout(() => {
                    this.isCreating = false
                }, 100)
            }
        }

        async updateProject (data) {
            try {
                const res = await this.ajaxUpdatePM(data)
                if (res) {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('updateProjectSuccuess')
                    })
                    this.closeDialog()
                    await this.getProjects()
                } else {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('exception.apiError')
                    })
                }
                setTimeout(() => {
                    this.isCreating = false
                }, 100)
            } catch (err) {
                const message = err.message || this.$t('exception.apiError')
                this.$bkMessage({
                    theme: 'error',
                    message
                })
                setTimeout(() => {
                    this.isCreating = false
                }, 100)
            }
        }

        async saveProject () {
            const data = this.newProject
            // @ts-ignore
            const valid = await this.$validator.validate()
            if (!valid) {
                return valid
            }
            if (data.bg_id === '') {
                this.$bkMessage({
                    theme: 'error',
                    message: this.$t('noBGErrorTips')
                })
                return false
            }
            if (data.dept_id === '') {
                this.$bkMessage({
                    theme: 'error',
                    message: this.$t('noDeptErrorTips')
                })
                return false
            }
            if (data.center_id === '') {
                this.$bkMessage({
                    theme: 'error',
                    message: this.$t('noCenterErrorTips')
                })
                return false
            }
            if (data.project_type === '') {
                this.$bkMessage({
                    theme: 'error',
                    message: this.$t('selectProjectTypePlaceholder')
                })
                return false
            }
            this.isCreating = true
            if (this.isNew) {
                this.addProject()
            } else {
                const id = this.newProject.project_id
                const params = {
                    id: id,
                    data: this.newProject
                }
                this.updateProject(params)
            }
            return true
        }

        cancelProject () {
            this.isCreating = false
            this.showDialog = false
        }
    }
</script>

<style lang="scss" scoped>
    @import '../../assets/scss/conf';
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
    }
    .biz-table {
        font-weight: normal;
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
    }
    .biz-pm-form {
        margin: 0 50px 15px auto;
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

    .devops-project-dialog {
        overflow: auto;
        button.disabled {
            background-color: #fafafa;
            border-color: #e6e6e6;
            color: #ccc;
            cursor: not-allowed;
            &:hover {
                background-color: #fafafa;
                border-color: #e6e6e6;
            }
        }
        .bk-form .bk-label {
            font-weight: 700;
            color: #737987;
            padding-right: 20px;
        }
        .project-dialog-error-tips {
            color: $dangerColor;
            font-size: 12px;
            .text-link {
                color: $primaryColor;
                padding: 0 5px;
            }
            &:before {
                display: none;
            }
        }
        .bk-dropdown-box {
            width: 200px;
            display: inline-block;
            vertical-align: middle;
        }
        .bk-form-input[disabled],
        .bk-form-select[disabled] {
            color: inherit;
        }
    }
</style>
