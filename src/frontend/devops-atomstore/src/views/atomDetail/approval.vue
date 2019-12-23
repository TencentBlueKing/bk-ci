<template>
    <article class="atom-information-wrapper">
        <div class="inner-header">
            <div class="title"> {{ $t('插件审批') }} </div>
        </div>
        <section class="version-content" v-bkloading="{ isLoading }">
            <template v-if="!isLoading">
                <div class="version-info-header">
                    <span class="info-title"> {{ $t('协作申请列表') }} </span>
                </div>
                <bk-table class="approval-table" :data="approveList" :empty-text="$t('暂无申请者')" :pagination="pagination" @page-change="pageChanged" @page-limit-change="pageCountChanged">
                    <bk-table-column :label="$t('申请人')" prop="applicant"></bk-table-column>
                    <bk-table-column :label="$t('审批状态')" prop="approveStatus" :formatter="statusFormatter"></bk-table-column>
                    <bk-table-column :label="$t('申请原因')">
                        <template slot-scope="props">
                            <span class="table-text" :title="props.row.content">{{props.row.content}}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('审批结果说明')">
                        <template slot-scope="props">
                            <span class="table-text" :title="props.row.approveMsg">{{props.row.approveMsg}}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('创建日期')" prop="createTime" :formatter="timeFormatter"></bk-table-column>
                    <bk-table-column :label="$t('更新日期')" prop="updateTime" :formatter="timeFormatter"></bk-table-column>
                    <bk-table-column :label="$t('操作')" width="120" class-name="handler-btn">
                        <template slot-scope="props">
                            <span class="update-btn" @click="approve(props.row)" v-if="props.row.approveStatus === 'WAIT'"> {{ $t('审批') }} </span>
                        </template>
                    </bk-table-column>
                </bk-table>
            </template>
        </section>

        <bk-sideslider :is-show.sync="approveRes.show" @hidden="clearFormData" :quick-close="true" :title="$t('审批')" width="565">
            <section slot="content" class="approve-form">
                <bk-form label-width="90" ref="validateForm" :model="approveRes">
                    <bk-form-item :label="$t('插件名称')">
                        {{currentAtom.name}}
                    </bk-form-item>
                    <bk-form-item :label="$t('申请人')">
                        {{approveRes.applicant}}
                    </bk-form-item>
                    <bk-form-item :label="$t('申请原因')">
                        {{approveRes.content}}
                    </bk-form-item>
                    <bk-form-item :label="$t('创建日期')">
                        {{timeFormatter({}, {}, approveRes.createTime)}}
                    </bk-form-item>
                    <bk-form-item :label="$t('审批结果')" :required="true">
                        <bk-radio-group v-model="approveRes.approveStatus">
                            <bk-radio value="PASS"> {{ $t('通过') }} </bk-radio>
                            <bk-radio value="REFUSE"> {{ $t('拒绝') }} </bk-radio>
                        </bk-radio-group>
                    </bk-form-item>
                    <bk-form-item :label="$t('审批原因')" :required="true" :rules="[{ required: true, message: $t('必填项') , trigger: 'change' }]" property="approveMsg">
                        <bk-input type="textarea" v-model="approveRes.approveMsg" :placeholder="$t('请输入审批原因')"></bk-input>
                    </bk-form-item>
                </bk-form>
                <form-tips :prompt-list="[$t('同意协作后，协作者将成为插件开发人员，可以：'), $t('1、修改插件代码'), $t('2、修改插件私有配置'), $t('3、提交版本升级插件'), $t('4、在协作者自己的调试项目下使用测试版本')]"></form-tips>
                <div class="approve-button">
                    <bk-button @click="clearFormData"> {{ $t('取消') }} </bk-button>
                    <bk-button theme="primary" @click="confirmApprove"> {{ $t('确认') }} </bk-button>
                </div>
            </section>
        </bk-sideslider>
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'
    import { convertTime } from '../../utils/index'
    import formTips from '@/components/common/formTips/index'

    export default {
        components: {
            formTips
        },

        data () {
            return {
                approveList: [],
                isLoading: false,
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
                'currentAtom': 'getCurrentAtom'
            }),

            atomCode () {
                return this.$route.params.atomCode
            }
        },

        created () {
            this.requestApproveList()
        },

        methods: {
            confirmApprove () {
                this.$refs.validateForm.validate().then((validator) => {
                    const { approveId, approveMsg, approveStatus } = this.approveRes
                    this.$store.dispatch('store/approval', { atomCode: this.atomCode, approveId, approveMsg, approveStatus }).then((res) => {
                        if (res) {
                            const currentData = this.approveList.find(item => item.approveId === this.approveRes.approveId) || {}
                            currentData.approveStatus = approveStatus
                            this.clearFormData()
                            this.$bkMessage({ message: this.$t('审批成功'), theme: 'success' })
                            this.requestApproveList()
                        }
                    }).catch(err => this.$bkMessage({ message: (err.message || err), theme: 'error' }))
                }, () => {})
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
                const data = Object.assign({}, this.pagination, { atomCode: this.atomCode })
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
                let str = this.$t('待审批')
                switch (val) {
                    case 'PASS':
                        str = this.$t('通过')
                        break
                    case 'REFUSE':
                        str = this.$t('拒绝')
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
    @import './../../assets/scss/conf';

    %flex {
        display: flex;
        justify-content: space-between;
    }
    .atom-information-wrapper {
        overflow: auto;
        .inner-header {
            @extend %flex;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                font-size: 16px;
            }
        }
        .version-content {
            padding: 20px;
            .version-info-header {
                line-height: 20px;
            }
            .approval-table {
                margin-top: 15px;
                /deep/ .bk-table-body-wrapper {
                    max-height: calc(100vh - 291px);
                    overflow-y: auto;
                }
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
    }
    /deep/ .bk-form .bk-label {
        line-height: 1.5;
    }
    /deep/ .bk-form-radio {
        margin-left: 10px;
        &:first-child {
            margin-left: 0px;
        }
    }
    /deep/ .form-tips {
        margin: 20px 0;
        p.tips-body {
            text-align: left;
        }
    }
</style>
