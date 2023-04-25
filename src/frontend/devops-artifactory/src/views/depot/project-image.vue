<template>
    <div class="biz-content">
        <div class="biz-top-bar">
            <div class="biz-project-image-title">
                项目镜像
            </div>
        </div>
        <div class="biz-content-wrapper" style="padding: 0;">
            <div class="biz-panel-header biz-project-image-query">
                <div class="left">
                    <bk-button theme="primary" @click="goIwiki()">手动推送镜像</bk-button>
                </div>
                <div class="right">
                    <div class="biz-search-input">
                        <input @keyup.enter="enterHandler" v-model="searchKey" type="text" class="bk-form-input" placeholder="搜索">
                        <a href="javascript:void(0)" class="biz-search-btn" @click="handleClick">
                            <i class="devops-icon icon-search icon-search-li"></i>
                        </a>
                    </div>
                </div>
            </div>
            <div class="biz-project-image-list" v-bkloading="{ isLoading: showLoading }">
                <template v-if="dataList.length && !showLoading">
                    <div class="list-item" v-for="(item, index) in dataList" :key="index">
                        <div class="left-wrapper">
                            <img src="./../../images/default_logo.jpg" class="logo" />
                        </div>
                        <div class="right-wrapper">
                            <div class="content">
                                <div class="info">
                                    <div class="title">
                                        <span>{{item.name}}</span>
                                    </div>
                                    <div class="attr">
                                        <span>类型：{{item.type || '--'}}</span>
                                        <span>来源：{{item.createdBy || '--'}}</span>
                                        <span>所属仓库：{{item.repoType || '--'}}</span>
                                    </div>
                                    <div class="desc">
                                        简介：{{item.desc || '--'}}
                                    </div>
                                </div>
                                <div class="detail" @click="toImageDetail(item)">
                                    详情<i class="devops-icon icon-angle-right"></i>
                                </div>
                            </div>
                        </div>
                    </div>
                </template>
                <template v-else-if="!showLoading">
                    <div class="empty">
                        <p class="title">暂时没有数据！</p>
                    </div>
                </template>
                <template v-else>
                    <div class="loading"></div>
                </template>
            </div>
            <div class="biz-page-box">
                <bk-pagination
                    size="small"
                    :current.sync="pageConf.curPage"
                    :show-limit="false"
                    :limit="pageConf.pageSize"
                    :limit-list="pageConf.limitList"
                    :count="pageConf.count"
                    @change="pageChange">
                </bk-pagination>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'

    export default {
        data () {
            return {
                imageDialogConf: {
                    isShow: false,
                    width: 640,
                    hasHeader: false,
                    closeIcon: false
                },
                uploading: false,
                showLoading: true,
                // 查询条件
                searchKey: '',
                pageConf: {
                    count: 0,
                    pageSize: 5,
                    curPage: 1,
                    limitList: [5]
                },
                bkMessageInstance: null,
                winHeight: 0
            }
        },
        computed: {
            ...mapGetters('artifactory', [
                'getProjectId'
            ]),
            projectId () {
                return this.getProjectId()
            },
            projectCode () {
                return this.$route.params.projectId
            },
            dataList () {
                return this.$store.state.artifactory.projectImage.dataList
            }
        },
        async mounted () {
            await this.$store.dispatch('artifactory/requestProjectDetail', { projectCode: this.$route.params.projectId })
            this.winHeight = window.innerHeight
            this.projId = this.projectId || '000'
            localStorage.removeItem('backRouterName')
            this.getFirstPage()
        },
        destroyed () {
            this.bkMessageInstance && this.bkMessageInstance.close()
        },
        methods: {
            goIwiki () {
                window.open(`${DOCS_URL_PREFIX}/x/Lozm`)
            },
            toImageDetail (item) {
                localStorage.setItem('backRouterName', 'projectImage')
                // this.$router.push({
                //     name: 'imageDetail',
                //     params: {
                //         repo: item.repo
                //     }
                // })
                this.$router.push(`image-detail#${item.repo}`)
            },

            /**
             * 搜索框 enter 事件处理
             *
             * @param {Object} e 事件对象
             */
            enterHandler (e) {
                this.getFirstPage()
            },
            /**
             * 获取数据
             *
             * @param {Object} params ajax 查询参数
             */
            async fetchData (params = {}) {
                this.showLoading = true
                // 去掉类型过滤 默认查询所有数据
                const filters = 'all'

                try {
                    const res = await this.$store.dispatch('artifactory/getProjectImage', Object.assign({}, params, {
                        searchKey: this.searchKey,
                        filters,
                        projectCode: this.projectCode
                    }))

                    const count = res.total || 0
                    this.pageConf.count = count
                } catch (e) {
                    this.bkMessageInstance = this.$bkMessage({
                        theme: 'error',
                        message: e.message || e.data.msg || e.statusText
                    })
                } finally {
                    this.showLoading = false
                }
            },
            /**
             * 首页
             */
            getFirstPage () {
                this.fetchData({
                    limit: this.pageConf.pageSize,
                    start: 0
                })
            },
            /**
             * 翻页
             *
             * @param {number} page 页码
             */
            pageChange (page) {
                this.fetchData({
                    projId: this.projId,
                    limit: this.pageConf.pageSize,
                    start: this.pageConf.pageSize * (page - 1)
                })
            },

            /**
             * 搜索按钮点击
             *
             * @param {Object} e 对象
             */
            handleClick (e) {
                this.getFirstPage()
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf.scss';
    @import './../../scss/mixins/clearfix';

    .biz-image-library-sideslider {
        .biz-log-box {
            margin: 19px 55px;
        }
        .item-wrapper {
            & + .item-wrapper {
                margin-top: 23px;
            }
            li {
                font-size: 14px;
                line-height: 28px;
            }
            li.label {
                font-weight: 700;
                color: #52525d;
                list-style-type: disc;
            }
            li.desc {
                color: #737987;
                position: relative;
            }
            .code {
                background-color: #333948;
                border-radius: 2px;
                color: #fff;
                width: 544px;
                // height: 54px;
                height: 74px;
                // line-height: 54px;
                // padding-left: 19px;
                padding: 10px 19px;
                margin-top: 8px;
                // &:hover {
                //     .copy-btn {
                //         visibility: visible;
                //     }
                // }
            }
            .copy-btn {
                position: absolute;
                top: 36px;
                font-size: 20px;
                right: 170px;
                cursor: pointer;
                color: #fff;
                visibility: hidden;
            }
            .tip {
                margin-top: 8px;
            }
        }
    }

    .biz-project-image-title {
        display: inline-block;
        height: 60px;
        line-height: 60px;
        font-size: 16px;
        margin-left: 20px;
    }

    .biz-project-image-query {
        .left {
            .biz-search-input {
                width: 240px;
                margin-right: 20px;
                .biz-search-btn {
                    .icon-search-li {
                        color: #c3cdd7;
                    }
                }
            }
            .bk-form-input {
                padding-right: 35px;
            }
            a.bk-text-button {
                font-size: 14px;
            }
        }
        .right {
            a.bk-text-button {
                font-size: 14px;
            }
        }
    }
    .biz-project-image-list {
        margin-left: 20px;
        margin-top: 8px;
        margin-right: 20px;
        .empty {
            background-color: #fff;
            padding: 75px 30px;
            border-radius: 2px;
            -webkit-box-shadow: 0 0 3px rgba(0, 0, 0, 0.1);
            box-shadow: 0 0 3px rgba(0, 0, 0, 0.1);
            text-align: center;
        }
        .loading {
            background-color: transparent;
            padding: 111px 30px;
            border-radius: 4px;
            text-align: center;
        }
        .list-item {
            @include clearfix;
            width: 100%;
            // height: 100px;
            display: inline-block;
            border-radius: 2px;
            border: 1px solid $borderColor;
            background-color: #fff;
            margin-bottom: 7px;
            &:hover {
                border: 1px solid #3c96ff;
            }
            .left-wrapper {
                position: relative;
                float: left;
                width: 100px;
                height: 100%;
                margin-right: -100px;
                border-right: 1px solid #ebf0f5;
                padding: 25px;
                .logo {
                    width: 48px;
                    height: 48px;
                    vertical-align: middle;
                }
            }
            .right-wrapper {
                @include clearfix;
                float: right;
                width: 100%;
                height: 100%;
                .content {
                    position: relative;
                    margin-left: 100px;
                    height: 100%;

                    padding-left: 25px;
                    .info {
                        padding: 17px 0;
                        font-size: 12px;
                        padding-right: 260px;
                    }
                    .title {
                        font-size: 14px;
                        margin-bottom: 6px;
                        position: relative;
                        span {
                            font-weight: 700;
                        }
                        i {
                            cursor: pointer;
                            margin-left: 5px;
                            // position: absolute;
                            display: inline-block;
                            top: 1px;
                            &.icon-star-shape {
                                color: #ffb400;
                            }
                            &.icon-star {
                                color: #c4ced8;
                            }
                        }
                        .bk-tooltip-content {
                            font-weight: 400;
                        }
                    }
                    .attr {
                        margin-bottom: 4px;
                        color: #c3cdd7;
                        span {
                            margin-right: 35px;
                        }
                    }
                    .detail {
                        position: absolute;
                        -webkit-transform: translate(0, -50%);
                        transform: translate(0, -50%);
                        top: 50%;
                        font-size: 14px;
                        right: 50px;
                        color: #0082ff;
                        cursor: pointer;
                        i {
                            border: 1px solid #0082ff;
                            border-radius: 50%;
                            padding: 4px;
                            -webkit-transform: scale(.6, .6);
                            transform: scale(.6, .6);
                            position: absolute;
                            font-weight: 700;
                            top: -2px;
                            right: -23px;
                        }
                    }
                }
            }
        }
    }
    .mc-operate-audit-table {
        i.devops-icon {
            font-size: 16px;
            vertical-align: middle;
            position: relative;
            top: -1px;
            margin-right: 7px;
            &.success {
                color: $iconSuccessColor;
            }
            &.fail {
                color: $iconFailColor;
            }
        }
    }

    .dialog-title {
        border-bottom: 1px solid #dde4eb;
        background-color: #fff;
        height: 60px;
        line-height: 59px;
        font-size: 16px;
        padding: 0 20px;
        border-top-left-radius: 2px;
        border-top-right-radius: 2px;
    }

    .image-upload-wrapper {
        min-height: 150px;
        padding: 30px;
        .tip {
            text-align: left;
            font-size: 14px;
            color: #737987;
            margin-bottom: 15px;
            line-height: 1.5;
        }
    }

    .biz-actions {
        position: absolute;
        right: 20px;
        top: 19px;
    }
</style>
