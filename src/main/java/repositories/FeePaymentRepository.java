package repositories;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import domain.FeePayment;

@Repository
public interface FeePaymentRepository extends JpaRepository<FeePayment, Integer> {

	@Query("select f from FeePayment f where f.type like 'Pending' and f.purchaser.id = ?1")
	Page<FeePayment> findAllPendingByUser(int id, Pageable pageable);

	@Query("select f from FeePayment f where f.type like 'Rejected'")
	Page<FeePayment> findAllRejected(Pageable page);
	
	@Query("select f from FeePayment f where f.type like 'Pending'")
	Page<FeePayment> findAllPending(Pageable page);
	
	@Query("select f from FeePayment f where f.type like 'Accepted'")
	Page<FeePayment> findAllAccepted(Pageable page);
}