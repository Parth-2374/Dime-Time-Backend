package com.dimetime.service;

import com.dimetime.entity.AuditLog;
import com.dimetime.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Transactional
    public void logActivity(String activity, String username) {
        String activeUser = (username != null && !username.isEmpty()) ? username : "SYSTEM";
        AuditLog log = new AuditLog(activity, activeUser);
        auditLogRepository.save(log);
    }

    @Transactional
    public void logActivity(String activity, String username, String entityType, String entityReference) {
        String activeUser = (username != null && !username.isEmpty()) ? username : "SYSTEM";
        AuditLog log = new AuditLog(activity, activeUser);
        log.setEntityType(entityType);
        log.setEntityReference(entityReference);
        auditLogRepository.save(log);
    }

    @Transactional
    public void logSecurityActivity(String activity, String username, String role, String ipAddress) {
        String activeUser = (username != null && !username.isEmpty()) ? username : "SYSTEM";
        AuditLog log = new AuditLog(activity, activeUser, role, ipAddress);
        auditLogRepository.save(log);
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    @Transactional
    public void deleteLog(Long id) {
        auditLogRepository.deleteById(id);
    }
}
