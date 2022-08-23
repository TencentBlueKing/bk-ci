<template>
    <article class="visible-setting">
        <h5 class="visible-header">
            <bk-button theme="primary" @click="showDialog = true" :disabled="!userInfo.isProjectAdmin">{{ $t('store.添加') }}</bk-button>
            <bk-button @click="bitchrRemove" :disabled="!userInfo.isProjectAdmin">{{ $t('store.批量删除') }}</bk-button>
        </h5>

        <section v-bkloading="{ isLoading }" class="visible-table">
            <bk-table :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#fff' }"
                :empty-text="$t('store.未设置可见对象时，仅成员可以安装到名下项目中使用。设置可见对象后，对应用户可以在Store中查看并安装使用。')"
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
        </section>

        <organization-dialog :show-dialog="showDialog"
            :is-loading="isSaveOrg"
            @saveHandle="saveHandle"
            @cancelHandle="cancelHandle">
        </organization-dialog>

        <bk-dialog v-model="deleteObj.show"
            :loading="deleteObj.loading"
            @confirm="requestDeleteVisiable"
            @cancel="deleteObj.show = false"
            :title="$t('store.删除')"
        >
            {{ deleteObj.name ? `${$t('store.确定删除')}(${deleteObj.name})？` : $t('store.确定删除选中的可见对象？') }}
        </bk-dialog>
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'
    import organizationDialog from '@/components/organization-dialog'

    export default {
        components: {
            organizationDialog
        },

        data () {
            return {
                isLoading: true,
                isSaveOrg: false,
                showDialog: false,
                visibleList: [],
                statusMap: {
                    APPROVED: this.$t('store.审核通过'),
                    APPROVING: this.$t('store.待审核'),
                    REJECT: this.$t('store.审核驳回')
                },
                deleteObj: {
                    show: false,
                    loading: false,
                    name: '',
                    id: '',
                    index: ''
                }
            }
        },

        computed: {
            ...mapGetters('store', {
                detail: 'getDetail',
                userInfo: 'getUserInfo'
            }),

            isEnterprise () {
                return VERSION_TYPE === 'ee'
            }
        },

        created () {
            this.requestList()
        },

        methods: {
            requestList () {
                const initMethodMap = {
                    atom: () => this.$store.dispatch('store/requestVisibleList', { atomCode: this.detail.atomCode }),
                    template: () => this.$store.dispatch('store/requesttplVisibleList', { templateCode: this.detail.templateCode }),
                    image: () => this.$store.dispatch('store/requestImageVisableList', this.detail.imageCode),
                    service: () => this.$store.dispatch('store/requestServiceVisableList', this.detail.serviceCode)
                }
                const type = this.$route.params.type
                this.isLoading = true
                initMethodMap[type]().then((res = {}) => {
                    const deptInfos = res.deptInfos || []
                    this.visibleList = deptInfos.map((x) => {
                        x.selected = false
                        return x
                    })
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isLoading = false
                })
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

            saveHandle (params) {
                const type = this.$route.params.type
                let method
                switch (type) {
                    case 'atom':
                        params.atomCode = this.detail.atomCode
                        method = () => this.$store.dispatch('store/setVisableDept', { params })
                        break
                    case 'template':
                        params.templateCode = this.detail.templateCode
                        method = () => this.$store.dispatch('store/setTplVisableDept', { params })
                        break
                    case 'image':
                        params.imageCode = this.detail.imageCode
                        method = () => this.$store.dispatch('store/setImageVisableDept', { params })
                        break
                    case 'service':
                        params.serviceCode = this.detail.serviceCode
                        method = () => this.$store.dispatch('store/setServiceVisableDept', { params })
                        break
                }
                this.isSaveOrg = true
                method().then(() => {
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
                    return false
                })
                if (!target.length) {
                    this.$bkMessage({
                        message: this.$t('store.请至少选择一个可见对象'),
                        theme: 'error',
                        limit: 1
                    })
                } else {
                    this.deleteObj.show = true
                    this.deleteObj.name = ''
                    this.deleteObj.id = target.map(val => val.deptId).join(',')
                }
            },

            handleDelete (row) {
                if (!this.userInfo.isProjectAdmin) return
                this.deleteObj.show = true
                this.deleteObj.name = row.deptName
                this.deleteObj.id = row.deptId
            },

            requestDeleteVisiable () {
                const deptIds = this.deleteObj.id
                const deleteMethodMap = {
                    atom: () => this.$store.dispatch('store/requestDeleteVisiable', { atomCode: this.detail.atomCode, deptIds }),
                    template: () => this.$store.dispatch('store/deleteTplVisiable', { templateCode: this.detail.templateCode, deptIds }),
                    image: () => this.$store.dispatch('store/requestDeleteImageVis', { imageCode: this.detail.imageCode, deptIds }),
                    service: () => this.$store.dispatch('store/requestDeleteServiceVis', { serviceCode: this.detail.serviceCode, deptIds })
                }
                const type = this.$route.params.type
                this.deleteObj.loading = true
                deleteMethodMap[type]().then(() => {
                    (String(deptIds).split(',')).forEach(id => {
                        const index = this.visibleList.findIndex(x => String(x.deptId) === String(id))
                        this.visibleList.splice(index, 1)
                    })
                    this.$bkMessage({ message: this.$t('store.删除成功'), theme: 'success' })
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.deleteObj.loading = false
                    this.deleteObj.show = false
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .visible-setting {
        background: #fff;
        padding: 3.2vh;
        overflow: auto;
        .visible-header {
            font-weight: normal;
            margin-bottom: 3.2vh;
        }
        .visible-table {
            height: calc(100% - 32px - 3.2vh);
            ::v-deep.bk-table {
                height: 100%;
                .bk-table-body-wrapper {
                    max-height: calc(100% - 43px);
                    overflow-y: auto;
                }
            }
        }
        .disable {
            cursor: not-allowed;
            color: #bcbcbc;
        }
    }
</style>
