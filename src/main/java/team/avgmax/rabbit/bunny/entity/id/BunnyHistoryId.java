package team.avgmax.rabbit.bunny.entity.id;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.avgmax.rabbit.bunny.exception.BunnyError;
import team.avgmax.rabbit.bunny.exception.BunnyException;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class BunnyHistoryId implements Serializable {
    private LocalDate date;
    private String bunnyId;

    public static BunnyHistoryId of(LocalDate date, String bunnyId) {
        if (date == null || bunnyId == null || bunnyId.isBlank()) {
            throw new BunnyException(BunnyError.BUNNYHISTORY_NOT_FOUND);
        }

        return new BunnyHistoryId(date, bunnyId);
    }
}
