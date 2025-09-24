package team.avgmax.rabbit.bunny.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.repository.custom.BunnyRepositoryCustom;

import java.util.Optional;

public interface BunnyRepository extends JpaRepository<Bunny, String>, BunnyRepositoryCustom {

    Page<Bunny> findAllByOrderByCreatedAtDesc(Pageable pageable); // GOT 탑승한 버니들 (모든 Bunny 조회) + 페이징
    Page<Bunny> findAllByOrderByMarketCapDesc(Pageable pageable); // Top 5 버니들 (Bunny 의 시가총액이 가장 큰 순으로 5개 조회) + 페이징

    boolean existsByBunnyName(String bunnyName);

    Optional<Bunny> findByBunnyName(String bunnyName); // 버니 상세 조회에 사용, Optional<>로 NullPointException 방지
    Optional<Bunny> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
