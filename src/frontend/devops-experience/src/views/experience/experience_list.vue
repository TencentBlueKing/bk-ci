<template>
    <div class="release-list-wrapper" v-bkloading="{ isLoading: loading.isLoading, title: loading.title }">
        <content-header>
            <div slot="left">{{ $route.meta.title }}</div>
            <div slot="right" v-if="showContent">
                <bk-checkbox :checked="getIsShowExpired" @change="toggleExpired">
                    显示已过期体验
                </bk-checkbox>
                <!-- <label class="bk-form-checkbox" style="margin-right: 0;">
                    <input type="checkbox"  @click="toggleExpired">
                </label> -->
                
            </div>
        </content-header>
        <section class="sub-view-port">
            <div v-if="showContent && releaseList.length">
                <bk-table
                    :data="releaseList"
                    @row-click="toRowDetail"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @page-limit-change="handlePageLimitChange"
                >
                    <bk-table-column label="文件名（版本号）" prop="name" min-width="150">
                        <template slot-scope="props">
                            <i v-if="props.row.expired" class="devops-icon icon-expired-experience"></i>
                            <span class="link-text" :title="`${props.row.name}（ ${props.row.version} ）`">{{ props.row.name }}（{{ props.row.version }}）</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="版本描述" prop="remark" min-width="100"></bk-table-column>
                    <bk-table-column label="平台" prop="platformLabel"></bk-table-column>
                    <bk-table-column label="来源" prop="sourceLabel"></bk-table-column>
                    <bk-table-column label="发布人" prop="creator"></bk-table-column>
                    <bk-table-column label="体验结束时间" prop="formatExpireDate"></bk-table-column>
                    <bk-table-column label="操作" width="150">
                        <template slot-scope="props">
                            <div class="operate-cell">
                                <template v-if="!props.row.expired && props.row.online">
                                    <span v-if="!props.row.permissions.canExperience " v-bk-tooltips="{ content: '你没有该版本的体验权限' }" class="devops-icon icon-qrcode"></span>
                                    <bk-popover placement="left" theme="light" v-if="props.row.permissions.canExperience">
                                        <i class="devops-icon icon-qrcode" @mouseover="requestUrl(props.row)"></i>
                                        <p slot="content" class="qrcode-box" v-if="props.row.permissions.canExperience" v-bkloading="{ isLoading: !curIndexItemUrl }">
                                            <qrcode class="qrcode-view" :text="curIndexItemUrl" :size="100"></qrcode>
                                        </p>
                                    </bk-popover>
                                </template>
                                <i v-bk-tooltips="{ content: '该体验已过期' }" class="devops-icon icon-qrcode expired-text" v-else></i>
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
                                    class="mr5"
                                    text
                                    @click.stop="toEditRow(props.row)"
                                >
                                    编辑
                                </bk-button>
                                <bk-button
                                    v-if="props.row.online && !props.row.expired"
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
                                <span v-bk-tooltips="{ content: '该体验已下架' }" class="expired-text" v-else>下架</span>
                            </div>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
            <empty-data
                v-if="showContent && !releaseList.length"
                :empty-info="emptyInfo"
                :to-create-fn="toCreateFn"
            >
            </empty-data>
        </section>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import { convertTime } from '@/utils/util'
    import qrcode from '@/components/devops/qrcode'
    import emptyData from './empty-data'
    import { EXPERIENCE_TASK_RESOURCE_TYPE, EXPERIENCE_TASK_RESOURCE_ACTION } from '@/utils/permission'

    export default {
        components: {
            emptyData,
            qrcode
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
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            ...mapGetters('experience', [
                'getIsShowExpired'
            ])
        },
        watch: {
            projectId () {
                this.init()
            },
            getIsShowExpired () {
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
            /**
             * 获取发布列表
             */
            async requestList (reset = true) {
                try {
                    const res = await this.$store.dispatch('experience/requestExpList', {
                        projectId: this.projectId,
                        params: {
                            expired: this.getIsShowExpired
                        }
                    })
                    
                    const platformLabelMap = {
                        ANDROID: 'Android',
                        IOS: 'iOS'
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
        }
    }
</style>
