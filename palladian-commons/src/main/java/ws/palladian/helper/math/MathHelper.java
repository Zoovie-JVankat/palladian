package ws.palladian.helper.math;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The MathHelper provides mathematical functionality.
 * We use FastMath for the following default math functions due to faster speeds: cos, sin, pow, atan2, log, and exp.
 * See https://gist.github.com/ijuma/840120 and https://blog.juma.me.uk/2011/02/23/performance-of-fastmath-from-commons-math/
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class MathHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MathHelper.class);

    /**
     * Public, non-final, so that it can be assigned with a custom seed.
     */
    public static Random RANDOM = new Random();

    private static final Map<Double, String> FRACTION_MAP;

    private static final Map<Double, Double> LOC_Z_MAPPING;

    private static final Pattern FRACTION_PATTERN = Pattern.compile("(\\d+)/(\\d+)");
    private static final Pattern EX_PATTERN = Pattern.compile("\\d+\\.\\d+e-?\\d+");
    private static final Pattern CLEAN_PATTERN1 = Pattern.compile("^[^0-9]+?(?=[a-z-][0-9]|[0-9]|$)");
    private static final Pattern CLEAN_BEFORE_NUMBER = Pattern.compile("^[^0-9]+");
    private static final Pattern CLEAN_PATTERN_REST_AFTER = Pattern.compile("((?<=\\d)[^0-9., ]+(.*)?)|( / .*)");
    private static final Pattern CLEAN_PATTERN1_AFTER = Pattern.compile("(?<=\\d)[^0-9., ]*( .*)?");
    private static final Pattern CLEAN_PATTERN2 = Pattern.compile("\\.(?!\\d)");
    private static final Pattern CLEAN_PATTERN3 = Pattern.compile("(?<!\\d)\\.");
    private static final Pattern CLEAN_PATTERN4 = Pattern.compile("(?<=\\d),(?=\\d\\d?($|\\s))");
    public static final Pattern PARENTHESES_PATTERN = Pattern.compile("[(){}\\[\\]]");

    /**
     * The supported confidence levels.
     */
    public static final Collection<Double> CONFIDENCE_LEVELS;

    static {
        FRACTION_MAP = new HashMap<>();
        FRACTION_MAP.put(0.5, "1/2");
        FRACTION_MAP.put(0.3333, "1/3");
        FRACTION_MAP.put(0.6667, "2/3");
        FRACTION_MAP.put(0.25, "1/4");
        FRACTION_MAP.put(0.75, "3/4");
        FRACTION_MAP.put(0.2, "1/5");
        FRACTION_MAP.put(0.4, "2/5");
        FRACTION_MAP.put(0.6, "3/5");
        FRACTION_MAP.put(0.8, "4/5");
        FRACTION_MAP.put(0.1667, "1/6");
        FRACTION_MAP.put(0.8333, "5/6");
        FRACTION_MAP.put(0.1429, "1/7");
        FRACTION_MAP.put(0.2857, "2/7");
        FRACTION_MAP.put(0.4286, "3/7");
        FRACTION_MAP.put(0.5714, "4/7");
        FRACTION_MAP.put(0.7143, "5/7");
        FRACTION_MAP.put(0.8571, "6/7");
        FRACTION_MAP.put(0.125, "1/8");
        FRACTION_MAP.put(0.375, "3/8");
        FRACTION_MAP.put(0.625, "5/8");
        FRACTION_MAP.put(0.875, "7/8");
        FRACTION_MAP.put(0.1111, "1/9");
        FRACTION_MAP.put(0.2222, "2/9");
        FRACTION_MAP.put(0.4444, "4/9");
        FRACTION_MAP.put(0.5556, "5/9");
        FRACTION_MAP.put(0.7778, "7/9");
        FRACTION_MAP.put(0.8889, "8/9");
        FRACTION_MAP.put(0.1, "1/10");
        FRACTION_MAP.put(0.3, "3/10");
        FRACTION_MAP.put(0.7, "7/10");
        FRACTION_MAP.put(0.9, "9/10");

        Map<Double, Double> locZMapping = new LinkedHashMap<>();
        locZMapping.put(0.75, 1.151);
        locZMapping.put(0.85, 1.139);
        locZMapping.put(0.90, 1.645);
        locZMapping.put(0.95, 1.96);
        locZMapping.put(0.975, 2.243);
        locZMapping.put(0.985, 2.43);
        locZMapping.put(0.99, 2.577);
        locZMapping.put(0.999, 3.3);
        LOC_Z_MAPPING = Collections.unmodifiableMap(locZMapping);
        CONFIDENCE_LEVELS = Collections.unmodifiableSet(locZMapping.keySet());
    }

    private MathHelper() {
        // no instances.
    }

    /**
     * <p>
     * Calculate the Jaccard similarity between two sets. <code>J(A, B) = |A intersection B| / |A union B|</code>.
     * </p>
     *
     * @param setA The first set, not <code>null</code>.
     * @param setB The second set, not <code>null</code>.
     * @return The Jaccard similarity in the range [0, 1].
     * @deprecated Use {@link SetSimilarities#JACCARD}.
     */
    @Deprecated
    public static <T> double computeJaccardSimilarity(Set<T> setA, Set<T> setB) {
        return SetSimilarities.JACCARD.getSimilarity(setA, setB);
    }

    /**
     * <p>
     * Calculate the overlap coefficient between two sets.
     * <code>Overlap(A, B) = |A intersection B| / min(|A|, |B|)</code>.
     * </p>
     *
     * @param setA The first set.
     * @param setB The second set.
     * @return The overlap coefficient in the range [0, 1].
     * @deprecated Use {@link SetSimilarities#OVERLAP}.
     */
    @Deprecated
    public static <T> double computeOverlapCoefficient(Set<T> setA, Set<T> setB) {
        return SetSimilarities.OVERLAP.getSimilarity(setA, setB);
    }

    /**
     * <p>
     * Calculate the confidence interval with a given confidence level and mean. For more information see here: <a
     * href="http://www.bioconsulting.com/calculation_of_the_confidence_interval.htm">Calculation Of The Confidence
     * Interval</a>.
     * </p>
     *
     * @param samples         The number of samples used, greater zero.
     * @param confidenceLevel The level of confidence. Must be one of the values in {@link #CONFIDENCE_LEVELS}.
     * @param mean            The mean, in range [0,1]. If unknown, assume worst case with mean = 0.5.
     * @return The calculated confidence interval.
     */
    public static double computeConfidenceInterval(long samples, double confidenceLevel, double mean) {
        Validate.isTrue(samples > 0, "samples must be greater zero");
        Validate.isTrue(0 <= mean && mean <= 1, "mean must be in range [0,1]");
        Double z = LOC_Z_MAPPING.get(confidenceLevel);
        if (z == null) {
            throw new IllegalArgumentException("confidence level must be one of: {" + StringUtils.join(CONFIDENCE_LEVELS, ", ") + "}, but was " + confidenceLevel);
        }
        return z * Math.sqrt(mean * (1 - mean) / samples);
    }

    public static double round(double number, int digits) {
        if (Double.isNaN(number)) {
            return Double.NaN;
        }
        double numberFactor = FastMath.pow(10.0, digits);
        return Math.round(numberFactor * number) / numberFactor;
    }

    public static double ceil(double number, int digits) {
        if (Double.isNaN(number)) {
            return Double.NaN;
        }
        double numberFactor = FastMath.pow(10.0, digits);
        return Math.ceil(numberFactor * number) / numberFactor;
    }

    public static double floor(double number, int digits) {
        if (Double.isNaN(number)) {
            return Double.NaN;
        }
        double numberFactor = FastMath.pow(10.0, digits);
        return Math.floor(numberFactor * number) / numberFactor;
    }

    /**
     * <p>
     * Check whether one value is in a certain range of another value. For example, value1: 5 is within the range: 2 of
     * value2: 3.
     * </p>
     *
     * @param value1 The value to check whether it is in the range of the other value.
     * @param value2 The value for which the range is added or subtracted.
     * @param range  The range.
     * @return <tt>True</tt>, if value1 <= value2 + range && value1 >= value2 - range, <tt>false</tt> otherwise.
     */
    public static boolean isWithinRange(double value1, double value2, double range) {
        double numMin = value2 - range;
        double numMax = value2 + range;

        return value1 <= numMax && value1 >= numMin;
    }

    /**
     * <p>
     * Check whether one value is in a certain interval. For example, value: 5 is within the interval min: 2 to max: 8.
     * </p>
     *
     * @param value The value to check whether it is in the interval.
     * @param min   The min value of the interval.
     * @param max   the max value of the interval
     * @return <tt>True</tt>, if value >= min && value <= max, <tt>false</tt> otherwise.
     */
    public static boolean isWithinInterval(double value, double min, double max) {
        return value <= max && value >= min;
    }

    public static boolean isWithinMargin(double value1, double value2, double margin) {
        double numMin = value1 - margin * value1;
        double numMax = value1 + margin * value1;

        return value2 < numMax && value2 > numMin;
    }

    public static boolean isWithinCorrectnessMargin(double questionedValue, double correctValue, double correctnessMargin) {
        double numMin = correctValue - correctnessMargin * correctValue;
        double numMax = correctValue + correctnessMargin * correctValue;

        return questionedValue < numMax && questionedValue > numMin;
    }

    public static int faculty(int number) {
        int faculty = number;
        while (number > 1) {
            number--;
            faculty *= number;
        }
        return faculty;
    }

    /**
     * <p>
     * Check whether two numeric intervals overlap.
     * </p>
     *
     * @param start1 The start1.
     * @param end1   The end1.
     * @param start2 The start2.
     * @param end2   The end2.
     * @return True, if the intervals overlap, false otherwise.
     */
    public static boolean overlap(int start1, int end1, int start2, int end2) {
        return Math.max(start1, start2) < Math.min(end1, end2);
    }

    // public static double computeRootMeanSquareError(String inputFile, final String columnSeparator) {
    // // array with correct and predicted values
    // final List<double[]> values = new ArrayList<double[]>();
    //
    // LineAction la = new LineAction() {
    // @Override
    // public void performAction(String line, int lineNumber) {
    // String[] parts = line.split(columnSeparator);
    //
    // double[] pair = new double[2];
    // pair[0] = Double.valueOf(parts[0]);
    // pair[1] = Double.valueOf(parts[1]);
    //
    // values.add(pair);
    // }
    // };
    //
    // FileHelper.performActionOnEveryLine(inputFile, la);
    //
    // return computeRootMeanSquareError(values);
    // }

    // /**
    // * @deprecated Use the {@link Stats} instead.
    // */
    // @Deprecated
    // public static double computeRootMeanSquareError(List<double[]> values) {
    // double sum = 0.0;
    // for (double[] d : values) {
    // sum +=FastMath.pow(d[0] - d[1], 2);
    // }
    //
    // return Math.sqrt(sum / values.size());
    // }

    /**
     * Calculate similarity of two lists of the same size.
     *
     * @param list1 The first list.
     * @param list2 The second list.
     * @return The similarity of the two lists.
     */
    public static ListSimilarity computeListSimilarity(List<String> list1, List<String> list2) {
        // get maximum possible distance
        int summedMaxDistance = 0;
        int summedMaxSquaredDistance = 0;
        int distance = list1.size() - 1;
        for (int i = list1.size(); i > 0; i -= 2) {
            summedMaxDistance += 2 * distance;
            summedMaxSquaredDistance += 2 * FastMath.pow(distance, 2);
            distance -= 2;
        }

        // get real distance between lists
        int summedRealDistance = 0;
        int summedRealSquaredDistance = 0;
        int position1 = 0;
        Stats stats = new SlimStats();

        for (String entry1 : list1) {
            int position2 = 0;
            for (String entry2 : list2) {
                if (entry1.equals(entry2)) {
                    summedRealDistance += Math.abs(position1 - position2);
                    summedRealSquaredDistance += FastMath.pow(position1 - position2, 2);

                    double[] values = new double[2];
                    values[0] = position1;
                    values[1] = position2;
                    stats.add(Math.abs(position1 - position2));
                    break;
                }
                position2++;
            }

            position1++;
        }

        double similarity = 1 - (double) summedRealDistance / (double) summedMaxDistance;
        double squaredShiftSimilarity = 1 - (double) summedRealSquaredDistance / (double) summedMaxSquaredDistance;
        double rootMeanSquareError = stats.getRmse();

        return new ListSimilarity(similarity, squaredShiftSimilarity, rootMeanSquareError);
    }

    public static ListSimilarity computeListSimilarity(String listFile, final String separator) {

        // two list
        final List<String> list1 = new ArrayList<String>();
        final List<String> list2 = new ArrayList<String>();

        LineAction la = new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] parts = line.split(separator);
                list1.add(parts[0]);
                list2.add(parts[1]);
            }
        };

        FileHelper.performActionOnEveryLine(listFile, la);

        return computeListSimilarity(list1, list2);
    }

    /**
     * <p>
     * Transform an IP address to a number.
     * </p>
     *
     * @param ipAddress The IP address given in w.x.y.z notation.
     * @return The integer of the IP address.
     */
    public static Long ipToNumber(String ipAddress) {
        String[] addrArray = ipAddress.split("\\.");

        long num = 0;
        for (int i = 0; i < addrArray.length; i++) {
            int power = 3 - i;
            num += Integer.parseInt(addrArray[i]) % 256 * FastMath.pow(256, power);
        }
        return num;
    }

    /**
     * <p>
     * Transform a number into an IP address.
     * </p>
     *
     * @param number The integer to be transformed.
     * @return The IP address.
     */
    public static String numberToIp(long number) {
        return (number >> 24 & 0xFF) + "." + (number >> 16 & 0xFF) + "." + (number >> 8 & 0xFF) + "." + (number & 0xFF);
    }

    /**
     * <p>
     * Return a random entry from a given collection.
     * </p>
     *
     * @param list The collection from we want to sample from.
     * @return A random entry from the collection.
     */
    public static <T> T getRandomEntry(List<T> list) {
        int randomIndex = getRandomIntBetween(0, list.size() - 1);
        return list.get(randomIndex);
    }

    public static <T> T getRandomEntry(Set<T> set) {
        return getRandomEntry(new ArrayList<>(set));
    }

    /**
     * <p>
     * Return a random entry from a given collection and use reservoir sampling (might be VERY slow for slightly collections > 1000 entries)
     * </p>
     *
     * @param collection The collection from we want to sample from.
     * @return A random entry from the collection.
     */
    public static <T> T getRandomEntryWithSampling(Collection<T> collection) {
        // Collection<T> randomSample = randomSample(collection, 1);
        Collection<T> randomSample = sample(collection, 1);
        return CollectionHelper.getFirst(randomSample);
    }

    // /**
    // * <p>
    // * Create a random sample from a given collection.
    // * </p>
    // *
    // * @param collection The collection from we want to sample from.
    // * @param sampleSize The size of the sample.
    // * @return A collection with samples from the collection.
    // */
    // public static <T> Collection<T> randomSample(Collection<T> collection, int sampleSize) {
    //
    // if (collection.size() < sampleSize) {
    // LOGGER.debug(
    // "tried to sample from a collection that was smaller than the sample size (Collection: {}, sample size: {}",
    // collection.size(), sampleSize);
    // return collection;
    // } else if (collection.size() == sampleSize) {
    // return collection;
    // }
    //
    // Set<Integer> randomNumbers = MathHelper.createRandomNumbers(sampleSize, 0, collection.size());
    //
    // Set<Integer> indicesUsed = new HashSet<Integer>();
    // Set<T> sampledCollection = new HashSet<T>();
    //
    // for (int randomIndex : randomNumbers) {
    //
    // int currentIndex = 0;
    // for (T o : collection) {
    //
    // if (currentIndex < randomIndex) {
    // currentIndex++;
    // continue;
    // }
    //
    // sampledCollection.add(o);
    // indicesUsed.add(randomIndex);
    // break;
    // }
    //
    // }
    //
    // return sampledCollection;
    // }

    /**
     * <p>
     * Create a random sampling of the given size using a <a
     * href="http://en.wikipedia.org/wiki/Reservoir_sampling">Reservoir Sampling</a> algorithm. The input data can be
     * supplied as iterable, thus does not have to fit in memory. Only the created random sample is kept in memory.
     *
     * @param input The iterable providing the input data, not <code>null</code>.
     * @param k     The size of the sampling.
     * @return A {@link Collection} with the random sample of size k (or smaller, in case the input data did not provide
     * enough samples).
     */
    public static <T> Collection<T> sample(Iterable<T> input, int k) {
        return sample(input.iterator(), k);
    }

    /**
     * <p>
     * Create a random sampling of the given size using a <a
     * href="http://en.wikipedia.org/wiki/Reservoir_sampling">Reservoir Sampling</a> algorithm. The input data can be
     * supplied as iterator, thus does not have to fit in memory. Only the created random sample is kept in memory.
     *
     * @param input The iterator providing the input data, not <code>null</code>.
     * @param k     The size of the sampling.
     * @return A {@link Collection} with the random sample of size k (or smaller, in case the input data did not provide
     * enough samples).
     */
    public static <T> Collection<T> sample(Iterator<T> input, int k) {
        Validate.notNull(input, "input must not be null");
        Validate.isTrue(k >= 0, "k must be greater/equal zero");
        List<T> sample = new ArrayList<>(k);
        for (int i = 0; i < k; i++) {
            if (input.hasNext()) {
                sample.add(input.next());
            } else {
                break;
            }
        }

        int i = k + 1;
        while (input.hasNext()) {
            T item = input.next();
            int j = RANDOM.nextInt(i++);
            if (j < k) {
                sample.set(j, item);
            }
        }
        return sample;
    }

    /**
     * <p>
     * Create numbers random numbers between [min,max).
     * </p>
     *
     * @param numbers Number of numbers to generate.
     * @param min     The minimum number.
     * @param max     The maximum number.
     * @return A set of random numbers between min and max.
     */
    public static Set<Integer> createRandomNumbers(int numbers, int min, int max) {
        Set<Integer> randomNumbers = new HashSet<Integer>();

        if (max - min < numbers) {
            LOGGER.warn("the range between min ({}) and max ({}) is not enough to create enough random numbers", min, max);
            return randomNumbers;
        }
        while (randomNumbers.size() < numbers) {
            double nd = RANDOM.nextDouble();
            int randomNumber = (int) (nd * max + min);
            randomNumbers.add(randomNumber);
        }

        return randomNumbers;
    }

    /**
     * <p>
     * Returns a random number in the interval [low,high].
     * </p>
     *
     * @param low  The minimum number.
     * @param high The maximum number.
     * @return The random number within the interval.
     */
    public static int getRandomIntBetween(int low, int high) {
        int hl = high - low;
        return (int) Math.round(RANDOM.nextDouble() * hl + low);
    }

    /**
     * Calculate the parameters for a regression line. A series of x and y must be given. y = beta * x + alpha
     * TODO multiple regression model:
     * http://www.google.com/url?sa=t&source=web&cd=6&ved=0CC8QFjAF&url=http%3A%2F%2Fwww.
     * bbn-school.org%2Fus%2Fmath%2Fap_stats
     * %2Fproject_abstracts_folder%2Fproj_student_learning_folder%2Fmultiple_reg__ludlow
     * .pps&ei=NQQ7TOHNCYacOPan6IoK&usg=AFQjCNEybhIQVP2xwNGHEdYMgqNYelp1lQ&sig2=cwCNr11vMv0PHwdwu_LIAQ,
     * http://www.stat.ufl.edu/~aa/sta6127/ch11.pdf
     * <p>
     * See <a href="http://en.wikipedia.org/wiki/Simple_linear_regression">http://en.wikipedia.org/wiki/
     * Simple_linear_regression</a> for an explanation.
     *
     * @param x A series of x values.
     * @param y A series of y values.
     * @return The parameter alpha [0] and beta [1] for the regression line.
     */
    public static double[] performLinearRegression(double[] x, double[] y) {
        double[] alphaBeta = new double[2];

        if (x.length != y.length) {
            LOGGER.warn("linear regression input is not correct, for each x, there must be a y");
        }
        double n = x.length;
        double sx = 0;
        double sy = 0;
        double sxx = 0;
        // double syy = 0;
        double sxy = 0;

        for (int i = 0; i < n; i++) {
            sx += x[i];
            sy += y[i];
            sxx += x[i] * x[i];
            // syy += y[i] * y[i];
            sxy += x[i] * y[i];
        }

        double beta = (n * sxy - sx * sy) / (n * sxx - sx * sx);
        double alpha = sy / n - beta * sx / n;

        alphaBeta[0] = alpha;
        alphaBeta[1] = beta;

        return alphaBeta;
    }

    /**
     * <p>
     * Calculates the Precision and Average Precision for a ranked list. Pr and AP for each rank are returned as a two
     * dimensional array, where the first dimension indicates the Rank k, the second dimension distinguishes between Pr
     * and AP. Example:
     * </p>
     *
     * <pre>
     * double[][] ap = MathHelper.calculateAP(rankedList);
     * int k = rankedList.size() - 1;
     * double prAtK = ap[k][0];
     * double apAtK = ap[k][1];
     * </pre>
     *
     * @param rankedList                  The ranked list with Boolean values indicating the relevancies of the items.
     * @param totalNumberRelevantForQuery The total number of relevant documents for the query.
     * @return A two dimensional array containing Precision @ Rank k and Average Precision @ Rank k.
     */
    public static double[][] computeAveragePrecision(List<Boolean> rankedList, int totalNumberRelevantForQuery) {

        // number of relevant entries at k
        int numRelevant = 0;

        // sum of all relevant precisions at k
        double relPrSum = 0;
        double[][] result = new double[rankedList.size()][2];

        for (int k = 0; k < rankedList.size(); k++) {

            boolean relevant = rankedList.get(k);

            if (relevant) {
                numRelevant++;
            }

            double prAtK = (double) numRelevant / (k + 1);

            if (relevant) {
                relPrSum += prAtK;
            }

            double ap = relPrSum / totalNumberRelevantForQuery;

            result[k][0] = prAtK;
            result[k][1] = ap;
        }

        return result;
    }

    public static double log2(double num) {
        return FastMath.log(num) / FastMath.log(2);
    }

    public static long crossTotal(long s) {
        if (s < 10) {
            return s;
        }
        return crossTotal(s / 10) + s % 10;
    }

    /**
     * <p>
     * Compute the Pearson's correlation coefficient between to variables.
     * </p>
     *
     * @param x A list of double values from the data series of the first variable.
     * @param y A list of double values from the data series of the second variable.
     * @return The Pearson correlation coefficient.
     */
    public static double computePearsonCorrelationCoefficient(List<Double> x, List<Double> y) {

        double sumX = 0.;
        double sumY = 0.;

        for (Double v : x) {
            sumX += v;
        }
        for (Double v : y) {
            sumY += v;
        }

        double avgX = sumX / x.size();
        double avgY = sumY / y.size();

        double nominator = 0.;
        double denominatorX = 0.;
        double denominatorY = 0.;

        for (int i = 0; i < x.size(); i++) {
            nominator += (x.get(i) - avgX) * (y.get(i) - avgY);
            denominatorX += FastMath.pow(x.get(i) - avgX, 2);
            denominatorY += FastMath.pow(y.get(i) - avgY, 2);
        }

        double denominator = Math.sqrt(denominatorX * denominatorY);

        return nominator / denominator;
    }

    /**
     * <p>
     * Try to translate a number into a fraction, e.g. 0.333 = 1/3.
     * </p>
     *
     * @return The fraction of the number if it was possible to transform, otherwise the number as a string.
     * @parameter number A number.
     */
    public static String numberToFraction(Double number) {
        String fraction = StringUtils.EMPTY;

        String sign = number >= 0 ? StringUtils.EMPTY : "-";
        number = Math.abs(number);

        int fullPart = (int) Math.floor(number);
        number = number - fullPart;

        double minMargin = 1;
        for (Entry<Double, String> fractionEntry : FRACTION_MAP.entrySet()) {

            double margin = Math.abs(fractionEntry.getKey() - number);

            if (margin < minMargin) {
                fraction = fractionEntry.getValue();
                minMargin = margin;
            }

        }

        if (number < 0.05 && number >= 0) {
            fraction = "0";
        } else if (number > 0.95 && number <= 1) {
            fraction = "1";
        }

        if (fraction.isEmpty() || number > 1 || number < 0) {
            fraction = String.valueOf(number);
        } else if (fullPart > 0) {
            if (!fraction.equalsIgnoreCase("0")) {
                fraction = fullPart + " " + fraction;
            } else {
                fraction = String.valueOf(fullPart);
            }
        }

        return sign + fraction;
    }

    /**
     * <p>
     * Calculate all combinations for a given array of items.
     * </p>
     * <p>
     * For example, the string "a b c" will return 7 combinations (2^3=8 but all empty is not allowed, hence 7):
     *
     * <pre>
     * a b c
     * a b
     * a c
     * b c
     * c
     * b
     * a
     * </pre>
     *
     * </p>
     *
     * @param items A tokenized string to get the spans for.
     * @return A collection of spans.
     */
    public static <T> Collection<List<T>> computeAllCombinations(T[] items) {

        // create bitvector (all bit combinations other than all zeros)
        int bits = items.length;
        List<List<T>> combinations = new ArrayList<List<T>>();

        int max = (int) FastMath.pow(2, bits);
        for (long i = 1; i < max; i++) {
            List<T> combination = new LinkedList<T>();
            if (computeCombinationRecursive(i, items, combination, 0)) {
                combinations.add(combination);
            }
        }

        return combinations;
    }

    /**
     * <p>
     * Recursive computation function for combinations.
     * </p>
     *
     * @param bitPattern   The pattern describing the indices in the list of {@code items} to include in the resulting
     *                     combination.
     * @param items        The list of items to construct combinations from.
     * @param combination  The result combination will be constructed into this list.
     * @param currentIndex The current index in the list of items. For this call the algorithm needs to decide whether
     *                     to include the item at that position in the combination or not based on whether the value in
     *                     {@code bitPattern} module 2 is 1 ({@code true}) or 0 ({@code false}).
     * @return {@code true} if the computed combination was computed successfully.
     */
    private static <T> boolean computeCombinationRecursive(long bitPattern, T[] items, List<T> combination, int currentIndex) {
        if (bitPattern % 2 != 0) {
            combination.add(items[currentIndex]);
        }
        long nextBitPattern = bitPattern / 2;
        if (nextBitPattern < 1) {
            return true;
        } else {
            return computeCombinationRecursive(nextBitPattern, items, combination, ++currentIndex);
        }
    }

    public static double computeEuclideanVectorDistance(float[] vector1, float[] vector2) {
        return Math.sqrt(computeL2VectorDistance(vector1, vector2));
    }

    public static double computeL2VectorDistance(float[] vector1, float[] vector2) {
        double distance = 0;
        for (int idx = 0; idx < vector1.length; idx++) {
            double value = vector1[idx] - vector2[idx];
            distance += value * value;
        }
        return distance;
    }

    public static double computeManhattanVectorDistance(float[] vector1, float[] vector2) {
        double distance = 0;
        for (int idx = 0; idx < vector1.length; idx++) {
            distance += Math.abs(vector1[idx] - vector2[idx]);
        }
        return distance;
    }

    public static double computeCosineSimilarity(float[] vector1, float[] vector2) {

        double dotProduct = computeDotProduct(vector1, vector2);
        double magnitude1 = computeMagnitude(vector1);
        double magnitude2 = computeMagnitude(vector2);

        return dotProduct / (magnitude1 * magnitude2);
    }

    public static double computeCosineDistance(float[] vector1, float[] vector2) {
        return 1 - computeCosineSimilarity(vector1, vector2);
    }

    public static double computeDotProduct(float[] vector1, float[] vector2) {
        double dotProduct = 0.0;

        for (int i = 0; i < Math.min(vector1.length, vector2.length); i++) {
            dotProduct += vector1[i] * vector2[i];
        }

        return dotProduct;
    }

    public static double computeMagnitude(float[] vector) {
        double magnitude = 0.0;

        for (float double1 : vector) {
            magnitude += double1 * double1;
        }

        return Math.sqrt(magnitude);
    }

    /**
     * <p>
     * Parse a numeric expression in a string to a double.
     * </p>
     *
     * <pre>
     * "0.5" => 0.5
     * "1/2" => 0.5
     * "½" => 0.5
     * "3 1/8" => 3.125
     * "1½" => 1.5
     * "1 ½" => 1.5
     * </pre>
     *
     * @param stringNumber The string containing the numeric expression.
     * @return The parsed double.
     */
    public static Double parseStringNumber(String stringNumber) {
        return parseStringNumber(stringNumber, null);
    }

    public static Double forceParseStringNumber(String stringNumber) {
        stringNumber = CLEAN_BEFORE_NUMBER.matcher(stringNumber).replaceAll(StringUtils.EMPTY);
        return parseStringNumber(stringNumber, null);
    }

    public static List<Double> parseStringNumbers(String stringNumber) {
        List<Double> numbers = new ArrayList<>();

        List<String> rest = new ArrayList<>();
        Double number = parseStringNumber(stringNumber, null, rest);
        numbers.add(number);
        if (!rest.isEmpty()) {
            numbers.add(parseStringNumber(rest.get(0), null, rest));
        }
        CollectionHelper.removeNulls(numbers);

        return numbers;
    }

    public static Double parseStringNumber(String stringNumber, Double defaultIfNothingFound) {
        return parseStringNumber(stringNumber, defaultIfNothingFound, new ArrayList<>());
    }

    public static Double parseStringNumber(String stringNumber, Double defaultIfNothingFound, List<String> rest) {
        Validate.notNull(stringNumber);

        stringNumber = stringNumber.toLowerCase();

        Double value = null;

        // find fraction characters
        Set<String> remove = new HashSet<>();
        if (stringNumber.contains("¼")) {
            if (value == null) {
                value = 0.;
            }
            value += 1 / 4.;
            remove.add("¼");
        }
        if (stringNumber.contains("½")) {
            if (value == null) {
                value = 0.;
            }
            value += 1 / 2.;
            remove.add("½");
        }
        if (stringNumber.contains("¾")) {
            if (value == null) {
                value = 0.;
            }
            value += 3 / 4.;
            remove.add("¾");
        }
        if (stringNumber.contains("⅓")) {
            if (value == null) {
                value = 0.;
            }
            value += 1 / 3.;
            remove.add("⅓");
        }
        if (stringNumber.contains("⅔")) {
            if (value == null) {
                value = 0.;
            }
            value += 2 / 3.;
            remove.add("⅔");
        }
        if (stringNumber.contains("⅕")) {
            if (value == null) {
                value = 0.;
            }
            value += 1 / 5.;
            remove.add("⅕");
        }
        if (stringNumber.contains("⅖")) {
            if (value == null) {
                value = 0.;
            }
            value += 2 / 5.;
            remove.add("⅖");
        }
        if (stringNumber.contains("⅗")) {
            if (value == null) {
                value = 0.;
            }
            value += 3 / 5.;
            remove.add("⅗");
        }
        if (stringNumber.contains("⅘")) {
            if (value == null) {
                value = 0.;
            }
            value += 4 / 5.;
            remove.add("⅘");
        }
        if (stringNumber.contains("⅙")) {
            if (value == null) {
                value = 0.;
            }
            value += 1 / 6.;
            remove.add("⅙");
        }
        if (stringNumber.contains("⅚")) {
            if (value == null) {
                value = 0.;
            }
            value += 5 / 6.;
            remove.add("⅚");
        }
        if (stringNumber.contains("⅛")) {
            if (value == null) {
                value = 0.;
            }
            value += 1 / 8.;
            remove.add("⅛");
        }
        if (stringNumber.contains("⅜")) {
            if (value == null) {
                value = 0.;
            }
            value += 3 / 8.;
            remove.add("⅜");
        }
        if (stringNumber.contains("⅝")) {
            if (value == null) {
                value = 0.;
            }
            value += 5 / 8.;
            remove.add("⅝");
        }
        if (stringNumber.contains("⅞")) {
            if (value == null) {
                value = 0.;
            }
            value += 7 / 8.;
            remove.add("⅞");
        }

        for (String string : remove) {
            stringNumber = stringNumber.replace(string, StringUtils.EMPTY);
        }

        // resolve fractions like "1/2"
        String lastFractionFound = "";
        Matcher matcher = FRACTION_PATTERN.matcher(stringNumber);
        if (matcher.find() && matcher.group(1) != null && matcher.group(2) != null && matcher.group(1).length() < 11 && matcher.group(2).length() < 11) {
            int nominator = Integer.parseInt(matcher.group(1));
            int denominator = Integer.parseInt(matcher.group(2));
            if (value == null) {
                value = 0.;
            }
            double v = nominator / (double) denominator;
            value += v;
            lastFractionFound = v + "";
            stringNumber = stringNumber.replace(matcher.group(), StringUtils.EMPTY);
        }

        // number.numberEX e.g. 4.4353E3 = 4435.3
        Matcher exPattern = EX_PATTERN.matcher(lastFractionFound + stringNumber);
        if (exPattern.find()) {
            try {
                if (value == null) {
                    value = 0.;
                }
                value = Double.parseDouble(exPattern.group(0));
                return value;
            } catch (Exception e) {
                return defaultIfNothingFound;
            }
        }

        //// parse the rest
        stringNumber = " " + stringNumber;
        stringNumber = CLEAN_PATTERN1.matcher(stringNumber).replaceAll(StringUtils.EMPTY);
        if (rest != null) {
            rest.addAll(StringHelper.getRegexpMatches(CLEAN_PATTERN_REST_AFTER, stringNumber).stream().filter(s -> !s.trim().isEmpty()).collect(Collectors.toList()));
            stringNumber = CLEAN_PATTERN_REST_AFTER.matcher(stringNumber).replaceAll(StringUtils.EMPTY);
        }
        stringNumber = CLEAN_PATTERN1_AFTER.matcher(stringNumber).replaceAll(StringUtils.EMPTY);

        // comma to periods if there are commas for decimal separation
        stringNumber = CLEAN_PATTERN4.matcher(stringNumber).replaceAll(".");
        stringNumber = stringNumber.replace(",", "");

        stringNumber = CLEAN_PATTERN2.matcher(stringNumber).replaceAll(StringUtils.EMPTY);
        stringNumber = CLEAN_PATTERN3.matcher(stringNumber).replaceAll(StringUtils.EMPTY);
        stringNumber = stringNumber.trim();
        if (!stringNumber.isEmpty()) {
            try {
                if (value == null) {
                    value = 0.;
                }
                value += Double.parseDouble(stringNumber);
            } catch (Exception e) {
                return defaultIfNothingFound;
            }
        } else if (value == null) {
            return defaultIfNothingFound;
        }

        return value;
    }

    /**
     * Map two natural numbers (non-negative!) to a third natural number. N x N => N.
     * f(a,b) = c where there are now two settings for a and b that produce the same c.
     * <p>
     * The mapping for two maximum most 16 bit integers (65535, 65535) will be 8589803520 which as you see cannot be fit
     * into 32 bits and must be long.
     *
     * @param a The first number.
     * @param b The second number.
     * @return The target number.
     * See https://en.wikipedia.org/wiki/Pairing_function
     */
    public static long cantorize(int a, int b) {
        return (((long) a + (long) b) * ((long) a + (long) b + 1) / 2) + (long) b;
    }

    /**
     * <p>
     * Calculate the <a href="http://en.wikipedia.org/wiki/Order_of_magnitude">order of magnitude</a> for a given
     * number. E.g. <code>orderOfMagnitude(100) = 2</code>.
     * </p>
     *
     * @param number The number.
     * @return The order of magnitude for the given number.
     */
    public static int getOrderOfMagnitude(double number) {
        if (number == 0) {
            // this version works fine for me, but don't know, if this is mathematically correct, see:
            // http://www.mathworks.com/matlabcentral/fileexchange/28559-order-of-magnitude-of-number
            return 0;
        }
        return (int) Math.floor(Math.log10(number));
    }

    /**
     * <p>
     * Add two int values and check for integer overflows.
     *
     * @param a The first value.
     * @param b The second value (negative value to subtract).
     * @return The sum of the given values.
     * @throws ArithmeticException in case of a numeric overflow.
     */
    public static int add(int a, int b) throws ArithmeticException {
        int sum = a + b;
        if ((a & b & ~sum | ~a & ~b & sum) < 0) {
            throw new ArithmeticException("Overflow for " + a + "+" + b);
        }
        return sum;
    }

    // Code taken from: https://github.com/benhamner/Metrics/blob/master/Python/ml_metrics/average_precision.py

    public static <T> double getAveragePrecision(Set<T> actual, List<T> predicted, int k) {
        Objects.requireNonNull(actual, "actual was null");
        Objects.requireNonNull(predicted, "predicted was null");
        if (k <= 0) {
            throw new IllegalStateException("k must be greater one");
        }
        if (actual.isEmpty()) {
            return 0;
        }
        if (predicted.size() > k) {
            predicted = predicted.subList(0, k);
        }
        double score = 0.0;
        int numHits = 0;
        for (int i = 0; i < predicted.size(); i++) {
            T p = predicted.get(i);
            if (actual.contains(p) && !predicted.subList(0, i).contains(p)) {
                numHits++;
                score += numHits / (i + 1.0);
            }
        }
        return score / Math.min(actual.size(), k);
    }

    public static <T> double getMeanAveragePrecision(Iterable<? extends Pair<? extends Set<T>, ? extends List<T>>> data, int k) {
        double meanAveragePrecision = 0;
        int n = 0;
        for (Pair<? extends Set<T>, ? extends List<T>> pair : data) {
            n++;
            meanAveragePrecision += getAveragePrecision(pair.getKey(), pair.getValue(), k);
        }
        return meanAveragePrecision / n;
    }

    /**
     * Faster implementation than default java @see http://www.java-gaming.org/topics/extremely-fast-atan2/36467/msg/346112/view.html#msg346112
     */
    public static float atan(float y) {
        return atan2(y, 1.0f);
    }

    public static float atan2(float y, float x) {
        final float PI = 3.1415927f;
        final float PI_2 = PI / 2f;
        final float MINUS_PI_2 = -PI_2;
        if (x == 0.0f) {
            if (y > 0.0f) {
                return PI_2;
            }
            if (y == 0.0f) {
                return 0.0f;
            }
            return MINUS_PI_2;
        }

        final float atan;
        final float z = y / x;
        if (Math.abs(z) < 1.0f) {
            atan = z / (1.0f + 0.28f * z * z);
            if (x < 0.0f) {
                return (y < 0.0f) ? atan - PI : atan + PI;
            }
            return atan;
        } else {
            atan = PI_2 - z / (z * z + 0.28f);
            return (y < 0.0f) ? atan - PI : atan;
        }
    }

    public static double atan2(double y, double x) {
        final double PI_2 = Math.PI / 2.;
        final double MINUS_PI_2 = -PI_2;
        if (x == 0.0) {
            if (y > 0.0) {
                return PI_2;
            }
            if (y == 0.0) {
                return 0.0;
            }
            return MINUS_PI_2;
        }

        final double atan;
        final double z = y / x;
        if (Math.abs(z) < 1.0) {
            atan = z / (1.0 + 0.28 * z * z);
            if (x < 0.0) {
                return (y < 0.0) ? atan - Math.PI : atan + Math.PI;
            }
            return atan;
        } else {
            atan = PI_2 - z / (z * z + 0.28);
            return (y < 0.0) ? atan - Math.PI : atan;
        }
    }

    /**
     * Limit a value to a range. E.g. for offset you want the value between 0 and 100. -x becomes 0 and 101 becomes 100.
     *
     * @param value The value to limit.
     * @param min   The minimally allowed value.
     * @param max   The maximally allowed value.
     * @return The limited number.
     */
    public static int limitToRange(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }

    public static double limitToRange(Double value, double min, double max, double defaultIfNull) {
        if (value == null) {
            return defaultIfNull;
        }
        return Math.min(max, Math.max(min, value));
    }
}
