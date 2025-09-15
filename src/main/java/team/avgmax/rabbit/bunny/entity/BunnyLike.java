package team.avgmax.rabbit.bunny.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import team.avgmax.rabbit.bunny.entity.id.BunnyLikeId;
import team.avgmax.rabbit.global.entity.BaseTime;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(BunnyLikeId.class)
public class BunnyLike extends BaseTime {

    @Id
    @Column(name = "bunny_id", length = 26, nullable = false)
    private String bunnyId;

    @Id
    @Column(name = "user_id", length = 26, nullable = false)
    private String userId;

    public static BunnyLike create(String bunnyId, String userId) { 
        return BunnyLike.builder()
                .bunnyId(bunnyId)
                .userId(userId)
                .build();
    }
}
