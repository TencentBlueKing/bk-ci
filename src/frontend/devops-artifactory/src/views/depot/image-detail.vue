<template>
    <div class="biz-content" v-bkloading="{ isLoading: showLoading }">
        <div class="biz-top-bar">
            <div class="biz-image-detail-title">
                <span class="bk-icon icon-arrows-left" style="color: #3c96ff; cursor: pointer; font-weight: 600;" @click="backImageLibrary"></span>
                {{imageName}}
            </div>
        </div>
        <div class="biz-image-detail-content-wrapper">
            <div class="biz-header-content">
                <div class="left-wrapper">
                    <div class="logo">
                        <img src="./../../images/default_logo_normal.jpg" />
                    </div>
                    <div class="left-content">
                        <p class="image-name">{{imageName}}</p>
                        <p class="download-count">{{downloadCount + '次下载'}}</p>
                    </div>
                </div>
                <div class="right-wrapper">
                    <div class="top-content">
                        <div class="updator">
                            <p>最近更新人</p>
                            <p>{{modifiedBy}}</p>
                        </div>
                        <div class="update-date">
                            <p>最近更新时间</p>
                            <p>{{modified}}</p>
                        </div>
                    </div>
                    <div class="bottom-content">
                        <p>仓库相对地址</p>
                        <p>{{imagePath}}</p>
                    </div>
                </div>
            </div>
            <div class="tag-count" v-if="dataList.length"><span>TAG</span>{{'(' + tagCount + ')'}}</div>
            <table class="bk-table has-table-hover biz-table biz-image-detail-table" :style="{ minHeight: `${tableHeight - 40}px` }">
                <thead>
                    <tr>
                        <th style="width: 15%; text-align: left;padding-left: 30px;">
                            名称
                        </th>
                        <th>大小</th>
                        <th>最近更新时间</th>
                        <th>所属仓库</th>
                    </tr>
                </thead>
                <tbody>
                    <template v-if="dataList.length">
                        <tr v-for="(item, index) in dataList" :key="index"
                            @mouseover="lastOverImage = index"
                            @mouseout="lastOverImage = -1">
                            <td style="text-align: left;padding-left: 30px;">
                                <div class="tag-name-item">
                                    <span>{{item.tag || '--'}}</span>
                                    <span title="复制镜像" class="copy-btn"
                                        :data-clipboard-text="item.image"
                                        v-show="index === lastOverImage"
                                        @click="copyImage">
                                        <icon :name="'copy'" size="18" />
                                    </span>
                                </div>
                            </td>
                            <td>{{item.size || '--'}}</td>
                            <td>{{item.modified || '--'}}</td>
                            <td>
                                <template v-if="item.artifactorys.length">
                                    <div v-for="(art, inx) in item.artifactorys" :key="inx" :class="[art === &quot;DEV&quot; ? &quot;dev-art&quot; : &quot;prod-art&quot;, &quot;art-tag&quot;]">
                                        {{art === 'DEV' ? '研发仓库' : '生产仓库'}}
                                    </div>
                                    <!-- span class="handler-btn" v-if="imageType === 'private'" @click="setBuildImage(item)">拷贝为构建镜像</span !-->
                                </template>
                                <template v-else>--</template>
                            </td>
                        </tr>
                        <tr v-if="showScrollLoading">
                            <td colspan="4">
                                <div class="loading-row" v-bkloading="{ isLoading: true }"></div>
                            </td>
                        </tr>
                        <tr v-if="!hasNext" class="empty-row">
                            <td colspan="4">
                                没有更多TAG
                            </td>
                        </tr>
                    </template>
                    <template v-else>
                        <tr class="no-hover">
                            <td colspan="4">
                                <div class="bk-message-box">
                                    <p class="message empty-message">暂时没有数据</p>
                                </div>
                            </td>
                        </tr>
                    </template>
                </tbody>
            </table>
        </div>
    </div>
</template>

<script>
    import Clipboard from 'clipboard'
    import { getScrollHeight, getScrollTop, getWindowHeight } from './../../utils/util'
    import { mapGetters } from 'vuex'

    export default {
        data () {
            return {
                showLoading: false,
                showScrollLoading: false,
                hasNext: true,
                hasPrevious: false,
                isBeforeDestroy: false,
                bkMessageInstance: null,
                imageName: '',
                repo: '',
                dataList: [],
                imageDetailList: [],
                tagCount: 0,
                created: '',
                modified: '',
                modifiedBy: '',
                imagePath: '',
                imageType: '',
                downloadCount: 0,
                tableHeight: window.innerHeight - 20 - 219 - 20 - 20 - 10,
                pageSize: 0,
                curPage: 1,
                lastOverImage: -1
            }
        },
        computed: {
            ...mapGetters('artifactory', [
                'getProjectId'
            ]),
            projectId () {
                return this.getProjectId(this.projectCode)
            },
            projectCode () {
                return this.$route.params.projectId
            },
            backRouterName () {
                return localStorage.getItem('backRouterName')
            },
            imageRepo () {
                return this.$route.hash.substring(1)
            }
        },
        async created () {
            await this.$store.dispatch('artifactory/requestProjectDetail', { projectCode: this.$route.params.projectId })
            if (!this.imageRepo) {
                this.$router.push({
                    name: this.backRouterName
                })
            } else {
                this.pageSize = Math.ceil(this.tableHeight / 41 + 10)
                this.isBeforeDestroy = false
                await this.fetchImageLibraryData(this.imageRepo, (this.curPage - 1) * this.pageSize, this.pageSize)
            }
        },
        mounted () {
            self.addEventListener('resize', () => {
                if (this.tagCount < 10) {
                    this.tableHeight = this.tagCount * 41
                } else {
                    this.tableHeight = window.innerHeight - 20 - 219 - 20 - 20 - 10
                }
            })
            self.addEventListener('scroll', async e => {
                if (this.showScrollLoading || this.isBeforeDestroy || this.tagCount < 10) {
                    return
                }
                if (getScrollTop() + getWindowHeight() >= getScrollHeight()) {
                    if (this.hasNext) {
                        this.showScrollLoading = true
                        this.curPage = this.curPage + 1
                        await this.fetchImageLibraryData(this.imageRepo, (this.curPage - 1) * this.pageSize, this.pageSize)
                    }
                }
            })
        },
        beforeDestroy () {
            this.isBeforeDestroy = true
        },
        destroyed () {
            this.bkMessageInstance && this.bkMessageInstance.close()
        },
        methods: {
            backImageLibrary () {
                this.$router.push({
                    name: this.backRouterName
                })
            },
            reset () {
                this.imageDetailList.splice(0, this.imageDetailList.length, ...[])
                this.tagCount = 0
                this.imageName = ''
                this.created = ''
                this.modified = ''
                this.modifiedBy = ''
                this.imagePath = ''
                this.downloadCount = 0
            },

            /**
             * 获取详情数据
             */
            async fetchImageLibraryData (repo = '', offset = 0, limit = 10) {
                if (!this.showScrollLoading) {
                    this.reset()
                    this.showLoading = true
                }
                const params = {
                    imageRepo: repo
                }
                try {
                    const imageDetail = await this.$store.dispatch('artifactory/getImageLibraryDetail', params)
                    if (this.showScrollLoading) {
                        this.hasNext = imageDetail.has_next
                        this.hasPrevious = imageDetail.has_previous
                    } else {
                        this.tagCount = imageDetail.tagCount || 0
                        if (this.tagCount < 10) {
                            this.tableHeight = this.tagCount * 41
                            this.hasNext = true
                        }
                        this.imageName = imageDetail.name
                        this.created = imageDetail.created
                        this.modified = imageDetail.modified
                        this.modifiedBy = imageDetail.modifiedBy
                        this.imagePath = imageDetail.imagePath
                        this.downloadCount = imageDetail.downloadCount
                        this.imageType = imageDetail.type
                    }
                    this.imageDetailList.splice(0, this.imageDetailList.length, ...(imageDetail.tags || []))
                    if (this.imageDetailList.length) {
                        this.imageDetailList.forEach(item => {
                            this.dataList.push(item)
                        })
                    }
                } catch (e) {
                    this.bkMessageInstance = this.$bkMessage({
                        theme: 'error',
                        message: e.message || e.data.msg || e.statusText
                    })
                } finally {
                    this.showLoading = false
                    const scrollTop = getScrollTop()
                    if (scrollTop !== 0) {
                        window.scrollTo(0, getScrollTop() - 40)
                    }
                    setTimeout(() => {
                        this.showScrollLoading = false
                    }, 100)
                }
            },
            async setBuildImage (image) {
                let message, theme
                try {
                    await this.$store.dispatch('artifactory/setBuildImage', {
                        projectCode: this.projectCode,
                        imageRepo: image.repo,
                        imageTag: image.tag
                    })
                    message = '镜像已拷贝为构建镜像，请在流水线中使用'
                    theme = 'success'
                } catch (err) {
                    theme = 'error'
                    message = err.message || err
                } finally {
                    message && this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            copyImage (image) {
                this.clipboardInstance = new Clipboard('.copy-btn')
                this.clipboardInstance.on('success', e => {
                    this.$bkMessage({
                        theme: 'success',
                        message: '复制成功',
                        limit: 1
                    })
                })
            }
        }
    }
</script>

<style lang="scss">
    @import './image-detail.scss';
</style>
