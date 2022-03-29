<template>
    <span style="font-size:0" v-bind="$attrs" v-on="$listeners">
        <svg :width="size" :height="size" style="fill: currentColor; stroke: currentColor">
            <title v-if="title">{{ title }}</title>
            <use v-bind="{ 'xlink:href': svgHref }"></use>
        </svg>
    </span>
</template>

<script>
    export default {
        name: 'svg-logo',
        props: {
            name: String,
            size: {
                type: [String, Number],
                default: 18
            },
            title: {
                type: String
            }
        },
        computed: {
            svgHref () {
                const id = `bk-pipeline-${this.name}`
                return document.getElementById(id) ? `#${id}` : '#bk-pipeline-order'
            }
            
        }
    }
</script>
