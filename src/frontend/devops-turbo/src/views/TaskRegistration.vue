<template>
    <div class="task-registration-wrapper">
        <header-process :process-head="getProcessHead"></header-process>

        <scheme class="sub-view-port" v-if="getProcessHead.process === 'scheme'"></scheme>
        <build-public class="sub-view-port" v-if="getProcessHead.process === 'buildPublic'"></build-public>
        <build-third class="sub-view-port wrapper-container-third" v-if="getProcessHead.process === 'buildThird'" ref="buildThird"></build-third>
        <build-install class="sub-view-port wrapper-container-Install" v-if="getProcessHead.process === 'buildInstall'"></build-install>
        <regist-success class="sub-view-port" v-if="getProcessHead.process === 'registSuccess'"></regist-success>

        <bk-dialog
            v-model="getDialogOpt.isShow"
            :width="getDialogOpt.imgType === 'distccImg' ? '700' : '577'"
            :padding="getDialogOpt.padding"
            :has-header="false"
            :show-footer="false"
            :close-icon="false"
            :ext-cls="'build-dialog'">
            <div class="build-dialog-wrapper">
                <i class="bk-dialog-close bk-icon icon-close" v-if="getDialogOpt.hasClose" @click.stop="dialogClose"></i>
                <div v-if="'picture' === getDialogOpt.contentType" :class="['dialog-img-container', { 'distccImg': getDialogOpt.imgType === 'distccImg', 'ccacheImg': getDialogOpt.imgType === 'ccacheImg' }]">
                    <img :src="getDialogOpt.imgSrc">
                </div>
                <build-progress class="build-progress"
                    v-if="'progress' === getDialogOpt.contentType"
                    :progress-width="getDialogOpt.progressWidth"
                    :error-message="getDialogOpt.errorMessage"
                    :rest-date="getDialogOpt.restDate"
                    :current-state="getDialogOpt.currentState"
                    @resInstall="resInstall">
                </build-progress>
            </div>
        </bk-dialog>
    </div>
</template>

<script>
    import headerProcess from '@/components/turbo/headerProcess'
    import buildProgress from '@/components/turbo/buildProgress'
    import scheme from '@/components/turbo/scheme'
    import buildPublic from '@/components/turbo/buildPublic'
    import buildThird from '@/components/turbo/buildThird'
    import buildInstall from '@/components/turbo/buildInstall'
    import registSuccess from '@/components/turbo/registSuccess'
    import { mapGetters } from 'vuex'

    export default {
        components: {
            headerProcess,
            buildProgress,
            scheme,
            buildPublic,
            buildThird,
            buildInstall,
            registSuccess
        },
        computed: {
            ...mapGetters('turbo', [
                'getProcessHead',
                'getDialogOpt'
            ]),
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            projectId: async function () {
                this.$store.commit('turbo/resetRegister')
            }
        },
        beforeDestroy () {
            this.$store.commit('turbo/resetRegister')
        },
        methods: {
            dialogClose () {
                this.$store.commit('turbo/setDialogOpt', { isShow: false })
            },
            registCancel () {
                this.$bkInfo({
                    title: '确认要离开',
                    subTitle: '离开后，新编辑的数据将丢失',
                    confirm: '确认',
                    cancel: '取消',
                    confirmFn: (done) => {
                        this.$router.push({
                            name: 'acceleration',
                            params: this.$router.params
                        })
                        done()
                    }
                })
            },
            resInstall () {
                this.$refs.buildThird.installSoftware()
            }
        }
    }
</script>

<style lang="scss">
    @import '../assets/scss/conf.scss';
    
    .build-dialog {
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            padding: 0;
        }
        .bk-dialog-position {
            text-align: center;
        }
        .bk-dialog-style {
            display: inline-block;
            width: auto;
        }
        .build-dialog-wrapper {
            position: relative;
        }
        .build-progress {
            width: 640px;
            height: 120px;
            padding: 21px 25px 0 25px;
        }
        img {
            width: 100%;
            // max-width: 95vw;
            // max-height: 95vh;
        }
        .bk-dialog-close {
            position: absolute;
            top: 10px;
            right: 10px;
            cursor: pointer;
        }
        .dialog-img-container {
            &.ccacheImg,
            &.distccImg {
                width: 577px;
                padding: 27px 78px 38px 79px;
            }
            &.distccImg {
                width: 700px;
                padding: 20px;
            }
        }
    }
</style>
