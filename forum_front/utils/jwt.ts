// JWT 토큰에서 payload 추출
export function decodeJWT(token: string): any {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch (error) {
    return null
  }
}

// JWT 토큰에서 username 추출
export function getUsernameFromToken(): string | null {
  if (typeof window === 'undefined') return null
  
  const { getCookie } = require('@/utils/cookies')
  const token = getCookie('accessToken')
  if (!token) return null

  const decoded = decodeJWT(token)
  return decoded?.sub || decoded?.username || null
}

