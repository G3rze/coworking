package com.gerson.coworking.repository;

import com.gerson.coworking.domain.entity.Reservation;
import com.gerson.coworking.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @EntityGraph(attributePaths = {"space", "user"})
    List<Reservation> findAll();

    @EntityGraph(attributePaths = {"space", "user"})
    Optional<Reservation> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.space.id = :spaceId " +
           "AND r.date = :date AND r.status IN ('PENDING_PAYMENT', 'CONFIRMED') " +
           "AND r.startTime < :endTime AND r.endTime > :startTime")
    List<Reservation> findConflictingReservationsWithLock(UUID spaceId, LocalDate date, LocalTime startTime, LocalTime endTime);

    default boolean existsConflictingReservation(UUID spaceId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return !findConflictingReservationsWithLock(spaceId, date, startTime, endTime).isEmpty();
    }

    @EntityGraph(attributePaths = {"space", "user"})
    List<Reservation> findByUserId(UUID userId);

    @EntityGraph(attributePaths = {"space", "user"})
    List<Reservation> findBySpaceId(UUID spaceId);

    @EntityGraph(attributePaths = {"space", "user"})
    @Query("SELECT r FROM Reservation r WHERE " +
           "(:spaceId IS NULL OR r.space.id = :spaceId) AND " +
           "(:dateFrom IS NULL OR r.date >= :dateFrom) AND " +
           "(:dateTo IS NULL OR r.date <= :dateTo) AND " +
           "(:status IS NULL OR r.status = :status)")
    List<Reservation> findByFilter(UUID spaceId, LocalDate dateFrom,
                                    LocalDate dateTo, ReservationStatus status);

    @EntityGraph(attributePaths = {"space"})
    @Query("SELECT r FROM Reservation r WHERE r.date >= :dateFrom AND r.date <= :dateTo AND r.status = 'CONFIRMED'")
    List<Reservation> findConfirmedReservationsInDateRange(LocalDate dateFrom, LocalDate dateTo);
}
