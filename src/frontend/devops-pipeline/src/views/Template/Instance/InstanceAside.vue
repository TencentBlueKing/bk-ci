<template>
    <div class="instance-aside">
        <div class="operate">
            <bk-button
                v-if="isInstanceCreateType"
                icon="plus"
            >
                {{ $t('new') }}
            </bk-button>
            <i18n
                v-else
                class="select-pipeline"
                tag="div"
                path="template.selectPipelineNum"
            >
                <span class="select-num">
                    {{ 5 }}
                </span>
            </i18n>
            <div class="batch-edit-btn">
                <Logo
                    name="edit"
                    size="14"
                    style="fill:#3c96ff;position:relative;top:2px;"
                />
                <span>
                    {{ $t('template.batchEdit') }}
                </span>
            </div>
        </div>
        <ul class="instance-list">
            <li class="item active">
                模板实例名称1
            </li>
            <li class="item">
                模板实例名称2
            </li>
            <li class="item">
                模板实例名称3
            </li>
        </ul>
    </div>
</template>

<script setup>
    import { defineProps } from 'vue'
    import Logo from '@/components/Logo'
    
    defineProps({
        isInstanceCreateType: Boolean
    })
</script>

<style lang="scss" scoped>
    .instance-aside {
        height: 100%;
        font-size: 12px;
        padding: 24px;
        .operate {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .select-pipeline {
            color: #4d4f56;
        }
        .select-num {
            font-weight: 700;
        }
        .batch-edit-btn {
            cursor: pointer;
            color: #3A84FF;
        }
    }
    .instance-list {
        list-style: none;
        margin-top: 20px;
        .item {
            height: 32px;
            line-height: 32px;
            padding: 0 15px;
            border-radius: 2px;
            background: #F5F7FA;
            margin-bottom: 10px;
            cursor: pointer;
            &.active,
            &:hover {
                color: #3A84FF;
                font-weight: 700;
                background: #E1ECFF;
            }
        }
    }
</style>
