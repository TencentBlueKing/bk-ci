<template>
    <div
        class="release-list-wrapper"
        v-bkloading="{ isLoading: loading.isLoading, title: loading.title }"
    >
        <content-header>
            <div slot="left">{{ $route.meta.title }}</div>
        </content-header>
        <keep-alive>
            <section class="sub-view-port">
                <div class="filter-warpper">
                    <bk-checkbox
                        class="mr15 filter-checkbox"
                        :checked="getIsShowExpired"
                        @change="toggleExpired"
                    >
                        显示已过期体验
                    </bk-checkbox>
                    <div class="date-prepend">
                        发起时间
                    </div>
                    <bk-date-picker
                        class="date-picker mr15"
                        :value="createDaterange"
                        type="datetimerange"
                        placeholder="选择日期范围"
                        :options="{
                            disabledDate: time => time.getTime() > Date.now()
                        }"
                        @clear="handleClearCreateDate"
                        @change="handleChangeCreateDate"
                        @pick-success="handlePickSuccessCreateDate"
                    ></bk-date-picker>
    
                    <div class="date-prepend">
                        结束时间
                    </div>
                    <bk-date-picker
                        class="date-picker mr15"
                        :value="endDaterange"
                        type="datetimerange"
                        placeholder="选择日期范围"
                        @clear="handleClearEndDate"
                        @change="handleChangeEndDate"
                        @pick-success="handlePickSuccessEndDate"
                    ></bk-date-picker>
                    <search-select
                        v-model="searchValue"
                        clearable
                        filter
                        class="experience-search-input"
                        :data="searchList"
                        :show-condition="false"
                        placeholder="文件名 / 版本号 / 版本标题 / 版本描述 / 分组标识 / 应用名称 / 平台 / 发布人"
                    ></search-select>
                </div>
                <bk-table
                    :data="releaseList"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @page-limit-change="handlePageLimitChange"
                    v-bkloading="{ isLoading: isTableLoading }"
                >
                    <bk-table-column
                        label="文件名（版本号）"
                        prop="name"
                        min-width="150"
                    >
                        <template slot-scope="props">
                            <i
                                v-if="props.row.expired"
                                class="devops-icon icon-expired-experience"
                            ></i>
                            <span
                                class="link-text"
                                :title="`${props.row.name}（ ${props.row.version} ）`"
                                @click="toRowDetail(props.row)"
                            >{{ props.row.name }}（{{ props.row.version }}）</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        label="版本描述"
                        prop="remark"
                        min-width="100"
                    ></bk-table-column>
                    <bk-table-column
                        label="平台"
                        prop="platformLabel"
                    ></bk-table-column>
                    <bk-table-column
                        label="来源"
                        prop="sourceLabel"
                    ></bk-table-column>
                    <bk-table-column
                        label="发布人"
                        prop="creator"
                    ></bk-table-column>
                    <bk-table-column
                        label="体验结束时间"
                        prop="formatExpireDate"
                    ></bk-table-column>
                    <bk-table-column
                        label="操作"
                        width="150"
                    >
                        <template slot-scope="props">
                            <div class="operate-cell">
                                <template v-if="!props.row.expired && props.row.online">
                                    <span
                                        v-if="!props.row.permissions.canExperience "
                                        v-bk-tooltips="{ content: '你没有该版本的体验权限' }"
                                        class="devops-icon icon-qrcode"
                                    ></span>
                                    <bk-popover
                                        placement="left"
                                        theme="light"
                                        v-if="props.row.permissions.canExperience"
                                    >
                                        <i
                                            class="devops-icon icon-qrcode"
                                            @mouseover="requestUrl(props.row)"
                                        ></i>
                                        <p
                                            slot="content"
                                            class="qrcode-box"
                                            v-if="props.row.permissions.canExperience"
                                            v-bkloading="{ isLoading: !curIndexItemUrl }"
                                        >
                                            <qrcode
                                                class="qrcode-view"
                                                :text="curIndexItemUrl"
                                                :size="100"
                                            ></qrcode>
                                        </p>
                                    </bk-popover>
                                </template>
                                <i
                                    v-bk-tooltips="{ content: '该体验已过期' }"
                                    class="devops-icon icon-qrcode expired-text"
                                    v-else
                                ></i>
                                <bk-button
                                    v-perm="{
                                        hasPermission: props.row.permissions.canEdit,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId: projectId,
                                            resourceType: EXPERIENCE_TASK_RESOURCE_TYPE,
                                            resourceCode: props.row.experienceHashId,
                                            action: EXPERIENCE_TASK_RESOURCE_ACTION.EDIT
                                        }
                                    }"
                                    text
                                    @click.stop="toEditRow(props.row)"
                                >
                                    编辑
                                </bk-button>
                                <template v-if="props.row.online && !props.row.expired">
                                    <bk-button
                                        class="operate-btn"
                                        v-perm="{
                                            hasPermission: props.row.permissions.canDelete,
                                            disablePermissionApi: true,
                                            tooltips: '没有权限',
                                            permissionData: {
                                                projectId: projectId,
                                                resourceType: EXPERIENCE_TASK_RESOURCE_TYPE,
                                                resourceCode: props.row.experienceHashId,
                                                action: EXPERIENCE_TASK_RESOURCE_ACTION.DELETE
                                            }
                                        }"
                                        text
                                        @click.stop="toDropOff(props.row)"
                                    >
                                        下架
                                    </bk-button>
                                    <span
                                        v-bk-tooltips="{ content: '删除之前，请先下架体验' }"
                                        class="expired-text"
                                    >删除</span>
                                </template>
                                <template v-else>
                                    <span
                                        v-bk-tooltips="{ content: '该体验已下架' }"
                                        class="expired-text"
                                    >下架</span>
                                    <bk-button
                                        class="operate-btn"
                                        v-perm="{
                                            hasPermission: props.row.permissions.canDelete,
                                            disablePermissionApi: true,
                                            tooltips: '没有权限',
                                            permissionData: {
                                                projectId: projectId,
                                                resourceType: EXPERIENCE_TASK_RESOURCE_TYPE,
                                                resourceCode: props.row.experienceHashId,
                                                action: EXPERIENCE_TASK_RESOURCE_ACTION.DELETE
                                            }
                                        }"
                                        text
                                        @click.stop="toDeleteRow(props.row)"
                                    >
                                        删除
                                    </bk-button>
                                </template>
                            </div>
                        </template>
                    </bk-table-column>
                    <template #empty>
                        <empty-data
                            :empty-info="emptyInfo"
                            :to-create-fn="toCreateFn"
                        >
                        </empty-data>
                    </template>
                </bk-table>
            </section>
        </keep-alive>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import { convertTime } from '@/utils/util'
    import qrcode from '@/components/devops/qrcode'
    import emptyData from './empty-data'
    import { EXPERIENCE_TASK_RESOURCE_TYPE, EXPERIENCE_TASK_RESOURCE_ACTION } from '@/utils/permission'
    import '@blueking/search-select/dist/styles/index.css'

    export default {
        components: {
            emptyData,
            qrcode,
            SearchSelect: () => import('@blueking/search-select')
        },
        data () {
            const { projectId } = this.$route.params
            return {
                EXPERIENCE_TASK_RESOURCE_TYPE,
                EXPERIENCE_TASK_RESOURCE_ACTION,
                showContent: false,
                curIndexItemUrl: '',
                defaultCover: require('@/images/qrcode_app.png'),
                releaseList: [],
                totalList: [],
                loading: {
                    isLoading: false,
                    title: ''
                },
                emptyInfo: {
                    title: '暂无体验',
                    desc: '您可以在新增体验中新增一个体验任务',
                    permissionData: {
                        projectId: projectId,
                        resourceType: EXPERIENCE_TASK_RESOURCE_TYPE,
                        resourceCode: projectId,
                        action: EXPERIENCE_TASK_RESOURCE_ACTION.CREATE
                    }
                },
                pagination: {
                    current: 1,
                    limit: 20,
                    count: 0
                },
                modelValue: ['now-2d/d', 'now'],
                timezone: 'Asia/Shanghai',
                filterParams: {
                    createDateBegin: '',
                    createDateEnd: '',
                    endDateBegin: '',
                    endDateEnd: '',
                    name: '',
                    version: '',
                    remark: '',
                    versionTitle: '',
                    creator: ''
                },
                searchValue: [],
                createDaterange: ['', ''],
                createDaterangeCache: ['', ''],
                endDaterange: ['', ''],
                endDaterangeCache: ['', ''],
                isTableLoading: false
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            ...mapGetters('experience', [
                'getIsShowExpired'
            ]),
            searchList () {
                const list = [
                    {
                        name: '文件名',
                        default: true,
                        id: 'name'
                    },
                    {
                        name: '版本号',
                        id: 'version'
                    },
                    {
                        name: '版本标题',
                        id: 'versionTitle'
                    },
                    {
                        name: '版本描述',
                        id: 'remark'
                    },
                    {
                        name: '分组标识',
                        id: 'classify'
                    },
                    {
                        name: '应用名称',
                        id: 'experienceName'
                    },
                    {
                        name: '平台',
                        id: 'platform',
                        children: [
                            {
                                name: 'Android',
                                id: 'ANDROID'
                            },
                            {
                                name: 'IOS',
                                id: 'IOS'
                            },
                            {
                                name: 'HarmonyOS Next',
                                id: 'HAP'
                            }
                        ]
                    },
                    {
                        name: '发布人',
                        id: 'creator'
                    }
                ]
                return list.filter((data) => {
                    return !this.searchValue.find(val => val.id === data.id)
                })
            }
        },
        watch: {
            projectId () {
                this.init()
            },
            getIsShowExpired () {
                this.requestList()
            },
            searchValue (val) {
                this.requestList()
            }
        },
        async mounted () {
            await this.init()
        },
        methods: {
            async init () {
                const {
                    loading
                } = this

                loading.isLoading = true
                loading.title = '数据加载中，请稍候'

                try {
                    await this.requestList()
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.showContent = true
                    this.loading.isLoading = false
                }
            },
            /**
             * 获取发布列表
             */
            async requestList (reset = true) {
                try {
                    this.isTableLoading = true
                    const filterParams = {}
                    this.searchValue.forEach(item => {
                        const id = item.id
                        const value = item.values[0].id
                        filterParams[id] = value
                    })
                    const res = await this.$store.dispatch('experience/requestExpList', {
                        projectId: this.projectId,
                        params: {
                            expired: this.getIsShowExpired,
                            createDateBegin: String(this.createDaterange[0])?.slice(0, 10) ?? '',
                            createDateEnd: String(this.createDaterange[1])?.slice(0, 10) ?? '',
                            endDateBegin: String(this.endDaterange[0])?.slice(0, 10) ?? '',
                            endDateEnd: String(this.endDaterange[1])?.slice(0, 10) ?? '',
                            ...filterParams
                        }
                    })
                    this.isTableLoading = false
                    
                    const platformLabelMap = {
                        ANDROID: 'Android',
                        IOS: 'iOS',
                        HAP: 'HarmonyOS Next'
                    }
                    const sourceLabelMap = {
                        PIPELINE: '流水线',
                        WEB: '手动创建'
                    }
                    
                    this.totalList = res.map(item => ({
                        ...item,
                        platformLabel: platformLabelMap[item.platform],
                        sourceLabel: sourceLabelMap[item.source],
                        formatExpireDate: this.localConvertTime(item.expireDate).split(' ')[0]
                    }))
                    this.pagination.count = this.totalList.length
                    const start = reset ? 0 : (this.pagination.current - 1) * this.pagination.limit
                    const end = start + this.pagination.limit
                    if (reset) {
                        this.pagination.current = 1
                    }
                    this.releaseList = this.totalList.slice(start, end)
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                    return []
                }
            },
            handlePageChange (page) {
                this.pagination.current = page
                const start = (page - 1) * this.pagination.limit
                const end = start + this.pagination.limit
                
                this.releaseList = this.totalList.slice(start, end)
            },
            handlePageLimitChange (limit) {
                this.pagination.limit = limit
                this.pagination.current = 1
                const start = 0
                const end = start + limit
                this.releaseList = this.totalList.slice(start, end)
            },
            async requestUrl (row) {
                this.curIndexItemUrl = ''
                if (row.permissions.canExperience) {
                    try {
                        const res = await this.$store.dispatch('experience/requestExternalUrl', {
                            projectId: this.projectId,
                            experienceHashId: row.experienceHashId
                        })

                        this.curIndexItemUrl = res.url
                    } catch (err) {
                        const message = err.message ? err.message : err
                        const theme = 'error'

                        this.$bkMessage({
                            message,
                            theme
                        })
                    }
                }
            },
            toRowDetail (row) {
                this.$router.push({
                    name: 'experienceDetail',
                    params: {
                        projectId: this.projectId,
                        experienceId: row.experienceHashId,
                        type: 'detail'
                    }
                })
            },
            toEditRow (row) {
                if (row.permissions.canEdit) {
                    this.$router.push({
                        name: 'editExperience',
                        params: {
                            projectId: this.projectId,
                            experienceId: row.experienceHashId
                        }
                    })
                }
            },
            async toDropOff (row) {
                if (row.permissions.canEdit) {
                    this.$bkInfo({
                        title: '确认',
                        subTitle: '确认下架该体验',
                        confirmFn: async () => {
                            let message, theme

                            try {
                                await this.$store.dispatch('experience/toOfflineExp', {
                                    projectId: this.projectId,
                                    experienceHashId: row.experienceHashId
                                })

                                message = '下架成功'
                                theme = 'success'
                            } catch (err) {
                                message = err.data ? err.data.message : err
                                theme = 'error'
                            } finally {
                                this.$bkMessage({
                                    message,
                                    theme
                                })

                                this.requestList(false)
                            }
                        }
                    })
                }
            },
            async toDeleteRow (row) {
                if (row.permissions.canDelete) {
                    this.$bkInfo({
                        title: '确认',
                        subTitle: '确认删除该体验',
                        confirmFn: async () => {
                            let message, theme

                            try {
                                await this.$store.dispatch('experience/deleteExp', {
                                    projectId: this.projectId,
                                    experienceHashId: row.experienceHashId
                                })

                                message = '删除成功'
                                theme = 'success'
                            } catch (err) {
                                message = err.data ? err.data.message : err
                                theme = 'error'
                            } finally {
                                this.$bkMessage({
                                    message,
                                    theme
                                })

                                this.requestList(false)
                            }
                        }
                    })
                }
            },
            toggleExpired (isExpired) {
                this.$store.dispatch('experience/updateIsExpired', {
                    isExpired
                })
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp * 1000)
            },
            toCreateFn () {
                this.$router.push({
                    name: 'createExperience',
                    params: {
                        projectId: this.projectId
                    }
                })
            },
            // 体验发起时间
            handleClearCreateDate () {
                this.createDaterange = ['', '']
                this.requestList()
            },
            
            handleChangeCreateDate (date, type) {
                const startTime = new Date(date[0]).getTime() || ''
                const endTime = new Date(date[1]).getTime() || ''
                this.createDaterangeCache = [startTime, endTime]
            },
            
            handlePickSuccessCreateDate () {
                this.createDaterange = this.createDaterangeCache
                this.requestList()
            },

            // 体验结束时间
            handleClearEndDate () {
                this.endDaterange = ['', '']
                this.requestList()
            },
            
            handleChangeEndDate (date, type) {
                const startTime = new Date(date[0]).getTime() || ''
                const endTime = new Date(date[1]).getTime() || ''
                this.endDaterangeCache = [startTime, endTime]
            },
            
            handlePickSuccessEndDate () {
                this.endDaterange = this.endDaterangeCache
                this.requestList()
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import './../../scss/conf';

    .release-list-wrapper {
        height: 100%;
        overflow: hidden;
        .link-text {
            cursor: pointer;
            color: $primaryColor;
        }
        .devops-icon.icon-expired-experience{
            position: absolute;
            left: -3px;
            top: -3px;
            font-size: 36px;
            color: $fontLighterColor;
        }
        .operate-cell {
            display: flex;
            align-items: center;
            color: $primaryColor;
            .bk-tooltip {
                height: 12px;
            }
            > span {
                margin-left: 12px;
                &:first-child {
                    margin-left: 0;
                }
            }
            .expired-text {
                cursor: default;
                color: $fontLighterColor;
            }
            .icon-qrcode {
                margin-right: 10px;
            }
            .operate-btn {
                margin-left: 12px;
            }
        }
    }
    .filter-warpper {
        display: flex;
        align-items: center;
        height: 32px;
        margin-bottom: 20px;
        .filter-checkbox {
            display: inline-flex;
            flex-shrink: 0;
        }
        .date-prepend {
            flex-shrink: 0;
            height: 32px;
            line-height: 32px;
            border: 1px solid #c4c6cc;
            font-size: 12px;
            color: #63656e;
            padding: 0 5px;
            border-right: none;
        }
        .date-picker {
            flex-shrink: 0;
            max-width: 290px;
        }
    }
</style>

<style lang="scss">
    .experience-search-input {
        width: 420px;
        background-color: #fff;
        ::placeholder {
            color: #c4c6cc;
        }
        .input-box {
            overflow: hidden;
        }
    }
</style>
