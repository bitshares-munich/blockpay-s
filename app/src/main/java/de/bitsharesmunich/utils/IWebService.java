package de.bitsharesmunich.utils;

import android.media.Image;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.bitsharesmunich.models.AccountBlock;
import de.bitsharesmunich.models.AccountDetails;
import de.bitsharesmunich.models.AccountUpgrade;
import de.bitsharesmunich.models.Altcoin;
import de.bitsharesmunich.models.BlocktradesSessionResponse;
import de.bitsharesmunich.models.BlocktradesTradingPair;
import de.bitsharesmunich.models.CCAssets;
import de.bitsharesmunich.models.CapFeedResponse;
import de.bitsharesmunich.models.CoinMarketCapItemResponse;
import de.bitsharesmunich.models.DecodeMemo;
import de.bitsharesmunich.models.DecodeMemosArray;
import de.bitsharesmunich.models.EquivalentComponentResponse;
import de.bitsharesmunich.models.ExchangeRate;
import de.bitsharesmunich.models.GenerateKeys;
import de.bitsharesmunich.models.InitiateTradeResponse;
import de.bitsharesmunich.models.InputEstimate;
import de.bitsharesmunich.models.LtmFee;
import de.bitsharesmunich.models.QrHash;
import de.bitsharesmunich.models.RegisterAccountOL;
import de.bitsharesmunich.models.RegisterAccountResponse;
import de.bitsharesmunich.models.ResponseBinFormat;
import de.bitsharesmunich.models.RewardResponse;
import de.bitsharesmunich.models.TradingPair;
import de.bitsharesmunich.models.TransactionAltCoin;
import de.bitsharesmunich.models.TransactionIdResponse;
import de.bitsharesmunich.models.TransactionSmartCoin;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by mbilal on 4/7/16.
 */
public interface IWebService {

    @Headers({"Content-Type: application/x-www-form-urlencoded"})
    @GET("/assets/")
    Call<CCAssets> getAssets();

    @Headers({"Content-Type: application/json"})
    @POST("/")
    Call<DecodeMemosArray> getDecodedMemosArray(@Body Map<String, String> params);

    @Headers({"Content-Type: application/x-www-form-urlencoded"})
    @GET("/avatar/{path}/")
    Call<Image> getGravatarImage(@Path("path") String path);

    @Headers({"Content-Type: application/json"})
    @GET("/api/v2/coins/")
    Call<Altcoin[]> getAltcoins();

    @Headers({"Content-Type: application/json"})
    @POST("/get_qr_hash/")
    Call<QrHash> getQrHash(@Body Map<String, String> params);

    @Headers({"Content-Type: application/json"})
    @POST("/get_qr_hash_w_note/")
    Call<QrHash> getQrHashWNote(@Body Map<String, String> params);

    @Headers({"Content-Type: application/json"})
    @POST("/ws/")
    Call<AccountBlock> verifyAccount(@Body Map<String, Object> params);

    @Headers({"Content-Type: application/json"})
    @POST("/api/v2/simple-api/initiate-trade/")
    Call<InitiateTradeResponse> initiateTrade(@Body Map<String, String> params);

    @GET("/get_transactions/{accountId}/{orderId}")
    Call<TransactionSmartCoin[]> getTransactionSmartCoin(@Path("accountId") String accountId, @Path("orderId") String orderId);

    @Headers({"Content-Type: application/json"})
    @GET("/api/v2/transactions")
    Call<TransactionAltCoin[]> getTransactionAltCoinToken(@Query("sessionToken") String sessionToken);

    @GET("/api/v2/simple-api/transactions")
    Call<TransactionAltCoin[]> getTransactionAltCoin(@Query("inputAddress") String inputAddress);

    @Headers({"Content-Type: application/json"})
    @POST("/")
    Call<RewardResponse> getRewardResponse(@Body Map<String, String> params);

    @GET("/get_exchange_rates/{baseId}/{quoteId}")
    Call<ExchangeRate> getExchangeRates(@Path("baseId") String baseId, @Path("quoteId") String quoteId);

    @GET("/api/v2/estimate-input-amount")
    Call<InputEstimate> estimateInputAmount(@Query("inputCoinType") String inputCoinType, @Query("outputCoinType") String outputCoinType, @Query("outputAmount") String outputAmount);

    @GET("/json")
    Call<CapFeedResponse> getCapfeedData();

    @GET("/v1/ticker/")
    Call<List<CoinMarketCapItemResponse>> getCoinMarketcapData(@Query("limit") int limit);

    @GET("/api/v2/trading-pairs/{inputCoinType}/{outputCoinType}")
    Call<TradingPair> getTradingPair(@Path("inputCoinType") String inputCoinType, @Path("outputCoinType") String outputCoinType);

    @Headers({"Content-Type: application/json"})
    @POST("/")
    Call<ResponseBinFormat> getBytesFromBrainKey(@Body HashMap<String, String> params);

    @Headers({"Content-Type: application/json"})
    @POST("/")
    Call<GenerateKeys> getGeneratedKeys(@Body Map<String, String> params);

    @Headers({"Content-Type: application/json"})
    @POST("/api/v1/accounts")
    Call<RegisterAccountResponse> getReg(@Body Map<String, HashMap> params);

    @Headers({"Content-Type: application/json"})
    @POST("/api/v1/accounts")
    Call<RegisterAccountOL> getRegOL(@Body Map<String, Object> params);

    @Headers({"Content-Type: application/json"})
    @POST("/")
    Call<AccountDetails> getAccountFromBin(@Body HashMap<String, Object> params);

    @Headers({"Content-Type: application/json"})
    @POST("/")
    Call<AccountDetails> getAccount(@Body Map<String, String> params);

    @Headers({"Content-Type: application/json"})
    @POST("/")
    Call<DecodeMemo> getDecodedMemo(@Body Map<String, String> params);

    @Headers({"Content-Type: application/json"})
    @POST("/")
    Call<EquivalentComponentResponse> getEquivalentComponent(@Body Map<String, String> params);

    @Headers({"Content-Type: application/json"})
    @POST("/")
    Call<AccountUpgrade> getAccountUpgrade(@Body Map<String, String> params);

    @Headers({"Content-Type: application/json"})
    @POST("/")
    Call<LtmFee> getLtmFee(@Body Map<String, String> params);

    @Headers({"Content-Type: application/json"})
    @POST("/api/v2/sessions")
    Call<BlocktradesSessionResponse> getBlocktradeToken(@Body Map<String, Object> params);

    @GET("/{md5Email}.json")
    Call<Object> getGravatarProfile(@Path("md5Email") String md5Email);

    @Headers({"Content-Type: application/json"})
    @POST("/")
    Call<TransactionIdResponse> getTransactionIdComponent(@Body Map<String, String> params);

    @GET("/api/holders")
    Call<ResponseBody> getHolders(@Query("asset") String assetSymbol);

    @GET("/api/v2/trading-pairs")
    Call<List<BlocktradesTradingPair>> getTradingPairs();
}
