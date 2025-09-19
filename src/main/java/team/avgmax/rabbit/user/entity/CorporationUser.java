package team.avgmax.rabbit.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import team.avgmax.rabbit.global.util.UlidGenerator;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CorporationUser extends User {

    @Id
    @Column(name = "corporation_user_id", length = 26, updatable = false, nullable = false)
    @Builder.Default
    private String id = UlidGenerator.generateMonotonic();

    private String corporationName;

    private String bin;

    private String managerTeam;

    private String managerRole;

    private String managerGrade;
}
