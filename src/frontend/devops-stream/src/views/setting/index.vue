<template>
    <article class="setting-home">
        <aside class="aside-nav">
            <h3 class="nav-title">
                <i class="bk-icon icon-arrows-left" @click="backHome"></i>
                {{$t('setting.settings')}}
            </h3>
            <ul>
                <li v-for="setting in settingList"
                    :key="setting.name"
                    @click="goToPage(setting)"
                    :class="{ 'nav-item text-ellipsis': true, active: curSetting.name === setting.name, disabled: !setting.enable }"
                >
                    <icon :name="setting.icon" size="18"></icon>
                    <span class="text-ellipsis item-text" v-bk-overflow-tips>{{ setting.label }}</span>
                </li>
            </ul>
        </aside>

        <router-view class="setting-main"></router-view>
    </article>
</template>

<script>
    import { modifyHtmlTitle } from '@/utils'
    import { mapState } from 'vuex'

    export default {
        data () {
            return {
                curSetting: {}
            }
        },

        computed: {
            ...mapState(['projectId', 'projectSetting', 'projectInfo']),

            // 基础设置页时才判断是否enableCi
            enableCi () {
                return this.$route.name !== 'basicSetting' || (this.projectSetting && this.projectSetting.enableCi)
            },

            settingList () {
                return [
                    { label: this.$t('setting.baseSetting'), name: 'basicSetting', icon: 'edit', enable: true },
                    { label: this.$t('setting.credentialSetting'), name: 'credentialList', icon: 'lock', enable: this.enableCi },
                    { label: this.$t('setting.agentPool'), name: 'agentPools', icon: 'cc-cabinet', enable: this.enableCi },
                    { label: this.$t('setting.experienceGroups'), name: 'expGroups', icon: 'user', enable: this.enableCi }
                ]
            }
        },

        watch: {
            '$route.name': {
                handler (name) {
                    this.initRoute(name)
                },
                immediate: true
            }
        },

        created () {
            this.setHtmlTitle()
        },

        methods: {
            initRoute (name) {
                let settingIndex = 0
                switch (name) {
                    case 'basicSetting':
                        settingIndex = 0
                        break
                    case 'credentialList':
                    case 'credentialSettings':
                        settingIndex = 1
                        break
                    case 'agentPools':
                    case 'agentList':
                    case 'addAgent':
                    case 'agentDetail':
                    case 'poolSettings':
                        settingIndex = 2
                        break
                    case 'expGroups':
                        settingIndex = 3
                        break
                }
                this.curSetting = this.settingList[settingIndex]
            },

            setHtmlTitle () {
                const title = this.projectInfo?.path_with_namespace + ' : Setting'
                modifyHtmlTitle(title)
            },

            goToPage ({ name, params, enable }) {
                if (enable) {
                    this.$router.push({ name, params })
                }
            },

            backHome () {
                this.goToPage({ name: 'buildList', enable: true })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .setting-home {
        display: flex;
        flex-direction: row;
        .setting-main {
            flex: 1;
        }
    }
</style>
