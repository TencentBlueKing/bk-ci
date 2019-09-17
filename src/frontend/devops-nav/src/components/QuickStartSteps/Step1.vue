<template>
    <div class="step-wrapper">
        <p class="step-desc">
            项目是划分资源的最大单位，不同项目之间的资源是互不可见的
            <a
                :href="`${DOCS_URL_PREFIX}`"
                class="text-link"
                target="_blank"
            >了解更多信息</a>
        </p>
        <form>
            <label>选择项目：</label>
            <div class="step-project-box">
                <bk-select
                    v-model="selectedProjectId"
                    class="project-selector"
                    placeholder="请选择项目"
                    searchable
                    @selected="handleProjectChange"
                >
                    <bk-option
                        v-for="item in selectProjectList"
                        :id="item.project_code"
                        :key="item.project_code"
                        :name="item.project_name"
                    />
                    <template slot="extension">
                        <span @click="createNewProject">新建项目</span>
                    </template>
                </bk-select>
            </div>
            <p
                v-if="isError"
                class="error-tip"
            >
                <i class="bk-icon icon-info-circle" />
                <span>
                    <template v-if="errorTip === 2">你选择的项目正在审批中</template>
                    <template v-else>必须选择一个项目</template>
                </span>
            </p>
        </form>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Prop } from 'vue-property-decorator'
    import { State, Action } from 'vuex-class'
    import eventBus from '../../utils/eventBus'

    @Component
    export default class Step1 extends Vue {
        @State projectList
        @State demo
        @Action selectDemoProject
        @Action toggleProjectDialog

        @Prop({ default: false })
        isError: boolean = false
        errorTip: number = 1
        DOCS_URL_PREFIX: string = DOCS_URL_PREFIX

        selectedProjectId: string = this.demo ? this.demo.projectId : ''

        get selectProjectList (): object[] {
          const list = this.projectList.filter(item => ((item.approval_status === 1 || item.approval_status === 2) && !item.is_offlined))
          const finalList = list.map(item => ({
            ...item,
            project_name: item.approval_status === 1 ? `${item.project_name}` : item.project_name
          }))
          return finalList
        }

        validate () {
          // if (this.selectedProjectId === '') {
          //     this.errorTip = 1
          //     return false
          // } else if(this.projectList.filter(item => item.approval_status === 1).find(item => item.project_code === this.selectedProjectId)) {
          //     this.errorTip = 2
          //     return false
          // } else {
          //     return true
          // }
          if (this.selectedProjectId === '') {
            this.errorTip = 1
            return false
          } else {
            return true
          }
        }

        handleProjectChange (id: string): void {
          this.selectedProjectId = id
          const project: ObjectMap = this.selectProjectList.find((proj: ObjectMap) => proj.project_code === id)
          this.selectDemoProject({
            project
          })
        }

        createNewProject (): void {
          this.toggleProjectDialog({
            showProjectDialog: true
          })
        }

        created () {
          this.selectedProjectId = this.demo ? this.demo.projectId : ''
          eventBus.$off('addNewProject')
          eventBus.$on('addNewProject', project => {
            // 默认选中新创建的项目\
            this.selectedProjectId = project.english_name
            this.selectDemoProject({
              project: {
                ...project,
                project_code: this.selectedProjectId
              }
            })
          })
        }
    }
</script>

<style lang='scss'>
    @import '../../assets/scss/conf';
    .step-wrapper {
        > form {
            label {
                display: block;
                margin-bottom: 10px;
                font-weight: bold;
            }
            .step-project-box {
                display: flex;
                align-items: center;
                .project-selector {
                    width: 320px;
                    margin-right: 15px;
                }
                > span {
                    color: $primaryColor;
                    cursor: pointer;
                }
            }
            .error-tip {
                position: relative;
                padding: 10px 0 0 0;
                display: flex;
                width: 324px;
                i {
                    color: $dangerColor;
                    font-size: 16px;
                }
                span {
                    font-size: 12px;
                    padding-left: 4px;
                    line-height: 20px;
                    margin-top: -2px;
                }
            }
        }
    }
</style>
