<template>
    <div
        class="devops-iframe-content"
        :class="{ 'showTopPrompt': showExplorerTips === 'true' && isShowPreviewTips && !chromeExplorer }"
    >
        <div
            v-if="isAnyPopupShow"
            class="iframe-over-layout"
        />
        <div
            v-bkloading="{ isLoading }"
            :style="{ height: &quot;100%&quot; }"
        >
            <iframe
                v-if="src"
                id="iframe-box"
                ref="iframeEle"
                allowfullscreen
                :src="src"
                @load="onLoad"
            />
        </div>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Watch } from 'vue-property-decorator'
    import eventBus from '../utils/eventBus'
    import { urlJoin, queryStringify, getServiceAliasByPath } from '../utils/util'
    import { State } from 'vuex-class'

    Component.registerHooks([
        'beforeRouteEnter',
        'beforeRouteLeave',
        'beforeRouteUpdate'
    ])

    @Component
    export default class IframeView extends Vue {
        isLoading: boolean = true
        initPath: string = ''
        src: string = ''
        leaving: boolean = false
        showExplorerTips: string = localStorage.getItem('showExplorerTips')

        $refs: {
            iframeEle: HTMLIFrameElement
        }

        @State projectList
        @State isAnyPopupShow
        @State isShowPreviewTips
        @State user
        @State headerConfig

        created () {
          this.init()
          eventBus.$on('goHome', this.backHome) // 触发返回首页事件
        }

        beforeRouteLeave (to, from, next) {
          if (!this.leaving && location.href.indexOf('pipeline') > -1 && (location.href.indexOf('edit') > -1 || location.href.indexOf('setting') > -1)) {
            this.leaveConfirm(to, from, next)
            return
          }
          next()
        }
        
        leaveConfirm (to, from, next) {
          this.leaving = true
          this.$bkInfo({
            title: '确认要离开',
            subTitle: '离开后，新编辑的数据将丢失',
            confirmFn: () => {
              this.src = null
              this.$nextTick(() => {
                next(true)
              })
            },
            cancelFn: () => {
              next(false)
              this.leaving = false
            }
          })
        }

        beforeDestroy () {
          this.leaving = false
        }

        get needLoading (): boolean {
          return this.$route.name === 'codecc' || this.$route.name === 'job'
        }

        get chromeExplorer () :boolean {
          const explorer = window.navigator.userAgent
          return explorer.indexOf('Chrome') >= 0 && explorer.indexOf('QQ') === -1
        }

        backHome () {
          if (this.needLoading) {
            this.isLoading = true
          }
          if (this.$refs.iframeEle) {
            this.iframeUtil.goHome(this.$refs.iframeEle.contentWindow)
          }
        }

        init () {
          const { showProjectList } = this.headerConfig
          const { projectIdType } = this.$route.meta
          const query = queryStringify(this.$route.query)
          const path = this.$route.path.replace('/console', '')
          const hash = this.$route.hash
            
          if (showProjectList) {
            const reg = /^\/?\w+\/(([\w-]+)\/?)(\S*)\/?$/
            const matchResult = path.match(reg)
            const { projectId } = this.$route.params
            const initPath = matchResult ? matchResult[3] : ''

            if (projectIdType === 'path') {
              this.src = urlJoin(window.currentPage.iframe_url, projectId, initPath) + `${query ? '?' + query : ''}` + hash
            } else {
              const query = Object.assign(this.$route.query, {
                projectId
              })
              this.src = urlJoin(window.currentPage.iframe_url, initPath) + '?' + queryStringify(query) + hash
              console.log(window.currentPage.iframe_url, initPath)
            }
          } else {
            const reg = /^\/?\w+\/(\S*)\/?$/
            const initPath = path.match(reg) ? path.replace(reg, '$1') : ''
            const query = Object.assign({
              project_code: localStorage.getItem('projectId')
            }, this.$route.query)
            console.log(window.currentPage.iframe_url, initPath)
            this.src = urlJoin(window.currentPage.iframe_url, initPath) + '?' + queryStringify(query) + hash
          }
        }
        onLoad () {
          this.isLoading = false
          if (this.$refs.iframeEle) {
            const childWin = this.$refs.iframeEle.contentWindow
            this.iframeUtil.syncProjectList(childWin, this.projectList)
            this.iframeUtil.syncUserInfo(childWin, this.user)
            if (this.$route.params.projectId) {
              this.iframeUtil.syncProjectId(childWin, this.$route.params.projectId)
            }
          }
        }

        isSameModule (newPath: string, oldPath: string): boolean {
          return getServiceAliasByPath(newPath) === getServiceAliasByPath(oldPath)
        }

        @Watch('$route')
        routeChange (newRoute: ObjectMap, oldRoute: ObjectMap): void {
          const { path, params } = newRoute
          const { path: oldPath, params: oldParams } = oldRoute
          if (!this.isSameModule(path, oldPath)) {
            this.isLoading = true
            this.init()
          } else if (params.projectId !== oldParams.projectId) {
            if (this.needLoading) {
              this.isLoading = true
            }
            if (this.$refs.iframeEle && params.projectId) { // 将当前projectId同步到子窗口
              this.iframeUtil.syncProjectId(this.$refs.iframeEle.contentWindow, params.projectId)
            }
          }
        }

        @Watch('projectList')
        handleProjectListChange (projectList, oldList) {
          if (this.$refs.iframeEle) {
            const childWin = this.$refs.iframeEle.contentWindow
            this.iframeUtil.syncProjectList(childWin, projectList)
          }
        }

        @Watch('user')
        handleUserChange (user) {
          if (this.$refs.iframeEle) {
            const childWin = this.$refs.iframeEle.contentWindow
            this.iframeUtil.syncUserInfo(childWin, user)
          }
        }
    }
</script>

<style lang="scss">
    @import '../assets/scss/conf';
    .devops-iframe-content {
        position: absolute;
        left: 0;
        top: $headerHeight;
        right: 0;
        bottom: 0;
        overflow: hidden;
        min-width: 1280px;
        .iframe-over-layout {
            height: 100%;
            width: 100%;
            position: absolute;
            z-index: 2;
        }
        iframe {
            width: 100%;
            min-height: 100%;
            border: 0;
        }
    }
    .showTopPrompt {
        top: 81px;
    }
</style>
