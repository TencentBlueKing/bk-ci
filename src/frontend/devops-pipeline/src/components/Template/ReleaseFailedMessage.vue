<template>
    <section class="release-failed-msg-wrapper">
        <p class="title">
            <logo
                class="update-icon"
                name="exclamation-circle"
                :size="12"
            />
            <span>
                {{ $t('template.failedReason') }}
            </span>
        </p>
        <ul>
            <li
                v-for="(value, name) in data"
                :key="name"
                class="list-item"
            >
                <ExpandableText
                    :name="name"
                    :content="value"
                />
            </li>
        </ul>
    </section>
</template>

<script setup>
    import ExpandableText from './ExpandableText.vue'
    defineProps({
        data: Object
    })
</script>

<style lang="scss">
    .release-failed-msg-wrapper {
        width: 552px;
        max-height: 230px;
        text-align: left;
        padding: 10px;
        border: 1px solid #F8B4B4;
        border-radius: 2px;
        background: #FFF0F0;
        font-size: 12px;
        overflow: auto;
        &::-webkit-scrollbar-thumb {
            background-color: #c4c6cc !important;
            border-radius: 5px !important;
            &:hover {
                background-color: #979ba5 !important;
            }
        }
        &::-webkit-scrollbar {
            width: 8px !important;
            height: 8px !important;
        }

        .title {
            margin-bottom: 8px;
            color: #4D4F56;
            font-weight: 700;
        }
        .list-item {
            width: 100%;
            .pipeline-name {
                flex-shrink: 0;
                margin-right: 5px;
            }
        }
    }
</style>
