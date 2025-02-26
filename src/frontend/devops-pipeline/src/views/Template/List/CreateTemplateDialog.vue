<template>
    <bk-dialog
        v-model="value"
        width="700"
        header-position="left"
        :title="$t('template.addTemplate')"
        :ok-text="$t('template.create')"
        @confirm="handConfirmCreateTemplate"
        @cancel="handleCancelCreateTemplate"
    >
        <bk-form
            ref="templateForm"
            :label-width="100"
            ext-cls="create-template-form"
            :model="templateFormData"
        >
            <bk-form-item
                required
                
                property="name"
                :label="$t('template.name')"
            >
                <bk-input
                    v-model="templateFormData.name"
                />
            </bk-form-item>
            <bk-form-item
                required
                :label="$t('template.type')"
                property="type"
            >
                <ul class="template-type-selector">
                    <li
                        v-for="item in templateTypeMap"
                        :class="['template-type-item', {
                            'active': item.isActive
                        }]"
                        :key="item.value"
                        @click="handleChangeTemplateType(item.value)"
                    >
                        {{ item.name }}
                    </li>
                </ul>
                <div
                    v-if="templateTypeTips"
                    class="template-type-tips"
                >
                    {{ templateTypeTips }}
                </div>
            </bk-form-item>
            <bk-form-item
                property="desc"
                :label="$t('template.desc')"
            >
                <bk-input
                    v-model="templateFormData.desc"
                    type="textarea"
                    :maxlength="200"
                    :rows="3"
                />
            </bk-form-item>
        </bk-form>
    </bk-dialog>
</template>

<script>
    export default {
        name: 'CreateTemplateDialog',
        props: {
            value: {
                type: Boolean,
                default: false
            }
        },
        data () {
            const getDefaultFormData = () => {
                return {
                    projectId: this.projectId,
                    creator: this.userName,
                    type: 'PIPELINE',
                    source: 'CUSTOM',
                    name: '',
                    desc: ''
                }
            }
            return {
                getDefaultFormData,
                templateFormData: {}
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            userName () {
                return this.$userInfo?.username || window?.userInfo?.username || ''
            },
            templateTypeMap () {
                return [
                    {
                        name: this.$t('template.pipelineTemplate'),
                        value: 'PIPELINE',
                        isActive: this.templateFormData.type === 'PIPELINE'
                    }
                ]
            },
            templateTypeTips () {
                const tipsMap = {
                    PIPELINE: this.$t('template.pipelineTypeTips')
                }
                return tipsMap[this.templateFormData.type] || ''
            }
        },
        created () {
            this.templateFormData = this.getDefaultFormData()
        },
        methods: {
            handleChangeTemplateType (type) {
                this.templateFormData.type = type
            },
            handConfirmCreateTemplate () {
                try {
                    this.$refs.templateForm.validate().then(async () => {
                        const res = await this.$store.dispatch('pipelines/createTemplate', {
                            projectId: this.projectId,
                            params: this.templateFormData
                        })
                        if (res.data) {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('创建模板成功')
                            })
                        }
                    })
                } catch (e) {
                    this.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                } finally {
                    this.$emit('update:value', false)
                    this.$emit('confirm')
                }
            },

            handleCancelCreateTemplate () {
                this.$emit('update:value', false)
                this.templateFormData = this.getDefaultFormData()
            }
        }
    }
</script>

<style lang="scss">
    .create-template-form {
        .bk-label .bk-label-text {
            font-weight: normal;
        }
    }
</style>
<style lang="scss" scoped>
    .template-type-selector {
        .template-type-item {
            display: inline-block;
            width: 152px;
            height: 44px;
            line-height: 44px;
            background: #FFFFFF;
            border: 1px solid #C4C6CC;
            border-radius: 2px;
            align-items: center;
            text-align: center;
            margin-right: 8px;
            margin-bottom: 8px;
            cursor: pointer;
            &.active {
                color: #3A84FF;
                background: #F0F5FF;
                border: 1px solid #3A84FF;
            }
        }
    }
    .template-type-tips {
        font-size: 12px;
        color: #979BA5;
        line-height: 20px;
        background: #F5F7FA;
        padding: 8px 16px;
    }
</style>
