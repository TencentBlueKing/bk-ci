<template>
    <div class="devops-header">
        <div class="header-left-bar">
            <router-link
                class="header-logo"
                to="/console/"
                @click.native="setDocumentTitle"
            >
                <span>
                    <Logo
                        :name="headerLogoName"
                        width="auto"
                        height="28"
                    />
                </span>
            </router-link>
            <template v-if="showProjectList">
                <bk-select ref="projectDropdown"
                    class="bkdevops-project-selector"
                    :placeholder="$t('selectProjectPlaceholder')"
                    :value="projectId"
                    :clearable="false"
                    :searchable="true"
                    @selected="handleProjectChange"
                    @toggle="handleDropdownVisible"
                    :enable-virtual-scroll="selectProjectList && selectProjectList.length > 3000"
                    :list="selectProjectList"
                    id-key="projectCode"
                    display-key="projectName"
                >
                    <bk-option
                        v-for="item in selectProjectList"
                        :key="item.projectCode"
                        :id="item.projectCode"
                        :name="item.projectName"
                    >
                    </bk-option>
                    <template slot="extension">
                        <div
                            class="bk-selector-create-item"
                            @click.stop.prevent="popProjectDialog()"
                        >
                            <i class="devops-icon icon-plus-circle" />
                            <span class="text">{{ $t('newProject') }}</span>
                        </div>
                        <div
                            class="bk-selector-create-item"
                            @click.stop.prevent="goToPm"
                        >
                            <i class="devops-icon icon-apps" />
                            <span class="text">{{ $t('projectManage') }}</span>
                        </div>
                    </template>
                </bk-select>
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
            <locale-switcher v-if="!isInIframe"></locale-switcher>
            <span class="seperate-line">|</span>
            <!-- <feed-back class='feed-back-icon'></feed-back> -->
            <i
                class="devops-icon icon-helper"
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
    import { Action, Getter, State } from 'vuex-class'
    import eventBus from '../../utils/eventBus'
    import { urlJoin } from '../../utils/util'
    import LocaleSwitcher from '../LocaleSwitcher/index.vue'
    import Logo from '../Logo/index.vue'
    import ProjectDialog from '../ProjectDialog/index.vue'
    import DevopsSelect from '../Select/index.vue'
    import User from '../User/index.vue'
    import NavMenu from './NavMenu.vue'

    @Component({
        components: {
            User,
            NavMenu,
            ProjectDialog,
            Logo,
            DevopsSelect,
            LocaleSwitcher
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

        get headerLogoName (): string {
            const logoArr = ['devops-logo']
            const localeConst = this.$i18n.locale === 'zh-CN' ? '' : 'en'
            if (localeConst) {
                logoArr.push(localeConst)
            }
            return logoArr.join('-')
        }

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
            const name = this.currentPage && this.currentPage.name ? this.currentPage.name : ''
            const charPos = name.indexOf('(')
            return charPos > -1 ? name.slice(0, charPos) : name
        }

        get serviceLogo (): string {
            return this.currentPage && this.currentPage.logoUrl ? this.currentPage.logoUrl : 'placeholder'
        }

        get selectProjectList (): Project[] {
            return this.enableProjectList.map(project => ({
                ...project,
                id: project.projectCode,
                name: project.projectName
            }))
        }

        get isInIframe () {
            return top !== window
        }

        $refs: {
            projectDropdown: any
        }

        created () {
            eventBus.$on('show-project-menu', () => {
                const ele = this.$refs.projectDropdown && this.$refs.projectDropdown.$el
                if (ele) {
                    const triggerEle = ele.querySelector('.bk-select-name')
                    triggerEle && triggerEle.click()
                }
            })

            eventBus.$on('hide-project-menu', () => {
                if (this.isDropdownMenuVisible) {
                    const ele = this.$refs.projectDropdown && this.$refs.projectDropdown.$el
                    if (ele) {
                        const triggerEle = ele.querySelector('.bk-select-name')
                        triggerEle && triggerEle.click()
                    }
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

            reload
? location.href = path
: this.$router.replace({
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
            window.setProjectIdCookie(id)

            if ((!oldProject && project.gray) || (oldProject && oldProject.gray !== project.gray)) {
                this.goHomeById(id, true)
            }
        }

        to (url: string): void {
            window.open(url, '_blank')
        }

        goToDocs (): void {
            this.to(this.BKCI_DOCS.BKCI_DOC)
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

        setDocumentTitle () {
            document.title = String(this.$t('documentTitleHome'))
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
                display: flex;
                > span {
                    display: inline-flex;

                }
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
                    top: 7px;
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

            >.devops-icon:hover,
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

            > .devops-icon {
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
            .text {
                color: $primaryColor;
            }
        }
        &:first-child {
            border-top: 0
        }
    }
</style>
