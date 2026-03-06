<template>
    <section class="client-basic-info">
        <img
            v-if="detail.logoUrl"
            :src="detail.logoUrl"
            class="app-logo"
            alt="logo"
        >
        <div class="info-list">
            <div class="info-item">
                <span class="label">{{ $t('store.应用名称') }}{{ showColon ? '：' : '' }}</span>
                <span class="value">{{ detail.name || '--' }}</span>
            </div>
            <div class="info-item">
                <span class="label">{{ $t('store.应用分类') }}{{ showColon ? '：' : '' }}</span>
                <span class="value">{{ detail.classify?.classifyName || '--' }}</span>
            </div>
            <div class="info-item">
                <span class="label">{{ $t('store.应用简介') }}{{ showColon ? '：' : '' }}</span>
                <span class="value">{{ detail.summary || '--' }}</span>
            </div>
            <div class="info-item description-item">
                <span class="label">{{ $t('store.详细描述') }}{{ showColon ? '：' : '' }}</span>
                <div class="value">
                    <mavon-editor
                        v-if="detail.description"
                        :editable="false"
                        default-open="preview"
                        :subfield="false"
                        :toolbars-flag="false"
                        :box-shadow="false"
                        :external-link="false"
                        preview-background="#fff"
                        :language="$i18n.locale === 'en-US' ? 'en' : $i18n.locale"
                        :value="detail.description"
                    />
                    <span v-else>--</span>
                </div>
            </div>
        </div>
    </section>
</template>

<script setup name="ClientBasicInfo">
    defineProps({
        detail: {
            type: Object,
            required: true
        },
        showColon: {
            type: Boolean,
            default: false
        }
    })
</script>

<style lang="scss" scoped>
.client-basic-info {
    display: flex;
    font-size: 12px;
    
    .app-logo {
        width: 100px;
        height: 100px;
        margin-right: 32px;
        flex-shrink: 0;
    }
    
    .info-list {
        flex: 1;
    }
    
    .info-item {
        display: flex;
        margin-bottom: 12px;
        
        &:last-child {
            margin-bottom: 0;
        }
        
        &.description-item {
            .label {
                margin-bottom: 8px;
            }
            
            .value {
                width: 100%;
            }
            
            ::v-deep .v-note-wrapper {
                max-height: 500px;
                border: 1px solid #c4c6cc;
                
                .v-note-panel {
                    box-shadow: none;
                }
            }
        }
        
        .label {
            flex-shrink: 0;
            width: 150px;
            color: #63656e;
        }
        
        .value {
            flex: 1;
            color: #313238;
            min-height: 40px;
            
            ::v-deep .v-note-wrapper {
                border: none;
                box-shadow: none;
                
                .v-note-panel {
                    border: none;
                }
                
                .v-show-content {
                    padding: 0;
                    background: #fff;
                }
            }
        }
    }
}
</style>
