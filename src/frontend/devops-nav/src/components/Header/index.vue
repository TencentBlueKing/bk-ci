<template>
    <div class="devops-header">
        <div class="header-left-bar">
            <router-link
                class="header-logo"
                to="/console/"
            >
                <Logo
                    name="devops-logo"
                    width="100"
                    height="28"
                />
            </router-link>
            <template v-if="showProjectList">
                <devops-select
                    ref="projectDropdown"
                    class="bkdevops-project-selector"
                    :placeholder="$t('selectProjectPlaceholder')"
                    :value="projectId"
                    :clearable="false"
                    :options="selectProjectList"
                    :searchable="true"
                    @selected="handleProjectChange"
                    @toggle="handleDropdownVisible"
                >
                    <template slot="extension">
                        <div
                            class="bk-selector-create-item"
                            @click.stop.prevent="popProjectDialog()"
                        >
                            <i class="bk-icon icon-plus-circle" />
                            <span class="text">{{ $t('newProject') }}</span>
                        </div>
                        <div
                            class="bk-selector-create-item"
                            @click.stop.prevent="goToPm"
                        >
                            <i class="bk-icon icon-apps" />
                            <span class="text">{{ $t('projectManage') }}</span>
                        </div>
                    </template>
                </devops-select>
            </template>
            <nav-menu v-if="showNav" />
            <h3
                v-if="title"
                class="service-title"
                @click="goHome"
            >
                <logo
                    :name="serviceLogo"
                    size="20"
                />
                {{ title }}
            </h3>
        </div>
        <div class="header-right-bar">
            <qrcode class="feed-back-icon" />
            <span class="seperate-line">|</span>
            <!-- <feed-back class='feed-back-icon'></feed-back> -->
            <i
                class="bk-icon icon-helper"
                @click.stop="goToDocs"
            />
            <User
                class="user-info"
                v-bind="user"
            />
        </div>

        <project-dialog
            :init-show-dialog="showProjectDialog"
            :title="projectDialogTitle"
        />
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { State, Action, Getter } from 'vuex-class'
    import User from '../User/index.vue'
    import NavMenu from './NavMenu.vue'
    import FeedBack from './FeedBack.vue'
    import Qrcode from './Qrcode.vue'
    import Logo from '../Logo/index.vue'
    import DevopsSelect from '../Select/index.vue'
    import ProjectDialog from '../ProjectDialog/index.vue'
    import eventBus from '../../utils/eventBus'
    import * as cookie from 'js-cookie'
    import { urlJoin } from '../../utils/util'

    @Component({
        components: {
            User,
            NavMenu,
            FeedBack,
            Qrcode,
            ProjectDialog,
            Logo,
            DevopsSelect
        }
    })
    export default class Header extends Vue {
        @State user
        @State currentPage
        @State showProjectDialog
        @State projectDialogTitle
        @State headerConfig

        @Getter enableProjectList

        @Action toggleProjectDialog
        @Action togglePopupShow

        isDropdownMenuVisible: boolean = false
        isShowTooltip: boolean = true

        get showProjectList (): boolean {
            return this.headerConfig.showProjectList
        }
        get showNav (): boolean {
            return this.headerConfig.showNav
        }
        get projectId (): string {
            return this.$route.params.projectId
        }
        get title (): string {
            return this.currentPage && this.currentPage.name ? this.currentPage.name : ''
        }
        get serviceLogo (): string {
            return this.$route.meta.logo
        }
        get selectProjectList (): Project[] {
            return this.enableProjectList.map(project => ({
                ...project,
                id: project.projectCode,
                name: project.projectName
            }))
        }

        $refs: {
            projectDropdown: any
        }

        created () {
            eventBus.$on('show-project-menu', () => {
                const ele = this.$refs.projectDropdown.$el
                ele && ele.click()
            })

            eventBus.$on('hide-project-menu', () => {
                if (this.isDropdownMenuVisible) {
                    const ele = this.$refs.projectDropdown.$el
                    ele && ele.click()
                }
            })

            eventBus.$on('show-project-dialog', (project: Project) => {
                this.popProjectDialog(project)
            })
        }

        handleDropdownVisible (isShow: boolean): void {
            if (this.isDropdownMenuVisible !== isShow) {
                this.togglePopupShow(isShow)
            }
            this.isDropdownMenuVisible = isShow
        }

        goHome (): void {
            eventBus.$emit('goHome')
            const homeRouter = this.$route.meta.to

            if (homeRouter) {
                this.$router.push({
                    name: homeRouter,
                    params: this.$route.params
                })
            }
        }

        goHomeById (projectId: string, reload: boolean = false): void {
            const hasProjectId = this.currentPage.show_project_list
            let path = urlJoin('/console', this.currentPage.link_new)
            if (hasProjectId) {
                if (this.currentPage.project_id_type === 'path') {
                    path = urlJoin(path, projectId)
                } else {
                    path += `?projectId=${projectId}`
                }
            }

            reload ? location.href = path : this.$router.replace({
                path
            })
        }

        handleProjectChange (id: string) {
            const { projectId } = this.$route.params
            const oldProject = this.selectProjectList.find(project => project.projectCode === projectId)
            const project = this.selectProjectList.find(project => project.projectCode === id)
            
            if (projectId && !oldProject) { // 当前无权限时返回首页
                this.goHomeById(id)
            } else {
                this.$router.replace({
                    params: {
                        projectId: id
                    }
                })
            }

            cookie.set(X_DEVOPS_PROJECT_ID, id, {
                domain: 'oa.com',
                path: '/'
            })

            if ((!oldProject && project.gray) || (oldProject && oldProject.gray !== project.gray)) {
                localStorage.setItem('projectId', id)
                this.goHomeById(id, true)
            }
        }

        to (url: string): void {
            window.open(url, '_blank')
        }

        goToDocs (): void {
            this.to('/console/docs')
        }

        goToPm (): void {
            this.to('/console/pm')
        }

        popProjectDialog (project: object): void {
            this.toggleProjectDialog({
                showProjectDialog: true,
                project
            })
            if (this.$refs.projectDropdown && typeof this.$refs.projectDropdown.close === 'function') {
                this.$refs.projectDropdown.close()
            }
        }

        closeTooltip (): void {
            this.isShowTooltip = false
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';

    $headerBgColor: #191929;
    .link {
        color: white;
        margin: 0 20px;
    }
    .devops-header {
        height: $headerHeight;
        display: flex;
        align-items: center;
        position: relative;
        z-index: 1002;
        min-width: 1280px;
        background-color: $headerBgColor;
        transition: all .3s ease;
        .header-left-bar {
            height: 100%;
            flex: 1;
            display: flex;
            align-items: center;
            .header-logo {
                margin-left: 15px;
                margin-right: 15px;
                width: 230px;
            }
            $dropdownBorder: #2a2a42;
            .bkdevops-project-selector {
                width: 233px;
                color: $fontColor;
                border-color: $dropdownBorder;
                background-color: $headerBgColor;
                
                height: 36px;
                line-height: 36px;

                &:hover,
                &.active,
                &.is-focus {
                    border-color: $dropdownBorder !important;
                    background-color: black;
                    color: white !important;
                    box-shadow: none;
                }
                .bk-select-angle {
                    color: white;
                }
                .bk-tooltip-ref {
                    outline: none;
                }
                .bk-select-dropdown .bk-select-name {
                    color: $fontLigtherColor;
                    height: 36px;
                    line-height: 36px;
                    font-size: 14px;
                    outline: none;
                }
            }

            .service-title {
                display: flex;
                align-items: center;
                height: 100%;
                padding: 0 18px;
                margin-left: 10px;
                color: $fontLigtherColor;
                font-size: 14px;
                cursor: pointer;

                &:hover {
                    color: white;
                    background-color: black;
                }
                > svg {
                    margin-right: 5px;
                }
            }
        }

        .header-right-bar {
            justify-self: flex-end;
            height: $headerHeight;
            display: flex;
            display: flex;
            align-items: center;

            >.bk-icon:hover,
            >.feed-back-icon:hover,
            >.user-info:hover,
            >.feed-back-icon.active,
            >.user-info.active {
                color: white;
                background-color: black;
            }

            > .seperate-line {
                padding: 0 5px;
                font-size: 20px;
                // color: $fontLigtherColor;
                line-height: $headerHeight;
            }

            > .bk-icon {
                padding: 0 10px;
                font-size: 20px;
                color: $fontLigtherColor;
                line-height: $headerHeight;
                cursor: pointer;
            }

            > .user-info {
                margin: 0 10px;
            }
        }
    }
    .bk-selector-create-item{
        cursor: pointer;
        &:hover {
            color: $primaryColor;
        }
    }
</style>
