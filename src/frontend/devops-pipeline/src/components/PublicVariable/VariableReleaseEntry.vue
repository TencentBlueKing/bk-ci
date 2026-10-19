<template>
    <section class="variable-release-entry">
        <header class="variable-release-header">
            <VariableBreadCrumb
                :variable-name="'test111'"
            />
            <aside class="variable-release-right-aside">
                <bk-button
                    @click="$router.back()"
                >
                    {{ $t('cancel') }}
                </bk-button>
                <bk-button
                    theme="primary"
                    @click="handleShowReleaseSlider"
                >
                    {{ $t('release') }}
                </bk-button>
            </aside>
        </header>
        <section class="variable-content-wrapper">
            <basic-info
                class="variable-detail-main"
                is-release
                read-only
                :group-data="groupData"
            />
        </section>
        <release-variable-slider
            :value.sync="showReleaseSlider"
        />
    </section>
</template>

<script setup>
    import { ref, computed, watch } from 'vue'
    import UseInstance from '@/hook/useInstance'
    import VariableBreadCrumb from './VariableBreadCrumb'
    import ReleaseVariableSlider from './ReleaseVariableSlider'
    import BasicInfo from './BasicInfo'
    const { proxy } = UseInstance()
    const showReleaseSlider = ref(false)

    const groupData = computed(() => proxy.$store.state.publicVar.groupData)
    watch(() => groupData.value?.groupName, (val) => {
        if (!val) {
            proxy.$router.push({
                name: 'PublicVarList'
            })
        }
    }, {
        deep: true,
        immediate: true
    })
    function handleShowReleaseSlider () {
        showReleaseSlider.value = true
    }
</script>

<style lang="scss" >
@import './../../scss/conf';
@import '@/scss/mixins/ellipsis';
.variable-release-entry {
    .variable-release-header {
        width: 100%;
        height: 48px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        align-self: stretch;
        background-color: white;
        box-shadow: 0 2px 5px 0 #333c4808;
        border-bottom: 1px solid $borderLightColor;
        padding: 0 0 0 24px;
    }
    .variable-release-right-aside {
        height: 100%;
        display: flex;
        justify-self: flex-end;
        align-items: center;
        grid-gap: 10px;
        padding-right: 16px;
    }
    
    .variable-content-wrapper {
        overflow: hidden;
        height: calc(100% - 48px);
    }
    .variable-detail-main {
        padding: 0 20px 85px
    }
}
            
</style>