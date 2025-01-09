package ws.palladian.retrieval.cooccurrence;

import org.apache.commons.lang3.Validate;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CountMatrix;
import ws.palladian.helper.collection.PairMatrix;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * <p>
 * A co-occurrence matrix.
 * </p>
 *
 * @param <T> The type of objects this {@link CooccurrenceMatrix} keeps.
 * @author Philipp Katz
 */
public final class CooccurrenceMatrix implements Serializable {

    private static final long serialVersionUID = 2L;

    private static final String SEPARATOR = "###";

    private static final String FREQ_HEADER = SEPARATOR + "frequencies" + SEPARATOR;

    private static final String COOC_HEADER = SEPARATOR + "cooccurrences" + SEPARATOR;

    private final CountMatrix<String> pairs;

    private final Bag<String> items;

    public CooccurrenceMatrix() {
        pairs = new CountMatrix<>(new PairMatrix<>());
        items = new Bag<>();
    }

    public CooccurrenceMatrix add(String itemA, String itemB) {
        add(itemA, itemB, 1);
        return this;
    }

    public CooccurrenceMatrix add(String itemA, String itemB, int count) {
        pairs.add(itemB, itemA, count);
        //        items.add(itemA, count);
        //        items.add(itemB, count);
        return this;
    }

    public CooccurrenceMatrix add(String item) {
        add(item, 1);
        return this;
    }

    public CooccurrenceMatrix set(String itemA, String itemB, int count) {
        pairs.set(itemA, itemB, count);
        return this;
    }

    public CooccurrenceMatrix add(String item, int count) {
        items.add(item, count);
        return this;
    }

    public CooccurrenceMatrix set(String item, int count) {
        items.set(item, count);
        return this;
    }

    public int getCount(String item) {
        return items.count(item);
    }

    public int getCount(String itemA, String itemB) {
        return pairs.get(itemB, itemA);
    }

    public int getNumItems() {
        return items.size();
    }

    public int getNumUniqueItems() {
        return items.unique().size();
    }

    public int getNumPairs() {
        return pairs.getSum();
    }

    public double getProbability(String item) {
        return getProbability(item, false);
    }

    public double getProbability(String item, boolean smoothing) {
        int s1 = smoothing ? 1 : 0;
        int s2 = smoothing ? getNumUniqueItems() : 0;
        return (double) (getCount(item) + s1) / (getNumItems() + s2);
    }

    /**
     * <p>
     * Get the conditional probability P(itemA|itemB), i.e. the probability for itemA, given itemB. Calculated as
     * <code>P(itemA|itemB) = count(itemB,itemA) / count(itemB)</code>.
     * </p>
     *
     * @param itemA First item, not <code>null</code>.
     * @param itemB Second item, not <code>null</code>.
     * @return
     */
    public double getConditionalProbability(String itemA, String itemB) {
        return getConditionalProbability(itemA, itemB, false);
    }

    public double getConditionalProbability(String itemA, String itemB, boolean smoothing) {
        Validate.notNull(itemA, "itemA must not be null");
        Validate.notNull(itemB, "itemB must not be null");
        // XXX according to the lecture slides, add-one smoothing is not well suited for n-gram modelling,
        // consider implementing better smoothing algorithm, see lecture PDFs, page 71 ff.
        int s1 = smoothing ? 1 : 0;
        int s2 = smoothing ? items.unique().size() : 0;
        return (double) (getCount(itemB, itemA) + s1) / (getCount(itemB) + s2);
    }

    /**
     * <p>
     * Clear the content of this {@link CooccurrenceMatrix}.
     * </p>
     */
    public void clear() {
        pairs.clear();
        items.clear();
    }

    public void save(OutputStream stream) {
        PrintWriter writer = null;
        try {
            long totalCount = (long) items.uniqueItems().size();
            totalCount += (long) pairs.getRowKeys().size() * pairs.getColumnKeys().size();
            ProgressMonitor monitor = new ProgressMonitor();
            monitor.startTask(null, totalCount);
            writer = new PrintWriter(stream);
            writer.println(FREQ_HEADER);
            for (String term : items.uniqueItems()) {
                writer.println(term + SEPARATOR + items.count(term));
                monitor.increment();
            }
            writer.println(COOC_HEADER);
            for (String term1 : pairs.getRowKeys()) {
                for (String term2 : pairs.getColumnKeys()) {
                    int count = pairs.getCount(term1, term2);
                    if (count != 0) {
                        writer.println(term1 + SEPARATOR + term2 + SEPARATOR + count);
                    }
                    monitor.increment();
                }
            }
        } finally {
            FileHelper.close(writer);
        }
    }

    public static CooccurrenceMatrix load(File file) throws IOException {
        Validate.notNull(file, "file must not be null");
        final CooccurrenceMatrix matrix = new CooccurrenceMatrix();
        final boolean[] readFrequencies = {true};
        FileHelper.performActionOnEveryLine(new GZIPInputStream(new FileInputStream(file)), new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                if (lineNumber != 0 && lineNumber % 100000 == 0) {
                    System.out.print('.');
                }
                if (readFrequencies[0]) {
                    if (line.equals(FREQ_HEADER)) {
                        return;
                    }
                    if (line.equals(COOC_HEADER)) {
                        readFrequencies[0] = false;
                        return;
                    }
                    String[] split = line.split(SEPARATOR);
                    if (split.length != 2) {
                        // System.err.println("Error in " + line);
                        return;
                    }
                    matrix.set(split[0], Integer.valueOf(split[1]));
                } else {
                    String[] split = line.split(SEPARATOR);
                    if (split.length != 3) {
                        // System.err.println("Error in " + line);
                        return;
                    }
                    matrix.set(split[0], split[1], Integer.valueOf(split[2]));
                }
            }
        });
        System.out.println();
        return matrix;
    }

    public Bag<String> getItems() {
        return items;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CooccurrenceMatrix [numItems=");
        builder.append(getNumItems());
        builder.append(", numUniqueItems=");
        builder.append(getNumUniqueItems());
        builder.append(", numPairs=");
        builder.append(getNumPairs());
        builder.append("]");
        return builder.toString();
    }

    public String toVerboseString() {
        StringBuilder builder = new StringBuilder();
        builder.append("numItems: ").append(getNumItems()).append('\n');
        builder.append("numUniqueItems: ").append(getNumUniqueItems()).append('\n');
        builder.append("numPairs: ").append(getNumPairs()).append('\n').append('\n');
        for (String item : items.uniqueItems()) {
            builder.append(item).append(" : ").append(items.count(item)).append('\n');
        }
        builder.append('\n').append('\n');
        builder.append(pairs);
        return builder.toString();
    }

    public static void main(String[] args) {
        CooccurrenceMatrix cooccurrenceMatrix = new CooccurrenceMatrix();

        // Sample input: a list of sentences
        List<String> sentences = Arrays.asList("the cat sat on the mat", "the dog sat on the mat");

        // Step 3: Process each sentence to build the co-occurrence counts
        for (String sentence : sentences) {
            String[] words = sentence.split(" ");

            // Count co-occurrences
            for (int i = 0; i < words.length; i++) {
                String word1 = words[i];
                // For each word, count co-occurrences with every other word that comes after it
                for (int j = i + 1; j < words.length; j++) {
                    String word2 = words[j];
                    cooccurrenceMatrix.add(word1, word2);
                    cooccurrenceMatrix.add(word2, word1);
                    cooccurrenceMatrix.add(word1, 1);
                    cooccurrenceMatrix.add(word2, 1);
                }
            }
        }

        System.out.println(cooccurrenceMatrix.getConditionalProbability("cat", "mat"));
        System.out.println(cooccurrenceMatrix.getConditionalProbability("sat", "mat"));
    }

}
