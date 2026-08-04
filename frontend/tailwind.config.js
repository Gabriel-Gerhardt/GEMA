const { colors, radii, fontFamily } = require('./src/theme/tokens');

/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./App.tsx', './src/**/*.{ts,tsx}'],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors: {
        cream: colors.cream,
        'green-primary': colors.green.primary,
        'green-deep': colors.green.deep,
        'green-mid': colors.green.mid,
        'mint-surface': colors.mint.surface,
        'mint-border': colors.mint.border,
        gold: colors.gold.DEFAULT,
        'gold-dark': colors.gold.dark,
        'text-primary': colors.text.primary,
        'text-muted': colors.text.muted,
        'text-placeholder': colors.text.placeholder,
        border: colors.border,
        danger: colors.danger,
        success: colors.success,
      },
      borderRadius: {
        button: `${radii.button}px`,
        'button-sm': `${radii.buttonSm}px`,
      },
      fontFamily,
    },
  },
  plugins: [],
};
