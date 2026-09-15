package charlie.gtalent_spring_boot_260801.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import charlie.gtalent_spring_boot_260801.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
}