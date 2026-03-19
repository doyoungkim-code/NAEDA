package com.ssafy.naeda.domain.store.bootstrap;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;
import org.springframework.stereotype.Component;

@Component
public class PublicStoreCoordinateConverter {

    private static final double LATITUDE_OFFSET = 0.013271073238785;
    private static final double LONGITUDE_OFFSET = -0.00462756244125;

    private final CoordinateTransform transform;

    public PublicStoreCoordinateConverter() {
        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem source = crsFactory.createFromName("EPSG:5174");
        CoordinateReferenceSystem target = crsFactory.createFromName("EPSG:4326");
        this.transform = new CoordinateTransformFactory().createTransform(source, target);
    }

    public LatLng convert(Double x, Double y) {
        if (x == null || y == null) {
            return null;
        }

        ProjCoordinate source = new ProjCoordinate(x, y);
        ProjCoordinate target = new ProjCoordinate();
        transform.transform(source, target);

        double latitude = round(target.y + LATITUDE_OFFSET);
        double longitude = round(target.x + LONGITUDE_OFFSET);
        if (latitude < 30.0 || latitude > 40.0 || longitude < 120.0 || longitude > 135.0) {
            return null;
        }
        return new LatLng(latitude, longitude);
    }

    private double round(double value) {
        return Math.round(value * 1_000_000d) / 1_000_000d;
    }

    public record LatLng(double latitude, double longitude) {
    }
}
