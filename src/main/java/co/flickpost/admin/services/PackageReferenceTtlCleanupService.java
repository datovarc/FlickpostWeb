package co.flickpost.admin.services;

import co.flickpost.admin.repositories.PackageReferenceDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PackageReferenceTtlCleanupService {

    private static final Logger logger = LogManager.getLogger(PackageReferenceTtlCleanupService.class);
    private static final int TTL_DAYS = 60;

    @Autowired
    private PackageReferenceDao packageReferenceDao;

    @Scheduled(cron = "0 0 7 * * *")
    public void purgeExpiredPackageReferences() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(TTL_DAYS);
        int deleted = packageReferenceDao.deleteOlderThanOrNullTtl(cutoff);
        logger.info("PackageReference TTL cleanup completed. cutoff={} deletedCount={}", cutoff, deleted);
    }
}
