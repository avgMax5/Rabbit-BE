package team.avgmax.rabbit.user.repository.custom;

import java.math.BigDecimal;

public interface PersonalUserRepositoryCustom {
    void addCarrotForUpdate(String userId, BigDecimal delta);
}
