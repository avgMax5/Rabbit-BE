package team.avgmax.rabbit.user.entity.enums;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

@Getter
@RequiredArgsConstructor
public enum LinkType {
    GITHUB("github.com", "https://github.com/favicon.ico"),
    INSTAGRAM("instagram.com", "https://instagram.com/favicon.ico"),
    LINKEDIN("linkedin.com", "https://linkedin.com/favicon.ico"),
    TISTORY("tistory.com", "https://tistory.com/favicon.ico"),
    NAVER_BLOG("blog.naver.com", "https://blog.naver.com/favicon.ico"),
    VELOG("velog.io", "https://velog.io/favicon.ico"),
    YOUTUBE("youtube.com", "https://youtube.com/favicon.ico"),
    TWITTER("twitter.com", "https://twitter.com/favicon.ico"),
    ETC("*", null);
    
    private final String domain;
    private final String faviconUrl;

    public static LinkType fromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return ETC;
        }
        
        try {
            String host = extractHost(url);
            if (host == null) {
                return ETC;
            }
            
            return Stream.of(values())
                    .filter(linkType -> !linkType.domain.equals("*") && 
                           (host.endsWith("." + linkType.domain) || host.equals(linkType.domain)))
                    .findFirst()
                    .orElse(ETC);
        } catch (Exception e) {
            return ETC;
        }
    }
    
    public String getFaviconUrl(String url) {
        if (this != ETC) {
            return faviconUrl;
        }
        
        try {
            String host = extractHost(url);
            return host != null ? "https://" + host + "/favicon.ico" : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String extractHost(String url) {
        try {
            String normalizedUrl = url.startsWith("http") ? url : "https://" + url;
            URI uri = URI.create(normalizedUrl);
            return Optional.ofNullable(uri.getHost()).orElse(uri.getAuthority());
        } catch (Exception e) {
            return null;
        }
    }
}