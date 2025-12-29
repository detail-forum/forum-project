package com.pgh.api_practice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig {

    @Value("${app.upload.dir:C:/app-data/uploads}")
    private String uploadDir;

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 모든 API 경로 허용
                        // allowedOriginPatterns 사용 (와일드카드 지원)
                        .allowedOriginPatterns(
                            "http://localhost:3000",
                            "http://localhost:80",
                            "http://127.0.0.1:3000",
                            "http://127.0.0.1:80",
                            "http://211.110.30.142",  // 프로덕션 서버 IP (HTTP)
                            "http://211.110.30.142:80",
                            "https://forum.rjsgud.com",  // 프로덕션 HTTPS 도메인
                            "https://www.forum.rjsgud.com",  // www 서브도메인
                            "http://*",
                            "https://*"
                        )
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // /uploads/ 경로로 요청이 오면 업로드 디렉토리의 파일을 제공
                String uploadPath = Paths.get(uploadDir).toUri().toString();
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations("file:" + uploadPath)
                        .setCachePeriod(86400); // 24시간 캐싱
            }
        };
    }
}
