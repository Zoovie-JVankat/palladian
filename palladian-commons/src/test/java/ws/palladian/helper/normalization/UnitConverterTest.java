package ws.palladian.helper.normalization;

import org.junit.Test;
import ws.palladian.helper.conversion.UnitConverter;

import static org.junit.Assert.assertEquals;

/**
 * Test cases for the conversion of units.
 *
 * @author David Urbansky
 */
public class UnitConverterTest {

    @Test
    public void testConvertUnit() {
        assertEquals(6.60011103, UnitConverter.convert(187.11, "g", "oz"), 0.);
        assertEquals(720, UnitConverter.convert(2592000., "joule", "Wh"), 0.);
        assertEquals(63, UnitConverter.convert(63., "mm", "mm"), 0.);
        assertEquals(6.3, UnitConverter.convert(63., "mm", "cm"), 0.);
        assertEquals(32., UnitConverter.convert(0., "celsius", "fahrenheit"), 0.01);
        assertEquals(273.15, UnitConverter.convert(0., "celsius", "kelvin"), 0.01);
        assertEquals(0., UnitConverter.convert(32., "fahrenheit", "celsius"), 0.01);
        assertEquals(273.15, UnitConverter.convert(32., "fahrenheit", "kelvin"), 0.01);
        assertEquals(0., UnitConverter.convert(273.15, "kelvin", "celsius"), 0.01);
        assertEquals(32., UnitConverter.convert(273.15, "kelvin", "fahrenheit"), 0.01);

        assertEquals(3.53, UnitConverter.convert(100., "mL", "ounces"), 0.01);
    }

}