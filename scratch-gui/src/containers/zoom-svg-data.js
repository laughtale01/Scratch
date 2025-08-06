// 修正済みのSVGデータ（opacity問題を解決）

// ズームインSVG（opacity: 0.75を1に変更）
export const ZOOM_IN_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 36 36">
  <circle fill="#231f20" opacity="0.15" cx="18" cy="18" r="18"/>
  <circle fill="#fff" cx="18" cy="18" r="16"/>
  <g opacity="1">
    <circle fill="none" stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" cx="18" cy="18" r="7"/>
    <line stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" x1="23" y1="23" x2="26" y2="26"/>
    <line stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" x1="16" y1="18" x2="20" y2="18"/>
    <line stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" x1="18" y1="16" x2="18" y2="20"/>
  </g>
</svg>`;

// ズームアウトSVG（opacity: 0.75を1に変更）
export const ZOOM_OUT_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 36 36">
  <circle fill="#231f20" opacity="0.15" cx="18" cy="18" r="18"/>
  <circle fill="#fff" cx="18" cy="18" r="16"/>
  <g opacity="1">
    <circle fill="none" stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" cx="18" cy="18" r="7"/>
    <line stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" x1="23" y1="23" x2="26" y2="26"/>
    <line stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" x1="16" y1="18" x2="20" y2="18"/>
  </g>
</svg>`;

// ズームリセットSVG（opacity: 0.75を1に変更）
export const ZOOM_RESET_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 36 36">
  <circle fill="#231f20" opacity="0.15" cx="18" cy="18" r="18"/>
  <circle fill="#fff" cx="18" cy="18" r="16"/>
  <g opacity="1">
    <path fill="none" stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M22.68,11.32a7,7,0,0,1,0,9.9l-4.95,4.95a7,7,0,0,1-9.9,0"/>
    <polyline fill="none" stroke="#575e75" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" points="19 11.13 22.87 11.13 22.87 15"/>
  </g>
</svg>`;

// Data URL版（エンコード不要）
export const ZOOM_IN_DATA_URL = 'data:image/svg+xml,' + encodeURIComponent(ZOOM_IN_SVG);
export const ZOOM_OUT_DATA_URL = 'data:image/svg+xml,' + encodeURIComponent(ZOOM_OUT_SVG);
export const ZOOM_RESET_DATA_URL = 'data:image/svg+xml,' + encodeURIComponent(ZOOM_RESET_SVG);