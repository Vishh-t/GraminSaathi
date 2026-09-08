/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#f0fdf4',
          100: '#dcfce7',
          200: '#bbf7d0',
          300: '#8bd79b',
          400: '#4ade80',
          500: '#1f6c3a',
          600: '#166534',
          700: '#0f4d27',
          800: '#0a3a1d',
          900: '#062b15',
        },
        surface: {
          DEFAULT: '#f9f9ff',
          dim: '#d3daef',
          low: '#f1f3ff',
          container: '#e9edff',
          high: '#e1e8fd',
        },
        warning: {
          50: '#fffbeb',
          100: '#fef3c7',
          200: '#fde68a',
          300: '#fcd34d',
          400: '#fbbf24',
          500: '#f59e0b',
          600: '#d97706',
          700: '#b45309',
          800: '#92400e',
          900: '#78350f',
        },
        danger: {
          50: '#fef2f2',
          100: '#fee2e2',
          200: '#fecaca',
          300: '#fca5a5',
          400: '#f87171',
          500: '#ef4444',
          600: '#dc2626',
          700: '#b91c1c',
          800: '#991b1b',
          900: '#7f1d1d',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        hindi: ['"Noto Sans Devanagari"', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}