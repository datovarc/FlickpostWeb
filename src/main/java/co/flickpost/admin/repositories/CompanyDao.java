package co.flickpost.admin.repositories;

import co.flickpost.admin.caches.CompanyCache;
import co.flickpost.admin.models.Company;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.List;

@Repository
@Transactional
public class CompanyDao {

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    private static final Logger logger = LogManager.getLogger(CompanyDao.class);

    @Transactional
    public List<Company> getAllCompanies(){
        CompanyCache companyCache = CompanyCache.getInstance();
        if(!companyCache.getCompanies().isEmpty()){
            logger.info("Got companies from cache.");
            return companyCache.getCompanies();
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();

        logger.info("Transaction started.");

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Company> cq = cb.createQuery(Company.class);
        Root<Company> root = cq.from(Company.class);

        List<Company> companies = entityManager.createQuery(cq).getResultList();
        companyCache.setCompanies(companies);

        transaction.commit();
        logger.info("Transaction committed.");
        entityManager.close();

        return companies;
    }

}