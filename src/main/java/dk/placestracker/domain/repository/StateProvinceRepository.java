package dk.placestracker.domain.repository;

import dk.placestracker.domain.model.StateProvince;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for StateProvince reference data.
 *
 * @author Dmitriy Kopylenko
 */
public interface StateProvinceRepository extends MongoRepository<StateProvince, String> {

    Optional<StateProvince> findByCountryCodeAndCode(String countryCode, String code);

    List<StateProvince> findAllByOrderByCountryCodeAscDisplayOrderAscNameAsc();

    List<StateProvince> findByCountryCodeOrderByDisplayOrderAscNameAsc(String countryCode);
}
