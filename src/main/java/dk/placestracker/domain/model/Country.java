package dk.placestracker.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Reference data for countries.
 *
 * @author Dmitriy Kopylenko
 */
@Document(collection = "countries")
public record Country(
        @Id
        String id,

        @Indexed(unique = true)
        String code,

        String name,

        Integer displayOrder
) {
    public static Country create(String code, String name, Integer displayOrder) {
        return new Country(null, code, name, displayOrder);
    }
}
