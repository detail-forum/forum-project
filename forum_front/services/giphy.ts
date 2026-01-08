// Giphy API 서비스
// Next.js 빌드 타임에 환경 변수가 번들에 포함됩니다
const GIPHY_API_KEY = process.env.NEXT_PUBLIC_GIPHY_API_KEY || ''
const GIPHY_API_URL = 'https://api.giphy.com/v1'

export interface GiphyGif {
  id: string
  title: string
  images: {
    fixed_height: {
      url: string
      width: string
      height: string
    }
    original: {
      url: string
      width: string
      height: string
    }
  }
  url: string
}

export interface GiphySearchResponse {
  data: GiphyGif[]
  pagination: {
    total_count: number
    count: number
    offset: number
  }
}

export const giphyApi = {
  // 트렌딩 GIF 가져오기
  getTrending: async (limit: number = 25, offset: number = 0): Promise<GiphySearchResponse> => {
    if (!GIPHY_API_KEY || GIPHY_API_KEY.trim() === '') {
      console.warn('GIPHY API 키가 설정되지 않았습니다. GIF 기능이 비활성화됩니다.')
      return { data: [], pagination: { total_count: 0, count: 0, offset: 0 } }
    }

    try {
      const response = await fetch(
        `${GIPHY_API_URL}/gifs/trending?api_key=${GIPHY_API_KEY}&limit=${limit}&offset=${offset}&rating=g`
      )
      
      if (!response.ok) {
        console.error('GIPHY API 요청 실패:', response.status, response.statusText)
        return { data: [], pagination: { total_count: 0, count: 0, offset: 0 } }
      }
      
      return response.json()
    } catch (error) {
      console.error('GIPHY API 요청 중 오류:', error)
      return { data: [], pagination: { total_count: 0, count: 0, offset: 0 } }
    }
  },

  // GIF 검색
  search: async (query: string, limit: number = 25, offset: number = 0): Promise<GiphySearchResponse> => {
    if (!query.trim()) {
      return { data: [], pagination: { total_count: 0, count: 0, offset: 0 } }
    }

    if (!GIPHY_API_KEY || GIPHY_API_KEY.trim() === '') {
      console.warn('GIPHY API 키가 설정되지 않았습니다. GIF 검색 기능이 비활성화됩니다.')
      return { data: [], pagination: { total_count: 0, count: 0, offset: 0 } }
    }

    try {
      const response = await fetch(
        `${GIPHY_API_URL}/gifs/search?api_key=${GIPHY_API_KEY}&q=${encodeURIComponent(query)}&limit=${limit}&offset=${offset}&rating=g&lang=ko`
      )
      
      if (!response.ok) {
        console.error('GIPHY API 요청 실패:', response.status, response.statusText)
        return { data: [], pagination: { total_count: 0, count: 0, offset: 0 } }
      }
      
      return response.json()
    } catch (error) {
      console.error('GIPHY API 요청 중 오류:', error)
      return { data: [], pagination: { total_count: 0, count: 0, offset: 0 } }
    }
  },
}
