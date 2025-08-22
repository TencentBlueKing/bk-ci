<template>
    <div class="text-container">
        <div
            ref="contentEl"
            :class="['content', { 'line-clamp': isExpanded }]"
            @click="toggle"
        >
            {{ name }}: {{ JSON.parse(content).message }}
        </div>
        <bk-button
            v-if="showExpandBtn"
            @click.stop="toggle"
            text
            class="expand-btn"
        >
            {{ isExpanded ? $t('settings.fold') : $t('settings.open') }}
        </bk-button>
    </div>
</template>
  
  <script setup>
    import { ref, onMounted, nextTick, watch } from 'vue'
  
    const props = defineProps({
        content: String,
        name: String
    })
  
    const isExpanded = ref(false)
    const showExpandBtn = ref(false)
    const contentEl = ref(null)
  
    const checkOverflow = async () => {
        await nextTick()
        if (!contentEl.value) return
    
        const style = window.getComputedStyle(contentEl.value)
        const lineHeight = parseInt(style.lineHeight) || 18
        const maxAllowedHeight = lineHeight * 3
    
        showExpandBtn.value = contentEl.value.scrollHeight > maxAllowedHeight
    }
  
    watch(() => props.content, checkOverflow)
  
    onMounted(() => {
        checkOverflow()
        window.addEventListener('resize', checkOverflow)
    })
  
    const toggle = () => {
        if (showExpandBtn.value) {
            isExpanded.value = !isExpanded.value
        }
    }
  </script>
  
  <style lang="scss" scoped>
  .text-container {
    align-items: flex-end;
    vertical-align: top;
    margin-bottom: 5px;
    word-break: break-all;
  }
  
  .content {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
    overflow: hidden;
    max-width: 600px;
    line-height: 1.5;
    transition: all 0.3s;
  }
  
  .line-clamp {
    -webkit-line-clamp: 100 !important;
  }
  
  .expand-btn {
    font-size: 12px;
  }
  
  .expand-btn:hover {
    text-decoration: underline;
  }
  </style>
