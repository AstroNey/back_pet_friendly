package lns.back.backend_pet_friendly.domain.model;

public record Coordinates(double latitude, double longitude) {

    public Coordinates {
        if (latitude < -90 || latitude > 90)   throw new IllegalArgumentException("Invalid latitude: "  + latitude);
        if (longitude < -180 || longitude > 180) throw new IllegalArgumentException("Invalid longitude: " + longitude);
    }

    /** Distance en km via formule Haversine. */
    public double distanceTo(Coordinates other) {
        final double R = 6371.0;
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
