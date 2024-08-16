<template>
    <div class="step-wrapper">
        <p class="step-desc">
            {{ $t('quickStart.projectDescription') }}
        </p>
        <form>
            <label>{{ $t('quickStart.selectProject') }}</label>
            <div class="step-project-box">
                <bk-select
                    v-model="selectedProjectId"
                    class="project-selector"
                    :placeholder="$t('selectProjectPlaceholder')"
                    searchable
                    @selected="handleProjectChange"
                >
                    <bk-option
                        v-for="item in enableProjectList"
                        :id="item.projectCode"
                        :key="item.projectCode"
                        :name="item.projectName"
                    />
                    <template slot="extension">
                        <span @click="createNewProject">{{ $t('newProject') }}</span>
                    </template>
                </bk-select>
            </div>
            <p
                v-if="isError"
                class="error-tip"
            >
                <i class="devops-icon icon-info-circle" />
                <span>
                    <template v-if="errorTip === 2">
                        {{ $t("quickStart.approvalingTips") }}
                        <a
                            class="text-link"
                            href="wxwork://message/?username=DevOps"
                        >
                            {{ $t("blueShieldAssistant") }}
                        </a>
                    </template>
                    <template v-else>
                        {{ $t("quickStart.noProjectTips") }}
                    </template>
                </span>
            </p>
        </form>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Prop } from 'vue-property-decorator'
    import { State, Action, Getter } from 'vuex-class'
    import eventBus from '../../utils/eventBus'

    @Component
    export default class Step1 extends Vue {
        @Getter enableProjectList
        @State demo
        @Action selectDemoProject
        @Action toggleProjectDialog

        @Prop({ default: false })
        isError: boolean = false

        errorTip: number = 1

        selectedProjectId: string = this.demo ? this.demo.projectId : ''

        validate () {
            // if (this.selectedProjectId === '') {
            //     this.errorTip = 1
            //     return false
            // } else if(this.projectList.filter(item => item.approvalStatus === 1).find(item => item.projectCode === this.selectedProjectId)) {
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
            const project: ObjectMap = this.enableProjectList.find((proj: ObjectMap) => proj.projectCode === id)
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
                this.selectedProjectId = project.englishName
                this.selectDemoProject({
                    project: {
                        ...project,
                        projectCode: this.selectedProjectId
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
