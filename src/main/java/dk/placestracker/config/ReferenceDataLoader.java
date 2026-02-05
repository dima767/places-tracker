package dk.placestracker.config;

import dk.placestracker.domain.model.Country;
import dk.placestracker.domain.model.StateProvince;
import dk.placestracker.domain.repository.CountryRepository;
import dk.placestracker.domain.repository.StateProvinceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Loads reference data (countries and states/provinces) into the database on startup.
 * Only includes North American countries: United States, Canada, and Mexico.
 *
 * @author Dmitriy Kopylenko
 */
@Component
public class ReferenceDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReferenceDataLoader.class);

    private final CountryRepository countryRepository;
    private final StateProvinceRepository stateProvinceRepository;

    public ReferenceDataLoader(CountryRepository countryRepository,
                               StateProvinceRepository stateProvinceRepository) {
        this.countryRepository = countryRepository;
        this.stateProvinceRepository = stateProvinceRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        loadCountries();
        loadStatesProvinces();
    }

    private void loadCountries() {
        if (countryRepository.count() > 0) {
            log.debug("Countries already loaded, skipping initialization");
            return;
        }

        log.info("Loading North American country reference data...");

        List<Country> countries = List.of(
                Country.create("US", "United States", 1),
                Country.create("CA", "Canada", 2),
                Country.create("MX", "Mexico", 3)
        );

        countryRepository.saveAll(countries);
        log.info("Loaded {} North American countries", countries.size());
    }

    private void loadStatesProvinces() {
        if (stateProvinceRepository.count() > 0) {
            log.debug("States/Provinces already loaded, skipping initialization");
            return;
        }

        log.info("Loading state/province reference data...");

        List<StateProvince> statesProvinces = List.of(
                // US States
                StateProvince.create("AL", "Alabama", "US", 1),
                StateProvince.create("AK", "Alaska", "US", 2),
                StateProvince.create("AZ", "Arizona", "US", 3),
                StateProvince.create("AR", "Arkansas", "US", 4),
                StateProvince.create("CA", "California", "US", 5),
                StateProvince.create("CO", "Colorado", "US", 6),
                StateProvince.create("CT", "Connecticut", "US", 7),
                StateProvince.create("DE", "Delaware", "US", 8),
                StateProvince.create("FL", "Florida", "US", 9),
                StateProvince.create("GA", "Georgia", "US", 10),
                StateProvince.create("HI", "Hawaii", "US", 11),
                StateProvince.create("ID", "Idaho", "US", 12),
                StateProvince.create("IL", "Illinois", "US", 13),
                StateProvince.create("IN", "Indiana", "US", 14),
                StateProvince.create("IA", "Iowa", "US", 15),
                StateProvince.create("KS", "Kansas", "US", 16),
                StateProvince.create("KY", "Kentucky", "US", 17),
                StateProvince.create("LA", "Louisiana", "US", 18),
                StateProvince.create("ME", "Maine", "US", 19),
                StateProvince.create("MD", "Maryland", "US", 20),
                StateProvince.create("MA", "Massachusetts", "US", 21),
                StateProvince.create("MI", "Michigan", "US", 22),
                StateProvince.create("MN", "Minnesota", "US", 23),
                StateProvince.create("MS", "Mississippi", "US", 24),
                StateProvince.create("MO", "Missouri", "US", 25),
                StateProvince.create("MT", "Montana", "US", 26),
                StateProvince.create("NE", "Nebraska", "US", 27),
                StateProvince.create("NV", "Nevada", "US", 28),
                StateProvince.create("NH", "New Hampshire", "US", 29),
                StateProvince.create("NJ", "New Jersey", "US", 30),
                StateProvince.create("NM", "New Mexico", "US", 31),
                StateProvince.create("NY", "New York", "US", 32),
                StateProvince.create("NC", "North Carolina", "US", 33),
                StateProvince.create("ND", "North Dakota", "US", 34),
                StateProvince.create("OH", "Ohio", "US", 35),
                StateProvince.create("OK", "Oklahoma", "US", 36),
                StateProvince.create("OR", "Oregon", "US", 37),
                StateProvince.create("PA", "Pennsylvania", "US", 38),
                StateProvince.create("RI", "Rhode Island", "US", 39),
                StateProvince.create("SC", "South Carolina", "US", 40),
                StateProvince.create("SD", "South Dakota", "US", 41),
                StateProvince.create("TN", "Tennessee", "US", 42),
                StateProvince.create("TX", "Texas", "US", 43),
                StateProvince.create("UT", "Utah", "US", 44),
                StateProvince.create("VT", "Vermont", "US", 45),
                StateProvince.create("VA", "Virginia", "US", 46),
                StateProvince.create("WA", "Washington", "US", 47),
                StateProvince.create("WV", "West Virginia", "US", 48),
                StateProvince.create("WI", "Wisconsin", "US", 49),
                StateProvince.create("WY", "Wyoming", "US", 50),

                // Canadian Provinces and Territories
                StateProvince.create("AB", "Alberta", "CA", 1),
                StateProvince.create("BC", "British Columbia", "CA", 2),
                StateProvince.create("MB", "Manitoba", "CA", 3),
                StateProvince.create("NB", "New Brunswick", "CA", 4),
                StateProvince.create("NL", "Newfoundland and Labrador", "CA", 5),
                StateProvince.create("NT", "Northwest Territories", "CA", 6),
                StateProvince.create("NS", "Nova Scotia", "CA", 7),
                StateProvince.create("NU", "Nunavut", "CA", 8),
                StateProvince.create("ON", "Ontario", "CA", 9),
                StateProvince.create("PE", "Prince Edward Island", "CA", 10),
                StateProvince.create("QC", "Quebec", "CA", 11),
                StateProvince.create("SK", "Saskatchewan", "CA", 12),
                StateProvince.create("YT", "Yukon", "CA", 13),

                // Mexican States
                StateProvince.create("AGS", "Aguascalientes", "MX", 1),
                StateProvince.create("BC", "Baja California", "MX", 2),
                StateProvince.create("BCS", "Baja California Sur", "MX", 3),
                StateProvince.create("CAM", "Campeche", "MX", 4),
                StateProvince.create("CHS", "Chiapas", "MX", 5),
                StateProvince.create("CHH", "Chihuahua", "MX", 6),
                StateProvince.create("COA", "Coahuila", "MX", 7),
                StateProvince.create("COL", "Colima", "MX", 8),
                StateProvince.create("DUR", "Durango", "MX", 9),
                StateProvince.create("GTO", "Guanajuato", "MX", 10),
                StateProvince.create("GRO", "Guerrero", "MX", 11),
                StateProvince.create("HGO", "Hidalgo", "MX", 12),
                StateProvince.create("JAL", "Jalisco", "MX", 13),
                StateProvince.create("MEX", "México", "MX", 14),
                StateProvince.create("MIC", "Michoacán", "MX", 15),
                StateProvince.create("MOR", "Morelos", "MX", 16),
                StateProvince.create("NAY", "Nayarit", "MX", 17),
                StateProvince.create("NLE", "Nuevo León", "MX", 18),
                StateProvince.create("OAX", "Oaxaca", "MX", 19),
                StateProvince.create("PUE", "Puebla", "MX", 20),
                StateProvince.create("QRO", "Querétaro", "MX", 21),
                StateProvince.create("ROO", "Quintana Roo", "MX", 22),
                StateProvince.create("SLP", "San Luis Potosí", "MX", 23),
                StateProvince.create("SIN", "Sinaloa", "MX", 24),
                StateProvince.create("SON", "Sonora", "MX", 25),
                StateProvince.create("TAB", "Tabasco", "MX", 26),
                StateProvince.create("TAM", "Tamaulipas", "MX", 27),
                StateProvince.create("TLA", "Tlaxcala", "MX", 28),
                StateProvince.create("VER", "Veracruz", "MX", 29),
                StateProvince.create("YUC", "Yucatán", "MX", 30),
                StateProvince.create("ZAC", "Zacatecas", "MX", 31),
                StateProvince.create("CMX", "Ciudad de México", "MX", 32)
        );

        stateProvinceRepository.saveAll(statesProvinces);
        log.info("Loaded {} states/provinces", statesProvinces.size());
    }
}
