<template>
    <div class="timeline-collapse">
        <div
            class="collapse-item"
            v-for="(item, index) in 3"
            :key="index"
        >
            <div
                :class="{
                    'collapse-item-header': true,
                    'active': aa === index
                }" @click="aa === index ? aa = -1 : aa = index">
                [master] commit [ca0c98a1] pushed by fayewang
                <bk-icon
                    :class="{
                        'right-shape': true,
                        'right-down': aa === index
                    }"
                    svg
                    type="angle-right"
                    width="24"
                    height="24"
                />
                <a
                    v-if="aa === index"
                    class="one-click-trigger"
                    @click.stop="handleOneClickTrigger"
                >
                    {{ $t('codelib.一键重新触发') }}
                </a>
            </div>
            <table
                :class="{
                    'trigger-list-table': true,
                    'is-show-table': aa === index
                }"
                v-if="aa === index">
                <tbody>
                    <tr v-for="i in 3" :key="i">
                        <td width="25%">
                            <div class="cell">CI-App2.{{ index }}迭代</div>
                        </td>
                        <td width="55%">
                            <div class="cell">TRIGGER_NOT_MATCH  |  something wrong! the trigger is not match, and this is a vary longlonglong reason. this is a vary longlonglong reason.this is a vary longlonglonglonglong re…</div>
                        </td>
                        <td width="15%">
                            <div class="cell">
                                <a class="click-trigger">{{ $t('codelib.重新触发') }}</a>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'timeline-collapse',
        data () {
            return {
                aa: -1
            }
        },
        methods: {
            /**
             * 一键重新触发
             */
            handleOneClickTrigger () {
                console.log('一键重新触发')
            }
        }
    }
</script>

<style lang="scss" scoped>
    @keyframes fade-in {
    0% {
        opacity: 0;
    }
    30% {
        opacity: 0.3;
    }
    60% {
        opacity: 0.8;
    }
    100% {
        opacity: 1;
    }
    }
    .timeline-collapse {
        .collapse-item-header {
            position: relative;
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0 10px;
            height: 28px;
            font-size: 12px;
            border-radius: 2px;
            background-color: #FAFBFD;
            margin-bottom: 8px;
            cursor: pointer;
            &.active {
                background-color: #E1ECFF;
            }
            .one-click-trigger {
                position: absolute;
                right: 60px;
                font-size: 12px;
            }
        }
        .right-shape {
            transition: 200ms transform;
            &.right-down {
                transform: rotate(90deg);
            }
        }
        .trigger-list-table {
            width: 100%;
            margin: 8px 0;
            border: 1px solid #dfe0e5;
            border-radius: 2px;
            transition: opacity 3s linear;
            tr {
                border-bottom: 1px solid #dfe0e5;
                height: 42px;
                max-height: 56px;
            }
            td {
                padding: 8px 16px 8px;
                .cell {
                    font-size: 12px;
                    line-height: 20px;
                    display: -webkit-box;
                    overflow: hidden;
                    -webkit-line-clamp: 2;
                    -webkit-box-orient: vertical;
                }
            }
        }
        .is-show-table {
            animation: fade-in 1s ease-in-out;
        }
    }
</style>
