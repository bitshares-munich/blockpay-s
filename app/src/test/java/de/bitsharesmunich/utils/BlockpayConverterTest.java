package de.bitsharesmunich.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.bitsharesmunich.graphenej.Asset;
import de.bitsharesmunich.graphenej.AssetAmount;

/**
 * Created by nelson on 3/28/17.
 */
public class BlockpayConverterTest {
    private Asset input;
    private Asset core;
    private Asset output;
    private double endToEnd = 0.92524432;
    private double coreToEnd = 0.0053053;

    @Before
    public void setUp() throws Exception {
        input = new Asset("1.3.121", "USD", 4);
        core = new Asset("1.3.0", "BTS", 5);
        output = new Asset("1.3.120", "EUR", 4);
    }

    @Test
    public void testConversion(){
        BlockpayConverter converter = new BlockpayConverter(input, core, output, endToEnd, coreToEnd);
        AssetAmount coreConverted = converter.convert(10000, BlockpayConverter.INPUT_TO_CORE);
        AssetAmount outputConverted = converter.convert(10000, BlockpayConverter.INPUT_TO_OUTPUT);
        Assert.assertEquals(17440000, coreConverted.getAmount().longValue());
        Assert.assertEquals(9252, outputConverted.getAmount().longValue());
    }
}