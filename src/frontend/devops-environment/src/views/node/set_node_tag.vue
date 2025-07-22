<template>
    <div
        class="node-detail-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }"
    >
        <content-header class="info-header">
            <div slot="left">
                <i
                    class="devops-icon icon-arrows-left"
                    @click="toNodeList"
                ></i>
                <span class="header-text">{{ $t('environment.batchSetTag') }}</span>
            </div>
            <div
                slot="right"
                class="node-handle"
            >
                <i class="devops-icon icon-cog"></i>
                <span
                    class="copy-btn"
                    @click="showAddTag"
                >
                    {{ $t('environment.addOrShowTag') }}
                </span>
            </div>
        </content-header>
        <div
            class="sub-view-port"
            v-show="showContent"
        >
        </div>
    </div>
</template>

<script>
    export default {
        data () {
            return {
                showContent: false,
                loading: {
                    isLoading: false,
                    title: this.$t('environment.loadingTitle')
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
        },
        watch: {
            projectId: async function (val) {
                this.$router.push({ name: 'envList' })
            },
        },
        mounted () {
          
        },
        methods: {
            toNodeList () {
                this.$router.push({ name: 'nodeList' })
            },
            showAddTag () {},
        }
    }
</script>

<style lang="scss" scoped>
@import '@/scss/conf';
.node-detail-wrapper {
    height: 100%;
    overflow: hidden;
    .info-header {
        height: 54px;
        line-height: 54px;
        background-color: #FAFBFD;
        border-bottom: none;
        box-shadow: none;
        .icon-arrows-left {
            margin-right: 4px;
            cursor: pointer;
            color: $iconPrimaryColor;
            font-size: 14px;
            font-weight: 600;
            vertical-align: middle;
        }
        .header-text {
            font-size: 14px;
            color: #313238;
        }
        .icon-cog {
            vertical-align: middle;
        }
        .node-handle {
            color: #3A84FF;
            cursor: pointer;
        }
        .copy-btn {
            margin-left: 0;
        }
    }
    .sub-view-port {
        height: calc(100vh - 158px);
        overflow: auto;
        padding: 20px;
        padding-top: 0;
    }
}
</style>
