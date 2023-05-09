<template>
    <article class="api-setting">
        <h5 class="api-header">
            <bk-input :placeholder="$t('store.请输入关键字搜索')" class="api-input" v-model="apiName" @input="getApiList" clearable></bk-input>
            <bk-button theme="primary" @click="showAddApi">{{ $t('store.申请API') }}</bk-button>
        </h5>

        <section v-bkloading="{ isLoading: isLoading || isCanceling }" class="g-scroll-pagination-table">
            <bk-table :data="apiList"
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#fff' }"
                v-if="!isLoading"
                :pagination="pagination"
                @page-change="pageChange"
                @page-limit-change="pageLimitChange"
            >
                <bk-table-column label="SDK API" prop="apiName" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.权限等级')" prop="apiLevel" :formatter="levelFormatter" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="$t('store.状态')" prop="apiStatus" show-overflow-tooltip>
                    <template slot-scope="props">
                        <span v-bk-tooltips="{ content: props.row.approveMsg, disabled: props.row.apiStatus !== 'REFUSE', width: 500 }" :class="props.row.apiStatus">
                            {{props.row.apiStatus | statusFilter}}
                        </span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.操作')" width="180" class-name="handler-btn">
                    <template slot-scope="props">
                        <bk-button text @click="handleApply(props.row)" v-if="['REFUSE'].includes(props.row.apiStatus)">{{ $t('store.申请') }}</bk-button>
                        <bk-button text @click="handleCancelApply(props.row)" v-if="props.row.apiStatus === 'WAIT'">{{ $t('store.撤单') }}</bk-button>
                    </template>
                </bk-table-column>
            </bk-table>

            <bk-sideslider :is-show.sync="showAdd" :quick-close="true" :title="$t('store.申请API')" :width="640" :before-close="closeAddApi">
                <bk-form :label-width="100" :model="apiObj" slot="content" class="add-api" ref="apiForm">
                    <bk-form-item label="API" :required="true" :rules="[requireRule('API')]" property="apiNameList" error-display-type="normal">
                        <bk-select v-model="apiObj.apiNameList" searchable multiple :loading="isLoadingUnApprovalApiList" @change="handleChangeForm">
                            <bk-option v-for="api in unApprovalApiList"
                                :key="api.apiName"
                                :id="api.apiName"
                                :name="api.aliasName">
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.使用场景')" :rules="[requireRule($t('store.使用场景'))]" :required="true" property="applyDesc" error-display-type="normal">
                        <bk-input type="textarea" :rows="3" v-model="apiObj.applyDesc" :placeholder="$t('store.请输入使用场景')" @change="handleChangeForm"></bk-input>
                    </bk-form-item>
                    <bk-form-item>
                        <bk-button theme="primary" @click="saveApi" :loading="isSaving">{{ $t('store.保存') }}</bk-button>
                        <bk-button @click="closeAddApi" :disabled="isSaving">{{ $t('store.取消') }}</bk-button>
                    </bk-form-item>
                </bk-form>
            </bk-sideslider>
        </section>
    </article>
</template>

<script>
    import api from '@/api'
    import { mapGetters } from 'vuex'

    export default {
        filters: {
            statusFilter (val) {
                const local = window.devops || {}
                const statusMap = {
                    WAIT: local.$t('store.待审批'),
                    PASS: local.$t('store.通过'),
                    REFUSE: local.$t('store.拒绝'),
                    CANCEL: local.$t('store.取消')
                }
                return statusMap[val]
            }
        },

        data () {
            return {
                apiList: [],
                unApprovalApiList: [],
                isLoading: true,
                isSaving: false,
                isLoadingUnApprovalApiList: false,
                isCanceling: false,
                showAdd: false,
                apiName: '',
                apiObj: {
                    apiNameList: [],
                    applyDesc: '',
                    language: ''
                },
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 10
                }
            }
        },

        computed: {
            ...mapGetters('store', {
                detail: 'getDetail'
            })
        },

        created () {
            this.getApiList()
        },

        methods: {
            requireRule (name) {
                return {
                    required: true,
                    message: this.$t('store.validateMessage', [name, this.$t('store.必填项')]),
                    trigger: 'blur'
                }
            },

            getApiList () {
                this.isLoading = true
                const params = {
                    apiName: this.apiName,
                    page: this.pagination.count,
                    pageSize: this.pagination.limit
                }
                api.requestSensitiveApiList('ATOM', this.detail.atomCode, params).then((res) => {
                    this.apiList = res.records || []
                }).catch(err => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.isLoading = false
                })
            },

            closeAddApi () {
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
                            this.apiObj = {
                                apiNameList: [],
                                applyDesc: '',
                                language: ''
                            }
                            setTimeout(() => {
                                this.showAdd = false
                            })
                            return true
                        }
                    })
                } else {
                    this.showAdd = false
                    this.apiObj = {
                        apiNameList: [],
                        applyDesc: '',
                        language: ''
                    }
                }
            },

            saveApi () {
                this.$refs.apiForm.validate().then(() => {
                    this.isSaving = true
                    api.requestApplySensitiveApi('ATOM', this.detail.atomCode, this.apiObj).then(() => {
                        this.apiObj = {
                            apiNameList: [],
                            applyDesc: '',
                            language: ''
                        }
                        setTimeout(() => {
                            this.showAdd = false
                        })
                        return this.getApiList()
                    }).catch((err) => {
                        this.$bkMessage({ message: (err.message || err), theme: 'error' })
                    }).finally(() => {
                        this.isSaving = false
                    })
                }, (validator) => {
                    this.$bkMessage({ message: validator.content || validator, theme: 'error' })
                })
            },

            handleApply (row) {
                this.apiObj.apiNameList = [row.apiName]
                this.apiObj.applyDesc = ''
                this.apiObj.language = this.detail.language
                this.showAddApi()
            },

            handleCancelApply (row) {
                this.isCanceling = true
                api.requestCancelSensitiveApi('ATOM', this.detail.atomCode, row.id).then((res) => {
                    return this.getApiList()
                }).catch((err) => {
                    this.$bkMessage({ message: (err.message || err), theme: 'error' })
                }).finally(() => {
                    this.isCanceling = false
                })
            },

            showAddApi () {
                window.changeFlag = false
                this.showAdd = true
                this.isLoadingUnApprovalApiList = true
                api.requestUnApprovalApiList('ATOM', this.detail.atomCode, { language: this.detail.language }).then((res) => {
                    this.unApprovalApiList = res || []
                }).catch((err) => {
                    this.$bkMessage({ message: (err.message || err), theme: 'error' })
                }).finally(() => {
                    this.isLoadingUnApprovalApiList = false
                })
            },

            pageChange (page) {
                if (page) this.pagination.current = page
                this.getApiList()
            },

            pageLimitChange (currentLimit, prevLimit) {
                if (currentLimit === this.pagination.limit) return

                this.pagination.current = 1
                this.pagination.limit = currentLimit
                this.getApiList()
            },

            levelFormatter (row, column, cellValue, index) {
                const levelMap = {
                    NORMAL: this.$t('store.普通'),
                    SENSITIVE: this.$t('store.敏感')
                }
                return levelMap[cellValue]
            },

            handleChangeForm () {
                window.changeFlag = true
            }
        }
    }
</script>

<style lang="scss" scoped>
    .api-setting {
        background: #fff;
        padding: 3.2vh;
        .api-header {
            margin-bottom: 3.2vh;
            color: #666;
            font-size: 14px;
            font-weight: normal;
            .api-input {
                width: 250px;
                margin-right: 14px;
            }
        }
        .add-api {
            padding: 32px;
        }
        .REFUSE {
            border-bottom: 1px dashed #63656e;
            display: inline-block;
        }
    }
</style>
