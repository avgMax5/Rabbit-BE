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

public interface HoldBunnyRepository extends JpaRepository<HoldBunny, String>, HoldBunnyRepositoryCustom {

    Optional<HoldBunny> findByHolderAndBunny(PersonalUser holder, Bunny bunny);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from HoldBunny h where h.holder = :holder and h.bunny = :bunny")
    Optional<HoldBunny> findByHolderAndBunnyForUpdate(@Param("holder") PersonalUser holder,
                                                      @Param("bunny") Bunny bunny);
}
