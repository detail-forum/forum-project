/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // /api/* 경로는 Next.js가 처리하지 않고 외부로 프록시
  async rewrites() {
    return []
  },
  // /api/* 경로를 정적 파일로 처리하지 않도록 설정
  async headers() {
    return []
  },
}

module.exports = nextConfig

