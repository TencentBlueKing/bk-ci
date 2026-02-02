/**
 * LogIcon Component
 * 日志组件专用的图标组件 - 提供内联 SVG 图标
 */
import { defineComponent, type PropType } from 'vue'

type IconName = 'angle-right' | 'download' | 'search' | 'filter' | 'close' | 'bug'

const iconMap: Record<IconName, (size: number) => string> = {
  'angle-right': (size) => `
    <svg width="${size}" height="${size}" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M6 12L10 8L6 4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
  `,
  'download': (size) => `
    <svg width="${size}" height="${size}" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M8 11V2M8 11L5 8M8 11L11 8M3 11V13C3 13.5523 3.44772 14 4 14H12C12.5523 14 13 13.5523 13 13V11" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
  `,
  'search': (size) => `
    <svg width="${size}" height="${size}" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="7" cy="7" r="4.5" stroke="currentColor" stroke-width="1.5"/>
      <path d="M10.5 10.5L14 14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
    </svg>
  `,
  'filter': (size) => `
    <svg width="${size}" height="${size}" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M2 4H14M4 8H12M6 12H10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
    </svg>
  `,
  'close': (size) => `
    <svg width="${size}" height="${size}" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M12 4L4 12M4 4L12 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
  `,
  'bug': (size) => `
    <svg width="${size}" height="${size}" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M8 2V1M8 2C9.65685 2 11 3.34315 11 5V6C11 6.55228 11.4477 7 12 7H13M8 2C6.34315 2 5 3.34315 5 5V6C5 6.55228 4.55228 7 4 7H3M3 7V9C3 9.55228 3.44772 10 4 10H5M3 7H1M12 7V9C12 9.55228 11.5523 10 11 10H10M12 7H15M5 10C5 10.5523 5.44772 11 6 11H7M10 10C10 10.5523 9.55228 11 9 11H8M7 11V13C7 13.5523 7.44772 14 8 14V14M9 11V13C9 13.5523 8.55228 14 8 14V14M8 14V15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
  `,
}

export const LogIcon = defineComponent({
  name: 'LogIcon',
  props: {
    name: {
      type: String as PropType<IconName>,
      required: true,
    },
    size: {
      type: Number,
      default: 16,
    },
  },
  setup(props) {
    return () => {
      const iconSvg = iconMap[props.name]?.(props.size) || ''
      return <span innerHTML={iconSvg} style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }} />
    }
  },
})
