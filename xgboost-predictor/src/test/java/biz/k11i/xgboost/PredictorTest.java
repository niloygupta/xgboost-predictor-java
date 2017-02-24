package biz.k11i.xgboost;

import biz.k11i.xgboost.util.FVec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

public abstract class PredictorTest {

    private static final List<FVec> TEST_DATA_AGARICUS;

    static {
        try {
            // zero-based test data
            TEST_DATA_AGARICUS = loadTestData("data/agaricus.txt.test.0");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static List<FVec> loadTestData(String path) throws IOException {
        return loadTestData(path, false);
    }

    static List<FVec> loadTestData(String path, boolean oneBasedIndex) throws IOException {
        try (InputStream stream = PredictorTest.class.getResourceAsStream(path);
             InputStreamReader isr = new InputStreamReader(stream);
             BufferedReader reader = new BufferedReader(isr)) {

            List<FVec> result = new ArrayList<>();
            int origin = oneBasedIndex ? 1 : 0;

            String line;
            while ((line = reader.readLine()) != null) {
                Map<Integer, Float> feat = new HashMap<>();

                for (String val : line.split("\\s")) {
                    if (!val.contains(":")) {
                        continue;
                    }

                    String[] pair = val.split(":");
                    feat.put(Integer.parseInt(pair[0]) - origin, Float.parseFloat(pair[1]));
                }

                result.add(FVec.Transformer.fromMap(feat));
            }

            return result;
        }
    }

    static Predictor newPredictor(String resourceName) throws IOException {
        try (InputStream stream = PredictorTest.class.getResourceAsStream(resourceName)) {
            return new Predictor(stream);
        }
    }

    static List<double[]> loadExpectedData(String resourceName) throws IOException {
        try (InputStream stream = PredictorTest.class.getResourceAsStream(resourceName);
             InputStreamReader isr = new InputStreamReader(stream);
             BufferedReader reader = new BufferedReader(isr)) {

            List<double[]> result = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                double[] valuesF = new double[values.length];
                for (int i = 0; i < values.length; i++) {
                    valuesF[i] = Double.parseDouble(values[i]);
                }
                result.add(valuesF);
            }

            return result;
        }
    }

    String modelNameWithVersion(String version, String modelName) {
        return "v" + version + "/" + modelName;
    }

    interface PredictorFunction<T> {
        T predict(FVec feat);
    }

    interface PredictorFunction2<T> {
        T predict(Predictor predictor, FVec feat);
    }

    void verifyDouble(String modelType, String modelName, String ext, PredictorFunction<double[]> function) throws IOException {
        verifyDouble(TEST_DATA_AGARICUS, modelType, modelName, ext, function);
    }

    void verifyDouble(List<FVec> testData, String modelType, String modelName, String ext, PredictorFunction<double[]> function) throws IOException {
        List<double[]> expected = loadExpectedData(String.format("model/%s/%s.%s", modelType, modelName, ext));

        for (int i = 0; i < testData.size(); i++) {
            double[] predicted = function.predict(testData.get(i));

            assertThat(
                    String.format("[%s.%s: %d] length", modelName, ext, i),
                    predicted.length,
                    is(expected.get(i).length));

            for (int j = 0; j < predicted.length; j++) {
                assertThat(
                        String.format("[%s.%s: %d] value[%d]", modelName, ext, i, j),
                        predicted[j], closeTo(expected.get(i)[j], 1e-5));
            }
        }
    }

    void verifyInt(String modelType, String modelName, String ext, PredictorFunction<int[]> function) throws IOException {
        verifyInt(TEST_DATA_AGARICUS, modelType, modelName, ext, function);
    }

    void verifyInt(List<FVec> testData, String modelType, String modelName, String ext, PredictorFunction<int[]> function) throws IOException {
        List<double[]> expected = loadExpectedData(String.format("model/%s/%s.%s", modelType, modelName, ext));

        for (int i = 0; i < testData.size(); i++) {
            int[] predicted = function.predict(testData.get(i));

            assertThat(
                    String.format("[%s.%s: %d] length", modelName, ext, i),
                    predicted.length,
                    is(expected.get(i).length));

            for (int j = 0; j < predicted.length; j++) {
                assertThat(
                        String.format("[%s.%s: %d] value[%d]", modelName, ext, i, j),
                        predicted[j], is((int) expected.get(i)[j]));
            }
        }
    }

    void verifyDouble(
            String modelName,
            String testDataName,
            String expectedDataName,
            String predictionTaskName,
            PredictorFunction2<double[]> function) throws IOException {

        Predictor predictor = newPredictor(modelName);
        List<FVec> testData = loadTestData(testDataName, true);
        List<double[]> expectedData = loadExpectedData(expectedDataName);

        for (int i = 0; i < testData.size(); i++) {
            double[] predicted = function.predict(predictor, testData.get(i));

            assertThat(
                    String.format("[model: %s, test: %s, expected: %s, task: %s] #%d",
                            modelName, testDataName, expectedDataName, predictionTaskName, i),
                    predicted.length,
                    is(expectedData.get(i).length));

            for (int j = 0; j < predicted.length; j++) {
                assertThat(
                        String.format("[model: %s, test: %s, expected: %s, task: %s] #%d[%d]",
                                modelName, testDataName, expectedDataName, predictionTaskName, i, j),
                        predicted[j], closeTo(expectedData.get(i)[j], 1e-5));
            }
        }
    }
}
