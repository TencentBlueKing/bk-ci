/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      width: {
        platformMainWidth: 'calc(100% - 240px)'
      },
      height: {
        mainHeight: 'calc(100% - 55px)',
        tableHeight: 'calc(100% - 48px)',
        formHeight: 'calc(100% - 66px)'
      },
      boxShadow: {
        '3xl': '0 3px 4px 0 #0000000a',
        exalmple: '-1px 0 0 0 #DCDEE5',
        eg: '0 2px 6px 0 #0000001a'
      }
    },
  },
  plugins: [],
}

