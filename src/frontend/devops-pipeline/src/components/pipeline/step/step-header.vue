<template>
    <section class="step-header clearfix">
        <div class="step-header__number">{{ ~~config.stageIndex + 1 }}</div>
        <div class="step-header__content clearfix"
            @click="titleClick">
            <p class="title text-overflow fl">
                {{ config.title }}
            </p>
            <p class="subtitle text-overflow fr">{{ config.subtitle }}</p>
            <!-- <div class="tools"
                :class="{'status': config.pageType === 'status'}">
                <template
                    v-if="config.tools.length > 0"
                    v-for="item of config.tools">
                    <i class="bk-icon"
                        :class="'icon-' + item.icon"
                        :title="item.alt"
                        @click="item.handler(config.stageIndex)">
                    </i>
                </template>
                <template v-if="config.pageType === 'status'">
                    <i class="bk-icon"
                        v-if="config.status !== 'loading'"
                        :class="['icon-' + statusMap[config.status], config.status]">
                    </i>
                    <span class="status-loading"
                        v-else>
                        <img src="./../../../images/step_loading.png" alt="loading">
                    </span>
                </template>
            </div> -->
        </div>
    </section>
</template>

<script>
    import { mapGetters } from 'vuex'

    export default {
        props: {
            config: {
                type: Object,
                default () {
                    return {
                        stageIndex: 0,
                        tools: [],
                        title: '',
                        subtitle: '',
                        pageType: 'edit',
                        status: 'loading'
                    }
                }
            }
        },
        computed: {
            ...mapGetters({
                'statusMap': 'pipeline/getStatusToIconMap'
            })
        },
        methods: {
            titleClick () {
                this.$emit('title-click')
            }
        }
    }
</script>
