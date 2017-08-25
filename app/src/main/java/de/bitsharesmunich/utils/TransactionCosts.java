package de.bitsharesmunich.utils;

import com.google.common.primitives.UnsignedLong;

import de.bitsharesmunich.graphenej.Asset;
import de.bitsharesmunich.graphenej.AssetAmount;

/**
 * Class used to calculate all costs for each exchange. These can be classified in 3 categories, which are:
 *
 * - PLATFORM_ASSET
 * In this case, we're dealing with a direct transfer in the Bitshares network and will be receiving
 * an asset, probably a smartcoin like bitUSD or bitEUR. There's the possibility that the merchant has
 * chosen BTS as its output asset though, in which case our only extra cost would be the Bitshares
 * Munich 0.5 % cut and its corresponding transfer fee.
 *
 * There's an extra transfer cost in case the merchant has the reward point system enabled and has enough
 * funds.
 *
 * - BRIDGE_DIRECT
 * In this case, we're dealing with a bridge-mediated exchange, where the customer would be sending a
 * certain amount of cryptocurrency to the bridge and it will be sending us the merchant required amount
 * of Bitshares asset. Again this output asset can be BTS, in which case we only need to ask for the
 * Bitshares Munich 0.5% cut and its transfer fee again.
 *
 * If the merchant has chosen any other smartcoin though, we would need to convert it to BTS before
 * sending it to the Bitshares Munich account. We would be incurring then in an extra operation cost
 * when creating a limit order.
 *
 *  - BRIDGE_INDIRECT
 *  In this case of bridge-mediated exchange, the merchant would be receiving always BTS as an intermediate
 *  step, because the bridge doesn't support a direct exchange between the customer's cryptocurrency
 *  and the merchant's desired output asset.
 *
 *  We would need then to convert this BTS into the merchant's desired asset, thus incurring in a
 *  limit order creation cost.
 *
 *  In summary it can be said that:
 *  - The Bitshares Munich cut + its asset transfer cost are always present.
 *  - There will always be an asset exchange cost unless the selected output asset is BTS.
 *  - In case the merchant has the reward point system on, there will be an extra asset transfer cost
 */
public class TransactionCosts {
    private final String TAG = this.getClass().getName();
    private Asset CORE_ASSET = new Asset("1.3.0");

    private AssetAmount mInputAmount;
    private AssetAmount bitsharesMunich;
    private AssetAmount mTransferFees;
    private AssetAmount mExchangeFees;
    private BlockpayConverter mConverter;
    private boolean mHasReward;

    public TransactionCosts(TransferType transferType, AssetAmount inputAmount, BlockpayConverter converter, boolean hasReward){
        mInputAmount = inputAmount;
        mConverter = converter;
        mHasReward = hasReward;

        bitsharesMunich = new AssetAmount(UnsignedLong.valueOf(0), CORE_ASSET);
        mTransferFees = new AssetAmount(UnsignedLong.valueOf(0), CORE_ASSET);
        mExchangeFees = new AssetAmount(UnsignedLong.valueOf(0), CORE_ASSET);

        if(transferType == TransferType.PLATFORM_ASSET){
            calculatePlatformAssetCosts();
        }else{
            calculateBridgeCosts();
        }
    }

    /**
     * Calculates the Bitshares Munich cut, in the core asset.
     * @return: The amount of BTS to send to the Bitshares Munich account
     */
    private AssetAmount getBitsharesMunichCut(){
        AssetAmount coreAmount = mConverter.convert(mInputAmount.getAmount().longValue(), BlockpayConverter.INPUT_TO_CORE);
        return coreAmount.multiplyBy(Constants.BITSHARES_MUNICH_FEE);
    }

    /**
     * Calculate the costs of using a bridge-enabled exchange
     */
    private void calculateBridgeCosts(){
        bitsharesMunich = getBitsharesMunichCut();
        mTransferFees = new AssetAmount(UnsignedLong.valueOf(Constants.FIXED_TRANSFER_FEE + Constants.EXTRA_NETWORK_FEE), mConverter.getCore());
        if(!mConverter.getOutput().getObjectId().equals(CORE_ASSET.getObjectId())){
            mExchangeFees = new AssetAmount(UnsignedLong.valueOf(Constants.LIMIT_ORDER_CREATE_BASIC_FEE), mConverter.getCore());
        }
    }

    /**
     * Calculate the costs of a basic Bitshares-supported exchange
     */
    private void calculatePlatformAssetCosts(){
        calculateBridgeCosts();
        if(mHasReward){
            mTransferFees = mTransferFees.multiplyBy(2);
        }
    }

    public AssetAmount getBitsharesMunich() {
        return bitsharesMunich;
    }

    public AssetAmount getTransferFees() {
        return mTransferFees;
    }

    public AssetAmount getExchangeFees() {
        return mExchangeFees;
    }
}
