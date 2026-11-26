import { ref, watch } from 'vue'

export type ColorMode = 'light' | 'dark'

const STORAGE_KEY = 'panel-color-mode'

function initialMode(): ColorMode {
  if (typeof window === 'undefined') return 'dark'
  try {
    return window.localStorage.getItem(STORAGE_KEY) === 'light' ? 'light' : 'dark'
  } catch {
    return 'dark'
  }
}

const colorMode = ref<ColorMode>(initialMode())

watch(
  colorMode,
  (mode) => {
    if (typeof document === 'undefined') return
    document.documentElement.dataset.theme = mode
    document.documentElement.style.colorScheme = mode
    try {
      window.localStorage.setItem(STORAGE_KEY, mode)
    } catch {
      // The current page can still switch themes when storage is unavailable.
    }
  },
  { immediate: true },
)

export function useTheme() {
  const toggleTheme = () => {
    colorMode.value = colorMode.value === 'dark' ? 'light' : 'dark'
  }

  return { colorMode, toggleTheme }
}
