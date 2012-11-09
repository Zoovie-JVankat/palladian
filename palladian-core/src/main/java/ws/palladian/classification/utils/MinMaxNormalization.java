package ws.palladian.classification.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.Instance;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * This class stores minimum and maximum values for a list of numeric features. It can be used to perform a Min-Max
 * normalization. Use {@link ClassificationUtils#calculateMinMaxNormalization(List)} to calculate the normalization
 * information.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class MinMaxNormalization implements Serializable {

    private static final long serialVersionUID = 7227377881428315427L;

    private final Map<String, Double> maxValues;
    private final Map<String, Double> minValues;

    /**
     * @param maxValues Map with maximum values for each numeric feature.
     * @param minValues Map with minimum values for each numeric feature.
     */
    MinMaxNormalization(Map<String, Double> maxValues, Map<String, Double> minValues) {
        this.maxValues = maxValues;
        this.minValues = minValues;
    }

    /**
     * <p>
     * Normalize a {@link List} of {@link Instance}s based on the normalization information. The values are modified
     * directly in place.
     * </p>
     * 
     * @param instances The List of Instances, not <code>null</code>.
     */
    public void normalize(List<Instance> instances) {
        Validate.notNull(instances, "instances must not be null");

        for (Instance instance : instances) {
            List<NumericFeature> numericFeatures = instance.getFeatureVector().getAll(NumericFeature.class);

            for (NumericFeature numericFeature : numericFeatures) {
                normalize(numericFeature);
            }
        }
    }

    private void normalize(NumericFeature numericFeature) {
        String featureName = numericFeature.getName();
        double featureValue = numericFeature.getValue();

        Double minValue = minValues.get(featureName);
        Double maxValue = maxValues.get(featureName);
        double maxMinDifference = maxValue - minValue;
        double normalizedValue = (featureValue - minValue) / maxMinDifference;

        numericFeature.setValue(normalizedValue);
    }

    /**
     * <p>
     * Normalize a {@link FeatureVector} based in the normalization information. The values are modified directly in
     * place.
     * </p>
     * 
     * @param featureVector The FeatureVector to normalize, not <code>null</code>.
     */
    public void normalize(FeatureVector featureVector) {
        Validate.notNull(featureVector, "featureVector must not be null");
        for (NumericFeature feature : featureVector.getAll(NumericFeature.class)) {
            normalize(feature);
        }
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("MinMaxNormalization:\n");
        List<String> names = new ArrayList<String>(minValues.keySet());
        Collections.sort(names);
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                toStringBuilder.append('\n');
            }
            String name = names.get(i);
            toStringBuilder.append(name).append(": ");
            toStringBuilder.append(minValues.get(name)).append("; ");
            toStringBuilder.append(maxValues.get(name));
        }
        return toStringBuilder.toString();
    }

}