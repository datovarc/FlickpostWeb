package co.flickpost.admin.repositories;

import co.flickpost.admin.models.Company;
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

    public List<Company> getAllCompanies(){
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Company> cq = cb.createQuery(Company.class);
        Root<Company> root = cq.from(Company.class);

        List<Company> companies = entityManager.createQuery(cq).getResultList();

        entityManager.close();

        return companies;
    }

}