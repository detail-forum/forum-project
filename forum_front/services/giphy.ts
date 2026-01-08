// Giphy API 서비스
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
    if (!GIPHY_API_KEY) {
      throw new Error('GIPHY API 키가 설정되지 않았습니다.')
    }

    const response = await fetch(
      `${GIPHY_API_URL}/gifs/trending?api_key=${GIPHY_API_KEY}&limit=${limit}&offset=${offset}&rating=g`
    )
    
    if (!response.ok) {
      throw new Error('GIPHY API 요청 실패')
    }
    
    return response.json()
  },

  // GIF 검색
  search: async (query: string, limit: number = 25, offset: number = 0): Promise<GiphySearchResponse> => {
    if (!GIPHY_API_KEY) {
      throw new Error('GIPHY API 키가 설정되지 않았습니다.')
    }

    if (!query.trim()) {
      return { data: [], pagination: { total_count: 0, count: 0, offset: 0 } }
    }

    const response = await fetch(
      `${GIPHY_API_URL}/gifs/search?api_key=${GIPHY_API_KEY}&q=${encodeURIComponent(query)}&limit=${limit}&offset=${offset}&rating=g&lang=ko`
    )
    
    if (!response.ok) {
      throw new Error('GIPHY API 요청 실패')
    }
    
    return response.json()
  },
}
