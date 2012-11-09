package ws.palladian.classification.nb;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.javatuples.Tuple;

import ws.palladian.classification.Model;
import ws.palladian.helper.collection.CountMap;

/**
 * <p>
 * The model implementation for the {@link NaiveBayesClassifier}.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class NaiveBayesModel implements Model {

    private static final long serialVersionUID = 1L;

    private final CountMap<Triplet<String, String, String>> nominalCounts;

    private final CountMap<String> categories;

    private final Map<Pair<String, String>, Double> sampleMeans;

    private final Map<Pair<String, String>, Double> standardDeviations;

    /**
     * <p>
     * Instantiate a new {@link NaiveBayesModel}.
     * </p>
     * 
     * @param nominalCounts {@link CountMap} with all nominal values. They have to be stored as {@link Triplet} (name,
     *            value, category), not <code>null</code>.
     * @param categories {@link CountMap} with all categories, not <code>null</code>.
     * @param sampleMeans {@link Map} with sample means as values for all numeric values. The keys have to be stored as
     *            {@link Tuple}s (name, category), not <code>null</code>.
     * @param standardDeviations {@link Map} with standard deviations as values for all numeric values. The keys have to
     *            be stored as {@link Tuple}s (name, category), not <code>null</code>.
     */
    NaiveBayesModel(CountMap<Triplet<String, String, String>> nominalCounts, CountMap<String> categories,
            Map<Pair<String, String>, Double> sampleMeans, Map<Pair<String, String>, Double> standardDeviations) {
        this.nominalCounts = nominalCounts;
        this.categories = categories;
        this.sampleMeans = sampleMeans;
        this.standardDeviations = standardDeviations;
    }

    /**
     * <p>
     * Get the unique names of all categories.
     * </p>
     * 
     * @return {@link Set} with category names.
     */
    public Set<String> getCategoryNames() {
        return categories.uniqueItems();
    }

    /**
     * <p>
     * Get the prior for the specified category.
     * </p>
     * 
     * @param category The category for which to get the prior, not <code>null</code>.
     * @return The prior for the specified category.
     */
    public double getPrior(String category) {
        Validate.notNull(category, "category must not be null");
        return (double)categories.getCount(category) / categories.totalSize();
    }

    /**
     * <p>
     * Get the probability for a nominal feature.
     * </p>
     * 
     * @param featureName The name of the nominal feature, not <code>null</code>.
     * @param featureValue The value of the nominal feature, not <code>null</code>.
     * @param category The category for which to determine the probability.
     * @param laplace The Laplace corrector, equal or greater than zero, zero denotes no correction.
     * @return The probability value for the specified feature/name in the specified category.
     */
    public double getProbability(String featureName, String featureValue, String category, double laplace) {
        Validate.notNull(featureName, "featureName must not be null");
        Validate.notNull(featureValue, "featureValue must not be null");
        Validate.notNull(category, "category must not be null");
        Validate.isTrue(laplace >= 0, "laplace corrector must be equal or greater than zero");

        int count = nominalCounts.getCount(new Triplet<String, String, String>(featureName, featureValue, category));

        // Laplace smoothing:
        // pretend we have seen each result once more than we actually did;
        // therefore, we must also add the number of categories to the denominator:
        // P(X = i) = n_i / N becomes P(X = i) = (n_i + 1) / (N + K)

        // return (double)(count + 1) / (categories.getCount(category) + categories.uniqueSize());
        return (double)(count + laplace) / (categories.getCount(category) + laplace * categories.uniqueSize());
    }

    /**
     * <p>
     * Get the standard deviation for a numeric feature in a specified category.
     * </p>
     * 
     * @param featureName Name of the numeric feature for which to get the standard deviation, not <code>null</code>.
     * @param category The category, not <code>null</code>.
     * @return The standard deviation for the specified numeric feature in the specified category, or <code>null</code>
     *         if no value exists.
     */
    private Double getStandardDeviation(String featureName, String category) {
        return standardDeviations.get(new Pair<String, String>(featureName, category));
    }

    /**
     * <p>
     * Get the mean for a numeric feature in a specified category.
     * </p>
     * 
     * @param featureName Name of the numeric feature for which to get the mean, not <code>null</code>.
     * @param category The category, not <code>null</code>.
     * @return The mean for the specified numeric feature in the specified category, or <code>null</code> if no value
     *         exists.
     */
    private Double getMean(String featureName, String category) {
        return sampleMeans.get(new Pair<String, String>(featureName, category));
    }

    /**
     * <p>
     * Get the density for a numeric feature.
     * </p>
     * 
     * @param featureName The name of the numeric feature, not <code>null</code>.
     * @param featureValue The value of the numeric feature.
     * @param category The category for which to determine the density.
     * @return The density value for the specified feature/name in the specified category.
     */
    public double getDensity(String featureName, double featureValue, String category) {
        Validate.notNull(featureName, "featureName must not be null");
        Validate.notNull(category, "category must not be null");

        Double standardDeviation = getStandardDeviation(featureName, category);
        Double mean = getMean(featureName, category);

        // I am currently not sure how to handle the case, when:
        // a) we have no values for [feature, category] combination, b) we have a standard deviation of zero.
        // Here, I just return zero and do not consider it for probability calculation. This seems to work, but I'm not
        // sure if Mr. Bayes would agree to this method. See also:
        // http://stackoverflow.com/questions/12344375/probability-density-function-with-zero-standard-deviation
        // http://stats.stackexchange.com/questions/35694/naive-bayes-fails-with-a-perfect-predictor
        if (standardDeviation == null || standardDeviation == 0) {
            return 0;
        }

        return 1 / (Math.sqrt(2 * Math.PI) * standardDeviation)
                * Math.pow(Math.E, -Math.pow(featureValue - mean, 2) / (2 * Math.pow(standardDeviation, 2)));

    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NaiveBayesModel [nominalCounts=");
        builder.append(nominalCounts);
        builder.append(", categories=");
        builder.append(categories);
        builder.append(", sampleMeans=");
        builder.append(sampleMeans);
        builder.append(", standardDeviations=");
        builder.append(standardDeviations);
        builder.append("]");
        return builder.toString();
    }

}