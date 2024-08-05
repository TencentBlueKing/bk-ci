<template>
    <section class="pipeline-edit-wrapper" v-bkloading="{ isLoading }">
        <header class="create-pipeline-header">
            <div>
                <span class="pointer">
                    {{ $t('setting') }}
                </span>
            </div>
        </header>
        <div class="setting-content-wrapper">
            <setting-base
                :is-enabled-permission="isEnabledPermission"
                :is-loading="isLoading"
                @setState="setState"
                @cancel="exit"
            ></setting-base>
        </div>
    </section>
</template>

<script>
    import SettingBase from '@/components/pipelineSetting/settingBase/index.vue'
    import { navConfirm } from '@/utils/util'
    import { mapState } from 'vuex'

    export default {
        components: {
            SettingBase
        },
        props: {
            isEnabledPermission: Boolean
        },
        data () {
            return {
                isEditing: false,
                confirmMsg: this.$t('editPage.confirmMsg'),
                confirmTitle: this.$t('editPage.confirmTitle'),
                cancelText: this.$t('cancel')
            }
        },
        computed: {
            ...mapState('pipelines', [
                'templateSetting'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            isLoading () {
                return !this.templateSetting
            }
        },
        mounted () {
            this.addLeaveListenr()
        },
        beforeDestroy () {
            this.removeLeaveListenr()
        },
        beforeRouteUpdate (to, from, next) {
            this.leaveConfirm(to, from, next)
        },
        beforeRouteLeave (to, from, next) {
            this.leaveConfirm(to, from, next)
        },
        methods: {
            setState (isEditing) {
                console.log('setState', isEditing)
                this.isEditing = isEditing
            },
            leaveConfirm (to, from, next) {
                if (this.isEditing) {
                    navConfirm({ content: this.confirmMsg, type: 'warning', cancelText: this.cancelText })
                        .then(next)
                        .catch(() => next(false))
                } else {
                    next(true)
                }
            },
            addLeaveListenr () {
                window.addEventListener('beforeunload', this.leaveSure)
            },
            removeLeaveListenr () {
                window.removeEventListener('beforeunload', this.leaveSure)
            },
            leaveSure (e) {
                e.returnValue = this.confirmMsg
                return this.confirmMsg
            },
            exit () {
                this.$router.push({
                    name: 'pipelinesTemplate'
                })
            }
        }
    }
</script>

<style lang="scss" >
    @import './../../scss/conf';
    .create-pipeline-header {
        display: flex;
        align-items: center;
        height: 60px;
        border-bottom: 1px solid $borderWeightColor;
        padding: 0 20px 0 30px;
        > p {
            flex: 1;
            > span {
                display: inline-block;
            }
        }
    }
    .pipeline-bar {
        display: flex;
        flex: 1;
        align-items: center;
        height: 100%;
        justify-content: flex-end;
    }
    .setting-content-wrapper {
        position: absolute;
        top: 80px;
        bottom: 0;
        width: 100%;
        overflow-y: auto;
    }
    .pipeline-setting{
        position:absolute;
        width: 100%;
        padding:  35px;
        & .setting-container{
            width: 60%;
            min-width: 880px;
        }
    }
</style>
