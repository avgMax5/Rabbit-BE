package team.avgmax.rabbit.bunny.repository;

import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
import team.avgmax.rabbit.bunny.entity.Bunny;
import team.avgmax.rabbit.bunny.repository.custom.BunnyRepositoryCustom;

import java.util.List;
import java.util.Optional;

public interface BunnyRepository extends JpaRepository<Bunny, String>, BunnyRepositoryCustom {

    // GOT 탑승한 버니들 (모든 Bunny 조회)
    List<Bunny> findAllByOrderByCreatedAtDesc();

    // Top 5 버니들 (Bunny 의 시가총액이 가장 큰 순으로 5개 조회)
    List<Bunny> findTop5ByOrderByMarketCapDesc();
  
    boolean existsByBunnyName(String bunnyName);

    // 버니 상세 조회에 사용, Optional<>로 NullPointException 방지
    Optional<Bunny> findByBunnyName(String bunnyName);

    Optional<Bunny> findByUserId(String userId);

}
