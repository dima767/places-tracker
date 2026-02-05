package dk.placestracker.service;

import dk.placestracker.web.dto.PlaceExtractResponse;

/**
 * Service for interacting with Google Maps APIs.
 *
 * @author Dmitriy Kopylenko
 */
public interface GoogleMapsService {

    /**
     * Extract place information from a Google Maps URL.
     *
     * @param mapsUrl Google Maps URL (shortened or full)
     * @return PlaceExtractResponse with extracted data or error
     */
    PlaceExtractResponse extractPlaceFromUrl(String mapsUrl);
}
