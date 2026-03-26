<template>
    <section v-bkloading="{ isLoading }">
        <bk-form
            :label-width="100"
            :model="formData"
            class="manage-detail-edit"
            ref="atomEdit"
            v-if="!isLoading"
        >
            <bk-form-item
                :label="$t('store.名称')"
                :rules="[requireRule($t('store.名称')), nameRule, numMax(40)]"
                :required="true"
                property="name"
                error-display-type="normal"
            >
                <bk-input
                    v-model="formData.name"
                    :placeholder="$t('store.请输入中英文名称，不超过40个字符')"
                ></bk-input>
            </bk-form-item>
            <bk-form-item
                :label="$t('store.范畴')"
                :required="true"
                error-display-type="normal"
            >
                <bk-checkbox-group
                    v-model="categoryValue"
                    ext-cls="category-checkbox-group"
                >
                    <template v-if="categoryValue.includes('PIPELINE')">
                        <bk-checkbox
                            :value="'PIPELINE'"
                            :disabled="true"
                            style="height: 32px; line-height: 32px;"
                        >
                            {{ $t('store.CI流水线') }}
                        </bk-checkbox>
                        <category-config
                            ref="pipelineCategoryConfig"
                            scope-type="pipeline"
                            :category-data="pipelineCategory"
                            :disabled="true"
                            :errors="{
                                sortError: formErrors.pipelineSortError
                            }"
                        ></category-config>
                    </template>
                    <template v-if="categoryValue.includes('CREATIVE_STREAM')">
                        <bk-checkbox
                            :value="'CREATIVE_STREAM'"
                            :disabled="true"
                            style="height: 32px; line-height: 32px;"
                        >
                            {{ $t('store.CP创作流') }}
                        </bk-checkbox>
                        <category-config
                            ref="creativeCategoryConfig"
                            scope-type="creative"
                            :category-data="creativeCategory"
                            :disabled="true"
                            :errors="{
                                sortError: formErrors.creativeSortError
                            }"
                        ></category-config>
                    </template>
                </bk-checkbox-group>
            </bk-form-item>
            <template v-if="userInfo.isProjectAdmin && VERSION_TYPE !== 'ee'">
                <bk-form-item
                    :label="$t('store.是否开源')"
                    property="visibilityLevel"
                >
                    <bk-radio-group
                        v-model="formData.visibilityLevel"
                        class="radio-group"
                    >
                        <bk-radio
                            :disabled="entry.disable"
                            :title="entry.title"
                            :value="entry.value"
                            v-for="(entry, key) in isOpenSource"
                            :key="key"
                            @click.native="formData.privateReason = ''"
                        >
                            {{ entry.label }}
                        </bk-radio>
                    </bk-radio-group>
                </bk-form-item>
                <bk-form-item
                    v-if="formData.visibilityLevel === 'PRIVATE'"
                    :label="$t('store.不开源原因')"
                    :rules="[requireRule($t('store.不开源原因'))]"
                    :required="true"
                    property="privateReason"
                    error-display-type="normal"
                >
                    <bk-input
                        type="textarea"
                        v-model="formData.privateReason"
                    ></bk-input>
                </bk-form-item>
            </template>
            <bk-form-item
                :label="$t('store.简介')"
                :rules="[requireRule($t('store.简介')), numMax(256)]"
                :required="true"
                property="summary"
                :desc="$t('store.展示在插件市场以及流水线选择插件页面。')"
                error-display-type="normal"
            >
                <bk-input
                    v-model="formData.summary"
                    :placeholder="$t('store.插件一句话简介，不超过256个字符')"
                ></bk-input>
            </bk-form-item>
            <bk-form-item
                :label="$t('store.详细描述')"
                property="description"
                :desc="`${$t('store.atomRemark')}<br>${$t('store.展示在插件市场查看插件详情界面，帮助用户快速了解插件和解决遇到的问题。')}`"
            >
                <mavon-editor
                    class="remark-input"
                    ref="mdHook"
                    v-model="formData.description"
                    :toolbars="toolbars"
                    :external-link="false"
                    :box-shadow="false"
                    :language="mavenLang"
                    preview-background="#fff"
                    @imgAdd="addImage"
                />
            </bk-form-item>
            <bk-form-item
                :label="$t('store.发布者')"
                :rules="[requireRule($t('store.发布者'))]"
                :required="true"
                property="publisher"
                error-display-type="normal"
            >
                <bk-select v-model="formData.publisher">
                    <bk-option
                        v-for="publisher in publishersList"
                        :key="publisher.id"
                        :id="publisher.publisherCode"
                        :name="publisher.publisherName"
                    ></bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item
                :required="true"
                property="logoUrl"
                error-display-type="normal"
                class="edit-logo"
            >
                <select-logo
                    :form="formData"
                    type="ATOM"
                    :is-err="false"
                    ref="logoUrlError"
                ></select-logo>
            </bk-form-item>
            <bk-form-item>
                <bk-button
                    theme="primary"
                    @click="save"
                    :loading="isSaving"
                >
                    {{ $t('store.保存') }}
                </bk-button>
                <bk-button
                    :disabled="isSaving"
                    @click="$router.back()"
                >
                    {{ $t('store.取消') }}
                </bk-button>
            </bk-form-item>
        </bk-form>
    </section>
</template>

<script>
    import selectLogo from '@/components/common/selectLogo'
    import categoryConfig from '@/components/category-config.vue'
    import { toolbars } from '@/utils/editor-options'
    import { mapGetters } from 'vuex'

    export default {
        components: {
            selectLogo,
            categoryConfig
        },

        props: {
            detail: Object
        },
        
        data () {
            return {
                formData: JSON.parse(JSON.stringify(this.detail)),
                pipelineCategory: this.initCategoryData('PIPELINE'),
                creativeCategory: this.initCategoryData('CREATIVE_STREAM'),
                categoryValue: this.getSelectedScopes(),
                formErrors: {
                    pipelineSortError: false,
                    creativeSortError: false
                },
                isLoading: true,
                isSaving: false,
                toolbars,
                nameRule: {
                    validator: (val) => (/^[\u4e00-\u9fa5a-zA-Z0-9-_. ]+$/.test(val)),
                    message: this.$t('store.由汉字、英文字母、数字、连字符、下划线或点组成，不超过40个字符'),
                    trigger: 'blur'
                },
                isOpenSource: [
                    { title: this.$t('store.否'), label: this.$t('store.否'), value: 'PRIVATE'},
                    { title: this.$t('store.是'), label: this.$t('store.是'), value: 'LOGIN_PUBLIC'},
                ],
                publishersList: []
            }
        },

        computed: {
            ...mapGetters('store', {
                userInfo: 'getUserInfo'
            }),

            userName () {
                return this.$store.state.user.username
            },
            mavenLang () {
                return this.$i18n.locale === 'en-US' ? 'en' : this.$i18n.locale
            }
        },

        watch: {
            formData: {
                handler () {
                    this.hasChange = true
                },
                deep: true
            }
        },

        created () {
            this.hackData()
            this.fetchPublishersList(this.detail.atomCode)
        },

        methods: {
            requireRule (name) {
                return {
                    required: true,
                    message: this.$t('store.validateMessage', [name, this.$t('store.必填项')]),
                    trigger: 'blur'
                }
            },

            numMax (num) {
                return {
                    validator: (val = '') => (val.length <= num),
                    message: this.$t('store.validateNum', [num]),
                    trigger: 'blur'
                }
            },

            hackData () {
                this.formData.labelList = this.formData.labelList.map(label => label.id)
                this.formData.description = this.formData.description || `#### ${this.$t('store.插件功能')}\n\n#### ${this.$t('store.适用场景')}\n\n#### ${this.$t('store["使用限制和受限解决方案[可选]"]')}\n\n#### ${this.$t('store.常见的失败原因和解决方案')}`
            },

            getSelectedScopes () {
                const scopes = []
                if (this.detail.serviceScopeDetails && this.detail.serviceScopeDetails.length > 0) {
                    this.detail.serviceScopeDetails.forEach(config => {
                        scopes.push(config.serviceScope)
                    })
                    return scopes
                }
                // 默认返回 CI流水线
                return ['PIPELINE']
            },

            initCategoryData (scope) {
                const config = this.detail.serviceScopeDetails?.find(item => item.serviceScope === scope) || {}
                
                // 从 jobTypeConfigs 中提取 jobTypes 和 os
                const jobTypes = config.jobTypeConfigs?.map(item => item.jobType) || (scope === 'PIPELINE' ? ['AGENT'] : ['CREATIVE_STREAM'])
                let os = []
                
                // 如果是 PIPELINE，查找 AGENT 类型的 osList
                if (scope === 'PIPELINE') {
                    const agentConfig = config.jobTypeConfigs?.find(item => item.jobType === 'AGENT')
                    os = agentConfig?.osList || []
                }
                
                return {
                    classifyCode: config.classifyCode || '',
                    jobTypes,
                    os,
                    labelList: config.labelList?.map(label => label.id) || []
                }
            },

            save () {
                this.$refs.atomEdit.validate().then(() => {
                    this.isSaving = true
                    const { name, summary, description, logoUrl, iconData, publisher, privateReason } = this.formData
                    
                    // 构建 serviceScopeConfigs 数组
                    const scopeConfigMap = {
                        PIPELINE: {
                            data: this.pipelineCategory,
                        },
                        CREATIVE_STREAM: {
                            data: this.creativeCategory
                        }
                    }
                    
                    const serviceScopeConfigs = this.categoryValue
                        .filter(scope => scopeConfigMap[scope])
                        .map(scope => {
                            const { data } = scopeConfigMap[scope]
                            const config = {
                                serviceScope: scope,
                                classifyCode: data.classifyCode,
                                labelIdList: (data.labelList || []).filter(id => id && id !== 'null' && id !== ' ')
                            }
                            
                            // 构建 jobTypeConfigs
                            const jobTypes = data.jobTypes || []
                            config.jobTypeConfigs = jobTypes.map(jobType => {
                                const jobTypeConfig = { jobType }
                                // 如果是 PIPELINE 范畴且是 AGENT 类型，添加 osList
                                if (scope === 'PIPELINE' && jobType === 'AGENT') {
                                    jobTypeConfig.osList = data.os || []
                                }
                                return jobTypeConfig
                            })
                            
                            return config
                        })
                    
                    const putData = {
                        atomCode: this.detail.atomCode,
                        data: {
                            name,
                            summary,
                            description,
                            logoUrl,
                            iconData,
                            publisher,
                            privateReason,
                            serviceScopeConfigs: serviceScopeConfigs.length > 0 ? serviceScopeConfigs : undefined
                        }
                    }
                    this.$store.dispatch('store/modifyAtomDetail', putData).then(() => {
                        const serviceScopeDetails = serviceScopeConfigs.map(config => {
                            const refName = config.serviceScope === 'PIPELINE' ? 'pipelineCategoryConfig' : 'creativeCategoryConfig'
                            const fullLabelList = this.$refs[refName]?.labelList || []
                            // 从 labelIdList（ID 数组）还原为 labelList（对象数组），保持与接口返回格式一致，以便正确回显
                            const labelList = (config.labelIdList || [])
                                .map(id => fullLabelList.find(label => label.id === id))
                                .filter(Boolean)
                            
                            return { ...config, labelList }
                        })
                        this.formData.serviceScopeDetails = serviceScopeDetails
                        this.$store.dispatch('store/clearDetail')
                        this.$store.dispatch('store/setDetail', this.formData)
                        this.$nextTick(() => {
                            this.hasChange = false
                            this.$router.back()
                        })
                    }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => (this.isSaving = false))
                }, (validator) => {
                    this.$bkMessage({ message: validator.content || validator, theme: 'error' })
                })
            },

            addImage (pos, file) {
                this.uploadimg(pos, file)
            },

            async uploadimg (pos, file) {
                const formData = new FormData()
                const config = {
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                }
                let message, theme
                formData.append('file', file)

                try {
                    const res = await this.$store.dispatch('store/uploadFile', {
                        formData,
                        config
                    })

                    this.$refs.mdHook.$img2Url(pos, res)
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                    this.$refs.mdHook.$refs.toolbar_left.$imgDel(pos)
                }
            },

            fetchPublishersList (atomCode) {
                this.$store.dispatch('store/getPublishersList', { atomCode }).then(res => {
                    this.publishersList = res
                    const result = this.publishersList.find(i => i.publisherCode === this.userName)
                    if (!result) {
                        this.publishersList.push({
                            publisherCode: this.userName,
                            publisherName: this.userName
                        })
                    }
                }).catch(() => []).finally(() => this.isLoading = false)
            }
        }
    }
</script>
