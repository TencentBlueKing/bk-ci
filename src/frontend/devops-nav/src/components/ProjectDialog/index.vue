<template>
    <bk-dialog
        v-model="showDialog"
        class="devops-project-dialog"
        :show-footer="true"
        :width="width"
        :quick-close="false"
        :close-icon="false"
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
                label="项目名称"
                :required="true"
                :is-error="errors.has('project_name')"
            >
                <bk-input
                    v-model="newProject.project_name"
                    v-validate="{ required: true, min: 4, max: 12, projectNameUnique: [newProject.project_id] }"
                    maxlength="12"
                    name="project_name"
                    placeholder="请输入4-12字符的项目名称"
                />
                <div
                    v-if="errors.has('project_name')"
                    slot="error-tips"
                    class="project-dialog-error-tips"
                >
                    {{ errors.first('project_name') }}
                    <span v-if="errors.first('project_name') === &quot;项目名称已存在&quot;" />
                </div>
            </devops-form-item>
            <devops-form-item
                label="英文缩写"
                :required="true"
                :rules="[]"
                property="english_name"
                :is-error="errors.has('english_name')"
                :error-msg="errors.first('english_name')"
            >
                <bk-input
                    v-model="newProject.english_name"
                    v-validate="{ required: true, min: 2, projectEnglishNameUnique: [newProject.project_id] }"
                    placeholder="请输入2-32字符的小写字母+数字，以小写字母开头"
                    name="english_name"
                    maxlength="32"
                    :disabled="!isNew"
                />
            </devops-form-item>
            <bk-form-item
                label="项目描述"
                :required="true"
                property="description"
            >
                <bk-input
                    v-model="newProject.description"
                    type="textarea"
                    maxlength="100"
                    placeholder="请输入项目描述"
                    name="description"
                />
            </bk-form-item>
            <bk-form-item
                label="项目类型"
                :required="true"
                property="project_type"
            >
                <bk-select
                    v-model="newProject.project_type"
                    placeholder="选择项目类型"
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
                    确定
                </bk-button>
                <bk-button
                    class="bk-dialog-btn bk-dialog-btn-cancel"
                    :disabled="isCreating"
                    @click="cancelProject"
                >
                    取消
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
        title: string = '新建项目'
        isNew: boolean = true
        isCreating: boolean = false
        deptLoading: any = {
          bg: false,
          dept: false,
          center: false
        }
        projectTypeList: object = [
          {
            id: 1,
            name: '手游'
          },
          {
            id: 2,
            name: '端游'
          },
          {
            id: 3,
            name: '页游'
          },
          {
            id: 4,
            name: '平台产品'
          },
          {
            id: 5,
            name: '支撑产品'
          }
        ]

        @State newProject
        @State showProjectDialog
        @Getter isEmptyProject
        @Action updateNewProject
        @Action checkProjectField
        @Action toggleProjectDialog
        @Action ajaxUpdatePM
        @Action ajaxAddPM
        @Action getProjects

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

        @Watch('showDialog')
        async watchDialog (show: boolean) {
          if (show) {
            this.$validator.reset()
            if (this.newProject.project_name) {
              this.isNew = false
            } else {
              this.isNew = true
            }
                
            this.title = this.isEmptyProject(this.newProject) ? '新建项目' : '编辑项目'
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
                message: '项目创建成功！'
              })
              this.closeDialog()
              await this.getProjects()
              eventBus.$emit('addNewProject', data)
            } else {
              this.$bkMessage({
                theme: 'error',
                message: '接口报错！'
              })
            }
            setTimeout(() => {
              this.isCreating = false
            }, 100)
          } catch (err) {
            this.$bkMessage({
              theme: 'error',
              message: err.message || '接口异常！'
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
                message: '项目修改成功！'
              })
              this.closeDialog()
              await this.getProjects()
            } else {
              this.$bkMessage({
                theme: 'error',
                message: '接口报错！'
              })
            }
            setTimeout(() => {
              this.isCreating = false
            }, 100)
          } catch (err) {
            const message = err.message || '接口异常！'
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
          console.log(data)
          const engReg = /^[a-z][a-z0-9]{1,32}$/
          if (data.project_name === '') {
            this.$bkMessage({
              theme: 'error',
              message: '项目名称不能为空！'
            })
            return false
          } else if (data.project_name.length <= 3 || data.project_name.length > 20) {
            this.$bkMessage({
              theme: 'error',
              message: '项目名称长度必须大于3字符小于21字符！'
            })
            return false
          }

          if (!engReg.test(data.english_name)) {
            this.$bkMessage({
              theme: 'error',
              message: '英文缩写必须由小写字母+数字组成，以小写字母开头，长度限制32字符！'
            })
            return false
          }
          if (data.description === '') {
            this.$bkMessage({
              theme: 'error',
              message: '请输入项目描述！'
            })
            return false
          }
          if (data.project_type === '') {
            this.$bkMessage({
              theme: 'error',
              message: '请选择项目类型！'
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

        async created () {
          this.title = this.isEmptyProject(this.newProject) ? '新建项目' : '编辑项目'
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
