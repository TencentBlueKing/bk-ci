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
                :is-error="errors.has('projectName')"
            >
                <bk-input
                    v-model="newProject.projectName"
                    v-validate="{ required: true, min: 1, max: 32, projectNameUnique: [newProject.projectCode] }"
                    maxlength="32"
                    name="projectName"
                    :placeholder="$t('projectNamePlaceholder')"
                />
                <div
                    v-if="errors.has('projectName')"
                    slot="error-tips"
                    class="project-dialog-error-tips"
                >
                    {{ errors.first('projectName') }}
                </div>
            </devops-form-item>
            <devops-form-item
                :label="$t('englishName')"
                :required="true"
                :rules="[]"
                property="englishName"
                :is-error="errors.has('englishName')"
                :error-msg="errors.first('englishName')"
            >
                <bk-input
                    v-model="newProject.englishName"
                    v-validate="{ required: true, min: 2, max: 32, projectEnglishNameReg: isNew, projectEnglishNameUnique: isNew }"
                    :placeholder="$t('projectEnglishNamePlaceholder')"
                    name="englishName"
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
    import {
        handleProjectNoPermission,
        RESOURCE_ACTION
    } from '@/utils/permission'

    @Component
    export default class ProjectDialog extends Vue {
        @Prop({ default: false })
        initShowDialog: boolean

        @Prop({ default: 860 })
        width: number | string

        descriptionLength: number = 100
        defaultProjectInfo: any = {}
        validate: object = {}
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
        @Action toggleProjectDialog
        @Action ajaxUpdatePM
        @Action ajaxAddPM
        @Action getProjects
        @Action resetNewProject

        handleProjectChange (e): void {
            const { name, value, type, checked } = e.target
            const isCheckbox = type === 'checkbox'

            this.updateNewProject({
                [name]: isCheckbox ? checked : value
            })
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
            return this.isNew ? this.$t('newProject') : this.$t('editProject')
        }

        @Watch('showDialog')
        async watchDialog (show: boolean) {
            if (show) {
                this.$validator.reset()
                if (this.newProject.projectName) {
                    this.isNew = false
                } else {
                    this.isNew = true
                }
            }
        }
        
        isErrorsRule (errors, rule): boolean {
            try {
                return errors.items.find(item => item.rule === rule)
            } catch (e) {
                console.error(e)
                return false
            }
        }

        closeDialog () {
            this.showDialog = false
        }

        async addProject (data) {
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
                    throw Error(this.$t('exception.apiError'))
                }
            } catch (err: any) {
                if (err.code === 403) {
                    const {
                        projectCode,
                        projectName,
                        routerTag
                    } = data.data || {}
                    const url = /rbac/.test(routerTag)
                            ? `/console/permission/apply?project_code=${projectCode}&resourceType=project&resourceName=${projectName}&action=project_create&iamResourceCode=${projectCode}&groupId&x-devops-project-id=${projectCode}`
                            : `/console/perm/apply-perm?project_code=${projectCode}&x-devops-project-id=${projectCode}`
                    handleProjectNoPermission(
                        {
                            projectId: projectCode,
                            resourceCode: projectCode,
                            action: RESOURCE_ACTION.EDIT
                        },
                        {
                            actionName: this.$t('createProject'),
                            groupInfoList: [{ url }],
                            resourceName: projectName,
                            resourceTypeName: this.$t('project')
                        }
                    )
                } else {
                    this.$bkMessage({
                        theme: 'error',
                        message: err.message || this.$t('exception.apiError')
                    })
                }
            } finally {
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
                    throw Error(this.$t('exception.apiError'))
                }
            } catch (e: any) {
                if (e.code === 403) {
                    const {
                        projectCode,
                        projectName,
                        routerTag
                    } = data.data || {}
                    const url = /rbac/.test(routerTag)
                            ? `/console/permission/apply?project_code=${projectCode}&resourceType=project&resourceName=${projectName}&action=project_enable&iamResourceCode=${projectCode}&groupId&x-devops-project-id=${projectCode}`
                            : `/console/perm/apply-perm?project_code=${projectCode}&x-devops-project-id=${projectCode}`
                    handleProjectNoPermission(
                        {
                            projectId: projectCode,
                            resourceCode: projectCode,
                            action: RESOURCE_ACTION.EDIT
                        },
                        {
                            actionName: this.$t('editProject'),
                            groupInfoList: [{ url }],
                            resourceName: projectName,
                            resourceTypeName: this.$t('project')
                        }
                    )
                } else {
                    this.$bkMessage({
                        theme: 'error',
                        message: e.message || this.$t('exception.apiError')
                    })
                }
                setTimeout(() => {
                    this.isCreating = false
                }, 100)
            } finally {
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
            this.isCreating = true
            if (this.isNew) {
                this.addProject(data)
            } else {
                const { projectCode } = this.newProject
                const params = {
                    projectCode,
                    data
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
