<template>
    <article
        class="task-detail-home"
        v-bkloading="{ isLoading }"
    >
        <template v-if="hasPermission">
            <bk-breadcrumb
                separator-class="bk-icon icon-angle-right"
                class="bread-crumb"
            >
                <bk-breadcrumb-item :to="{ name: 'taskList' }"> {{ $t('turbo.方案列表') }} </bk-breadcrumb-item>
                <bk-breadcrumb-item> {{ $t('turbo.查看加速方案') }} </bk-breadcrumb-item>
            </bk-breadcrumb>

            <section
                class="task-detail-body"
                v-if="!isLoading"
            >
                <main class="task-detail-left">
                    <task-basic
                        :form-data.sync="formData"
                        :only-edit="false"
                        ref="basic"
                    />
                    <task-param
                        :form-data.sync="formData"
                        :only-edit="false"
                    />
                </main>

                <main class="task-detail-right g-turbo-box">
                    <section class="g-turbo-task-tip task-use">
                        <h3 class="create-title g-turbo-deep-black-font"> {{ $t('turbo.使用方式') }} </h3>
                        <section v-html="formData.userManual"></section>
                    </section>

                    <section class="task-record">
                        <h3 class="create-title g-turbo-deep-black-font"> {{ $t('turbo.更新记录') }} </h3>
                        <bk-form
                            :label-width="130"
                            class="g-turbo-form-left record-form"
                        >
                            <bk-form-item :label="$t('turbo.创建人：')">
                                {{ formData.createdBy }}
                            </bk-form-item>
                            <bk-form-item :label="$t('turbo.创建时间：')">
                                {{ formData.createdDate }}
                            </bk-form-item>
                            <bk-form-item :label="$t('turbo.最近修改人：')">
                                {{ formData.updatedBy }}
                            </bk-form-item>
                            <bk-form-item :label="$t('turbo.修改时间：')">
                                {{ formData.updatedDate }}
                            </bk-form-item>
                        </bk-form>
                    </section>
                </main>
            </section>
        </template>
        <permission-exception
            v-else
            :message="errMessage"
        />
    </article>
</template>

<script>
    import { getPlanDetailById } from '@/api'
    import taskBasic from '@/components/task/basic'
    import taskParam from '@/components/task/param'
    import permissionException from '@/components/exception/permission.vue'

    export default {
        components: {
            taskBasic,
            taskParam,
            permissionException
        },

        data () {
            return {
                formData: {},
                isLoading: false,
                hasPermission: true,
                errMessage: ''
            }
        },

        mounted () {
            this.getPlanDetail()
        },

        methods: {
            getPlanDetail () {
                const planId = this.$route.params.id
                this.isLoading = true
                getPlanDetailById(planId).then((res) => {
                    this.formData = res
                }).catch((err) => {
                    if (err.code === 2300017) {
                        this.hasPermission = false
                        this.errMessage = err.message
                    } else {
                        this.$bkMessage({
                            message: err.message || err,
                            theme: 'error'
                        })
                    }
                }).finally(() => {
                    this.isLoading = false
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .task-detail-home {
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
    .task-detail-body {
        display: flex;
        .task-detail-left {
            flex: 874;
            margin-right: 20px;
        }
        .task-detail-right {
            flex: 360;
            line-height: 22px;
            padding: 0 32px;
            .task-use {
                margin-bottom: 20px;
                padding: 26px 0 20px;
            }
            .task-record {
                border-top: 1px solid #DCDEE5;
                padding: 26px 0;
            }
            ::v-deep .bk-form {
                .bk-label {
                    color: #999;
                    min-height: 14px;
                    line-height: 14px;
                }
                .bk-form-content {
                    color: #222222;
                    min-height: 14px;
                    line-height: 14px;
                }
                .bk-form-item+.bk-form-item {
                    margin-top: 24px;
                }
            }
        }
    }
    .create-title {
        font-size: 14px;
        line-height: 22px;
        margin-bottom: 17px;
    }
</style>
