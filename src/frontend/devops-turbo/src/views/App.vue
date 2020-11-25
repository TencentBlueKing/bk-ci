<template>
    <div class="devops-turbo">
        <aside-nav :nav="nav" :menu-click="menuClick">
            <router-view slot="content" class="turbo-content"></router-view>
        </aside-nav>
    </div>
</template>

<script>
    export default {
        name: 'app',
        computed: {
            nav () {
                return {
                    icon: 'turbo',
                    title: '编译加速',
                    url: '',
                    menu: [
                        {
                            id: 'turboOverview',
                            name: '总览',
                            icon: 'icon-overview',
                            showChildren: false,
                            disable: false
                        },
                        {
                            id: 'registration',
                            name: '任务注册',
                            icon: 'icon-task-registry',
                            showChildren: false
                        },
                        {
                            id: 'acceleration',
                            name: '加速任务',
                            icon: 'icon-speed-up',
                            showChildren: false,
                            disable: false
                        },
                        {
                            id: 'record',
                            name: '加速记录',
                            icon: 'icon-speed-record',
                            showChildren: false,
                            disable: false
                        }
                    ]
                }
            }
        },
        methods: {
            menuClick (name) {
                this.$router.push({
                    name
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '../assets/scss/conf';

    .devops-turbo {
        flex: 1;
        display: flex;
        // flex-direction: column;
        .side-header {
            display: flex;
            width: 100%;
            height: 60px;
            padding: 0 30px;
            border-bottom: 1px solid #dde4eb;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.03);
            background: white;
            align-items: center;
            > svg {
                margin-right: 11px;
            }
            > span {
                letter-spacing: .5px;
                font-size: 16px;
                color: #333948;
            }
            // > i {
            //     padding-left: 10px;
            //     color: #c4cdd6;
            //     cursor: pointer;
            // }
        }
        .sub-view-port {
            height: calc(100% - 60px);
            padding: 20px;
            overflow: auto;
        }
    }
    .turbo-side-bar {
        width: 240px;
        border-right: 1px solid $borderWeightColor;
    }
    .turbo-content {
        position: relative;
        align-items: stretch;
        flex: 1;
        overflow: hidden;
        .wrapper-header {
            position: relative;
            display: flex;
            justify-content: center;
            padding: 14px 20px 13px;
            width: 100%;
            height: 60px;
            line-height: 32px;
            border-bottom: 1px solid $borderWeightColor;
            color: $fontColor;
            .title {
                position: absolute;
                left: 20px;
                top: 14px;
                font-size: 16px;
            }
            .head-link {
                position: absolute;
                top: 14px;
                right: 30px;
                a {
                    color: #0082FF;
                }
            }
        }
    }
</style>
