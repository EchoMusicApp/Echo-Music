import { useEffect, useState } from 'react'

export function useIsMobileDevice(): boolean {
  const [isMobile, setIsMobile] = useState<boolean>(() => {
    if (typeof window === 'undefined') return false
    const ua = navigator.userAgent || ''
    const isMobileUa = /android|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(ua)
    const isSmallScreen = window.innerWidth <= 768
    return isMobileUa || isSmallScreen
  })

  useEffect(() => {
    const handleResize = () => {
      const ua = navigator.userAgent || ''
      const isMobileUa = /android|iphone|ipad|ipod|blackberry|iemobile|opera mini/i.test(ua)
      const isSmallScreen = window.innerWidth <= 768
      setIsMobile(isMobileUa || isSmallScreen)
    }

    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  return isMobile
}
