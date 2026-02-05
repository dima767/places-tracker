package dk.placestracker.service;

import dk.placestracker.domain.model.Country;
import dk.placestracker.domain.model.StateProvince;

import java.util.List;

/**
 * Service for loading reference data (countries, states/provinces).
 *
 * @author Dmitriy Kopylenko
 */
public interface ReferenceDataService {

    /**
     * Get all countries ordered by display order and name.
     */
    List<Country> getAllCountries();

    /**
     * Get all states/provinces ordered by country, display order, and name.
     */
    List<StateProvince> getAllStatesProvinces();

    /**
     * Get states/provinces for a specific country.
     */
    List<StateProvince> getStatesProvincesByCountry(String countryCode);
}
