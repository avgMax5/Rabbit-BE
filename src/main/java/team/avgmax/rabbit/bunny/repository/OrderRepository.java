package team.avgmax.rabbit.bunny.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import team.avgmax.rabbit.bunny.entity.Order;
import team.avgmax.rabbit.bunny.repository.custom.OrderRepositoryCustom;

public interface OrderRepository extends JpaRepository<Order, String>, OrderRepositoryCustom {
    List<Order> findAllByBunnyIdAndUserId(String bunnyId, String userId);
    List<Order> findOrdersByUserId(String personalUserId);
}
