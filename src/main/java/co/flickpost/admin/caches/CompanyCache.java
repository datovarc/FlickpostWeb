package co.flickpost.admin.caches;

import co.flickpost.admin.models.Company;

import java.util.ArrayList;
import java.util.List;

public class CompanyCache {
    private List<Company> companies;
    private static CompanyCache ourInstance = new CompanyCache();

    public static CompanyCache getInstance() {
        return ourInstance;
    }

    private CompanyCache() {
    }

    public List<Company> getCompanies() {
        if(companies == null){
            return new ArrayList<>();
        }

        return companies;
    }

    public void setCompanies(List<Company> companies) {
        this.companies = companies;
    }
}
