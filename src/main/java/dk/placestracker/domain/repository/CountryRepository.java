package dk.placestracker.domain.repository;

import dk.placestracker.domain.model.Country;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Country reference data.
 *
 * @author Dmitriy Kopylenko
 */
public interface CountryRepository extends MongoRepository<Country, String> {

    Optional<Country> findByCode(String code);

    List<Country> findAllByOrderByDisplayOrderAscNameAsc();
}
