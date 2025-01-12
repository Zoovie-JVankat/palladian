package ws.palladian.extraction.location;

import org.apache.commons.lang3.Validate;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.GeoUtils;
import ws.palladian.helper.math.FatStats;
import ws.palladian.helper.math.Stats;

import java.util.*;
import java.util.function.Predicate;

import static ws.palladian.extraction.location.LocationFilters.coordinate;
import static ws.palladian.helper.collection.CollectionHelper.convertSet;
import static ws.palladian.helper.collection.CollectionHelper.filterSet;
import static ws.palladian.helper.functional.Predicates.equal;
import static ws.palladian.helper.functional.Predicates.not;

/**
 * Provides various statistics for sets of {@link Location}s.
 *
 * @author Philipp Katz
 */
public class LocationSet extends AbstractSet<Location> {

    private final Set<Location> locations;

    public LocationSet(Collection<? extends Location> locations) {
        Validate.notNull(locations, "locations must not be null");
        this.locations = Collections.unmodifiableSet(new HashSet<>(locations));
    }

    // query methods

    /**
     * Get a filtered {@link LocationSet} with items matching the provided filter.
     *
     * @param filter The filter, not <code>null</code>.
     * @return A {@link LocationSet} with all items matching the filter.
     * @see LocationFilters
     */
    public LocationSet where(Predicate<? super Location> filter) {
        Validate.notNull(filter, "filter must not be null");
        return new LocationSet(filterSet(this, filter));
    }

//    /**
//     * Get a filtered {@link LocationSet} with items matching the provided filter, in case at least one item matches,
//     * otherwise return the current locations.
//     *
//     * @param filter The filter, not <code>null</code>.
//     * @return A {@link LocationSet} with all items matching the filter, or the current locations, in case the filtered
//     * result would be empty.
//     * @see LocationFilters
//     */
//    public LocationSet whereConditionally(Predicate<? super Location> filter) {
//        Validate.notNull(filter, "filter must not be null");
//        LocationSet temp = where(filter);
//        return temp.size() > 0 ? temp : this;
//    }

    // spatial properties

    /**
     * @return The geographic midpoint of all locations in this set, or a {@link GeoCoordinate#NULL} in case this set
     * was empty.
     * @see GeoUtils#getMidpoint(Collection)
     */
    public GeoCoordinate midpoint() {
        Set<GeoCoordinate> coordinates = coordinates();
        return coordinates.isEmpty() ? GeoCoordinate.NULL : GeoUtils.getMidpoint(coordinates);
    }

    /**
     * @return The geographic center of all locations in this set, or a {@link GeoCoordinate#NULL} in case this set was
     * empty.
     * @see GeoUtils#getCenterOfMinimumDistance(Collection)
     */
    public GeoCoordinate center() {
        Set<GeoCoordinate> coordinates = coordinates();
        return coordinates.isEmpty() ? GeoCoordinate.NULL : GeoUtils.getCenterOfMinimumDistance(coordinates);
    }

    /**
     * @return The largest distance between any pair in this set, or {@link GeoUtils#EARTH_MAX_DISTANCE_KM} in case this
     * set contained two or more locations without coordinates.
     */
    public double largestDistance() {
        Set<GeoCoordinate> coordinates = coordinates();
        if (size() > 1 && size() - coordinates.size() > 0) {
            return GeoUtils.EARTH_MAX_DISTANCE_KM;
        }
        return GeoUtils.getLargestDistance(coordinates);
    }

    /**
     * Get distance statistics from the given location to all locations in this set (except the given location, in case
     * it is also in this set).
     *
     * @param location The location, not <code>null</code>.
     * @return Distance statistics (e.g. mean, min, max, median, ...)
     */
    public Stats distanceStats(Location location) {
        Validate.notNull(location, "location must not be null");
        GeoCoordinate coordinate = CollectionHelper.coalesce(location.getCoordinate(), GeoCoordinate.NULL);
        LocationSet others = where(coordinate()).where(not(equal(location)));
        Stats distances = new FatStats();
        for (Location other : others) {
            distances.add(coordinate.distance(other.getCoordinate()));
        }
        return distances;
    }

    /**
     * Get the maximum distance from the given coordinate to each location in this set.
     *
     * @param coordinate The coordinate, not <code>null</code>.
     * @return The maximum distance, or {@link GeoUtils#EARTH_MAX_DISTANCE_KM} in case this set was empty.
     */
    public double minDistance(GeoCoordinate coordinate) {
        Validate.notNull(coordinate, "coordinate must not be null");
        double minDistance = GeoUtils.EARTH_MAX_DISTANCE_KM;
        for (GeoCoordinate other : coordinates()) {
            minDistance = Math.min(minDistance, other.distance(coordinate));
        }
        return minDistance;
    }

    /**
     * Get the maximum distance from the given coordinate to each location in this set.
     *
     * @param coordinate The coordinate, not <code>null</code>.
     * @return The minimum distance, or <code>0</code> in case this set was empty.
     */
    public double maxDistance(GeoCoordinate coordinate) {
        Validate.notNull(coordinate, "coordinate must not be null");
        double maxDistance = 0;
        for (GeoCoordinate other : coordinates()) {
            maxDistance = Math.max(maxDistance, other.distance(coordinate));
        }
        return maxDistance;
    }

    /**
     * @return All coordinates in this set, <code>null</code> values and {@link GeoCoordinate#NULL} are skipped.
     */
    public Set<GeoCoordinate> coordinates() {
        return convertSet(where(coordinate()), Location::getCoordinate);
    }

    // population properties

//    /**
//     * @return The biggest location in this set, or <code>null</code> in case this set was empty, or none of the
//     * locations had a population value.
//     * @see Location#getPopulation()
//     */
//    public Location biggest() {
//        Location biggest = null;
//        for (Location location : locations) {
//            Long population = location.getPopulation();
//            if (population != null) {
//                if (biggest == null || population > biggest.getPopulation()) {
//                    biggest = location;
//                }
//            }
//        }
//        return biggest;
//    }

//    /**
//     * @return The maximum population of all locations in this set.
//     * @see Location#getPopulation()
//     */
//    public long biggestPopulation() {
//        Location biggest = biggest();
//        return biggest == null || biggest.getPopulation() == null ? 0 : biggest.getPopulation();
//    }

    /**
     * @return The total population of all locations in this set.
     * @see Location#getPopulation()
     */
    public long totalPopulation() {
        long totalPopulation = 0;
        for (Location location : locations) {
            if (location.getPopulation() != null) {
                totalPopulation += location.getPopulation();
            }
        }
        return totalPopulation;
    }

    // hierarchical properties

    /**
     * @return The maximum hierarchy depth of all locations in this set.
     * @see Location#getAncestorIds()
     */
    public int maxHierarchyDepth() {
        int maxDepth = 1;
        for (Location location : locations) {
            maxDepth = Math.max(maxDepth, location.getAncestorIds().size());
        }
        return maxDepth;
    }

    // other

    /**
     * Check, whether this set contains a location.
     *
     * @param location The location, not <code>null</code>.
     * @return <code>true</code> in case the given location was contained in this set.
     */
    public boolean contains(Location location) {
        Validate.notNull(location, "location must not be null");
        return locations.contains(location);
    }

    /**
     * @return The number of locations in this set.
     */
    @Override
    public int size() {
        return locations.size();
    }

    @Override
    public Iterator<Location> iterator() {
        return locations.iterator();
    }

    /**
     * @return The first location in this set (determined by {@link Location#getId()}, or <code>null</code> in case this
     * set was empty.
     */
    public Location first() {
        Location first = null;
        for (Location location : locations) {
            if (first == null || first.getId() > location.getId()) {
                first = location;
            }
        }
        return first;
    }

}
