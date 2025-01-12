package ws.palladian.extraction.location.scope;

import org.apache.commons.lang3.Validate;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.LocationFilters;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.geo.GeoCoordinate;

import java.util.Collection;
import java.util.List;

public final class LeastDistanceScopeDetector extends AbstractRankingScopeDetector {

    private static final String NAME = "LeastDistance";

    public LeastDistanceScopeDetector(LocationExtractor extractor) {
        super(extractor);
    }

    @Override
    public Location getScope(Collection<LocationAnnotation> annotations) {
        Validate.notNull(annotations, "locations must not be null");
        List<Location> locationList = CollectionHelper.convertList(annotations, LocationAnnotation::getLocation);
        CollectionHelper.removeNulls(locationList);
        CollectionHelper.remove(locationList, LocationFilters.coordinate());

        double minDistanceSum = Double.MAX_VALUE;
        Location scopeLocation = null;

        for (int i = 0; i < locationList.size(); i++) {
            double currentDistanceSum = 0;
            Location currentLocation = locationList.get(i);
            GeoCoordinate currentCoordinate = currentLocation.getCoordinate();
            for (int j = 0; j < locationList.size(); j++) {
                GeoCoordinate otherCoordinate = locationList.get(j).getCoordinate();
                currentDistanceSum += currentCoordinate.distance(otherCoordinate);
            }
            if (currentDistanceSum < minDistanceSum) {
                minDistanceSum = currentDistanceSum;
                scopeLocation = currentLocation;
            }
        }
        return scopeLocation;
    }

    @Override
    public String toString() {
        return NAME;
    }

}
