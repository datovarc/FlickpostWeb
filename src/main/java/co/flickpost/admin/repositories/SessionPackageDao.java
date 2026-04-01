package co.flickpost.admin.repositories;

import co.flickpost.admin.models.Package;
import co.flickpost.admin.models.SessionPackage;
import co.flickpost.admin.models.json.DownloadRequest;
import co.flickpost.admin.models.json.PaginationFilter;
import co.flickpost.admin.models.json.PaginationRequest;
import co.flickpost.admin.validators.LocalDateValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Transactional
public class SessionPackageDao {

    private static final Logger logger = LogManager.getLogger(SessionPackageDao.class);

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    final static DateTimeFormatter packageDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final static String HQ = "HQ";
    private final static String ALL = "ALL";

    private final static String DATE_TIME = "dateTime";
    private final static String HUB = "hub";
    private final static String TRACKING_NUMBER = "trackingNumber";

    @Transactional
    public String insert(SessionPackage pkg) {
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
            entityManager.remove(entityManager.getReference(SessionPackage.class, id));
        }

        entityTransaction.commit();
        entityManager.close();

        return true;
    }

    @Transactional
    public Package findByTrackingNumber(String trackingNumber){
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();

        logger.info("Started searching for trackingNumber: {}", trackingNumber);
        transaction.begin();

        Query query = entityManager.createNativeQuery("SELECT * FROM session_package where tracking_number = ?", SessionPackage.class);
        query.setParameter(1, trackingNumber);
        List<Package> result = query.getResultList();

        transaction.commit();
        logger.info("Finished searching for trackingNumber: {}", trackingNumber);

        entityManager.close();
        return result != null && !result.isEmpty()? result.get(0) : null;
    }

    @Transactional
    public void batchUpdate(List<SessionPackage> packages, int batchSize){
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
    public void singleUpdate(SessionPackage packages){
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

        //Selecting
        CriteriaBuilder selectCriteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SessionPackage> selectQuery = selectCriteriaBuilder.createQuery(SessionPackage.class);
        Root<SessionPackage> selectRoot = selectQuery.from(SessionPackage.class);
        selectRoot.alias("rootAlias");
        CriteriaQuery<SessionPackage> paged = selectQuery.select(selectRoot);
        paged.orderBy(selectCriteriaBuilder.desc(selectRoot.get("dateTime")));

        //Counting
        CriteriaBuilder countCriteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = countCriteriaBuilder.createQuery(Long.class);
        Root<SessionPackage> countRoot = countQuery.from(SessionPackage.class);
        countRoot.alias("rootAlias");
        countQuery.select(countCriteriaBuilder.count(countRoot));
        List<Predicate> predicates = processPredicates(filters, countCriteriaBuilder, countRoot, userCompany);

        if(predicates != null && !predicates.isEmpty()){
            countQuery.where(predicates.toArray(new Predicate[]{}));
            paged.where(predicates.toArray(new Predicate[]{}));
        }

        Long count = entityManager.createQuery(countQuery).getSingleResult();

        TypedQuery<SessionPackage> pagedQuery = entityManager.createQuery(paged);

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


    private List<Predicate> processPredicates(List<PaginationFilter> filters, CriteriaBuilder cb, Root<SessionPackage> rootEntry, String userCompany){
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

        //Add User Filter
        if(!HQ.equalsIgnoreCase(userCompany)){
            predicates.add(cb.equal(rootEntry.get(HUB), userCompany));
        }

        return predicates;

    }

    @Transactional
    public List<SessionPackage> getMostRecent(int max){
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SessionPackage> cq = cb.createQuery(SessionPackage.class);
        Root<SessionPackage> root = cq.from(SessionPackage.class);
        cq.orderBy(cb.desc(root.get(DATE_TIME)));

        List<SessionPackage> packages = entityManager.createQuery(cq).setMaxResults(max).getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return packages;
    }

    @Transactional
    public List<SessionPackage> search(DownloadRequest request, String userCompany) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SessionPackage> cq = cb.createQuery(SessionPackage.class);
        Root<SessionPackage> rootEntry = cq.from(SessionPackage.class);


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

        CriteriaQuery<SessionPackage> filtered = cq.select(rootEntry).where(predicates.toArray(new Predicate[]{}));

        TypedQuery<SessionPackage> filteredQuery = entityManager.createQuery(filtered);
        List<SessionPackage> queryResult = filteredQuery.getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return queryResult;
    }

    @Transactional
    public List<SessionPackage> searchByCode(List<String> codes) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SessionPackage> cq = cb.createQuery(SessionPackage.class);
        Root<SessionPackage> rootEntry = cq.from(SessionPackage.class);


        List<Predicate> predicates = new ArrayList<>();
        Expression<String> trackingNumberExpression = rootEntry.get(TRACKING_NUMBER);
        Predicate codePredicate = trackingNumberExpression.in(codes);
        predicates.add(codePredicate);

        CriteriaQuery<SessionPackage> filtered = cq.select(rootEntry).where(predicates.toArray(new Predicate[]{}));

        TypedQuery<SessionPackage> filteredQuery = entityManager.createQuery(filtered);
        List<SessionPackage> queryResult = filteredQuery.getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        return queryResult;
    }

}