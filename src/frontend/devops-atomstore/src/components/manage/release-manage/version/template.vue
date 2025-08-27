<template>
    <section class="show-version g-scroll-pagination-table">
        <bk-button
            theme="primary"
            class="version-button"
            @click="toUpgrade"
        >
            {{ $t('store.新增版本') }}
        </bk-button>
        <bk-table
            :data="tableData"
            :outer-border="false"
            :header-border="false"
            :header-cell-style="{ background: '#fff' }"
            :pagination="pagination"
            @page-change="handlePageChange"
            @page-limit-change="handleLimitChange"
        >
            <bk-table-column
                v-for="col in columns"
                :key="col.prop"
                v-bind="col"
            >
                <template slot-scope="{ row }">
                    <span
                        v-if="col.prop === 'statusLabel'"
                        :class="['status-indicator', row.published ? 'published' : 'offline']"
                    ></span>
                    <span :title="row[col.prop]">{{ row[col.prop] ?? '--' }}</span>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('store.操作')"
                class-name="handler-btn"
            >
                <template slot-scope="props">
                    <section v-show="!index">
                        <bk-button
                            text
                            theme="primary"
                            size="small"
                            @click="toDetail"
                        >
                            {{ $t('store.查看') }}
                        </bk-button>
                        
                        <bk-button
                            text
                            theme="primary"
                            size="small"
                            v-if="props.row.published"
                            @click="offline(props.row)"
                        >
                            {{ $t('store.下架') }}
                        </bk-button>
                        <bk-button
                            v-else
                            text
                            theme="primary"
                            size="small"
                            @click="online(props.row)"
                        >
                            {{ $t('store.上架') }}
                        </bk-button>
                    </section>
                </template>
            </bk-table-column>
        </bk-table>

        <bk-sideslider
            :is-show.sync="offlineData.show"
            :title="offlineData.title"
            :quick-close="offlineData.quickClose"
            :width="offlineData.width"
        >
            <template slot="content">
                <bk-form
                    v-if="offlineData.form"
                    ref="offlineForm"
                    class="offline-template-version-form"
                    :model="offlineData.form"
                    :rules="offlineFormRules"
                    v-bkloading="{ isLoading: offlineData.isLoading }"
                >
                    <bk-form-item
                        :label="$t('store.versionName')"
                        property="versionName"
                    >
                        <span class="lh30">{{ offlineData.form.versionName }}</span>
                    </bk-form-item>
                    <bk-form-item
                        :label="$t('store.模板版本')"
                        property="version"
                    >
                        <span class="lh30">{{ offlineData.form.version }}</span>
                    </bk-form-item>
                    <bk-form-item
                        :label="$t('store.下架原因')"
                        required
                        property="reason"
                    >
                        <bk-input
                            type="textarea"
                            v-model="offlineData.form.reason"
                            :placeholder="$t('store.请输入下架原因')"
                        ></bk-input>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-button
                            theme="primary"
                            @click.native="submitOfflineTemplateVersion"
                        >
                            {{ $t('store.提交') }}
                        </bk-button>
                        <bk-button @click.native="cancelOfflineTemplate"> {{ $t('store.取消') }} </bk-button>
                    </bk-form-item>
                </bk-form>
            </template>
        </bk-sideslider>
    </section>
</template>

<script>
    import { convertTime } from '@/utils/index'
    import { mapActions } from 'vuex'

    export default {
        props: {
            versionList: Array,
            pagination: Object
        },
        emits: ['pageChanged', 'pageLimitChanged'],
        data () {
            return {
                offlineData: {
                    title: this.$t('store.下架模板'),
                    quickClose: true,
                    width: 565,
                    isLoading: false,
                    show: false,
                    form: null
                }
            }
        },

        computed: {
            columns () {
                return [
                    {
                        label: this.$t('store.版本'),
                        prop: 'versionName'
                    },
                    {
                        label: this.$t('store.状态'),
                        prop: 'statusLabel'
                    },
                    {
                        label: this.$t('store.创建人'),
                        prop: 'creator'
                    },
                    {
                        label: this.$t('store.创建时间'),
                        prop: 'createTime'
                    }
                ]
            },
            tableData () {
                return this.versionList.map(item => ({
                    ...item,
                    statusLabel: this.$t(`store.${item.published ? '已发布' : '已下架'}`),
                    createTime: convertTime(item.createTime)
                    
                }))
            },
            offlineFormRules () {
                return {
                    reason: [
                        {
                            required: true,
                            message: this.$t('store.validateMessage', [this.$t('store.下架原因'), this.$t('store.必填项')]),
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },

        methods: {
            ...mapActions('store', [
                'offlineTemplate',
                'releaseTemplateVersion'
            ]),
            handlePageChange (page) {
                this.$emit('pageChanged', page)
            },
            handleLimitChange (currentLimit, prevLimit) {
                this.$emit('pageLimitChanged', currentLimit, prevLimit)
            },

            async submitOfflineTemplateVersion () {
                try {
                    const valid = await this.$refs.offlineForm.validate()
                    if (!valid) {
                        throw new Error(this.$t('store.校验失败，请修改再试'))
                    }
                    const { templateCode, version, reason } = this.offlineData.form

                    this.offlineData.isLoading = true
                    await this.offlineTemplate({
                        templateCode,
                        version,
                        reason
                    })
                    this.cancelOfflineTemplate()
                    this.$emit('pageChanged')
                } catch (err) {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                } finally {
                    this.offlineData.isLoading = false
                }
            },

            cancelOfflineTemplate () {
                this.offlineData.show = false
                this.offlineData.form = null
            },

            offline (row) {
                this.offlineData.show = true
                this.offlineData.form = row
            },

            async online (row) {
                this.$bkInfo({
                    title: this.$t('store.onlineTips', [row.versionName]),
                    confirmFn: async () => {
                        try {
                            await this.releaseTemplateVersion({
                                version: row.version,
                                templateCode: row.templateCode
                            })
                            this.$emit('pageChanged')
                        } catch (e) {
                            this.$bkMessage({
                                theme: 'error',
                                message: e.message || e
                            })
                        }
                    }
                })
            },

            toDetail () {
                this.$router.push({
                    name: 'details',
                    params: {
                        type: 'template',
                        code: this.$route.params.code
                    }
                })
            },
            toUpgrade () {
                this.$router.push({
                    name: 'editTemplate',
                    params: {
                        templateCode: this.$route.params.code
                    },
                    query: {
                        type: 'edit'
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .version-detail {
        padding: 20px;
    }
    .offline-template-version-form {
        padding: 24px;
    }
</style>
