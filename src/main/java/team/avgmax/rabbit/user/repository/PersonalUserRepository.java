package team.avgmax.rabbit.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.avgmax.rabbit.user.entity.PersonalUser;

import java.util.Optional;

public interface PersonalUserRepository extends JpaRepository<PersonalUser, String> {   
    Optional<PersonalUser> findByEmail(String email);
}