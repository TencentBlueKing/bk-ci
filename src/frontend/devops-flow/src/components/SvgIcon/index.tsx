import { defineComponent } from 'vue'

export interface SvgIconProps {
  name: string
  size?: number | string,
  onClick?: (e: MouseEvent) => void,
}

export const SvgIcon = defineComponent<SvgIconProps>({
  name: 'SvgIcon',
  props: {
    name: String,
    size: {
      type: [Number, String],
      default: 16,
    },
  },
  setup(props: SvgIconProps) {
    return () => (
      <svg width={props.size} height={props.size} style="fill: currentColor">
        <use href={`#ci-flow-${props.name}`} />
      </svg>
    )
  },
})
