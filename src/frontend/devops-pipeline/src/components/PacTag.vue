<template>
    <bk-popover
        theme="light"
        trigger="click"
        :disabled="!info"
        :max-width="360"
    >
        <span class="pac-tag">
            <i class="devops-icon icon-code" />
            PAC
        </span>
        <section
            v-if="info"
            slot="content"
        >
            <h6 class="pac-enable-tip">{{ $t('pacEnableTips') }}</h6>
            <div class="pac-info">
                <p class="pac-info-row">
                    <logo
                        size="16"
                        name="CODE_GIT"
                    />
                    <bk-link
                        class="pac-info-row-link"
                        target="_blank"
                        :href="info.webUrl"
                    >
                        {{ info.pathWithNamespace }}
                    </bk-link>
                </p>
                <p class="pac-info-row">
                    <logo
                        size="16"
                        name="file"
                    />
                    <bk-link
                        class="pac-info-row-link"
                        target="_blank"
                        :href="info.fileUrl"
                    >
                        <span class="pac-info-row-link-text-span">
                            {{ info.filePath }}
                        </span>
                        <logo
                            class="pac-info-row-link-text-icon"
                            size="16"
                            name="tiaozhuan"
                        />
                    </bk-link>
                </p>
            </div>
        </section>
    </bk-popover>
</template>

<script>
    import Logo from '@/components/Logo'
    export default {
        name: 'PacTag',
        components: {
            Logo
        },
        props: {
            info: {
                type: Object
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis.scss';
    .pac-tag {
        background: #E1ECFF;
        border-radius: 12px;
        width: 60px;
        height: 22px;
        line-height: 1;
        display: grid;
        align-items: center;
        grid-auto-flow: column;
        font-size: 12px;
        color: #699DF4;
        cursor: pointer;
        &:hover {
            color: #3A84FF;
            .devops-icon {
                background: #3A84FF;
            }
        }
        .devops-icon {
            width: 20px;
            height: 20px;
            display: flex;
            align-items: center;
            justify-content: center;
            background: #699DF4;
            color: white;
            border-radius: 50%;
            flex-shrink: 0;
        }
    }
    .pac-enable-tip {
        margin: 0 0 8px 0;
        font-size: 12px;
        color: #979BA5;
    }
    .pac-info {
        padding: 12px;
        background: #F5F7FA;
        display: grid;
        grid-gap: 6px;
        &-row {
            display: flex;
            align-items: center;
            grid-auto-flow: column;
            grid-gap: 8px;
            grid-template-columns: min-content 1fr;
            color: #979BA5;
            overflow: hidden;
            >svg {
                flex-shrink: 0;
            }
            &-link {
                flex: 1;
                display: flex;
                overflow: hidden;
                cursor: pointer;
                justify-content: flex-start;
                .bk-link-text {
                    width: 100%;
                    display: flex;
                    align-items: center;
                    grid-gap: 8px;
                    .pac-info-row-link-text-span {
                        @include ellipsis();
                    }
                    .pac-info-row-link-text-icon {
                        flex-shrink: 0;
                        color: #3A84FF;
                    }
                }
            }
        }

    }
</style>
