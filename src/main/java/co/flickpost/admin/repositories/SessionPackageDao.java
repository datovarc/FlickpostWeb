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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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

    @PersistenceContext
    private EntityManager entityManager;

    final static DateTimeFormatter packageDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final static String HQ = "HQ";
    private final static String ALL = "ALL";

    private final static String DATE_TIME = "dateTime";
    private final static String HUB = "hub";
    private final static String TRACKING_NUMBER = "trackingNumber";

    public String insert(SessionPackage pkg) {
        entityManager.persist(pkg);
        return pkg.getTrackingNumber();
    }

    public boolean batchDelete(List<Long> ids) {
        for (Long id : ids) {
            entityManager.remove(entityManager.getReference(SessionPackage.class, id));
        }
        return true;
    }

    public SessionPackage findSessionPackageByTrackingNumber(String trackingNumber) {
        logger.info("Started searching session_package for trackingNumber: {}", trackingNumber);
        List<SessionPackage> result = entityManager.createNativeQuery("SELECT * FROM session_package where tracking_number = ?", SessionPackage.class)
                .setParameter(1, trackingNumber)
                .getResultList();
        logger.info("Finished searching session_package for trackingNumber: {}", trackingNumber);
        return result != null && !result.isEmpty() ? result.get(0) : null;
    }

    public void batchUpdate(List<SessionPackage> packages, int batchSize) {
        int packagesCount = packages.size();

        for (int i = 0; i < packagesCount; i++) {
            if (i > 0 && i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
            entityManager.merge(packages.get(i));
        }
    }

    public void singleUpdate(SessionPackage packages) {
        entityManager.merge(packages);
    }

    public void delete(SessionPackage sessionPackage) {
        entityManager.remove(entityManager.contains(sessionPackage) ? sessionPackage : entityManager.merge(sessionPackage));
    }

    public Map<String, Object> pagination(PaginationRequest request, String userCompany) {
        int pageSize = request.getPageSize();
        int pageNumber = request.getPageNumber();
        List<PaginationFilter> filters = request.getFilters();

        logger.info("Started pagination transaction. Pg. {}", request.getPageNumber());

        CriteriaBuilder selectCriteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SessionPackage> selectQuery = selectCriteriaBuilder.createQuery(SessionPackage.class);
        Root<SessionPackage> selectRoot = selectQuery.from(SessionPackage.class);
        selectRoot.alias("rootAlias");
        CriteriaQuery<SessionPackage> paged = selectQuery.select(selectRoot);
        paged.orderBy(selectCriteriaBuilder.desc(selectRoot.get("dateTime")));

        CriteriaBuilder countCriteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = countCriteriaBuilder.createQuery(Long.class);
        Root<SessionPackage> countRoot = countQuery.from(SessionPackage.class);
        countRoot.alias("rootAlias");
        countQuery.select(countCriteriaBuilder.count(countRoot));
        List<Predicate> predicates = processPredicates(filters, countCriteriaBuilder, countRoot, userCompany);

        if (predicates != null && !predicates.isEmpty()) {
            countQuery.where(predicates.toArray(new Predicate[]{}));
            paged.where(predicates.toArray(new Predicate[]{}));
        }

        Long count = entityManager.createQuery(countQuery).getSingleResult();
        TypedQuery<SessionPackage> pagedQuery = entityManager.createQuery(paged);

        int currentFirstValue = (pageNumber - 1) * pageSize;
        if (currentFirstValue >= count.intValue()) {
            logger.info("Committed pagination transaction. Pg. {}", request.getPageNumber());
            return null;
        }

        pagedQuery.setFirstResult(currentFirstValue);
        pagedQuery.setMaxResults(pageSize);
        Map<String, Object> paginationResult = new HashMap<>();
        paginationResult.put("packages", pagedQuery.getResultList());
        paginationResult.put("count", count);

        logger.info("Committed pagination transaction. Pg. {}", request.getPageNumber());
        return paginationResult;
    }

    public List<SessionPackage> findAllByFilters(List<PaginationFilter> filters, String userCompany) {
        logger.info("Started filtered fetch for Scan Session.");

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SessionPackage> cq = cb.createQuery(SessionPackage.class);
        Root<SessionPackage> root = cq.from(SessionPackage.class);
        cq.select(root).orderBy(cb.desc(root.get(DATE_TIME)));

        List<Predicate> predicates = processPredicates(filters, cb, root, userCompany);
        if (predicates != null && !predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[]{}));
        }

        List<SessionPackage> result = entityManager.createQuery(cq).getResultList();
        logger.info("Finished filtered fetch for Scan Session. Count: {}", result.size());
        return result;
    }

    private List<Predicate> processPredicates(List<PaginationFilter> filters, CriteriaBuilder cb, Root<SessionPackage> rootEntry, String userCompany) {
        List<Predicate> predicates = new ArrayList<>();

        LocalDateValidator dateValidator = new LocalDateValidator(packageDateFormat);

        if (filters != null) {
            for (PaginationFilter filter : filters) {
                Predicate newPredicate = null;
                Object value = filter.getValue();
                String field = filter.getField();

                if (DATE_TIME.equalsIgnoreCase(field) && dateValidator.isValid(value.toString())) {
                    if (">=".equalsIgnoreCase(filter.getType())) {
                        newPredicate = cb.greaterThanOrEqualTo(rootEntry.get(filter.getField()), LocalDate.parse(value.toString(), packageDateFormat).atStartOfDay());
                    } else if ("<=".equalsIgnoreCase(filter.getType())) {
                        newPredicate = cb.lessThanOrEqualTo(rootEntry.get(filter.getField()), LocalDate.parse(value.toString(), packageDateFormat).atTime(23, 59, 59));
                    }
                } else if (HUB.equalsIgnoreCase(field) && !ALL.equalsIgnoreCase(value.toString())) {
                    newPredicate = cb.equal(rootEntry.get(HUB), value.toString());
                } else if (TRACKING_NUMBER.equalsIgnoreCase(field)) {
                    newPredicate = cb.like(rootEntry.get(TRACKING_NUMBER), "%" + value.toString() + "%");
                }

                if (newPredicate != null) {
                    predicates.add(newPredicate);
                }
            }
        }

        if (!HQ.equalsIgnoreCase(userCompany)) {
            predicates.add(cb.equal(rootEntry.get(HUB), userCompany));
        }

        return predicates;
    }

    public List<SessionPackage> getMostRecent(int max) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SessionPackage> cq = cb.createQuery(SessionPackage.class);
        Root<SessionPackage> root = cq.from(SessionPackage.class);
        cq.orderBy(cb.desc(root.get(DATE_TIME)));
        return entityManager.createQuery(cq).setMaxResults(max).getResultList();
    }

    public List<SessionPackage> search(DownloadRequest request, String userCompany) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SessionPackage> cq = cb.createQuery(SessionPackage.class);
        Root<SessionPackage> rootEntry = cq.from(SessionPackage.class);

        List<Predicate> predicates = new ArrayList<>();
        if (request.getFromDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(rootEntry.get(DATE_TIME), request.getFromDate().atStartOfDay()));
        }
        if (request.getToDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(rootEntry.get(DATE_TIME), request.getToDate().atTime(23, 59, 59)));
        }
        if (HQ.equalsIgnoreCase(userCompany) && !ALL.equalsIgnoreCase(request.getHub())) {
            predicates.add(cb.equal(rootEntry.get(HUB), request.getHub()));
        } else if (!HQ.equalsIgnoreCase(userCompany)) {
            predicates.add(cb.equal(rootEntry.get(HUB), userCompany));
        }

        CriteriaQuery<SessionPackage> filtered = cq.select(rootEntry).where(predicates.toArray(new Predicate[]{}));
        return entityManager.createQuery(filtered).getResultList();
    }

    public List<SessionPackage> searchByCode(List<String> codes) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SessionPackage> cq = cb.createQuery(SessionPackage.class);
        Root<SessionPackage> rootEntry = cq.from(SessionPackage.class);

        List<Predicate> predicates = new ArrayList<>();
        Expression<String> trackingNumberExpression = rootEntry.get(TRACKING_NUMBER);
        Predicate codePredicate = trackingNumberExpression.in(codes);
        predicates.add(codePredicate);

        CriteriaQuery<SessionPackage> filtered = cq.select(rootEntry).where(predicates.toArray(new Predicate[]{}));
        return entityManager.createQuery(filtered).getResultList();
    }
}
