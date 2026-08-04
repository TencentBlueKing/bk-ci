<template>
    <article class="visible-setting">
        <h5 class="visible-header">
            <bk-button
                theme="primary"
                @click="showDialog = true"
                :disabled="!userInfo.isProjectAdmin"
            >
                {{ isDevx ? $t('store.添加范围') : $t('store.添加') }}
            </bk-button>
            <bk-button
                @click="bitchrRemove"
                :disabled="!userInfo.isProjectAdmin"
            >
                {{ $t('store.批量删除') }}
            </bk-button>
        </h5>

        <section
            v-bkloading="{ isLoading }"
            class="visible-table"
        >
            <bk-table
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#fff' }"
                :empty-text="$t('store.未设置可见对象时，仅成员可以安装到名下项目中使用。设置可见对象后，对应用户可以在Store中查看并安装使用。')"
                :data="visibleList"
                :max-height="521"
                @select="select"
                @select-all="selectAll"
            >
                <bk-table-column
                    type="selection"
                    width="70"
                    align="center"
                ></bk-table-column>
                <bk-table-column
                    :label="$t('store.可见对象')"
                    prop="visibleName"
                >
                    <template slot-scope="props">
                        <span v-if="routeType !== 'devx'">{{ props.row.visibleName }}</span>
                        <p v-else>
                            <span :class="['type-tag', props.row.visibleType === 'dept' ? 'type-dept' : 'type-project']">
                                {{ props.row.visibleType === 'dept' ? $t('store.组织') : $t('store.项目') }}
                            </span>
                            <span>{{ props.row.visibleName }}</span>
                        </p>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.状态')">
                    <template slot-scope="props">
                        <span>{{ statusMap[props.row.status] }}</span>
                        <span
                            class="audit-tips"
                            v-if="props.row.status === 'APPROVING'"
                        ><i class="devops-icon icon-info-circle"></i> {{ $t('store.由蓝盾管理员审核') }} </span>
                        <span
                            class="audit-tips"
                            v-else
                        >{{ props.row.comment }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('store.操作')"
                    width="120"
                    class-name="handler-btn"
                >
                    <template slot-scope="props">
                        <span
                            :class="[{ 'disable': !userInfo.isProjectAdmin } ,'update-btn']"
                            @click="handleDelete(props.row)"
                        > {{ $t('store.删除') }} </span>
                    </template>
                </bk-table-column>
            </bk-table>
        </section>

        <VisibleRangeDialog
            :show-dialog="showDialog"
            :is-loading="isSaveOrg"
            :route-type="routeType"
            @saveHandle="saveHandle"
            @cancelHandle="cancelHandle"
        >
        </VisibleRangeDialog>

        <bk-dialog
            v-model="deleteObj.show"
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
    import VisibleRangeDialog from '@/components/VisibleRangeDialog'

    export default {
        components: {
            VisibleRangeDialog
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
            },
            routeType () {
                return this.$route.params.type
            },
            isDevx () {
                return this.routeType === 'devx'
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
                    service: () => this.$store.dispatch('store/requestServiceVisableList', this.detail.serviceCode),
                    devx: () => this.$store.dispatch('store/getVisibilitiesList', this.detail.storeCode)
                }
                
                this.isLoading = true
                initMethodMap[this.routeType]().then((res = {}) => {
                    let list = []
                    if (this.routeType === 'devx') {
                        // 云研发模式
                        const deptInfos = (res.deptInfos || []).map(item => ({
                            ...item,
                            visibleName: item.deptName,
                            visibleId: item.deptId,
                            visibleType: 'dept'
                        }))
                        const projectInfos = (res.projectInfos || []).map(item => ({
                            ...item,
                            visibleName: item.projectName,
                            visibleId: item.projectCode,
                            visibleType: 'project'
                        }))
                        list = [...deptInfos, ...projectInfos]
                    } else {
                        list = (res.deptInfos || []).map(item => ({
                            ...item,
                            visibleName: item.deptName,
                            visibleId: item.deptId
                        }))
                    }
                    this.visibleList = list.map((x) => {
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
                    const isSelected = selection.findIndex((x) => x.visibleId === item.visibleId) > -1
                    item.selected = isSelected
                })
            },

            saveHandle (params) {
                let method
                switch (this.routeType) {
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
                    case 'devx':
                        params.storeCode = this.detail.storeCode
                        params.storeType = 'DEVX'
                        method = () => this.$store.dispatch('store/addVisibilitiesList', params)
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
                const target = this.visibleList.filter(val => val.selected)
                if (!target.length) {
                    this.$bkMessage({
                        message: this.$t('store.请至少选择一个可见对象'),
                        theme: 'error',
                        limit: 1
                    })
                } else {
                    this.deleteObj.show = true
                    this.deleteObj.name = ''
                    this.deleteObj.target = target
                }
            },

            handleDelete (row) {
                if (!this.userInfo.isProjectAdmin) return
                this.deleteObj.show = true
                this.deleteObj.name = row.visibleName
                this.deleteObj.target = [row]
            },

            requestDeleteVisiable () {
                const target = this.deleteObj.target || []
                let promise

                // atom、template、image、service 四种类型的配置
                const deleteConfigMap = {
                    atom: { action: 'store/requestDeleteVisiable', codeField: 'atomCode' },
                    template: { action: 'store/deleteTplVisiable', codeField: 'templateCode' },
                    image: { action: 'store/requestDeleteImageVis', codeField: 'imageCode' },
                    service: { action: 'store/requestDeleteServiceVis', codeField: 'serviceCode' }
                }

                const config = deleteConfigMap[this.routeType]
                if (config) {
                    const deptIds = target.map(item => item.visibleId).join(',')
                    const params = { deptIds }
                    params[config.codeField] = this.detail[config.codeField]
                    promise = this.$store.dispatch(config.action, params)
                } else if (this.routeType === 'devx') {
                    // 云研发模式：分别处理组织和项目
                    const deptIds = target.filter(item => item.visibleType === 'dept').map(item => item.visibleId)
                    const projectCodes = target.filter(item => item.visibleType === 'project').map(item => item.visibleId)
                    const params = { storeCode: this.detail.storeCode }
                    if (deptIds.length) params.deptIds = deptIds
                    if (projectCodes.length) params.projectCodes = projectCodes
                    promise = this.$store.dispatch('store/deleteVisibilitiesList', params)
                }

                if (promise) {
                    this.deleteObj.loading = true
                    promise.then(() => {
                        this.requestList()
                        this.$bkMessage({ message: this.$t('store.删除成功'), theme: 'success' })
                    }).catch((err) => {
                        this.$bkMessage({ message: err.message || err, theme: 'error' })
                    }).finally(() => {
                        this.deleteObj.loading = false
                        this.deleteObj.show = false
                        this.deleteObj.target = null
                    })
                }
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
        .type-tag {
            display: inline-block;
            padding: 0 6px;
            border-radius: 2px;
            line-height: 16px;
            margin-right: 8px;
            font-size: 10px;
        }
        .type-dept {
            color: #1768EF;
            background-color: #E1ECFF;
        }
        .type-project {
            color: #299E56;
            background-color: #DAF6E5;
        }
    }
</style>
