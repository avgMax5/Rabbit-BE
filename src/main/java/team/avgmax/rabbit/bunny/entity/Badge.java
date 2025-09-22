package team.avgmax.rabbit.bunny.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import team.avgmax.rabbit.bunny.entity.id.BadgeId;
import team.avgmax.rabbit.global.entity.BaseTime;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(BadgeId.class)
public class Badge extends BaseTime {

    @Id
    @Column(name = "bunny_id", length = 26, nullable = false)
    private String bunnyId;

    @Id
    @Column(name = "user_id", length = 26, nullable = false)
    private String userId;

    private String badgeImg;

    public static Badge create(String bunnyId, String userId, String corporationName) {
        return Badge.builder()
                .bunnyId(bunnyId)
                .userId(userId)
                .badgeImg(corporationName)
                .build();
    }
}
