<template>
    <div
        class="release-list-wrapper"
        v-bkloading="{ isLoading: loading.isLoading, title: loading.title }"
    >
        <content-header>
            <div slot="left">{{ $t(`experience.${$route.meta.title}`) }}</div>
        </content-header>
        <keep-alive>
            <section class="sub-view-port">
                <div class="filter-warpper">
                    <bk-checkbox
                        class="mr15 filter-checkbox"
                        :checked="getIsShowExpired"
                        @change="toggleExpired"
                    >
                        {{ $t('experience.show_expired') }}
                    </bk-checkbox>
                    <div class="date-prepend">
                        {{ $t('experience.create_time') }}
                    </div>
                    <bk-date-picker
                        class="date-picker mr15"
                        :value="createDaterange"
                        type="datetimerange"
                        :placeholder="$t('experience.select_date_range')"
                        :options="{
                            disabledDate: time => time.getTime() > Date.now()
                        }"
                        @clear="handleClearCreateDate"
                        @change="handleChangeCreateDate"
                        @pick-success="handlePickSuccessCreateDate"
                    ></bk-date-picker>
    
                    <div class="date-prepend">
                        {{ $t('experience.end_time') }}
                    </div>
                    <bk-date-picker
                        class="date-picker mr15"
                        :value="endDaterange"
                        type="datetimerange"
                        :placeholder="$t('experience.select_date_range')"
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
                        :placeholder="$t('experience.search_placeholder')"
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
                        :label="$t('experience.file_name')"
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
                        :label="$t('experience.version_desc')"
                        prop="remark"
                        min-width="100"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('experience.platform')"
                        prop="platformLabel"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('experience.source')"
                        prop="sourceLabel"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('experience.creator')"
                        prop="creator"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('experience.versionCreateTime')"
                        prop="formatRepoCreateTime"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('experience.expire_time')"
                        prop="formatExpireDate"
                    ></bk-table-column>
                    <bk-table-column
                        :label="$t('experience.operation')"
                        min-width="200"
                    >
                        <template slot-scope="props">
                            <div class="operate-cell">
                                <template v-if="props.row.isWindowsExp">
                                    <template v-if="!props.row.expired && props.row.online">
                                        <span
                                            v-if="!props.row.permissions.canExperience"
                                            v-bk-tooltips="{ content: $t('experience.no_experience_permission') }"
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
                                        v-bk-tooltips="{ content: $t('experience.experience_expired') }"
                                        class="devops-icon icon-qrcode expired-text"
                                        v-else
                                    ></i>
                                </template>
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
                                    {{ $t('experience.edit') }}
                                </bk-button>
                                <template v-if="props.row.online && !props.row.expired">
                                    <bk-button
                                        class="operate-btn"
                                        v-perm="{
                                            hasPermission: props.row.permissions.canDelete,
                                            disablePermissionApi: true,
                                            tooltips: $t('experience.no_permission'),
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
                                        {{ $t('experience.offline') }}
                                    </bk-button>
                                    <span
                                        v-bk-tooltips="{ content: $t('experience.offline_before_delete') }"
                                        class="expired-text"
                                    >{{ $t('experience.delete') }}</span>
                                </template>
                                <template v-else>
                                    <span
                                        v-bk-tooltips="{ content: $t('experience.experience_offlined') }"
                                        class="expired-text"
                                    >{{ $t('experience.offline') }}</span>
                                    <bk-button
                                        class="operate-btn"
                                        v-perm="{
                                            hasPermission: props.row.permissions.canDelete,
                                            disablePermissionApi: true,
                                            tooltips: $t('experience.no_permission'),
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
                                        {{ $t('experience.delete') }}
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
    import qrcode from '@/components/devops/qrcode'
    import { EXPERIENCE_TASK_RESOURCE_ACTION, EXPERIENCE_TASK_RESOURCE_TYPE } from '@/utils/permission'
    import { convertTime, platformMap } from '@/utils/util'
    import '@blueking/search-select/dist/styles/index.css'
    import { mapGetters } from 'vuex'
    import emptyData from './empty-data'

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
                    title: this.$t('experience.empty_title'),
                    desc: this.$t('experience.empty_desc'),
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
                        name: this.$t('experience.search_fields.name'),
                        default: true,
                        id: 'name'
                    },
                    {
                        name: this.$t('experience.search_fields.version'),
                        id: 'version'
                    },
                    {
                        name: this.$t('experience.search_fields.versionTitle'),
                        id: 'versionTitle'
                    },
                    {
                        name: this.$t('experience.search_fields.remark'),
                        id: 'remark'
                    },
                    {
                        name: this.$t('experience.search_fields.classify'),
                        id: 'classify'
                    },
                    {
                        name: this.$t('experience.search_fields.experienceName'),
                        id: 'experienceName'
                    },
                    {
                        name: this.$t('experience.search_fields.platform'),
                        id: 'platform',
                        children: Object.keys(platformMap).map(key =>({
                            id: key,
                            name: this.$t(`experience.platform_labels.${key}`)
                        }))
                    },
                    {
                        name: this.$t('experience.search_fields.creator'),
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
                loading.title = this.$t('experience.loading_title')

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
                    
                    const sourceLabelMap = {
                        PIPELINE: this.$t('experience.source_labels.PIPELINE'),
                        WEB: this.$t('experience.source_labels.WEB')
                    }
                    
                    this.totalList = res.map(item => ({
                        ...item,
                        platformLabel: this.$t(`experience.platform_labels.${item.platform}`),
                        isWindowsExp: item.platform !== platformMap.WIN,
                        sourceLabel: sourceLabelMap[item.source],
                        formatExpireDate: this.localConvertTime(item.expireDate).split(' ')[0],
                        formatRepoCreateTime: this.localConvertTime(item.repoCreateTime).split(' ')[0],
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
                        title: this.$t('experience.confirm'),
                        subTitle: this.$t('experience.confirm_offline'),
                        okText: this.$t('experience.offline'),
                        confirmFn: async () => {
                            let message, theme

                            try {
                                await this.$store.dispatch('experience/toOfflineExp', {
                                    projectId: this.projectId,
                                    experienceHashId: row.experienceHashId
                                })

                                message = this.$t('experience.offline_success')
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
                        title: this.$t('experience.confirm'),
                        subTitle: this.$t('experience.confirm_delete'),
                        okText: this.$t('experience.delete'),
                        confirmFn: async () => {
                            let message, theme

                            try {
                                await this.$store.dispatch('experience/deleteExp', {
                                    projectId: this.projectId,
                                    experienceHashId: row.experienceHashId
                                })

                                message = this.$t('experience.delete_success')
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
