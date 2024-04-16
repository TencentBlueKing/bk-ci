<template>
    <div
        v-bkloading="{ isLoading }"
        class="exec-script-view"
        :class="{ loading: isLoading }">
        <detail-item
            :label="$t('environment.脚本内容：')"
            layout="vertical">
            <ace-editor
                :lang="language"
                :options="languageOption"
                readonly
                :value="stepInfo.scriptContent" />
        </detail-item>
        <div>
            <detail-item :label="$t('environment.脚本参数：')">
                {{ stepInfo.scriptParam || '--' }}
            </detail-item>
            <detail-item :label="$t('environment.超时时长：')">
                {{ stepInfo.scriptTimeout }}（s）
            </detail-item>
            <detail-item :label="$t('environment.执行账号：')">
                {{ stepInfo.account.name }}
            </detail-item>
        </div>
        
        <slot />
    </div>
</template>
  <script>
    import AceEditor from './components/ace-editor'
    import DetailItem from './components/detail-layout/item'
    const formatScriptTypeValue = (value) => {
        const key = String(value).toLowerCase()
        const typeMap = {
            shell: 1,
            bat: 2,
            perl: 3,
            python: 4,
            powershell: 5,
            sql: 6,
            1: 'Shell',
            2: 'Bat',
            3: 'Perl',
            4: 'Python',
            5: 'Powershell',
            6: 'SQL'
        }
        return typeMap[key] || key
    }

    export default {
        name: '',
        components: {
            AceEditor,
            DetailItem
        },
        props: {
            data: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                stepInfo: {},
                executeAccountText: '',
                language: '',
                scriptName: '',
                scriptInfo: {},
                isShowScriptSource: false,
                requestQueue: [],
                isShowDiff: false
            }
        },
        computed: {
            isLoading () {
                return this.requestQueue.length > 0
            }
           
        },
        created () {
            this.stepInfo = Object.freeze(this.data.scriptStepInfo)
            this.language = formatScriptTypeValue(this.stepInfo.scriptLanguage)
            this.languageOption = [
                this.language
            ]
            if (this.stepInfo.scriptVersionId) {
                this.fetchScriptDetail()
            }
        },
        methods: {
            /**
             * @desc 更新脚本版本获取版本详情
             */
            fetchScriptDetail () {

            },
            /**
             * @desc 脚本版本对比
             */
            handleShowScriptVersionDiff () {
                this.isShowDiff = true
            },
            handleDiffClose () {
                this.isShowDiff = false
            },
            /**
             * @desc 编辑作业模板，更新步骤引用的脚本版本
             */
            handleUpdateScript () {
                this.$router.push({
                    name: 'templateEdit',
                    params: {
                        id: this.$route.params.id,
                        stepId: this.data.id
                    },
                    query: {
                        from: 'templateDetail'
                    }
                })
            }
        }
    }
  </script>
  <style lang="scss">
    .exec-script-view {
        &.loading {
            height: calc(100vh - 100px);
        }
    
        .detail-item {
            margin-bottom: 0;
        }
    
        .script-detail {
            color: #3a84ff;
            cursor: pointer;
        }
    
        .script-update-flag {
            display: inline-block;
    
            .script-update {
                color: #ff5656;
            }
        }
    }
  </style>
