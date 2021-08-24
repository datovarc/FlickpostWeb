package co.flickpost.admin.repositories;

import co.flickpost.admin.models.Shipment;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceUnit;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.List;

@Repository
@Transactional
public class ShipmentDao {

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    public String insert(Shipment shipment) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.persist(shipment);
        return shipment.getTrackingNumber();
    }

    public String update(Shipment shipment) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.merge(shipment);
        return shipment.getTrackingNumber();
    }

    public void batchUpdate(List<Shipment> shipments, int batchSize){
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();

        int shipmentsCount = shipments.size();

        try {
            entityTransaction.begin();

            for (int i = 0; i < shipmentsCount; i++) {
                if (i > 0 && i % batchSize == 0) {
                    entityTransaction.commit();
                    entityTransaction.begin();

                    entityManager.clear();
                }

                entityManager.merge(shipments.get(i));
            }

            entityTransaction.commit();
        } catch (RuntimeException e) {
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            throw e;
        }

        entityManager.close();
    }

    public Shipment find(String trackingNumber) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        return entityManager.find(Shipment.class, trackingNumber);
    }

    public List<Shipment> allEntries() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Shipment> cq = cb.createQuery(Shipment.class);
        Root<Shipment> rootEntry = cq.from(Shipment.class);
        CriteriaQuery<Shipment> all = cq.select(rootEntry);
        TypedQuery<Shipment> allQuery = entityManager.createQuery(all);
        return allQuery.getResultList();
    }

}