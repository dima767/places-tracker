package dk.placestracker.service;

import dk.placestracker.domain.model.Country;
import dk.placestracker.domain.model.StateProvince;
import dk.placestracker.domain.repository.CountryRepository;
import dk.placestracker.domain.repository.StateProvinceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Default implementation of ReferenceDataService.
 *
 * @author Dmitriy Kopylenko
 */
@Service
public class DefaultReferenceDataService implements ReferenceDataService {

    private final CountryRepository countryRepository;
    private final StateProvinceRepository stateProvinceRepository;

    public DefaultReferenceDataService(CountryRepository countryRepository,
                                       StateProvinceRepository stateProvinceRepository) {
        this.countryRepository = countryRepository;
        this.stateProvinceRepository = stateProvinceRepository;
    }

    @Override
    public List<Country> getAllCountries() {
        return countryRepository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    @Override
    public List<StateProvince> getAllStatesProvinces() {
        return stateProvinceRepository.findAllByOrderByCountryCodeAscDisplayOrderAscNameAsc();
    }

    @Override
    public List<StateProvince> getStatesProvincesByCountry(String countryCode) {
        return stateProvinceRepository.findByCountryCodeOrderByDisplayOrderAscNameAsc(countryCode);
    }
}
