package co.flickpost.admin.repositories;

import co.flickpost.admin.models.ScanSession;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ScanSessionDao extends CrudRepository<ScanSession, Long> {

    Optional<ScanSession> findFirstByStatusOrderByStartTimeDesc(String status);
}
