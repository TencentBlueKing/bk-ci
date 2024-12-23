<template>
    <section>
        <dashboard-header></dashboard-header>
        <section class="dashboard-container">
            <infinite-scroll class="repo-container-wrapper" ref="infiniteScroll" :data-fetcher="getRepoList" :page-size="limit" scroll-box-class-name="dashboard-container" v-slot="slotProps">
                <section v-if="recentProjects.length" class="recent-projects">
                    <div class="recent-title">{{$t('dashboard.recentProjects')}}</div>
                    <div class="recent-projects-container">
                        <div v-for="repo in recentProjects" :key="repo.id" class="repo-item recent-item">
                            <div class="repo-img">
                                <img class="img"
                                    :class="{ 'to-page-link': repo.ciInfo && repo.ciInfo.enableCI }"
                                    :src="repo.avatarUrl"
                                    @click="toProjectDetail('buildList', repo.nameWithNamespace, repo.ciInfo && repo.ciInfo.enableCI)"
                                >
                            </div>
                            <div class="repo-data">
                                <section v-if="repo.ciInfo && repo.ciInfo.enableCI">
                                    <div class="repo-name">
                                        <span class="to-page-link" :title="repo.nameWithNamespace" @click="toProjectDetail('buildList', repo.nameWithNamespace)">{{ repo.nameWithNamespace }}</span>
                                    </div>
                                    <div class="repo-desc">
                                        <div v-if="repo.ciInfo && repo.ciInfo.enableCI" class="repo-ci-info">
                                            <i :class="getIconClass(repo.ciInfo.lastBuildStatus)"></i>
                                            <span class="to-page-link" :title="repo.ciInfo.lastBuildMessage || 'Empty commit messages'" @click="toLastBuildDetail(repo)">{{ repo.ciInfo.lastBuildMessage || $t('dashboard.emptyCommit') }}</span>
                                        </div>
                                    </div>
                                </section>
                                <section v-else>
                                    <div class="repo-name" :title="repo.nameWithNamespace">
                                        {{ repo.nameWithNamespace }}
                                    </div>
                                    <div class="repo-desc">
                                        <bk-button theme="primary" @click="enableCi(repo)">{{$t('setting.enableCi')}}</bk-button>
                                    </div>
                                </section>
                            </div>
                        </div>
                    </div>
                </section>

                <div class="type-container">
                    <div class="navigation-header">
                        <ol class="header-nav">
                            <li v-for="item in typeList" :key="item.name" @click="changeType(item.type)" class="header-nav-item" :class="{ 'item-active': type === item.type }">
                                {{ item.name }}
                            </li>
                        </ol>
                    </div>
                </div>
                <div class="content-container">
                    <div style="margin-bottom: 15px;">
                        <bk-input :left-icon="'bk-icon icon-search'" :placeholder="$t('dashboard.filterByName')" :clearable="true" v-model="searchStr" @enter="search" @clear="search"></bk-input>
                    </div>
                    <div class="empty-repo" v-if="!slotProps.list.length">
                        <EmptyTableStatus :type="searchStr ? 'search-empty' : 'empty'" @clear="clearFilter" />
                    </div>
                    <div v-for="repo in slotProps.list" :key="repo.id" class="repo-item">
                        <div class="repo-img">
                            <img class="img"
                                :class="{ 'to-page-link': repo.ciInfo && repo.ciInfo.enableCI }"
                                :src="repo.avatarUrl"
                                @click="toProjectDetail('buildList', repo.nameWithNamespace, repo.ciInfo && repo.ciInfo.enableCI)"
                            >
                        </div>
                        <div class="repo-data max-width">
                            <section v-if="repo.ciInfo && repo.ciInfo.enableCI">
                                <div class="repo-name">
                                    <span class="to-page-link" @click="toProjectDetail('buildList', repo.nameWithNamespace)">{{ repo.nameWithNamespace }}</span>
                                </div>
                                <div class="repo-desc">
                                    <div v-if="repo.ciInfo && repo.ciInfo.enableCI" class="repo-ci-info" v-bk-overflow-tips>
                                        <i :class="getIconClass(repo.ciInfo.lastBuildStatus)"></i>
                                        <span class="to-page-link" @click="toLastBuildDetail(repo)">{{ repo.ciInfo.lastBuildMessage || $t('dashboard.emptyCommit') }}</span>
                                    </div>
                                </div>
                            </section>
                            <section v-else>
                                <div class="repo-name">
                                    {{ repo.nameWithNamespace }}
                                </div>
                                <div class="repo-desc" v-bk-overflow-tips>
                                    {{ repo.description || $t('dashboard.emptyDesc') }}
                                </div>
                            </section>
                        </div>
                        <div class="operation">
                            <bk-popover v-if="repo.ciInfo && repo.ciInfo.enableCI" class="dot-menu" ext-cls="dot-menu-wrapper" placement="right" ref="dotMenuRef" theme="dot-menu light" :arrow="false" offset="15" :distance="0">
                                <div class="dot-menu-trigger">
                                    <div class="footer-ext-dots">
                                        <div class="ext-dot"></div>
                                        <div class="ext-dot"></div>
                                        <div class="ext-dot"></div>
                                    </div>
                                </div>
                                <ul class="dot-menu-list" slot="content">
                                    <li @click="toProjectDetail('buildList', repo.nameWithNamespace)">{{$t('pipelines')}}</li>
                                    <li @click="toProjectDetail('basicSetting', repo.nameWithNamespace)">{{$t('settings')}}</li>
                                </ul>
                            </bk-popover>
                            <bk-button v-else theme="primary" @click="enableCi(repo)">{{$t('setting.enableCi')}}</bk-button>
                        </div>
                    </div>
                </div>
            </infinite-scroll>
        </section>
    </section>
</template>

<script>
    import { common, setting } from '@/http'
    import { getPipelineStatusClass, getPipelineStatusCircleIconCls } from '@/components/status'
    import EmptyTableStatus from '@/components/empty-table-status'
    import gitcode from './../images/home/gitcode.png'
    import infiniteScroll from '@/components/infinite-scroll'
    import dashboardHeader from '../components/dashboard-header.vue'

    export default {
        components: {
            EmptyTableStatus,
            infiniteScroll,
            dashboardHeader
        },
        data () {
            return {
                type: 'MY_PROJECT',
                limit: 30,
                searchStr: '',
                recentProjects: [],
                repoList: [],
                typeList: [
                    {
                        type: 'MY_PROJECT',
                        name: this.$t('dashboard.myProjects')
                    }
                ]
            }
        },
        created () {
            this.getRecentProjects()
            this.searchStr = this.$route.query.searchKey || ''
        },
        methods: {
            search () {
                this.$router.push({
                    query: {
                        searchKey: this.searchStr
                    }
                })
                this.updateList()
            },
            async updateList () {
                if (this.$refs.infiniteScroll) {
                    this.$nextTick(async () => {
                        await this.$refs.infiniteScroll.updateList()
                    })
                }
            },
            getRecentProjects () {
                common.getRecentProjects().then((res) => {
                    this.recentProjects = (res || []).map(item => ({
                        ...item,
                        avatarUrl: item.avatarUrl && (item.avatarUrl.endsWith('.jpg') || item.avatarUrl.endsWith('.jpeg') || item.avatarUrl.endsWith('.png')) ? (item.avatarUrl.replace('http:', '').replace('https:', '')) : gitcode
                    }))
                }).catch((err) => {
                    this.$bkMessage({
                        theme: 'error',
                        message: err.message || err
                    })
                })
            },
            async getRepoList (page = 1, pageSize = this.limit) {
                let hasNext = false
                await common.getStreamProjects(this.type, page, pageSize, this.searchStr).then((res = {}) => {
                    this.repoList = (res.records || []).map(item => ({
                        ...item,
                        avatarUrl: (item.avatarUrl.endsWith('.jpg') || item.avatarUrl.endsWith('.jpeg') || item.avatarUrl.endsWith('.png')) ? (item.avatarUrl.replace('http:', '').replace('https:', '')) : gitcode
                    }))
                    hasNext = res.hasNext
                }).catch((err) => {
                    this.$bkMessage({
                        theme: 'error',
                        message: err.message || err
                    })
                })
                return {
                    hasNext,
                    records: this.repoList || []
                }
            },
            
            changeType (type) {
                this.type = type
                this.searchStr = ''
                this.updateList()
            },

            toLastBuildDetail (repo) {
                if (repo.ciInfo.lastBuildPipelineId && repo.ciInfo.lastBuildId) {
                    this.$router.push({
                        name: 'buildDetail',
                        params: {
                            pipelineId: repo.ciInfo.lastBuildPipelineId,
                            buildId: repo.ciInfo.lastBuildId
                        },
                        hash: `#${repo.nameWithNamespace}`
                    })
                } else {
                    this.toProjectDetail('buildList', repo.nameWithNamespace)
                }
            },

            toProjectDetail (routeName, projectId, enableCi = true) {
                if (enableCi === true) {
                    this.$router.push({
                        name: routeName,
                        hash: `#${projectId}`
                    })
                }
            },
            
            enableCi (item) {
                setting.toggleEnableCi(true, {
                    id: item.id,
                    name: item.name,
                    name_with_namespace: item.nameWithNamespace,
                    https_url_to_repo: item.httpsUrlToRepo,
                    http_url_to_repo: item.httpsUrlToRepo.replace('https://', 'http://'),
                    web_url: item.webUrl
                }).then(res => {
                    this.updateList()
                    this.getRecentProjects()
                }).catch((err) => {
                    this.$bkMessage({
                        theme: 'primary',
                        message: err.message || err
                    })
                })
            },

            getIconClass (status) {
                return [getPipelineStatusClass(status), ...getPipelineStatusCircleIconCls(status)]
            },

            clearFilter () {
                this.searchStr = ''
                this.search()
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '@/css/conf';

    .dashboard-container {
        overflow: auto;
        min-width: 1280px;
        height: calc(100vh - 75px);
        .repo-container-wrapper {
            margin: 30px 30px 0;
        }
        .recent-projects {
            background: #fff;
            margin-bottom: 20px;
            .recent-title {
                padding: 20px 30px 0;
                font-size: 14px;
                color: rgba(0,0,0,0.9);
            }
            .recent-projects-container {
                display: flex;
                justify-content: flex-start;
                .recent-item {
                    flex: 1;
                    width: 25%;
                    .repo-data {
                        width: calc(100% - 120px);
                    }
                }
            }
        }
        
        .type-container {
            display: flex;
            align-items: center;
            background: #fff;
            height: 48px;

            .navigation-header {
                -webkit-box-flex: 1;
                -ms-flex: 1;
                flex: 1;
                height: 100%;
                display: -webkit-box;
                display: -ms-flexbox;
                display: flex;
                -webkit-box-align: center;
                -ms-flex-align: center;
                align-items: center;
                font-size: 14px;
                color: rgba(0,0,0,0.60);
                margin-left: 30px;
            }
            .navigation-header .header-nav {
                display: -webkit-box;
                display: -ms-flexbox;
                display: flex;
                padding: 0;
                margin: 0;
            }
            .navigation-header .header-nav-item {
                list-style: none;
                height: 50px;
                display: -webkit-box;
                display: -ms-flexbox;
                display: flex;
                -webkit-box-align: center;
                -ms-flex-align: center;
                align-items: center;
                margin-right: 40px;
                min-width: 56px
            }
            .navigation-header .header-nav-item.item-active {
                color:#3a84ff !important;
                border-bottom: 2px solid #3a84ff;
            }
            .navigation-header .header-nav-item:hover {
                cursor:pointer;
                color:#3a84ff;
            }
        }

        .content-container {
            margin-top: 15px;
            .empty-repo {
                height: 100%;
                min-height: 180px;
                background: #fff;
                display: flex;
            }
        }

        .repo-item {
            height: 88px;
            width: 100%;
            background: #fff;
            display: flex;
            align-items: center;
            margin-bottom: 10px;
            padding: 0 24px;
            cursor: default;
            .to-page-link {
                cursor: pointer;
            }
            .repo-img {
                .img {
                    width: 56px;
                    height: 56px;
                    border-radius: 28px;
                }
            }
            .max-width {
                max-width: calc(100% - 210px);
            }
            .repo-data {
                flex: 1;
                margin: 0 16px;
                .repo-name {
                    margin-bottom: 6px;
                    font-size: 16px;
                    color: rgba(0,0,0,0.90);
                    display: inline-block;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    width: 100%;
                }
                .repo-desc {
                    font-size: 14px;
                    color: #96A2B9;
                    display: inline-block;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    width: 100%;
                    .repo-ci-info {
                        width: 100%;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                    }
                    .bk-icon {
                        font-size: 20px;
                        &.executing {
                            font-size: 14px;
                        }
                        &.icon-exclamation, &.icon-exclamation-triangle, &.icon-clock {
                            font-size: 24px;
                        }
                        &.running {
                            color: #459fff;
                        }
                        &.canceled {
                            color: #f6b026;
                        }
                        &.danger {
                            color: #ff5656;
                        }
                        &.success {
                            color: #34d97b;
                        }
                        &.pause {
                            color: #ff9801;
                        }
                    }
                }
            }
            .operation {
                width: 120px;
                .ext-dot {
                    width: 3px;
                    height: 3px;
                    border-radius: 50%;
                    background-color: $fontWeightColor;
                    & + .ext-dot {
                        margin-top: 4px;
                    }
                }
                .dot-menu-trigger {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    width: 23px;
                    height: 100%;
                    text-align: center;
                    font-size: 0;
                    cursor: pointer;
                }
            }
        }
    }

    .dot-menu-wrapper {
        .tippy-tooltip {
            padding: 0 ;
        }
    }

    .dot-menu-list {
        padding: 4px 0;
        > li {
            font-size: 12px;
            line-height: 32px;
            text-align: left;
            padding: 0 20px;
            cursor: pointer;
            a {
                color: $fontColor;
                display: block;
            }
            &:hover {
                color: $primaryColor;
                background-color: #EAF3FF;
                a {
                    color: $primaryColor;
                }
            }
        }
    }
</style>
