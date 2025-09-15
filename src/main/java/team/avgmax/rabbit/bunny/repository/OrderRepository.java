package team.avgmax.rabbit.bunny.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import team.avgmax.rabbit.bunny.entity.Order;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findAllByBunnyIdAndUserId(String bunnyId, String userId);
}
