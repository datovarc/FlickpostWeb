package co.flickpost.admin.repositories;

import co.flickpost.admin.models.json.DownloadRequest;
import co.flickpost.admin.models.json.PaginationFilter;
import co.flickpost.admin.models.json.PaginationRequest;
import co.flickpost.admin.validators.LocalDateValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import co.flickpost.admin.models.Package;

import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Transactional
public class PackageDao {

    private static final Logger logger = LogManager.getLogger(PackageDao.class);

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    final static DateTimeFormatter packageDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final static String HQ = "HQ";
    private final static String ALL = "ALL";

    private final static String DATE_TIME = "dateTime";
    private final static String HUB = "hub";
    private final static String TRACKING_NUMBER = "trackingNumber";

    @Transactional
    public String insert(Package pkg) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();

        entityTransaction.begin();
        entityManager.clear();

        entityManager.persist(pkg);


        entityTransaction.commit();
        entityManager.close();

        return pkg.getTrackingNumber();
    }

    @Transactional
    public boolean batchDelete(List<Long> ids) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();

        entityTransaction.begin();
        entityManager.clear();

        for(Long id : ids) {
            entityManager.remove(entityManager.getReference(Package.class, id));
        }

        entityTransaction.commit();
        entityManager.close();

        return true;
    }

    @Transactional
    public void delete(Package pkg) {
        if (pkg == null || pkg.getId() == null) {
            return;
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();

        entityTransaction.begin();
        entityManager.clear();
        entityManager.remove(entityManager.getReference(Package.class, pkg.getId()));
        entityTransaction.commit();
        entityManager.close();
    }

    @Transactional
    public Package findByTrackingNumber(String trackingNumber){
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();

        logger.info("Started searching for trackingNumber: {}", trackingNumber);
        transaction.begin();

        Query query = entityManager.createNativeQuery("SELECT * FROM package where tracking_number = ?", Package.class);
        query.setParameter(1, trackingNumber);
        List<Package> result = query.getResultList();

        transaction.commit();
        logger.info("Finished searching for trackingNumber: {}", trackingNumber);

        entityManager.close();
        return result != null && !result.isEmpty()? result.get(0) : null;
    }

    @Transactional
    public void batchUpdate(List<Package> packages, int batchSize){
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();

        int packagesCount = packages.size();

        try {
            entityTransaction.begin();

            for (int i = 0; i < packagesCount; i++) {
                if (i > 0 && i % batchSize == 0) {
                    entityTransaction.commit();
                    entityTransaction.begin();

                    entityManager.clear();
                }

                entityManager.persist(packages.get(i));
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

    @Transactional
    public void singleUpdate(Package packages){
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction entityTransaction = entityManager.getTransaction();

        try {
            entityTransaction.begin();
            entityManager.merge(packages);
            entityTransaction.commit();
        } catch (RuntimeException e) {
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            throw e;
        }
        entityManager.close();

    }

    @Transactional
    public Map<String, Object> pagination(PaginationRequest request, String userCompany) {
        int pageSize = request.getPageSize();
        int pageNumber = request.getPageNumber();
        List<PaginationFilter> filters = request.getFilters();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();

        logger.info("Started pagination transaction. Pg. {}", request.getPageNumber());
        transaction.begin();

        CriteriaBuilder selectCriteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Package> selectQuery = selectCriteriaBuilder.createQuery(Package.class);
        Root<Package> selectRoot = selectQuery.from(Package.class);
        selectRoot.alias("rootAlias");
        CriteriaQuery<Package> paged = selectQuery.select(selectRoot);
        paged.orderBy(selectCriteriaBuilder.desc(selectRoot.get("dateTime")));

        CriteriaBuilder countCriteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = countCriteriaBuilder.createQuery(Long.class);
        Root<Package> countRoot = countQuery.from(Package.class);
        countRoot.alias("rootAlias");
        countQuery.select(countCriteriaBuilder.count(countRoot));
        List<Predicate> predicates = processPredicates(filters, countCriteriaBuilder, countRoot, userCompany);

        if(predicates != null && !predicates.isEmpty()){
            countQuery.where(predicates.toArray(new Predicate[]{}));
            paged.where(predicates.toArray(new Predicate[]{}));
        }

        Long count = entityManager.createQuery(countQuery).getSingleResult();

        TypedQuery<Package> pagedQuery = entityManager.createQuery(paged);

        int currentFirstValue = (pageNumber - 1) * pageSize;
        if(currentFirstValue >= count.intValue()) {
            transaction.commit();
            logger.info("Committed pagination transaction. Pg. {}", request.getPageNumber());
            entityManager.close();
            return null;
        }

        pagedQuery.setFirstResult(currentFirstValue);
        pagedQuery.setMaxResults(pageSize);
        Map<String, Object> paginationResult = new HashMap<>();
        paginationResult.put("packages", pagedQuery.getResultList());
        paginationResult.put("count", count);

        transaction.commit();
        logger.info("Committed pagination transaction. Pg. {}", request.getPageNumber());

        entityManager.close();

        return paginationResult;
    }


    private List<Predicate> processPredicates(List<PaginationFilter> filters, CriteriaBuilder cb, Root<Package> rootEntry, String userCompany){
        List<Predicate> predicates = new ArrayList<>();

        LocalDateValidator dateValidator = new LocalDateValidator(packageDateFormat);

        for(PaginationFilter filter : filters){
            Predicate newPredicate = null;
            Object value = filter.getValue();
            String field = filter.getField();

            if(DATE_TIME.equalsIgnoreCase(field) && dateValidator.isValid(value.toString())) {
                if(">=".equalsIgnoreCase(filter.getType())){
                    newPredicate = cb.greaterThanOrEqualTo(rootEntry.get(filter.getField()), LocalDate.parse(value.toString(), packageDateFormat).atStartOfDay());

                } else if("<=".equalsIgnoreCase(filter.getType())){
                    newPredicate = cb.lessThanOrEqualTo(rootEntry.get(filter.getField()), LocalDate.parse(value.toString(), packageDateFormat).atTime(23,59,59));
                }
            } else if(HUB.equalsIgnoreCase(field) && !ALL.equalsIgnoreCase(value.toString())){
                newPredicate = cb.equal(rootEntry.get(HUB), value.toString());
            } else if(TRACKING_NUMBER.equalsIgnoreCase(field)){
                newPredicate = cb.like(rootEntry.<String>get(TRACKING_NUMBER), "%"+value.toString()+"%");
            }

            if(newPredicate != null) {
                predicates.add(newPredicate);
            }
        }

        if(!HQ.equalsIgnoreCase(userCompany)){
            predicates.add(cb.equal(rootEntry.get(HUB), userCompany));
        }

        return predicates;

    }

    @Transactional
    public List<Package> getMostRecent(int max){
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Package> cq = cb.createQuery(Package.class);
        Root<Package> root = cq.from(Package.class);
        cq.orderBy(cb.desc(root.get(DATE_TIME)));

        List<Package> packages = entityManager.createQuery(cq).setMaxResults(max).getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return packages;
    }

    @Transactional
    public List<Package> search(DownloadRequest request, String userCompany) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Package> cq = cb.createQuery(Package.class);
        Root<Package> rootEntry = cq.from(Package.class);


        List<Predicate> predicates = new ArrayList<>();
        if(request.getFromDate() != null){
            predicates.add(cb.greaterThanOrEqualTo(rootEntry.get(DATE_TIME), request.getFromDate().atStartOfDay()));
        }
        if(request.getToDate() != null){
            predicates.add(cb.lessThanOrEqualTo(rootEntry.get(DATE_TIME), request.getToDate().atTime(23,59,59)));
        }
        if(HQ.equalsIgnoreCase(userCompany) && !ALL.equalsIgnoreCase(request.getHub())){
            predicates.add(cb.equal(rootEntry.get(HUB), request.getHub()));
        } else if(!HQ.equalsIgnoreCase(userCompany)){
            predicates.add(cb.equal(rootEntry.get(HUB), userCompany));
        }

        CriteriaQuery<Package> filtered = cq.select(rootEntry).where(predicates.toArray(new Predicate[]{}));

        TypedQuery<Package> filteredQuery = entityManager.createQuery(filtered);
        List<Package> queryResult = filteredQuery.getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return queryResult;
    }

    @Transactional
    public List<Package> searchByCode(List<String> codes) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Package> cq = cb.createQuery(Package.class);
        Root<Package> rootEntry = cq.from(Package.class);


        List<Predicate> predicates = new ArrayList<>();
        Expression<String> trackingNumberExpression = rootEntry.get(TRACKING_NUMBER);
        Predicate codePredicate = trackingNumberExpression.in(codes);
        predicates.add(codePredicate);

        CriteriaQuery<Package> filtered = cq.select(rootEntry).where(predicates.toArray(new Predicate[]{}));

        TypedQuery<Package> filteredQuery = entityManager.createQuery(filtered);
        List<Package> queryResult = filteredQuery.getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return queryResult;
    }

    @Transactional
    public List<Package> findAllPending() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        List<Package> result = entityManager.createQuery(
                "SELECT p FROM Package p WHERE p.status = :status ORDER BY p.dateTime DESC", Package.class)
                .setParameter("status", "UNKNOWN")
                .getResultList();

        transaction.commit();
        entityManager.close();
        return result;
    }

    @Transactional
    public List<Package> findPendingByTrackingNumbers(List<String> trackingNumbers) {
        if (trackingNumbers == null || trackingNumbers.isEmpty()) {
            return Collections.emptyList();
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        List<Package> result = entityManager.createQuery(
                "SELECT p FROM Package p WHERE p.status = :status AND p.trackingNumber IN :trackingNumbers ORDER BY p.dateTime DESC", Package.class)
                .setParameter("status", "UNKNOWN")
                .setParameter("trackingNumbers", trackingNumbers)
                .getResultList();

        transaction.commit();
        entityManager.close();
        return result;
    }

}
