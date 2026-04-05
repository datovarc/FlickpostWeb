package co.flickpost.admin.repositories;

import co.flickpost.admin.models.PackageReference;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@Transactional
public class PackageReferenceDao {

    @PersistenceContext
    private EntityManager entityManager;

    public PackageReference findByTrackingNumber(String trackingNumber) {
        Query query = entityManager.createNativeQuery("SELECT * FROM package_reference where tracking_number = ?", PackageReference.class);
        query.setParameter(1, trackingNumber);
        List<PackageReference> result = query.getResultList();
        return result != null && !result.isEmpty() ? result.get(0) : null;
    }

    public void save(PackageReference packageReference) {
        if (packageReference.getId() == null) {
            entityManager.persist(packageReference);
        } else {
            entityManager.merge(packageReference);
        }
    }

    public int deleteOlderThanOrNullTtl(LocalDateTime cutoff) {
        Query query = entityManager.createNativeQuery("DELETE FROM package_reference WHERE ttl_timestamp IS NULL OR ttl_timestamp <= ?");
        query.setParameter(1, cutoff);
        return query.executeUpdate();
    }
}
