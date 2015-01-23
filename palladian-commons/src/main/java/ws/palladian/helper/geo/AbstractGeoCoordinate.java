package ws.palladian.helper.geo;

import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static ws.palladian.helper.geo.GeoUtils.EARTH_RADIUS_KM;

import java.text.NumberFormat;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Default implementation for {@link GeoCoordinate} with implemented utility functionality.
 * </p>
 * 
 * @author pk
 */
public abstract class AbstractGeoCoordinate implements GeoCoordinate {

    /** Constant for formatting degrees. */
    private static final char DEGREES = '°';

    /** Constant for formatting minutes. */
    private static final char MINUTES = '′';

    /** Constant for formatting seconds. */
    private static final char SECONDS = '″';

    @Override
    public double distance(GeoCoordinate other) {
        Validate.notNull(other, "other must not be null");
        double lat1 = toRadians(getLatitude());
        double lon1 = toRadians(getLongitude());
        double lat2 = toRadians(other.getLatitude());
        double lon2 = toRadians(other.getLongitude());
        double dLat = (lat2 - lat1) / 2;
        double dLon = (lon2 - lon1) / 2;
        double a = sin(dLat) * sin(dLat) + cos(lat1) * cos(lat2) * sin(dLon) * sin(dLon);
        return 2 * EARTH_RADIUS_KM * atan2(sqrt(a), sqrt(1 - a));
    }

    @Override
    public String toDmsString() {
        Double lat = getLatitude();
        Double lng = getLongitude();
        if (lat == null || lng == null) {
            return StringUtils.EMPTY;
        }
        int[] latParts = getParts(lat);
        int[] lngParts = getParts(lng);
        String latSuffix = StringUtils.EMPTY;
        if (lat > 0) {
            latSuffix = "N";
        } else if (lat < 0) {
            latSuffix = "S";
        }
        String lngSuffix = StringUtils.EMPTY;
        if (lng > 0) {
            lngSuffix = "E";
        } else if (lng < 0) {
            lngSuffix = "W";
        }
        String latString = formatDms(latParts, latSuffix);
        String lngString = formatDms(lngParts, lngSuffix);
        return latString + "," + lngString;
    }

    /**
     * Produce something like <code>51°1′59″N,13°43′59″E</code>.
     * 
     * @param dmsParts date, minute, seconds parts; zero values will be cut.
     * @param suffix The suffix to append [NSEW].
     * @return The formatted string.
     */
    private static String formatDms(int[] dmsParts, String suffix) {
        StringBuilder dmsBuilder = new StringBuilder();
        dmsBuilder.append(dmsParts[0]).append(DEGREES);
        if (dmsParts[1] != 0 && dmsParts[2] != 0) {
            dmsBuilder.append(dmsParts[1]).append(MINUTES);
            if (dmsParts[2] != 0) {
                dmsBuilder.append(dmsParts[2]).append(SECONDS);
            }
        }
        dmsBuilder.append(suffix);
        return dmsBuilder.toString();
    }

    private static int[] getParts(double decimal) {
        int[] parts = new int[3];
        double temp = Math.abs(decimal);

        parts[0] = (int)temp;

        double mod = temp % 1;
        temp = mod * 60;
        parts[1] = (int)temp;

        mod = temp % 1;
        temp = mod * 60;
        parts[2] = (int)temp;
        return parts;
    }

    @Override
    public double[] getBoundingBox(double distance) {
        Validate.isTrue(distance >= 0, "distance must be equal/greater zero");
        // http://vinsol.com/blog/2011/08/30/geoproximity-search-with-mysql/
        double lat1 = getLatitude() - distance / 111.04;
        double lat2 = getLatitude() + distance / 111.04;
        double long1 = getLongitude() - distance / Math.abs(Math.cos(Math.toRadians(getLatitude())) * 111.04);
        double long2 = getLongitude() + distance / Math.abs(Math.cos(Math.toRadians(getLatitude())) * 111.04);
        return new double[] {lat1, long1, lat2, long2};
    }

    @Override
    public GeoCoordinate getCoordinate(double distance, double bearing) {
        Validate.isTrue(distance >= 0, "distance must be greater/equal zero");
        // http://www.movable-type.co.uk/scripts/latlong.html
        double latRad = toRadians(getLatitude());
        double lngRad = toRadians(getLongitude());
        double bearingRad = toRadians(bearing);
        double d = distance / EARTH_RADIUS_KM;
        double resultLatRad = asin(sin(latRad) * cos(d) + cos(latRad) * sin(d) * cos(bearingRad));
        double resultLngRad = lngRad
                + atan2(sin(bearingRad) * sin(d) * cos(latRad), cos(d) - sin(latRad) * sin(resultLatRad));
        double resultLat = toDegrees(resultLatRad);
        double resultLng = GeoUtils.normalizeLongitude(toDegrees(resultLngRad));
        return new ImmutableGeoCoordinate(resultLat, resultLng);
    }

    @Override
    public String toString() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        return String.format("(%s,%s)", format.format(getLatitude()), format.format(getLongitude()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(getLatitude());
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(getLongitude());
        result = prime * result + (int)(temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbstractGeoCoordinate other = (AbstractGeoCoordinate)obj;
        if (Double.doubleToLongBits(getLatitude()) != Double.doubleToLongBits(other.getLatitude()))
            return false;
        if (Double.doubleToLongBits(getLongitude()) != Double.doubleToLongBits(other.getLongitude()))
            return false;
        return true;
    }

}