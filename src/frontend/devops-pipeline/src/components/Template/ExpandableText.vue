<template>
    <div class="text-container">
        <div class="content">
            <span>{{ name }}: {{ parsedContent.message }}</span>
            <div
                v-if="parsedContent.errors?.length"
                class="error-details"
            >
                <div
                    v-for="err in parsedContent.errors"
                    :key="err.errorTitle"
                    class="error-category"
                >
                    <div class="error-category-title">{{ err.errorTitle }}</div>
                    <ul class="error-detail-list">
                        <li
                            v-for="(detail, idx) in err.errorDetails"
                            :key="idx"
                            v-html="detail"
                        ></li>
                    </ul>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
    import { computed } from 'vue'

    const props = defineProps({
        content: String,
        name: String
    })

    const parsedContent = computed(() => {
        try {
            return JSON.parse(props.content || '{}')
        } catch (e) {
            return { message: props.content || '' }
        }
    })
</script>

<style lang="scss" scoped>
.text-container {
    vertical-align: top;
    margin-bottom: 5px;
    word-break: break-all;
}

.content {
    max-width: 600px;
    overflow: auto;
    line-height: 1.5;
}

.error-details {
    margin-top: 6px;
    .error-category {
        margin-bottom: 4px;
    }
    .error-category-title {
        font-weight: 700;
        font-size: 12px;
    }
    .error-detail-list {
        padding-left: 20px;
        :deep(li) {
            list-style: circle;
        }
        :deep(a) {
            padding-left: 4px;
            color: #3a84ff !important;
        }
    }
}
</style>
