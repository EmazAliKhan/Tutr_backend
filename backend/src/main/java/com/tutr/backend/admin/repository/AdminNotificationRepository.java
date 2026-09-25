package com.tutr.backend.admin.repository;

import com.tutr.backend.admin.model.entity.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {

    List<AdminNotification> findByRecipientIdOrderByCreatedAtDesc(Long adminId);

    long countByRecipientIdAndIsReadFalse(Long adminId);

    long countByRecipientId(Long adminId);
}