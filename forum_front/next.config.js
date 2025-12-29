/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // /api/* 경로는 Next.js가 처리하지 않고 외부로 프록시
  async rewrites() {
    return [
      // 로컬 개발 환경: /uploads/ 경로를 백엔드로 프록시
      // 프로덕션에서는 Nginx가 직접 처리하므로 이 rewrite는 무시됨
      {
        source: '/uploads/:path*',
        destination: process.env.NEXT_PUBLIC_API_URL 
          ? `${process.env.NEXT_PUBLIC_API_URL.replace('/api', '')}/uploads/:path*`
          : 'http://localhost:8081/uploads/:path*',
      },
    ]
  },
  // /api/* 경로를 정적 파일로 처리하지 않도록 설정
  async headers() {
    return []
  },
}

module.exports = nextConfig

