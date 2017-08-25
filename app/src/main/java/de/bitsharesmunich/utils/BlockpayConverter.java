package de.bitsharesmunich.utils;

import com.google.common.primitives.UnsignedLong;

import de.bitsharesmunich.graphenej.Asset;
import de.bitsharesmunich.graphenej.AssetAmount;

/**
 * Created by nelson on 3/28/17.
 */

public class BlockpayConverter {
    public static final int INPUT_TO_CORE = 0;
    public static final int INPUT_TO_OUTPUT = 1;
    public static final int CORE_TO_OUTPUT = 2;
    public static final int CORE_TO_INPUT = 3;
    public static final int OUTPUT_TO_INPUT = 4;
    public static final int OUTPUT_TO_CORE = 5;

    private Asset input;
    private Asset core;
    private Asset output;

    private double mEndToEnd;
    private double mCoreToEnd;

    public BlockpayConverter(Asset input, Asset core, Asset output, double endToEnd, double coreToEnd){
        this.input = input;
        this.core = core;
        this.output = output;
        this.mEndToEnd = endToEnd;
        this.mCoreToEnd = coreToEnd;
    }

    public Asset getInput() {
        return input;
    }

    public Asset getCore() {
        return core;
    }

    public Asset getOutput() {
        return output;
    }

    public AssetAmount convert(long toConvert, int direction){
        long convertedAmount = 0;
        Asset asset = null;
        switch (direction){
            case INPUT_TO_CORE:
                convertedAmount = (long) (toConvert * (mEndToEnd / mCoreToEnd) * Math.pow(10, core.getPrecision() - input.getPrecision()));
                asset = core;
                break;
            case INPUT_TO_OUTPUT:
                convertedAmount = (long) (toConvert * mEndToEnd * Math.pow(10, output.getPrecision() - input.getPrecision()));
                asset = output;
                break;
            case CORE_TO_OUTPUT:
                convertedAmount = (long) (toConvert * mCoreToEnd * Math.pow(10, output.getPrecision() - core.getPrecision()));
                asset = output;
                break;
            case CORE_TO_INPUT:
                convertedAmount = (long) (toConvert * (mCoreToEnd / mEndToEnd) * Math.pow(10, input.getPrecision() - core.getPrecision()));
                asset = input;
                break;
            case OUTPUT_TO_INPUT:
                convertedAmount = (long) (toConvert * (1.0 / mEndToEnd) * Math.pow(10, input.getPrecision() - output.getPrecision()));
                asset = input;
                break;
            case OUTPUT_TO_CORE:
                convertedAmount = (long) (toConvert * (1.0 / mCoreToEnd) * Math.pow(10, core.getPrecision() - output.getPrecision()));
                asset = core;
                break;
        }
        return new AssetAmount(UnsignedLong.valueOf(convertedAmount), asset);
    }
}
