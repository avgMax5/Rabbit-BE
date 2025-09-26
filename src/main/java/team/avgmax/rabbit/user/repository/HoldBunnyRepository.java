package team.avgmax.rabbit.user.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.user.entity.HoldBunny;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.user.repository.custom.HoldBunnyRepositoryCustom;

import java.util.Optional;
import java.util.List;

public interface HoldBunnyRepository extends JpaRepository<HoldBunny, String>, HoldBunnyRepositoryCustom {
    List<HoldBunny> findByHolder(PersonalUser holder);

    List<HoldBunny> findByHolderId(String personalUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HoldBunny h where h.holder = :holder and h.bunny = :bunny")
    Optional<HoldBunny> findByHolderAndBunnyForUpdate(@Param("holder") PersonalUser holder,
                                                      @Param("bunny") Bunny bunny);
}
