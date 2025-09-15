package team.avgmax.rabbit.bunny.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.avgmax.rabbit.bunny.entity.Order;

public interface OrderRepository extends JpaRepository<Order,String> {
}
