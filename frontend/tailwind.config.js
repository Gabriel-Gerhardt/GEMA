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
        'green-mid-alt': colors.green.midAlt,
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
      // Named so screens stop hand-writing `rounded-tl-[24px] rounded-tr-[8px]…`
      // arbitrary values, which left the radius tokens defined but unused and
      // let the asymmetric-corner rule drift screen by screen.
      borderRadius: {
        card: `${radii.card}px`,
        'card-alt': `${radii.cardAlt}px`,
        'card-md': `${radii.cardMd}px`,
        'card-md-alt': `${radii.cardMdAlt}px`,
        'card-sm': `${radii.cardSm}px`,
        'card-sm-alt': `${radii.cardSmAlt}px`,
        tile: `${radii.tile}px`,
        'tile-alt': `${radii.tileAlt}px`,
        button: `${radii.button}px`,
        'button-sm': `${radii.buttonSm}px`,
        input: `${radii.input}px`,
        'input-sm': `${radii.inputSm}px`,
        qr: `${radii.qr}px`,
      },
      fontFamily,
    },
  },
  plugins: [],
};
