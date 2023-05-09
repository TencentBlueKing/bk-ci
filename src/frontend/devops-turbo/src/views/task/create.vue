<template>
    <article class="create-task-home">
        <bk-breadcrumb separator-class="bk-icon icon-angle-right" class="bread-crumb">
            <bk-breadcrumb-item :to="{ name: 'taskList' }"> {{ $t('turbo.方案列表') }} </bk-breadcrumb-item>
            <bk-breadcrumb-item> {{ $t('turbo.新增加速方案') }} </bk-breadcrumb-item>
        </bk-breadcrumb>
        <task-basic :form-data.sync="formData" ref="basic" />
        <task-param :form-data.sync="formData" ref="param" />
        <bk-button theme="primary" @click="submit"> {{ $t('turbo.提交') }} </bk-button>
        <bk-button @click="cancel"> {{ $t('turbo.取消') }} </bk-button>
    </article>
</template>

<script>
    import taskBasic from '@/components/task/basic'
    import taskParam from '@/components/task/param'
    import { addTurboPlan } from '@/api'

    export default {
        components: {
            taskBasic,
            taskParam
        },

        data () {
            return {
                formData: {
                    engineCode: '',
                    openStatus: true,
                    configParam: {}
                },
                isLoading: false
            }
        },

        methods: {
            submit () {
                const basicComponent = this.$refs.basic
                const basicFormComponent = basicComponent.$refs.createTask
                const basicForm = basicComponent.copyFormData
                const paramComponent = this.$refs.param
                const paramForm = paramComponent.copyFormData

                Promise.all([basicFormComponent.validate(), paramComponent.validate()]).then(() => {
                    this.isLoading = true
                    const postData = {
                        ...basicForm,
                        configParam: paramForm.configParam,
                        projectId: this.$route.params.projectId
                    }
                    addTurboPlan(postData).then((res) => {
                        this.$bkMessage({ theme: 'success', message: this.$t('turbo.添加成功') })
                        this.$router.push({
                            name: 'taskSuccess',
                            query: {
                                engineCode: postData.engineCode,
                                planId: res
                            }
                        })
                    }).catch((err) => {
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    }).finally(() => {
                        this.isLoading = false
                    })
                }, (validator) => {
                    this.$bkMessage({ message: validator.content || validator, theme: 'error' })
                })
            },

            cancel () {
                if (window.changeFlag) {
                    this.$bkInfo({
                        title: this.$t('确认离开当前页？'),
                        subHeader: this.$createElement('p', {
                            style: {
                                color: '#63656e',
                                fontSize: '14px',
                                textAlign: 'center'
                            }
                        }, this.$t('离开将会导致未保存信息丢失')),
                        okText: this.$t('离开'),
                        confirmFn: () => {
                            window.changeFlag = false
                            this.$router.back()
                        }
                    })
                } else {
                    this.$router.back()
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .create-task-home {
        padding: 10px 20px 28px;
        margin: 0 auto;
        .bread-crumb {
            font-size: 12px;
            margin-bottom: 10px;
            ::v-deep .bk-breadcrumb-separator {
                font-size: 14px;
            }
            .bk-breadcrumb-item:last-child {
                color: #000;
            }
        }
    }
</style>
