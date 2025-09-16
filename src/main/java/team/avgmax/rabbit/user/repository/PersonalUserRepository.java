package team.avgmax.rabbit.user.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.avgmax.rabbit.user.entity.PersonalUser;
import team.avgmax.rabbit.user.repository.custom.PersonalUserRepositoryCustom;

import java.util.Optional;

public interface PersonalUserRepository extends JpaRepository<PersonalUser, String>, PersonalUserRepositoryCustom {
    Optional<PersonalUser> findByEmail(String email);
    Optional<PersonalUser> getUserById(String personalUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from PersonalUser u where u.id = :id")
    PersonalUser findByIdForUpdate(@Param("id") String id);
}