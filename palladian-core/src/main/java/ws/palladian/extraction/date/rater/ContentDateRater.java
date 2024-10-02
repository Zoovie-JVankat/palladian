package ws.palladian.extraction.date.rater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.classification.dt.QuickDtClassifier;
import ws.palladian.classification.dt.QuickDtLearner;
import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.FeatureVector;
import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.helper.Cache;
import ws.palladian.helper.io.FileHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * <p>
 * This class evaluates content-dates. Doing this by dividing dates in three parts: Keyword in attribute, in text and no
 * keyword; each part will be rated different. Part one by keyword classes, see
 * {@link KeyWords#getKeywordPriority(String)} and age. Part two by distance of keyword an date, keyword classes and
 * age. Part three by age.
 * </p>
 *
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class ContentDateRater extends TechniqueDateRater<ContentDate> {
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentDateRater.class);

    private static final String CLASSIFIER_MODEL_PUB = "/dates_pub_model.gz";
    private static final String CLASSIFIER_MODEL_MOD = "/dates_mod_model.gz";

    private final QuickDtModel model;
    private final QuickDtClassifier predictor = new QuickDtClassifier();

    public ContentDateRater(PageDateType dateType) {
        super(dateType);
        if (dateType == PageDateType.PUBLISH) {
            model = loadModel(CLASSIFIER_MODEL_PUB);
        } else {
            model = loadModel(CLASSIFIER_MODEL_MOD);
        }
    }

    private QuickDtModel loadModel(String classifierModel) {
        QuickDtModel model = (QuickDtModel) Cache.getInstance().getDataObject(classifierModel);
        if (model == null) {
            InputStream inputStream = this.getClass().getResourceAsStream(CLASSIFIER_MODEL_PUB);
            if (inputStream == null) {
                throw new IllegalStateException("Could not load model file \"" + classifierModel + "\"");
            }

            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(new GZIPInputStream(inputStream));
                model = (QuickDtModel) objectInputStream.readObject();
                Cache.getInstance().putDataObject(classifierModel, model);
            } catch (IOException | ClassNotFoundException e) {
                throw new IllegalStateException("Error loading the model file \"" + classifierModel + "\": " + e.getMessage(), e);
            }
        }
        return model;
    }

    @Override
    public List<RatedDate<ContentDate>> rate(List<ContentDate> list) {
        List<RatedDate<ContentDate>> result = new ArrayList<>();

        for (ContentDate date : list) {
            if (dateType.equals(PageDateType.PUBLISH) && date.isInUrl()) {
                result.add(RatedDate.create(date, 0.99));
            } else if (dateType.equals(PageDateType.PUBLISH) && date.isInLdJson()) {
                result.add(RatedDate.create(date, 1.0));
            } else {
                FeatureVector featureVector = DateInstanceFactory.createFeatureVector(date);
                try {
                    CategoryEntries dbl = predictor.classify(featureVector, model);
                    result.add(RatedDate.create(date, dbl.getProbability(dbl.getMostLikelyCategory())));
                } catch (Exception e) {
                    LOGGER.error("Exception " + date.getDateString() + " " + featureVector, e);
                }
            }

        }
        return result;
    }

    /**
     * <p>
     * Build the model files for the classifier from the training CSV.
     * </p>
     *
     * @param inputCsv   The path to the CSV file.
     * @param outputPath The path and filename for the model file.
     * @throws IOException
     */
    private static void buildModel(String inputCsv, String outputPath) throws IOException {
        CsvDatasetReader instances = CsvDatasetReaderConfig.filePath(new File(inputCsv)).readHeader(true).create();
        QuickDtLearner learner = QuickDtLearner.randomForest(10);
        QuickDtModel model = learner.train(instances);
        FileHelper.serialize(model, outputPath);
    }

    public static void main(String[] args) throws IOException {
        buildModel("D:\\Dates_Pub_Mod\\dates_mod.csv", "src/main/resources/dates_mod_model.gz");
        buildModel("D:\\Dates_Pub_Mod\\dates_pub.csv", "src/main/resources/dates_pub_model.gz");
    }
}
