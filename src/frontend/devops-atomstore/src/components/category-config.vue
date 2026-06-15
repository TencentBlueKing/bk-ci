<template>
    <div class="category-config-content g-form-radio">
        <div
            class="bk-form-item is-required"
            :ref="`${scopeType}SortError`"
        >
            <label class="bk-label"> {{ $t('store.分类') }} </label>
            <div class="bk-form-content atom-item-content atom-classify-content">
                <bk-select
                    v-model="categoryData.classifyCode"
                    @selected="handleClassifyChange"
                    searchable
                    :clearable="false"
                >
                    <bk-option
                        v-for="(option, index) in sortList"
                        :key="index"
                        :id="option.classifyCode"
                        :name="option.classifyName"
                    >
                    </bk-option>
                </bk-select>
                <div
                    v-if="errors.sortError"
                    class="error-tips"
                >
                    {{ $t('store.分类不能为空') }}
                </div>
            </div>
        </div>
        <div
            class="bk-form-item is-required"
            :ref="`${scopeType}JobError`"
        >
            <label class="bk-label env-label"> {{ $t('store.适用Job类型') }} </label>
            <div class="bk-form-content atom-item-content">
                <template>
                    <!-- Pipeline 使用单选 -->
                    <bk-radio-group
                        v-if="scopeType === 'pipeline'"
                        v-model="categoryData.jobTypes[0]"
                        class="radio-group"
                    >
                        <span
                            v-for="(entry, key) in jobTypeList"
                            :key="key"
                        >
                            <bk-radio
                                v-show="entry.isShow"
                                :disabled="disabled"
                                :value="entry.value"
                                @click.native="handleJobTypeChange"
                            >{{ entry.label }}</bk-radio>
                        </span>
                    </bk-radio-group>
                    <!-- Creative 使用多选 -->
                    <bk-checkbox-group
                        v-else
                        v-model="categoryData.jobTypes"
                        class="checkbox-group"
                    >
                        <span
                            v-for="(entry, key) in jobTypeList"
                            :key="key"
                        >
                            <bk-checkbox
                                :value="entry.value"
                                :disabled="disabled"
                                v-bk-tooltips.top-start="entry.tooltips"
                                style=""
                                @change="handleJobTypeChange"
                            >
                                <span :class="{ 'show-tooltips': entry.tooltips }">{{ entry.label }}</span>
                            </bk-checkbox>
                        </span>
                    </bk-checkbox-group>
                </template>
                <div
                    v-if="errors.jobError"
                    class="error-tips"
                >
                    {{ $t('store.字段有误，请重新选择') }}
                </div>
            </div>
        </div>
        <!-- 仅 Pipeline AGENT 类型显示操作系统选择 -->
        <template v-if="scopeType === 'pipeline' && categoryData.jobTypes && categoryData.jobTypes[0] === 'AGENT'">
            <bk-checkbox-group
                v-model="categoryData.os"
                class="bk-form-content atom-os"
                :ref="`${scopeType}EnvError`"
            >
                <bk-checkbox
                    :value="entry.value"
                    v-for="(entry, key) in envList"
                    :key="key"
                    :disabled="disabled"
                    @click.native="handleOsChange"
                >
                    <p class="os-checkbox-label">
                        <i :class="{ 'devops-icon': true, [`icon-${entry.icon}`]: true }"></i>
                        <span class="bk-checkbox-text">{{ entry.label }}</span>
                    </p>
                </bk-checkbox>
            </bk-checkbox-group>
            <div
                v-if="errors.envError"
                class="error-tips env-error"
            >
                {{ $t('store.字段有误，请重新选择') }}
            </div>
        </template>
        <div class="bk-form-item">
            <label class="bk-label"> {{ $t('store.功能标签') }} </label>
            <div class="bk-form-content template-item-content">
                <bk-select
                    :placeholder="$t('store.请选择功能标签')"
                    v-model="categoryData.labelList"
                    @selected="handleClassifyChange"
                    show-select-all
                    searchable
                    multiple
                >
                    <bk-option
                        v-for="(option, index) in labelList"
                        :key="index"
                        :id="option.id"
                        :name="option.labelName"
                    >
                    </bk-option>
                </bk-select>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'CategoryConfig',
        props: {
            // 范畴类型：'pipeline' 或 'creative'
            scopeType: {
                type: String,
                required: true,
                validator: (value) => ['pipeline', 'creative'].includes(value)
            },
            // 范畴数据
            categoryData: {
                type: Object,
                required: true
            },
            // 错误状态
            errors: {
                type: Object,
                default: () => ({
                    sortError: false,
                    jobError: false,
                    envError: false
                })
            },
            disabled: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                sortList: [],
                labelList: [],
                pipelineJobTypeList: [
                    { label: this.$t('store.编译环境'), value: 'AGENT', isShow: true },
                    { label: this.$t('store.无编译环境'), value: 'AGENT_LESS', isShow: true }
                ],
                creativeJobTypeList: [
                    { label: this.$t('store.创作环境'), value: 'CREATIVE_STREAM', isShow: true },
                    { label: this.$t('store.云任务环境'), value: 'CLOUD_TASK', isShow: true, tooltips: this.$t('store.如发邮件通知等对运行环境无特殊要求的任务，无需运行在创作环境中。')}
                ],
                envList: [
                    { label: 'Linux', value: 'LINUX', icon: 'linux-view' },
                    { label: 'Windows', value: 'WINDOWS', icon: 'windows' },
                    { label: 'macOS', value: 'MACOS', icon: 'macos' }
                ]
            }
        },
        computed: {
            // 将 scopeType 转换为 API 需要的 serviceScope 参数
            serviceScope () {
                return this.scopeType === 'pipeline' ? 'PIPELINE' : 'CREATIVE_STREAM'
            },
            // 根据范畴类型返回对应的 Job 类型列表
            jobTypeList () {
                return this.scopeType === 'pipeline' ? this.pipelineJobTypeList : this.creativeJobTypeList
            }
        },
        async created () {
            // 组件创建时加载分类和标签数据
            await this.loadClassifyAndLabels()
            // 如果是 pipeline 范畴，需要检查容器列表以确定是否显示"无编译环境"
            if (this.scopeType === 'pipeline') {
                await this.checkAgentLessAvailability()
            }
        },
        methods: {
            async loadClassifyAndLabels () {
                try {
                    // 并行请求分类和标签数据
                    const [classifyRes, labelsRes] = await Promise.all([
                        this.$store.dispatch('store/requestAtomClassify', { serviceScope: this.serviceScope }),
                        this.$store.dispatch('store/requestAtomLables', { serviceScope: this.serviceScope })
                    ])
                    
                    // 过滤掉 trigger 分类
                    this.sortList = (classifyRes || []).filter(item => item.classifyCode !== 'trigger')
                    this.labelList = labelsRes || []
                } catch (err) {
                    const message = err.message ? err.message : err
                    this.$bkMessage({
                        message,
                        theme: 'error'
                    })
                }
            },
            
            async checkAgentLessAvailability () {
                try {
                    const containerList = await this.$store.dispatch('store/getContainerList')
                    // 检查是否有 type 为 'normal' 的容器，决定是否显示"无编译环境"选项
                    const hasNormalContainer = !!(containerList || []).find(i => i.type === 'normal')
                    this.pipelineJobTypeList[1].isShow = hasNormalContainer
                } catch (err) {
                    // 如果获取失败，默认不显示
                    this.pipelineJobTypeList[1].isShow = false
                }
            },
            handleClassifyChange () {
                this.$emit('classify-change')
            },
            handleJobTypeChange () {
                this.$emit('job-type-change')
            },
            handleOsChange () {
                this.$emit('os-change')
            },
            
            // 获取 Job 类型的显示标签
            getJobTypeLabel (jobType) {
                const job = this.jobTypeList.find(item => item.value === jobType)
                return job ? job.label : jobType
            }
        }
    }
</script>

<style lang="scss" scoped>
.category-config-content {
    font-size: 12px;
    padding: 16px;
    border-radius: 2px;
    background-color: #F5F7FA;
    
    // 标签样式
    .bk-label {
        width: 110px;
    }
    
    // 表单内容区域样式
    .bk-form-content {
        margin-left: 0;
        line-height: 32px;
        
        .bk-form-checkbox {
            margin: 10px 21px 20px 0;
        }
    }
    
    // 移除功能标签的必填符号
    .bk-form-item:not(.is-required) .bk-label:after {
        content: '' !important;
    }

    ::v-deep .bk-form-control {
        display: flex;
        flex-direction: column;
    }

    .atom-item-content {
        ::v-deep .bk-form-control {
            display: flex;
            flex-direction: row;
        }
    }

    ::v-deep .bk-form-control.atom-os {
        display: flex;
        flex-direction: row;
    }

    ::v-deep .bk-select {
        background-color: #fff;
    }

    .atom-classify-content {
        ::v-deep .bk-selector {
            width: 40%;
        }
    }

    .os-checkbox-label {
        display: flex;
        align-items: center;

        .devops-icon {
            position: relative;
            font-size: 16px;
            color: #979BA5;
        }

        .bk-checkbox-text {
            color: #333C48;
        }
    }

    .bk-radio-text {
        color: #333C48;
    }
    
    // 单选/复选组样式
    .radio-group,
    .checkbox-group {
        display: inline-block;
    }

    .atom-os .bk-form-checkbox:first-child {
        margin-left: 110px;
    }

    .env-error {
        margin: -10px 0 20px 110px;
    }

    .show-tooltips {
        border-bottom: 1px dashed #979BA5;
    }
}
</style>
