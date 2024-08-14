package ws.palladian.helper.normalization;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.StringLengthComparator;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.constants.UnitType;
import ws.palladian.helper.conversion.UnitConverter;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.PatternHelper;
import ws.palladian.helper.nlp.StringHelper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * The UnitNormalizer normalizes units.
 * </p>
 *
 * @author David Urbansky
 */
public class UnitNormalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnitNormalizer.class);

    private final static List<String> ALL_UNITS = new ArrayList<>();

    private final static Map<String, Pattern> PATTERNS = new HashMap<>();

    static {
        for (UnitType unitType : UnitType.values()) {
            ALL_UNITS.addAll(unitType.getUnitNames());
        }
        ALL_UNITS.sort(StringLengthComparator.INSTANCE);

        for (String unit : ALL_UNITS) {
            PATTERNS.put(unit, Pattern.compile("(?<=\\d|[(]|\\s|^)" + Pattern.quote(unit) + "(?=$|[-.,;:*)]|\\s)", Pattern.CASE_INSENSITIVE));
        }
    }

    private static boolean isForceUnit(String unit) {
        return UnitType.FORCE.contains(unit);
    }

    private static boolean isBandwidthUnit(String unit) {
        return UnitType.BANDWIDTH.contains(unit);
    }

    private static boolean isTimeUnit(String unit) {
        return UnitType.TIME.contains(unit);
    }

    private static boolean isDigitalUnit(String unit) {
        return UnitType.DIGITAL.contains(unit);
    }

    private static boolean isFrequencyUnit(String unit) {
        return UnitType.FREQUENCY.contains(unit);
    }

    private static boolean isRotationSpeedUnit(String unit) {
        return UnitType.ROTATION_SPEED.contains(unit);
    }

    private static boolean isLengthUnit(String unit) {
        return UnitType.LENGTH.contains(unit);
    }

    private static boolean isWeightUnit(String unit) {
        return UnitType.WEIGHT.contains(unit);
    }

    private static boolean isAreaUnit(String unit) {
        return UnitType.AREA.contains(unit);
    }

    private static boolean isAreaDensityUnit(String unit) {
        return UnitType.AREA_DENSITY.contains(unit);
    }

    private static boolean isDensityUnit(String unit) {
        return UnitType.DENSITY.contains(unit);
    }

    private static boolean isVolumeUnit(String unit) {
        return UnitType.VOLUME.contains(unit);
    }

    private static boolean isSoundVolumeUnit(String unit) {
        return UnitType.POWER_RATIO.contains(unit);
    }

    private static boolean isTemperatureUnit(String unit) {
        return UnitType.TEMPERATURE.contains(unit);
    }

    private static boolean isVoltageUnit(String unit) {
        return UnitType.VOLTAGE.contains(unit);
    }

    private static boolean isEnergyUnit(String unit) {
        return UnitType.ENERGY.contains(unit);
    }

    private static boolean isPowerUnit(String unit) {
        return UnitType.POWER.contains(unit);
    }

    private static boolean isTorqueUnit(String unit) {
        return UnitType.TORQUE.contains(unit);
    }

    private static boolean isSpeedUnit(String unit) {
        return UnitType.SPEED.contains(unit);
    }

    private static boolean isPressureUnit(String unit) {
        return UnitType.PRESSURE.contains(unit);
    }

    private static boolean isCurrentUnit(String unit) {
        return UnitType.CURRENT.contains(unit);
    }

    private static boolean isLuminanceUnit(String unit) {
        return UnitType.LUMINANCE.contains(unit);
    }

    private static boolean isLuminousFluxUnit(String unit) {
        return UnitType.LUMINOUS_FLUX.contains(unit);
    }

    private static boolean isElectricChargeUnit(String unit) {
        return UnitType.ELECTRIC_CHARGE.contains(unit);
    }

    private static boolean isCurrencyUnit(String unit) {
        return UnitType.CURRENCY.contains(unit);
    }

    private static boolean isFlowRateUnit(String unit) {
        return UnitType.FLOW_RATE.contains(unit);
    }

    public static String detectUnit(String text) {
        for (String unit : ALL_UNITS) {
            Matcher matcher = PATTERNS.get(unit).matcher(text);
            if (matcher.find()) {
                // special handling for "in" because it is "inch" but often another unit comes after, e.g. "height in cm" => cm not in
                if (matcher.group().equals("in")) {
                    String anotherUnitFound = detectUnit(text.substring(matcher.end()));
                    if (anotherUnitFound != null) {
                        return anotherUnitFound;
                    }
                }
                return unit;
            }
        }

        return null;
    }

    public static String detectUnit(String text, UnitType unitType) {
        for (String unit : unitType.getUnitNames()) {
            if (PATTERNS.get(unit).matcher(text).find()) {
                return unit;
            }
        }
        return null;
    }

    /**
     * <p>
     * Return a collection of units that are of the same type, e.g. if "cm" is given, all other length units are returned.
     * </p>
     *
     * @param unit The input unit.
     * @return A collection of units of the same type.
     */
    public static Collection<String> getAllUnitsOfSameType(String unit) {
        if (isDigitalUnit(unit)) {
            return UnitType.DIGITAL.getUnitNames();
        }
        if (isBandwidthUnit(unit)) {
            return UnitType.BANDWIDTH.getUnitNames();
        }
        if (isTimeUnit(unit)) {
            return UnitType.TIME.getUnitNames();
        }
        if (isFrequencyUnit(unit)) {
            return UnitType.FREQUENCY.getUnitNames();
        }
        if (isRotationSpeedUnit(unit)) {
            return UnitType.ROTATION_SPEED.getUnitNames();
        }
        if (isLengthUnit(unit)) {
            return UnitType.LENGTH.getUnitNames();
        }
        if (isWeightUnit(unit)) {
            return UnitType.WEIGHT.getUnitNames();
        }
        if (isAreaUnit(unit)) {
            return UnitType.AREA.getUnitNames();
        }
        if (isAreaDensityUnit(unit)) {
            return UnitType.AREA_DENSITY.getUnitNames();
        }
        if (isDensityUnit(unit)) {
            return UnitType.DENSITY.getUnitNames();
        }
        if (isVolumeUnit(unit)) {
            return UnitType.VOLUME.getUnitNames();
        }
        if (isSoundVolumeUnit(unit)) {
            return UnitType.POWER_RATIO.getUnitNames();
        }
        if (isTemperatureUnit(unit)) {
            return UnitType.TEMPERATURE.getUnitNames();
        }
        if (isPressureUnit(unit)) {
            return UnitType.PRESSURE.getUnitNames();
        }
        if (isLuminanceUnit(unit)) {
            return UnitType.LUMINANCE.getUnitNames();
        }
        if (isLuminousFluxUnit(unit)) {
            return UnitType.LUMINOUS_FLUX.getUnitNames();
        }
        if (isVoltageUnit(unit)) {
            return UnitType.VOLTAGE.getUnitNames();
        }
        if (isPowerUnit(unit)) {
            return UnitType.POWER.getUnitNames();
        }
        if (isEnergyUnit(unit)) {
            return UnitType.ENERGY.getUnitNames();
        }
        if (isTorqueUnit(unit)) {
            return UnitType.TORQUE.getUnitNames();
        }
        if (isFlowRateUnit(unit)) {
            return UnitType.FLOW_RATE.getUnitNames();
        }
        if (isForceUnit(unit)) {
            return UnitType.FORCE.getUnitNames();
        }
        if (isSpeedUnit(unit)) {
            return UnitType.SPEED.getUnitNames();
        }

        return new HashSet<>();
    }

    /**
     * <p>
     * Returns true if unitB is bigger than units. e.g. hours > minutes and GB > MB
     * </p>
     *
     * @param unitB The bigger unit.
     * @param unitS The smaller unit.
     * @return True if unitB is bigger than unitS.
     */
    public static boolean isBigger(String unitB, String unitS) {
        return unitLookup(unitB) > unitLookup(unitS);
    }

    /**
     * <p>
     * Returns true if units are the same unit type (time,distance etc.). e.g. MB and GB are digital size, hours and minutes are time units.
     * </p>
     *
     * @param unit1 The first unit.
     * @param unit2 The second unit.
     * @return True if both units are the same type.
     */
    public static boolean unitsSameType(String unit1, String unit2) {
        unit1 = unit1.toLowerCase().trim();
        unit2 = unit2.toLowerCase().trim();

        // check bandwidth units
        if (isBandwidthUnit(unit1) && isBandwidthUnit(unit2)) {
            return true;
        }

        // check time units
        if (isTimeUnit(unit1) && isTimeUnit(unit2)) {
            return true;
        }

        // digital units
        if (isDigitalUnit(unit1) && isDigitalUnit(unit2)) {
            return true;
        }

        // frequency units
        if (isFrequencyUnit(unit1) && isFrequencyUnit(unit2)) {
            return true;
        }

        // rotation speed units
        if (isRotationSpeedUnit(unit1) && isRotationSpeedUnit(unit2)) {
            return true;
        }

        // distances
        if (isLengthUnit(unit1) && isLengthUnit(unit2)) {
            return true;
        }

        // weight
        if (isWeightUnit(unit1) && isWeightUnit(unit2)) {
            return true;
        }

        // area
        if (isAreaUnit(unit1) && isAreaUnit(unit2)) {
            return true;
        }

        // area density
        if (isAreaDensityUnit(unit1) && isAreaDensityUnit(unit2)) {
            return true;
        }

        // pressure
        if (isPressureUnit(unit1) && isPressureUnit(unit2)) {
            return true;
        }

        // luminance
        if (isLuminanceUnit(unit1) && isLuminanceUnit(unit2)) {
            return true;
        }

        // luminous flux
        if (isLuminousFluxUnit(unit1) && isLuminousFluxUnit(unit2)) {
            return true;
        }

        // volume
        if (isVolumeUnit(unit1) && isVolumeUnit(unit2)) {
            return true;
        }

        // sound volume
        if (isSoundVolumeUnit(unit1) && isSoundVolumeUnit(unit2)) {
            return true;
        }

        // flow rate
        if (isFlowRateUnit(unit1) && isFlowRateUnit(unit2)) {
            return true;
        }

        // temperature
        if (isTemperatureUnit(unit1) && isTemperatureUnit(unit2)) {
            return true;
        }

        // torque
        if (isTorqueUnit(unit1) && isTorqueUnit(unit2)) {
            return true;
        }

        // force
        if (isForceUnit(unit1) && isForceUnit(unit2)) {
            return true;
        }

        // speed
        if (isSpeedUnit(unit1) && isSpeedUnit(unit2)) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * </p>
     *
     * @param unit The source unit.
     * @return The unit to which all values are normalized to, e.g. "second" for time units.
     */
    public static String findBaseUnit(String unit) {
        unit = unit.toLowerCase();
        Collection<String> allUnitsOfSameType = getAllUnitsOfSameType(unit);
        for (String unitType : allUnitsOfSameType) {
            double multiplier = unitLookup(unitType);
            if (multiplier == 1.) {
                return unitType;
            }
        }

        return null;
    }

    /**
     * <p>
     * Find the multiplier to normalize values with the given unit. For example, "kg" gets a multiplier of 1,000 as we normalize to grams.
     * </p>
     *
     * @param unit The unit string, e.g. "kg".
     * @return The multiplier.
     */
    public static double unitLookup(String unit) {
        unit = unit.trim();
        if (unit.endsWith(".")) {
            unit = unit.substring(0, unit.length() - 1);
        }

        // -1 means no multiplier found (hint for other function that a shorter sequence for the unit string might bring
        // a result)
        double multiplier = -1.0;

        ol:
        for (UnitType unitType : UnitType.values()) {
            for (Pair<List<String>, Double> pair : unitType.getUnits()) {
                for (String unitTypeUnit : pair.getLeft()) {
                    if (unit.equals(unitTypeUnit)) {
                        if (pair.getRight() != null) {
                            multiplier = pair.getRight();
                        }
                        break ol;
                    }
                }
            }
        }

        // nothing found? try case insensitive
        if (multiplier < 0) {
            unit = unit.toLowerCase();
            ol:
            for (UnitType unitType : UnitType.values()) {
                for (Pair<List<String>, Double> pair : unitType.getUnits()) {
                    for (String unitTypeUnit : pair.getLeft()) {
                        if (unit.equals(unitTypeUnit)) {
                            if (pair.getRight() == null) {
                                multiplier = -1.0;
                            } else {
                                multiplier = pair.getRight();
                            }
                            break ol;
                        }
                    }
                }
            }
        }

        return multiplier;
    }

    /**
     * Find special formats for combined values (well formed as "1 min 4 sec" are handled by getNormalizedNumber).
     *
     * <pre>
     * 1m20s => 80s
     * 1h2m20s => 3740s (1m:20s => 80s)
     * 00:01:20 => 80s
     * 1:20 => 80s
     * 5'9" => 175.26cm
     * 5'9'' => 175.26cm
     * </pre>
     *
     * @param number   The number.
     * @param unitText The text after the unit.
     * @return The combined value or -1 if number is not part of special format.
     */
    public static double handleSpecialFormat(double number, String unitText, int decimals) {
        double combinedValue;

        try {
            Pattern pattern;
            Matcher matcher;

            // 1m20s type
            pattern = PatternHelper.compileOrGet("\\Am(\\s)?(\\d)+s");
            matcher = pattern.matcher(unitText);
            if (matcher.find()) {
                combinedValue = number * 60; // minutes to seconds
                combinedValue += Double.parseDouble(matcher.group().substring(1, matcher.end() - 1));
                if (decimals > -1) {
                    return MathHelper.round(combinedValue, decimals);
                }
                return combinedValue;
            }

            // 1h2m20s, 1h2m type
            pattern = PatternHelper.compileOrGet("\\Ah(\\s)?(\\d)+m(\\s)?((\\d)+s)?");
            matcher = pattern.matcher(unitText);
            if (matcher.find()) {
                combinedValue = number * 3600; // hours to seconds
                int minutesIndex = unitText.indexOf("m");
                combinedValue += Double.parseDouble(matcher.group().substring(1, minutesIndex)) * 60; // minutes to
                // seconds
                int secondsIndex = unitText.indexOf("s");
                if (secondsIndex > -1) {
                    combinedValue += Double.parseDouble(matcher.group().substring(minutesIndex + 1, secondsIndex));
                }
                if (decimals > -1) {
                    return MathHelper.round(combinedValue, decimals);
                }
                return combinedValue;
            }

            // 01:01:20 type
            pattern = PatternHelper.compileOrGet("\\A:(\\d)+:(\\d)+");
            matcher = pattern.matcher(unitText);
            if (matcher.find()) {
                combinedValue = number * 3600; // hours to seconds
                int lastColonIndex = matcher.group().lastIndexOf(":");
                combinedValue += Double.parseDouble(matcher.group().substring(1, lastColonIndex)) * 60; // minutes to
                // seconds
                combinedValue += Double.parseDouble(matcher.group().substring(lastColonIndex + 1, matcher.end()));
                if (decimals > -1) {
                    return MathHelper.round(combinedValue, decimals);
                }
                return combinedValue;
            }

            // 01:20 type
            pattern = PatternHelper.compileOrGet("\\A:(\\d)+");
            matcher = pattern.matcher(unitText);
            if (matcher.find()) {
                combinedValue = number * 60; // minutes to seconds
                combinedValue += Double.parseDouble(matcher.group().substring(1, matcher.end()));
                if (decimals > -1) {
                    return MathHelper.round(combinedValue, decimals);
                }
                return combinedValue;
            }

            // 5'9" / 5' 9" type
            pattern = Pattern.compile("\\A'(\\s)?(\\d)+\"");
            matcher = pattern.matcher(unitText);
            if (matcher.find()) {
                combinedValue = number * unitLookup("ft"); // feet to centimeters
                combinedValue += Double.parseDouble(matcher.group().substring(1, matcher.end() - 1).trim()) * unitLookup("in"); // inches to centimeters
                if (decimals > -1) {
                    return MathHelper.round(combinedValue, decimals);
                }
                return combinedValue;
            }

            // 5'9'' / 5'9'' type
            pattern = PatternHelper.compileOrGet("\\A'(\\s)?(\\d)+''");
            matcher = pattern.matcher(unitText);
            if (matcher.find()) {
                combinedValue = number * unitLookup("ft"); // feet to centimeters
                combinedValue += Double.parseDouble(matcher.group().substring(1, matcher.end() - 2).trim()) * unitLookup("in"); // inches to centimeters
                if (decimals > -1) {
                    return MathHelper.round(combinedValue, decimals);
                }
                return combinedValue;
            }

            // per thousand, per 1000 type
            pattern = PatternHelper.compileOrGet("(\\Aper thousand)|(\\Aper 1000)");
            matcher = pattern.matcher(unitText);
            if (matcher.find()) {
                combinedValue = number / 10; // to percent
                if (decimals > -1) {
                    return MathHelper.round(combinedValue, decimals);
                }
                return combinedValue;
            }
        } catch (StringIndexOutOfBoundsException e) {
            LOGGER.error(unitText, e);
        }

        return -1.0;
    }

    /**
     * <p>
     * Transforms a normalized value to the target unit.
     * </p>
     *
     * @param unitTo The unit to transform.
     * @param value  The value to transform.
     * @return The transformed value.
     */
    public static double transorm(String unitTo, double value) {
        double divider = unitLookup(unitTo);
        if (divider != -1) {
            return value / divider;
        } else {
            return value;
        }
    }

    public static double transorm(String unitTo, String value) {
        return transorm(unitTo, Double.parseDouble(value));
    }

    public static UnitType getUnitType(String string) {
        String words[] = string.split(" ");
        UnitType unitType = UnitType.NONE;

        for (String word2 : words) {
            String word = word2.toLowerCase();
            if (isTimeUnit(word)) {
                unitType = UnitType.TIME;
            }
            if (isDigitalUnit(word)) {
                unitType = UnitType.DIGITAL;
            }
            if (isFrequencyUnit(word)) {
                unitType = UnitType.FREQUENCY;
            }
            if (isRotationSpeedUnit(word)) {
                unitType = UnitType.ROTATION_SPEED;
            }
            if (isLengthUnit(word)) {
                unitType = UnitType.LENGTH;
            }
            if (isWeightUnit(word)) {
                unitType = UnitType.WEIGHT;
            }
            if (isVolumeUnit(word)) {
                unitType = UnitType.VOLUME;
            }
            if (isAreaDensityUnit(word)) {
                unitType = UnitType.AREA_DENSITY;
            }
            if (isDensityUnit(word)) {
                unitType = UnitType.DENSITY;
            }
            if (isTemperatureUnit(word)) {
                unitType = UnitType.TEMPERATURE;
            }
            if (isPressureUnit(word)) {
                unitType = UnitType.PRESSURE;
            }
            if (isLuminanceUnit(word)) {
                unitType = UnitType.LUMINANCE;
            }
            if (isLuminousFluxUnit(word)) {
                unitType = UnitType.LUMINOUS_FLUX;
            }
            if (isCurrentUnit(word)) {
                unitType = UnitType.CURRENT;
            }
            if (isElectricChargeUnit(word)) {
                unitType = UnitType.ELECTRIC_CHARGE;
            }
            if (isBandwidthUnit(word)) {
                unitType = UnitType.BANDWIDTH;
            }
            if (isPowerUnit(word)) {
                unitType = UnitType.POWER;
            }
            if (isVoltageUnit(word)) {
                unitType = UnitType.VOLTAGE;
            }
            if (isCurrencyUnit(word)) {
                unitType = UnitType.CURRENCY;
            }
            if (isFlowRateUnit(word)) {
                unitType = UnitType.FLOW_RATE;
            }
            if (isForceUnit(word)) {
                unitType = UnitType.FORCE;
            }
            if (isTorqueUnit(word)) {
                unitType = UnitType.TORQUE;
            }
            if (isSpeedUnit(word)) {
                unitType = UnitType.SPEED;
            }
            if (unitType != UnitType.NONE) {
                break; // we found a unit
            }
        }
        return unitType;
    }

    public static double getNormalizedNumber(String unitText) throws NumberFormatException, NullPointerException {
        // add space in case it's missing "2.4Ghz" => "2.4 Ghz"
        unitText = PatternHelper.compileOrGet("(\\d(?:e-?\\d+)?)(?!e-?\\d)([A-Za-z°'\"])").matcher(unitText).replaceFirst("$1 $2").trim();
        String[] words = unitText.split(" ");

        if (words.length == 0) {
            words = unitText.trim().split("(?<=[0-9])(?=\\w)");
        }

        //        double number = Double.parseDouble(words[0]);
        Double number = MathHelper.parseStringNumber(unitText, null);

        if (number == null) {
            throw new NullPointerException("No number found in " + unitText);
        }

        String newUnitText = "";
        for (int i = 1; i < words.length; i++) {
            if (newUnitText.isEmpty() && StringHelper.containsNumber(words[i].substring(0, 1))) {
                continue;
            }
            newUnitText += words[i] + " ";
        }
        return getNormalizedNumber(number, newUnitText.trim(), -1, "");
    }

    public static double getNormalizedNumber(double number, String unitText) {
        return getNormalizedNumber(number, unitText, -1, "");
    }

    private static double getNormalizedNumber(double number, String unitText, String combinedSearchPreviousUnit) {
        return getNormalizedNumber(number, unitText, -1, combinedSearchPreviousUnit);
    }

    public static double getNormalizedNumber(double number, String unitText, int decimals, String combinedSearchPreviousUnit) {
        boolean combinedSearch = combinedSearchPreviousUnit.length() > 0;

        // test first whether number is part of a special format
        double specialFormatOutcome = handleSpecialFormat(number, StringHelper.trim(unitText, ":'\""), decimals);
        if (specialFormatOutcome != -1.0) {
            if (decimals > -1) {
                return MathHelper.round(specialFormatOutcome, decimals);
            }
            return specialFormatOutcome;
        }

        // trim again, delete also ":" this time
        if (!unitText.equals("\"") && !unitText.equals("”") && !unitText.equals("''")) {
            unitText = StringHelper.trim(unitText);
        }

        // some units are presented in optional plural form e.g. 5 hour(s) but some values are in brackets e.g.
        // (2.26GHz), decide here whether to delete closing
        // bracket or not
        if (!unitText.endsWith("(s)") && unitText.endsWith(")")) {
            unitText = unitText.substring(0, unitText.length() - 1);
        }

        String[] words = unitText.split(" ");

        int l = words.length;
        double multiplier = 1.0;
        String restWordSequence = ""; // keep the rest word sequence to check for combined values
        String wordSequence = "";
        while (l > 0) {
            wordSequence = "";
            restWordSequence = "";
            for (int i = 0; i < l; i++) {
                if (words[i].equals("\"") || words[i].equals("''")) {
                    wordSequence += " " + words[i];
                } else {
                    wordSequence += " " + StringHelper.trim(words[i]);
                }
            }
            for (int i = l; i < words.length; ++i) {
                // System.out.println("add ");
                if (words[i].equals("\"") || words[i].equals("''")) {
                    restWordSequence += " " + words[i];
                } else {
                    restWordSequence += " " + StringHelper.trim(words[i]);
                }
            }
            // System.out.println("current word sequence "+wordSequence);
            // Logger.getInstance().log("current word sequence "+wordSequence, false);
            multiplier = unitLookup(wordSequence);
            if (multiplier != -1.0) {
                // when a subsequent unit is searched is has to be smaller than the previous one
                // e.g. 1 hour 23 minutes (minutes < hour) otherwise 2GB 80GB causes problems
                if (combinedSearch && !(unitsSameType(combinedSearchPreviousUnit, wordSequence) && isBigger(combinedSearchPreviousUnit, wordSequence))) {
                    return 0.0;
                }
                break;
            } else if (getUnitType(wordSequence) == UnitType.TEMPERATURE) {
                number = UnitConverter.convertTemperature(number, wordSequence.toLowerCase().trim(), UnitType.TEMPERATURE.getBaseUnit());
            }
            l--;
        }

        if (multiplier < 0 && !combinedSearch) {
            // no unit found, do not change value of number
            multiplier = 1.0;
        } else if (multiplier < 0) {
            multiplier = 0.0;
        }

        number *= multiplier;

        // keep searching in unit text for combined values as:
        // 1 hour 52 minutes
        // 1 min 20 sec
        // 5 ft 9 in
        // because of trimming RAM: 2GB - 80GB HDD becomes 2GB 80GB
        // second unit must be same type (time, distance etc.) and smaller
        restWordSequence = restWordSequence.trim();
        Pattern pat = PatternHelper.compileOrGet("\\A" + RegExp.NUMBER);
        Matcher m = pat.matcher(restWordSequence);

        m.region(0, restWordSequence.length());

        try {
            if (m.find()) {
                number += getNormalizedNumber(Double.parseDouble(StringNormalizer.normalizeNumber(m.group())), restWordSequence.substring(m.end()), wordSequence);
            }
        } catch (NumberFormatException e) {
            LOGGER.error(m.group(), e);
        }

        if (decimals > -1) {
            return MathHelper.round(number, decimals);
        }
        return number;
    }

    /**
     * <p>
     * Transforms a given <b>normalized</b> value and transforms it to the most readable unit for its unit type. E.g. "0.5" with LENGTH will become "5mm".
     * </p>
     *
     * @param normalizedValue The value, normalized to its base value in its unit type.
     * @param unitType        The unit type of the normalized value.
     * @return A pair with the transformed value and the used unit.
     */
    public static Pair<Double, List<String>> smartTransform(Double normalizedValue, UnitType unitType) {
        double smallestReadableValue = normalizedValue;
        Pair<List<String>, Double> bestMatchingTransformation = null;
        for (Pair<List<String>, Double> entry : unitType.getUnits()) {

            double transformed = normalizedValue / entry.getRight();
            if ((transformed < smallestReadableValue && transformed > 1) || (transformed > smallestReadableValue && smallestReadableValue < 1)
                    || bestMatchingTransformation == null) {
                bestMatchingTransformation = entry;
                smallestReadableValue = transformed;
            }

        }

        return Pair.of(smallestReadableValue, bestMatchingTransformation.getLeft());
    }

    public static void main(String[] args) {
        StopWatch stopWatch = new StopWatch();

        // 4.736s / 4.665s / 5.373s / 4.956s
        // 3.638s / 3.996s / 3.197s / 2.967s
        for (int i = 0; i < 10000; i++) {
            String s = UnitNormalizer.detectUnit("3 meters bla blub did doooob");
        }

        System.out.println(stopWatch.getElapsedTimeString());
        System.exit(0);

        System.out.println(getNormalizedNumber(6, "ft 1.5 in 187 cm"));

        System.out.println(getUnitType("2.26 GHz"));
        // System.out.println(getNormalizedNumber("a 2.26 GHz"));
        System.out.println(getUnitType("21.4 million"));
        System.out.println(getNormalizedNumber("21.4 million"));
        System.out.println(getUnitType("2 hr. 32 min"));
        System.out.println(getNormalizedNumber("2 hr. 32 min"));
        System.out.println(getUnitType("3 ft 9 inches"));
        System.out.println(getNormalizedNumber("3 ft 9 inches"));

        System.out.println(getNormalizedNumber(2.26, "GHz)"));
        System.out.println(getNormalizedNumber(21.4, " million.[1]"));
        System.out.println(getNormalizedNumber(13, " per thousand asdf asdfisdf "));
        System.out.println(getNormalizedNumber(13, " per thousand. asdf asdfisdf "));
        System.out.println(getNormalizedNumber(13, " per 1000 asdf asdfisdf "));
        System.out.println(getNormalizedNumber(1.6, " GHz, 1024MB RAM"));
        System.out.println(getNormalizedNumber(1.6, "GHz, 1066MHz Front side bus"));

        // test combined search
        System.out.println(getNormalizedNumber(80, "'GB'))"));
        System.out.println(getNormalizedNumber(2, " hr. 32 min."));
        System.out.println(getNormalizedNumber(13.3, "\" adf fs"));
        System.out.println(getNormalizedNumber(6, "' 2''"));
        System.out.println(getNormalizedNumber(6, "'2\""));
        System.out.println(getNormalizedNumber(5, "hours 4 minutes 6seconds"));
        System.out.println(getNormalizedNumber(6, " h 30 min"));
        System.out.println(getNormalizedNumber(5, "ft 9 inches"));
        System.out.println(getNormalizedNumber(5, "\""));
        System.out.println(getNormalizedNumber(2, "mb 4 GB"));
        System.out.println(getNormalizedNumber(2, "mb 2mb"));
        System.out.println(getNormalizedNumber(2, "mb 100kb"));

        // types and sizes
        System.out.println(unitsSameType("gb", "mb"));
        System.out.println(unitsSameType("minute", "mb"));
        System.out.println(isBigger("minute", "second"));

        // test special format
        System.out.println(String.valueOf(getNormalizedNumber(Double.parseDouble("6"), "' 2'',")));
        System.out.println(handleSpecialFormat(6.0, "' 2'',", 3));
        System.out.println(handleSpecialFormat(5, "' 9''", 3));
        System.out.println(handleSpecialFormat(5, "'9''", 3));
        System.out.println(handleSpecialFormat(5, "' 9\"", 3));
        System.out.println(handleSpecialFormat(5, "'9\"", 3));
        System.out.println(handleSpecialFormat(0, ":59", 3));
        System.out.println(handleSpecialFormat(2, ":44", 3));
        System.out.println(handleSpecialFormat(4, ":2:40", 3));
        System.out.println(handleSpecialFormat(4, ":02:40", 3));
        System.out.println(handleSpecialFormat(4, ":20:40", 3));
        System.out.println(handleSpecialFormat(1, "h 20m 40s", 3));
        System.out.println(handleSpecialFormat(1, "h 20m", 3));
        System.out.println(handleSpecialFormat(2, "m 40s", 3));
        System.out.println(handleSpecialFormat(1, "h20m40s", 3));
        System.out.println(handleSpecialFormat(1, "h20m", 3));
        System.out.println(handleSpecialFormat(2, "m40s", 3));

        // test round
        System.out.println(MathHelper.round(0.2344223, 4));

        // test unit normalization
        System.out.println(getNormalizedNumber(5, "mpixel"));
        System.out.println(getNormalizedNumber(2, "megapixels"));
        System.out.println(getNormalizedNumber(30, "miles per hour is really fast"));
        System.out.println(getNormalizedNumber(20, "m kilometers"));
        System.out.println(getNormalizedNumber(53.4, "million, compared to"));
        System.out.println(getNormalizedNumber(125, "ft-lbs torque!!!"));
        System.out.println(getNormalizedNumber(125, "lb-ft torque, and power speed"));
        System.out.println(getNormalizedNumber(125, ""));
        System.out.println(getNormalizedNumber(1, "min 20s 23sdf sdf a__:"));
        System.out.println(getNormalizedNumber(1, "hour 30 minutes 20sdf"));
        System.out.println(getNormalizedNumber(5, "ft 9 in 20sdf"));
        System.out.println(getNormalizedNumber(1, "m20s 23sdf sdf a__:"));
        System.out.println(getNormalizedNumber(1, ":20 23sdf sdf a__:"));
        System.out.println(getNormalizedNumber(1, ":20 23sdf sdf a__:"));

        System.out.println(getNormalizedNumber(20, "inch"));
        System.out.println(transorm("inch", getNormalizedNumber(20, "inch")));

        // System.out.println(Double.parseDouble("8.589934592E9")/100000);

        // Locale.setDefault(Locale.ENGLISH);
        // DecimalFormat formatter = new DecimalFormat("#.###");
        // System.out.println(formatter.format(Double.parseDouble("8.589934592E3")));
        // System.out.println(formatter.format(Double.parseDouble("8.589934592E12")));
        // System.out.println(formatter.format(Double.parseDouble("8.589934592")));
        //
        //
        // String factString = "16";
        // String unitText = "GB sadf asdf";
        // // normalize units when given
        // if (factString.length() > 0) {
        // try {
        // factString = String.valueOf(MathHelper.getNormalizedNumber(Double.parseDouble(factString),unitText));
        // System.out.println(factString);
        // // make it a normalized string again (no .0)
        // factString = StringHelper.normalizeNumber(factString);
        // System.out.println("number after unit normalization "+factString);
        //
        // } catch (NumberFormatException e) {
        // e.printStackTrace();
        // }
        // }
    }
}