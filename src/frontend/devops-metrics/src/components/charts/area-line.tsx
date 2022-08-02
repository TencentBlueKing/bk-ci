import {
  defineComponent,
  PropType,
  watch,
  onMounted,
  onBeforeUnmount,
  ref,
} from 'vue';
import BKChart from '@blueking/bkcharts';

export interface IData {
  label: string,
  list: Array<string | number>,
  backgroundColor?: string,
  borderColor?: string
}

export default defineComponent({
  props: {
    data: Array as PropType<Array<IData>>,
    labels: Array,
    title: String,
  },

  setup(props) {
    const canvasRef = ref(null);
    let chart;

    const destoryChart = () => {
      chart?.destroy();
    };
    const draw = () => {
      destoryChart();
      const { data, labels, title } = props;
      chart = new BKChart(canvasRef.value, {
        type: 'line',
        data: {
          labels,
          datasets: data?.map(item => ({
            label: item.label,
            fill: true,
            backgroundColor: item.backgroundColor || 'rgba(43, 124, 255,0.3)',
            borderColor: item.borderColor || 'rgba(43, 124, 255,1)',
            lineTension: 0,
            borderWidth: 2,
            pointRadius: 2,
            pointHitRadius: 3,
            pointHoverRadius: 3,
            data: [...item.list],
          })),
        },
        options: {
          maintainAspectRatio: false,
          responsive: true,
          plugins: {
            tooltip: {
              bodySpacing: 10,
              mode: 'x',
              intersect: false,
              enableItemActive: true,
              singleInRange: true,
            },
            legend: {
              position: 'bottom',
              legendIcon: 'arc',
              align: 'center',
              labels: {
                padding: 10,
                usePointStyle: true,
                pointStyle: 'dash',
              },
            },
            crosshair: {
              enabled: true,
              mode: 'x',
              style: {
                x: {
                  enabled: true,
                  color: '#cde0ff',
                  weight: 1,
                  borderStyle: 'solid',
                },
                y: {
                  enabled: false,
                },
              },
            },
          },
          scales: {
            yAxes: {
              scaleLabel: {
                display: true,
                padding: 0,
              },
              grid: {
                drawTicks: false,
                borderDash: [5, 5],
              },
              title: {
                display: true,
                text: title,
                align: 'start',
                color: '#979BA5',
              },
              ticks: {
                padding: 10,
                color: '#979BA5',
              },
              min: 0,
            },
            xAxes: {
              scaleLabel: {
                display: true,
                padding: 0,
              },
              grid: {
                drawTicks: false,
                display: false,
              },
              ticks: {
                padding: 10,
                sampleSize: 20,
                autoSkip: true,
                maxRotation: 0,
              },
            },
          },
        },
      });
    };

    watch(
      [
        props.data,
        props.labels,
      ],
      draw,
    );

    onMounted(draw);

    onBeforeUnmount(destoryChart);

    return () => (
      <div class="canvas-wrapper">
        <canvas ref={canvasRef}></canvas>
      </div>
    );
  },
});
