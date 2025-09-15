package team.avgmax.rabbit.bunny.entity.id;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class BunnyLikeId implements Serializable {
    private String bunnyId;
    private String userId;
}
