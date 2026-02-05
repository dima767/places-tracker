package dk.placestracker.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Reference data for states and provinces.
 *
 * @author Dmitriy Kopylenko
 */
@Document(collection = "states_provinces")
@CompoundIndex(name = "country_code_idx", def = "{'countryCode': 1, 'code': 1}", unique = true)
public record StateProvince(
        @Id
        String id,

        String code,

        String name,

        @Indexed
        String countryCode,

        Integer displayOrder
) {
    public static StateProvince create(String code, String name, String countryCode, Integer displayOrder) {
        return new StateProvince(null, code, name, countryCode, displayOrder);
    }
}
