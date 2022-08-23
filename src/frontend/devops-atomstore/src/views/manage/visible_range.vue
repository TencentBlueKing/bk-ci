<template>
    <div class="visible-range-wrapper">
        <div class="inner-header">
            <div class="title"> {{ $t('store.可见范围') }} </div>
        </div>

        <section
            class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="visible-range-content" v-if="showContent && visibleList.length">
                <div class="handle-row" v-if="userInfo.isProjectAdmin">
                    <button class="bk-button bk-primary add-button" type="button" @click="addHandle()"> {{ $t('store.添加') }} </button>
                    <button class="bk-button bk-default" type="button" @click="bitchrRemove()"> {{ $t('store.批量删除') }} </button>
                </div>
                <bk-table style="margin-top: 15px;"
                    :empty-text="$t('store.未设置可见对象时，仅插件成员可以安装到名下项目中使用。设置可见对象后，对应用户可以在Store中查看并安装使用。')"
                    :data="visibleList"
                    @select="select"
                    @select-all="selectAll"
                >
                    <bk-table-column type="selection" width="70" align="center"></bk-table-column>
                    <bk-table-column :label="$t('store.可见对象')" prop="deptName"></bk-table-column>
                    <bk-table-column :label="$t('store.状态')">
                        <template slot-scope="props">
                            <span>{{ statusMap[props.row.status] }}</span>
                            <span class="audit-tips" v-if="props.row.status === 'APPROVING'"><i class="devops-icon icon-info-circle"></i> {{ $t('store.由蓝盾管理员审核') }} </span>
                            <span class="audit-tips" v-else>{{ props.row.comment }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('store.操作')" width="120" class-name="handler-btn">
                        <template slot-scope="props">
                            <span :class="[{ 'disable': !userInfo.isProjectAdmin } ,'update-btn']" @click="handleDelete(props.row)"> {{ $t('store.删除') }} </span>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
            <empty-tips v-if="showContent && !visibleList.length"
                :title="emptyTipsConfig.title"
                :desc="emptyTipsConfig.desc"
                :btns="emptyTipsConfig.btns">
            </empty-tips>
            <bk-tree
                ref="organizationTree"
                :data="treeList"
                :multiple="true"
                :node-key="'id'"
                :has-border="false">
            </bk-tree>
        </section>
        <organization-dialog :show-dialog="showDialog"
            :is-loading="isSaveOrg"
            @saveHandle="saveHandle"
            @cancelHandle="cancelHandle">
        </organization-dialog>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import emptyTips from '@/components/img-empty-tips'
    import organizationDialog from '@/components/organization-dialog'

    export default {
        components: {
            emptyTips,
            organizationDialog
        },
        data () {
            return {
                showContent: false,
                showDialog: false,
                isSaveOrg: false,
                visibleList: [],
                statusMap: {
                    'APPROVED': this.$t('store.审核通过'),
                    'APPROVING': this.$t('store.待审核'),
                    'REJECT': this.$t('store.审核驳回')
                },
                loading: {
                    isLoading: false,
                    title: ''
                }
            }
        },
        computed: {
            ...mapGetters('store', {
                'userInfo': 'getUserInfo'
            }),

            atomCode () {
                return this.$route.params.atomCode
            },

            emptyTipsConfig () {
                return {
                    title: this.$t('store.暂时没有设置可见范围'),
                    desc: this.$t('store.未设置可见对象时，仅插件成员可以安装到名下项目中使用。设置可见对象后，对应用户可以在Store中查看并安装使用。'),
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: () => this.addHandle(),
                            text: this.$t('store.添加'),
                            disable: !this.userInfo.isProjectAdmin
                        }
                    ]
                }
            }
        },

        async mounted () {
            await this.init()
        },
        methods: {
            async init () {
                this.loading.isLoading = true

                try {
                    await this.requestList()
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                        this.showContent = true
                    }, 100)
                }
            },
            async requestList () {
                try {
                    const res = await this.$store.dispatch('store/requestVisibleList', {
                        atomCode: this.atomCode
                    })
                    this.visibleList.splice(0, this.visibleList.length)
                    if (res.deptInfos) {
                        res.deptInfos.map(item => {
                            item.selected = false
                            this.visibleList.push(item)
                        })
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            addHandle () {
                this.showDialog = true
            },
            saveHandle (params) {
                params.atomCode = this.atomCode
                this.isSaveOrg = true

                this.$store.dispatch('store/setVisableDept', { params }).then(() => {
                    this.requestList()
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isSaveOrg = false
                    this.showDialog = false
                })
            },
            cancelHandle () {
                this.showDialog = false
            },
            bitchrRemove () {
                const target = this.visibleList.filter(val => {
                    if (val.selected) {
                        return val.deptId || val.deptId === 0
                    }
                })
                if (!target.length) {
                    this.$bkMessage({
                        message: this.$t('store.请至少选择一个可见对象'),
                        theme: 'error',
                        limit: 1
                    })
                } else {
                    const h = this.$createElement
                    const subHeader = h('p', {
                        style: {
                            textAlign: 'center'
                        }
                    }, this.$t('store.确定删除选中的可见对象？'))

                    this.$bkInfo({
                        title: this.$t('store.删除'),
                        subHeader,
                        confirmFn: async () => {
                            const deptIds = target.map(val => val.deptId).join(',')
                            this.requestDeleteVisiable(deptIds)
                        }
                    })
                }
            },
            select (selection, row) {
                row.selected = !row.selected
            },

            selectAll (selection = []) {
                this.visibleList.forEach((item) => {
                    const isSelected = selection.findIndex((x) => x.deptId === item.deptId) > -1
                    item.selected = isSelected
                })
            },
            async requestDeleteVisiable (deptIds) {
                let message, theme
                try {
                    await this.$store.dispatch('store/requestDeleteVisiable', {
                        atomCode: this.atomCode,
                        deptIds
                    })

                    message = this.$t('store.删除成功')
                    theme = 'success'
                    this.requestList()
                } catch (err) {
                    message = message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            handleDelete (row) {
                if (!this.userInfo.isProjectAdmin) return
                const h = this.$createElement
                const subHeader = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `${this.$t('store.确定删除')}(${row.deptName})？`)

                this.$bkInfo({
                    title: this.$t('store.删除'),
                    subHeader,
                    confirmFn: async () => {
                        this.requestDeleteVisiable(row.deptId)
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import './../../assets/scss/conf';
    .visible-range-wrapper {
        height: 100%;
        overflow: auto;
        .inner-header {
            display: flex;
            justify-content: space-between;
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
        .visible-range-content {
            height: 100%;
            padding: 20px;
            overflow: auto;
        }
        .tips-content {
            margin-bottom: 20px;
            padding: 16px 20px;
            border: 1px solid #DDE4EB;
            background: #fff;
            .tips-header {
                color: #4A4A4A;
            }
            .tips-item {
                margin-left: 22px;
            }
        }
        .handle-row {
            margin-bottom: 20px;
            .add-button {
                width: 96px;
            }
        }
        .visible-table {
            border: 1px solid $borderWeightColor;
            tbody {
                background-color: #fff;
            }
            th {
                height: 42px;
                padding: 2px 10px;
                color: #333C48;
                font-weight: normal;
                &:first-child {
                    padding-left: 20px;
                }
            }
            td {
                height: 42px;
                padding: 2px 10px;
                &:first-child {
                    padding-left: 20px;
                }
                &:last-child {
                    padding-right: 30px;
                }
            }
            .audit-tips {
                margin-left: 8px;
                color: #bcbcbc;
                cursor: default;
                i {
                    position: relative;
                    top: 1px;
                    margin-right: 2px;
                }
            }
            .handler-btn {
                span {
                    display: inline-block;
                    margin-right: 20px;
                    color: $primaryColor;
                    cursor: pointer;
                }
            }
            .no-data-row {
                padding: 40px;
                color: $fontColor;
                text-align: center;
            }
        }
    }
</style>
