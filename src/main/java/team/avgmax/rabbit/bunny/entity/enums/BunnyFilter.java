package team.avgmax.rabbit.bunny.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum BunnyFilter {

    LATEST("latest"), ALL("all"), CAPITALIZATION("capitalization"), DEFAULT("default");

    private final String value;

    // 입력 정규화 규칙 강화
    private static String normalize(String s) {
        if (s == null) return null;
        return s.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-');
    }

    public static BunnyFilter fromValue(String raw) {
        if (raw == null) return DEFAULT;

        // 입력 표준화
        String key = normalize(raw);

        return Arrays.stream(values())
                .filter(filter -> normalize(filter.value).equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid filter value: " + raw));
    }
}
