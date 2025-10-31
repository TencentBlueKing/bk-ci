<template>
    <div class="biz-container release-experience-wrapper experience">
        <aside-nav
            :nav="nav"
            :menu-click="handleClick"
        >
            <router-view
                slot="content"
                class="biz-content"
            ></router-view>
        </aside-nav>
    </div>
</template>

<script>
    import { mapState } from 'vuex'

    export default {
        computed: {
            ...mapState('experience', [
                'curReleaseDetail'
            ]),
            state () {
                return {
                    curReleaseDetail: this.curReleaseDetail
                }
            },
            nav () {
                return {
                    icon: 'experience',
                    title: this.$t('experience.experience'),
                    menu: [{
                               id: 'experienceList',
                               name: this.$t('experience.mobileRelease'),
                               icon: 'devops-icon icon-release',
                               showChildren: false,
                               children: [
                                   {
                                       id: 'experienceDetail',
                                       name: this.$t('experience.releaseDetail'),
                                       icon: 'devops-icon icon-release'
                                   },
                                   {
                                       id: 'editExperience',
                                       name: this.$t('experience.editRelease'),
                                       icon: 'devops-icon icon-release'
                                   }
                               ]
                           },
                           {
                               id: 'createExperience',
                               name: this.$t('experience.addExp'),
                               icon: 'devops-icon icon-experience',
                               showChildren: false,
                               children: []
                           },
                           {
                               id: 'setting',
                               name: this.$t('experience.setting'),
                               icon: 'devops-icon icon-cog',
                               showChildren: false,
                               children: []
                           }]
                }
            }
        },
        methods: {
            handleClick (name) {
                this.$router.push({
                    name
                })
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .release-experience-wrapper {
        min-width: 1200px;
    }
    .sub-view-port {
        height: calc(100% - 60px);
        padding: 20px;
        overflow: auto;
    }

    .bkdevops-aside-nav.bk-navigation {
        flex: 1;
    }

</style>
