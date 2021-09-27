package co.flickpost.admin.repositories;

import co.flickpost.admin.models.json.DownloadRequest;
import co.flickpost.admin.models.json.PaginationFilter;
import co.flickpost.admin.models.json.PaginationRequest;
import co.flickpost.admin.validators.LocalDateValidator;
import org.springframework.stereotype.Repository;

import co.flickpost.admin.models.Package;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceUnit;
import javax.persistence.TypedQuery;
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
public class PackageDao {

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    final static DateTimeFormatter packageDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

    public Map<String, Object> pagination(PaginationRequest request) {
        int pageSize = request.getPageSize();
        int pageNumber = request.getPageNumber();
        List<PaginationFilter> filters = request.getFilters();

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        //Selecting
        CriteriaBuilder selectCriteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Package> selectQuery = selectCriteriaBuilder.createQuery(Package.class);
        Root<Package> selectRoot = selectQuery.from(Package.class);
        selectRoot.alias("rootAlias");
        CriteriaQuery<Package> paged = selectQuery.select(selectRoot);
        paged.orderBy(selectCriteriaBuilder.desc(selectRoot.get("dateTime")));

        //Counting
        CriteriaBuilder countCriteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = countCriteriaBuilder.createQuery(Long.class);
        Root<Package> countRoot = countQuery.from(Package.class);
        countRoot.alias("rootAlias");
        countQuery.select(countCriteriaBuilder.count(countRoot));
        List<Predicate> predicates = processPredicates(filters, countCriteriaBuilder, countRoot);
        if(predicates != null && !predicates.isEmpty()){
            countQuery.where(predicates.toArray(new Predicate[]{}));
            paged.where(predicates.toArray(new Predicate[]{}));
        }

        Long count = entityManager.createQuery(countQuery).getSingleResult();

        TypedQuery<Package> pagedQuery = entityManager.createQuery(paged);

        int currentFirstValue = (pageNumber - 1) * pageSize;
        if(currentFirstValue >= count.intValue()) {
            return null;
        }

        pagedQuery.setFirstResult(currentFirstValue);
        pagedQuery.setMaxResults(pageSize);
        Map<String, Object> paginationResult = new HashMap<>();
        paginationResult.put("packages", pagedQuery.getResultList());
        paginationResult.put("count", count);

        entityManager.close();

        return paginationResult;
    }


    private List<Predicate> processPredicates(List<PaginationFilter> filters, CriteriaBuilder cb, Root<Package> rootEntry){
        if(filters == null || filters.isEmpty()) return null;

        LocalDateValidator dateValidator = new LocalDateValidator(packageDateFormat);

        List<Predicate> predicates = new ArrayList<>();
        for(PaginationFilter filter : filters){
            Predicate newPredicate = null;
            Object value = filter.getValue();

            if(dateValidator.isValid(value.toString())) {
                if(">=".equalsIgnoreCase(filter.getType())){
                    newPredicate = cb.greaterThanOrEqualTo(rootEntry.get(filter.getField()), LocalDate.parse(value.toString(), packageDateFormat).atStartOfDay());

                } else if("<=".equalsIgnoreCase(filter.getType())){
                    newPredicate = cb.lessThanOrEqualTo(rootEntry.get(filter.getField()), LocalDate.parse(value.toString(), packageDateFormat).atTime(23,59,59));
                }
            }


            predicates.add(newPredicate);
        }

        return predicates;

    }

    public List<Package> getMostRecent(int max){
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Package> cq = cb.createQuery(Package.class);
        Root<Package> root = cq.from(Package.class);
        cq.orderBy(cb.desc(root.get("dateTime")));

        List<Package> packages = entityManager.createQuery(cq).setMaxResults(max).getResultList();

        entityManager.close();

        return packages;
    }


    public List<Package> search(DownloadRequest request) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Package> cq = cb.createQuery(Package.class);
        Root<Package> rootEntry = cq.from(Package.class);


        List<Predicate> predicates = new ArrayList<>();
        if(request.getFromDate() != null){
            predicates.add(cb.greaterThanOrEqualTo(rootEntry.get("dateTime"), request.getFromDate().atStartOfDay()));
        }
        if(request.getToDate() != null){
            predicates.add(cb.lessThanOrEqualTo(rootEntry.get("dateTime"), request.getToDate().atTime(23,59,59)));
        }

        CriteriaQuery<Package> filtered = cq.select(rootEntry).where(predicates.toArray(new Predicate[]{}));

        TypedQuery<Package> filteredQuery = entityManager.createQuery(filtered);
        List<Package> queryResult = filteredQuery.getResultList();

        entityManager.close();

        return queryResult;
    }

}