<template>
    <main class="pipeline-group-auth">
        <aside class="pipeline-group-auth-role-panel">
            <header class="pipeline-group-auth-role-header">
                {{$t('权限角色')}}
            </header>
            <ul class="pipeline-group-auth-role-list">
                <li v-for="role in roles" :key="role.name" :class="{ active: role.name === '拥有者' }">
                    {{ role.name }}
                    <span>{{ role.count }}</span>
                </li>
            </ul>
            <footer>
                <bk-button>{{ $t('关闭组权限管理') }}</bk-button>
            </footer>
        </aside>
        <!-- <bk-exception type="403" scene="part">
            <p>{{$t('没有权限')}}</p>
            <p class="text-desc">{{$t('启用组权限管理后，组成员权限将作用于组内所有流水线')}}</p>
            <footer>
                <bk-button>{{$t('返回流水线组')}}</bk-button>
                <bk-button theme="primary">{{$t('启用组权限管理')}}</bk-button>
            </footer>
        </bk-exception> -->
    </main>
</template>

//  如需启用流水线组权限管理，请联系流水线创建者或CI管理员

<script>
    export default {
        computed: {
            roles () {
                return [
                    {
                        name: '组成员管理员',
                        count: 12
                    },
                    {
                        name: '拥有者',
                        count: 12
                    },
                    {
                        name: '执行者',
                        count: 12
                    },
                    {
                        name: '查看者',
                        count: 12
                    }
                ]
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis';
    .pipeline-group-auth {
        display: flex;
        flex: 1;
        align-items: center;
        margin: 24px;
        background: white;
        box-shadow: 0 2px 2px 0 rgba(0,0,0,0.15);
        .text-desc {
            margin: 8px 0 24px 0;
            font-size: 12px;
            color: #979BA5;
        }
        .pipeline-group-auth-role-panel {
            display: flex;
            flex-direction: column;
            width: 232px;
            height: 100%;
            background: #FAFBFD;
            border-right: 1px solid #DCDEE5;
            .pipeline-group-auth-role-header {
                display: flex;
                align-items: center;
                height: 52px;
                padding: 0 16px;
                color: black;
                border-bottom: 1px solid #DCDEE5;
            }
            .pipeline-group-auth-role-list {
                flex: 1;
                > li {
                    position: relative;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    height: 40px;
                    font-size: 14px;
                    padding: 0 32px;
                    border-bottom: 1px solid #DCDEE5;

                    &.active {
                        background: white;
                        &:after {
                            position: absolute;
                            content: '';
                            width: 1px;
                            height: 100%;
                            background: white;
                            right: -1px;
                            top: 0;
                        }
                    }
                    > span {
                        font-size: 12px;
                        background: #F0F1F5;
                        text-align: center;
                        line-height: 16px;
                        width: 32px;
                        @include ellipsis();
                    }
                }
            }
            > footer {
                display: flex;
                align-items: center;
                justify-content: center;
                margin: 24px;
            }
        }
    }
</style>
