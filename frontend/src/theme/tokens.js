// Design tokens transcribed from DESIGN.md ("Broto" design system, GAB-31).
// Single source of truth: required directly by tailwind.config.js (NativeWind
// classNames) and imported by the few call sites that need a raw value
// instead of a className (react-native-svg fills, Alert has no style prop).
// Keep in sync with DESIGN.md if the design ever changes.

const colors = {
  cream: '#fbf8f0',
  green: {
    primary: '#3e7a2f',
    deep: '#2f5233',
    mid: '#4f8f3d',
    midAlt: '#5a9c46',
  },
  mint: {
    surface: '#e9f3e3',
    border: '#d5e6c9',
  },
  gold: {
    DEFAULT: '#c97b2e',
    dark: '#8f5219',
  },
  text: {
    primary: '#332e22',
    muted: '#6b6354',
    placeholder: '#a8a293',
  },
  border: '#e8e1d3',
  danger: '#c23030',
  success: '#27803f',
  white: '#ffffff',
};

// The signature shape is an asymmetric corner: top-left and bottom-right
// rounded, top-right and bottom-left sharp. Which corners is what makes it read
// as intentional, so the pairs travel together — see DESIGN.md "Radius".
const radii = {
  card: 24,
  cardAlt: 8,
  cardMd: 20,
  cardMdAlt: 6,
  cardSm: 16,
  cardSmAlt: 5,
  tile: 22,
  tileAlt: 8,
  button: 14,
  // DESIGN.md's compact button (the emergency panel's "Ligar agora") is 13px.
  buttonSm: 13,
  input: 12,
  inputSm: 9,
  qr: 16,
  pill: 999,
};

/** Card shadow per DESIGN.md: tinted green rather than neutral black, barely-there lift. */
const shadow = {
  card: {
    shadowColor: colors.green.deep,
    shadowOpacity: 0.05,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
  },
};

const fontFamily = {
  figtree: ['Figtree_400Regular'],
  figtreeMedium: ['Figtree_500Medium'],
  figtreeSemibold: ['Figtree_600SemiBold'],
  figtreeBold: ['Figtree_700Bold'],
  figtreeExtrabold: ['Figtree_800ExtraBold'],
};

module.exports = { colors, radii, shadow, fontFamily };
