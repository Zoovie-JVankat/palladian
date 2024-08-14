package ws.palladian.helper.normalization;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.constants.UnitType;

public class UnitNormalizerTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testDetectUnit() {
        String input;

        collector.checkThat(UnitNormalizer.detectUnit("display size in mm"), Matchers.is("mm"));
        collector.checkThat(UnitNormalizer.detectUnit("display size (in)"), Matchers.is("in"));
        collector.checkThat(UnitNormalizer.getUnitType("°C"), Matchers.is(UnitType.TEMPERATURE));

        collector.checkThat(UnitNormalizer.getUnitType("N"), Matchers.is(UnitType.FORCE));
        collector.checkThat(UnitNormalizer.getUnitType("km/h"), Matchers.is(UnitType.SPEED));

        input = "100N";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.is("N"));

        input = "10 t";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.is("t"));

        input = "120 g/cm³";
        collector.checkThat(UnitNormalizer.getUnitType(input), Matchers.is(UnitType.DENSITY));

        input = "120 gr.";
        collector.checkThat(UnitNormalizer.getUnitType(input), Matchers.is(UnitType.WEIGHT));

        input = "bis zu 230mb/s";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.is("MB/s"));

        input = "230 MB/s";
        collector.checkThat(UnitNormalizer.getUnitType(input), Matchers.is(UnitType.BANDWIDTH));

        input = "1 mAh";
        collector.checkThat(UnitNormalizer.getUnitType(input), Matchers.is(UnitType.ELECTRIC_CHARGE));

        input = "1 A";
        collector.checkThat(UnitNormalizer.getUnitType(input), Matchers.is(UnitType.CURRENT));

        input = "100 kN/m^2";
        collector.checkThat(UnitNormalizer.getUnitType(input), Matchers.is(UnitType.PRESSURE));

        input = "100 kN/m²";
        collector.checkThat(UnitNormalizer.getUnitType(input), Matchers.is(UnitType.PRESSURE));

        input = "100 gpm";
        collector.checkThat(UnitNormalizer.getUnitType(input), Matchers.is(UnitType.FLOW_RATE));

        input = "1123 lm";
        collector.checkThat(UnitNormalizer.getUnitType(input), Matchers.is(UnitType.LUMINOUS_FLUX));

        input = "100kN/m²";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.is("kN/m²"));

        input = "230 Volt";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.is("Volt"));

        input = "39 hours";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.is("hours"));

        input = "filter_groess # filter_groess # gross # 39";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.nullValue());

        input = "screen up to 350 inches.";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.is("inches"));

        input = "5.3 gpm";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.is("gpm"));

        input = "1600 lumens";
        collector.checkThat(UnitNormalizer.detectUnit(input), Matchers.is("lumens"));
    }

    @Test
    public void testTranslationNormalized() {
        collector.checkThat(UnitNormalizer.getNormalizedNumber(5, UnitTranslator.translate("cm", Language.GERMAN)), Matchers.is(5.));
        collector.checkThat(UnitNormalizer.getNormalizedNumber(5, UnitTranslator.translate("Zoll", Language.GERMAN)), Matchers.is(12.7));
        collector.checkThat(UnitNormalizer.getNormalizedNumber(1, UnitTranslator.translate("kilowattstunde", Language.GERMAN)), Matchers.is(3600000.));

        collector.checkThat(UnitTranslator.translateUnitsOfInput("psi 30", Language.GERMAN), Matchers.is("psi 30"));
        collector.checkThat(UnitTranslator.translateUnitsOfInput("PS5 neu gekauft!11!!", Language.GERMAN), Matchers.is("ps5 neu gekauft!11!!"));
        collector.checkThat(UnitTranslator.translateUnitsOfInput("schleuderdrehzahl 7 U/min", Language.GERMAN), Matchers.is("schleuderdrehzahl 7 rpm"));
        collector.checkThat(UnitTranslator.translateUnitsOfInput("schleuderdrehzahl 7 u/minute", Language.GERMAN), Matchers.is("schleuderdrehzahl 7 rpm"));
        collector.checkThat(
                UnitTranslator.translateUnitsOfInput("description maximale schleuderdrehzahl: ca. 1.600 u/minute beim standardprogram baumwolle 60° c", Language.GERMAN),
                Matchers.is("description maximale schleuderdrehzahl: ca. 1.600 rpm beim standardprogram baumwolle 60° c"));
        collector.checkThat(UnitTranslator.translateUnitsOfInput("nach einer Fahrzeit von 7 stunden", Language.GERMAN), Matchers.is("nach einer fahrzeit von 7 hours"));
        collector.checkThat(UnitTranslator.translateUnitsOfInput("Altersempfehlung ab 9 jahren", Language.GERMAN), Matchers.is("altersempfehlung ab 9 years"));
        collector.checkThat(UnitTranslator.translateUnitsOfInput("mit 12 kilokalorien sehr gesund", Language.GERMAN), Matchers.is("mit 12 kilocalories sehr gesund"));
    }
}