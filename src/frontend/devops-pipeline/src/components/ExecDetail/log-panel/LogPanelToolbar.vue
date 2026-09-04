<template>
    <div v-if="visible" class="lp-toolbar">
        <div class="lp-levels">
            <label v-for="lv in allLevels" :key="lv" class="lp-level">
                <input
                    type="checkbox"
                    :value="lv"
                    :checked="selectedLevels.includes(lv)"
                    @change="toggleLevel(lv)"
                >
                {{ lv }}
            </label>
        </div>
        <input
            class="lp-search"
            :value="keyword"
            :placeholder="$t('logPanel.search')"
            @input="$emit('update:keyword', $event.target.value)"
        >
        <button type="button" class="lp-btn" @click="$emit('toggle-time')">
            {{ showTime ? $t('logPanel.hideTime') : $t('logPanel.showTime') }}
        </button>
        <button type="button" class="lp-btn" @click="$emit('toggle-wrap')">
            {{ wrap ? $t('logPanel.unwrap') : $t('logPanel.wrap') }}
        </button>
        <button type="button" class="lp-btn" @click="$emit('download')">
            {{ $t('downloadLog') }}
        </button>
    </div>
</template>

<script>
    const ALL = ['INFO', 'WARN', 'ERROR', 'DEBUG']

    export default {
        props: {
            visible: { type: Boolean, default: true },
            selectedLevels: { type: Array, default: () => ['INFO', 'WARN', 'ERROR'] },
            keyword: { type: String, default: '' },
            showTime: { type: Boolean, default: false },
            wrap: { type: Boolean, default: true }
        },
        data () {
            return { allLevels: ALL }
        },
        methods: {
            toggleLevel (lv) {
                const next = this.selectedLevels.includes(lv)
                    ? this.selectedLevels.filter(i => i !== lv)
                    : this.selectedLevels.concat(lv)
                this.$emit('update:selectedLevels', next.length ? next : ['INFO'])
            }
        }
    }
</script>

<style lang="scss" scoped>
.lp-toolbar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px 12px;
    padding: 8px 16px;
    color: #c4c6cc;
    font-size: 12px;
}
.lp-levels { display: flex; gap: 10px; }
.lp-level { display: flex; align-items: center; gap: 4px; cursor: pointer; }
.lp-search {
    width: 180px;
    height: 26px;
    padding: 0 8px;
    border: 1px solid #4b4d55;
    background: #2e3342;
    color: #fff;
    border-radius: 2px;
}
.lp-btn {
    height: 26px;
    padding: 0 10px;
    border: 1px solid #4b4d55;
    background: transparent;
    color: #c4c6cc;
    cursor: pointer;
    border-radius: 2px;
}
</style>
