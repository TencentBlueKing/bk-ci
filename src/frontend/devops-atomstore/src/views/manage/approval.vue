<template>
    <article class="manage-approve">
        <section class="version-content">
            <div class="version-info-header">
                <span class="info-title"> {{ $t('store.协作申请列表') }} </span>
            </div>
            <section v-bkloading="{ isLoading }" class="approval-table-contain g-scroll-pagination-table">
                <bk-table class="approval-table"
                    v-if="!isLoading"
                    :data="approveList"
                    :empty-text="$t('store.暂无申请者')"
                    :pagination="pagination"
                    @page-change="pageChanged"
                    @page-limit-change="pageCountChanged"
                    :outer-border="false"
                    :header-border="false"
                    :header-cell-style="{ background: '#fff' }"
                >
                    <bk-table-column :label="$t('store.申请人')" prop="applicant" show-overflow-tooltip></bk-table-column>
                    <bk-table-column :label="$t('store.审批状态')" prop="approveStatus" :formatter="statusFormatter" show-overflow-tooltip></bk-table-column>
                    <bk-table-column :label="$t('store.申请原因')" show-overflow-tooltip>
                        <template slot-scope="props">
                            <span class="table-text" :title="props.row.content">{{props.row.content}}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('store.审批结果说明')" show-overflow-tooltip>
                        <template slot-scope="props">
                            <span class="table-text" :title="props.row.approveMsg">{{props.row.approveMsg}}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('store.创建日期')" prop="createTime" :formatter="timeFormatter" show-overflow-tooltip></bk-table-column>
                    <bk-table-column :label="$t('store.更新日期')" prop="updateTime" :formatter="timeFormatter" show-overflow-tooltip></bk-table-column>
                    <bk-table-column :label="$t('store.操作')" width="120" class-name="handler-btn">
                        <template slot-scope="props">
                            <span class="update-btn" @click="approve(props.row)" v-if="props.row.approveStatus === 'WAIT'"> {{ $t('store.审批') }} </span>
                        </template>
                    </bk-table-column>
                </bk-table>
            </section>
        </section>

        <bk-sideslider :is-show.sync="approveRes.show" @hidden="clearFormData" :quick-close="true" :title="$t('store.审批')" width="565">
            <section slot="content" class="approve-form">
                <bk-form label-width="90" ref="validateForm" :model="approveRes">
                    <bk-form-item :label="$t('store.插件名称')">
                        {{detail.name}}
                    </bk-form-item>
                    <bk-form-item :label="$t('store.申请人')">
                        {{approveRes.applicant}}
                    </bk-form-item>
                    <bk-form-item :label="$t('store.申请原因')">
                        {{approveRes.content}}
                    </bk-form-item>
                    <bk-form-item :label="$t('store.创建日期')">
                        {{timeFormatter({}, {}, approveRes.createTime)}}
                    </bk-form-item>
                    <bk-form-item :label="$t('store.审批结果')" :required="true">
                        <bk-radio-group v-model="approveRes.approveStatus">
                            <bk-radio value="PASS"> {{ $t('store.通过') }} </bk-radio>
                            <bk-radio value="REFUSE"> {{ $t('store.拒绝') }} </bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                    <bk-form-item :label="$t('store.审批原因')" :required="true" :rules="[{ required: true, message: $t('store.必填项') , trigger: 'change' }]" property="approveMsg" error-display-type="normal">
                        <bk-input type="textarea" v-model="approveRes.approveMsg" :placeholder="$t('store.请输入审批原因')"></bk-input>
                    </bk-form-item>
                </bk-form>
                <form-tips :prompt-list="[$t('store.同意协作后，协作者将成为插件开发人员，可以：'), $t('store.1、修改插件代码'), $t('store.2、修改插件私有配置'), $t('store.3、提交版本升级插件'), $t('store.4、在协作者自己的调试项目下使用测试版本')]"></form-tips>
                <div class="approve-button">
                    <bk-button theme="primary" @click="confirmApprove" :loading="isApproving"> {{ $t('store.确认') }} </bk-button>
                    <bk-button @click="clearFormData" :disabled="isApproving"> {{ $t('store.取消') }} </bk-button>
                </div>
            </section>
        </bk-sideslider>
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'
    import { convertTime } from '@/utils/index'
    import formTips from '@/components/common/formTips/index'

    export default {
        components: {
            formTips
        },

        data () {
            return {
                approveList: [],
                isLoading: false,
                isApproving: false,
                approveRes: {
                    show: false,
                    approveStatus: 'PASS',
                    approveMsg: '',
                    approveId: ''
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
            this.requestApproveList()
        },

        methods: {
            confirmApprove () {
                this.$refs.validateForm.validate().then((validator) => {
                    const { approveId, approveMsg, approveStatus } = this.approveRes
                    this.isApproving = true
                    this.$store.dispatch('store/approval', { type: 'ATOM', code: this.detail.atomCode, approveId, approveMsg, approveStatus }).then((res) => {
                        if (res) {
                            const currentData = this.approveList.find(item => item.approveId === this.approveRes.approveId) || {}
                            currentData.approveStatus = approveStatus
                            this.clearFormData()
                            this.$bkMessage({ message: this.$t('store.审批成功'), theme: 'success' })
                            this.requestApproveList()
                        }
                    }).catch(err => this.$bkMessage({ message: (err.message || err), theme: 'error' })).finally(() => {
                        this.isApproving = false
                    })
                }, (validator) => {
                    this.$bkMessage({ message: validator.content || validator, theme: 'error' })
                })
            },

            clearFormData () {
                this.approveRes.show = false
                this.approveRes.approveStatus = 'PASS'
                this.approveRes.approveMsg = ''
                this.approveRes.approveId = ''
            },

            approve (row) {
                this.approveRes.show = true
                Object.assign(this.approveRes, row)
                this.approveRes.approveStatus = 'PASS'
            },

            requestApproveList () {
                this.isLoading = true
                const data = Object.assign({}, this.pagination, { type: 'ATOM', code: this.detail.atomCode })
                this.$store.dispatch('store/getApprovalList', data).then((res) => {
                    this.approveList = res.records || []
                    this.pagination.count = res.count
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isLoading = false))
            },

            pageCountChanged (currentLimit, prevLimit) {
                if (currentLimit === this.pagination.limit) return

                this.pagination.current = 1
                this.pagination.limit = currentLimit
                this.requestApproveList()
            },

            pageChanged (page) {
                this.pagination.current = page
                this.requestApproveList()
            },

            statusFormatter (obj, con, val) {
                let str = this.$t('store.待审批')
                switch (val) {
                    case 'PASS':
                        str = this.$t('store.通过')
                        break
                    case 'REFUSE':
                        str = this.$t('store.拒绝')
                        break
                }
                return str
            },

            timeFormatter (obj, con, val) {
                return convertTime(val)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .manage-approve {
        background: #fff;
    }
    .version-content {
        padding: 3.2vh 3.2vh 1.7vh;
        height: 100%;
        .version-info-header {
            line-height: 20px;
        }
        .approval-table-contain {
            height: calc(100% - 20px - 3.2vh);
        }
        .approval-table {
            margin-top: 3.2vh;
        }
    }
    .approve-form {
        padding: 20px 30px;
        .approve-button {
            display: flex;
            justify-content: center;
            .bk-primary {
                margin-left: 20px;
            }
        }
    }
    ::v-deep .bk-form-radio {
        margin-left: 10px;
        &:first-child {
            margin-left: 0px;
        }
    }
    ::v-deep .form-tips {
        margin: 20px 0;
        p.tips-body {
            text-align: left;
        }
    }
</style>
