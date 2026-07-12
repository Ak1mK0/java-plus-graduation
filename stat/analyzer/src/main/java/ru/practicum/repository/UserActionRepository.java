package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.UserAction;

import java.util.List;
import java.util.Optional;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserAction> findAllByEventId(List<Long> eventId);

    List<UserAction> findAllByUserId(Long userId);

    @Query("SELECT ua FROM UserAction ua " +
            "WHERE ua.userId = :userId " +
            "ORDER BY ua.actionTimestamp DESC")
    List<UserAction> findAllByUserIdOrderByActionTimestampDesc(@Param("userId") Long userId);
}
