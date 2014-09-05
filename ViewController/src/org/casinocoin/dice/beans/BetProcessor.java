package org.casinocoin.dice.beans;

import com.google.common.util.concurrent.MoreExecutors;

import de.odysseus.el.ExpressionFactoryImpl;

import de.odysseus.el.util.SimpleContext;

import java.math.BigDecimal;

import java.math.BigInteger;

import java.security.MessageDigest;

import java.security.SecureRandom;

import java.sql.Timestamp;

import javax.el.ExpressionFactory;

import javax.el.ValueExpression;

import oracle.adf.share.ADFContext;
import oracle.adf.share.logging.ADFLogger;

import oracle.jbo.ApplicationModule;
import oracle.jbo.AttributeList;
import oracle.jbo.Row;
import oracle.jbo.ViewObject;
import oracle.jbo.client.Configuration;

import oracle.jbo.server.AttributeListImpl;

import org.casinocoin.core.Address;
import org.casinocoin.core.AddressFormatException;
import org.casinocoin.core.InsufficientMoneyException;
import org.casinocoin.core.NetworkParameters;
import org.casinocoin.core.TransactionInput;
import org.casinocoin.core.TransactionOutput;
import org.casinocoin.core.Utils;
import org.casinocoin.core.Wallet;
import org.casinocoin.dice.DiceServer;
import org.casinocoin.dice.model.PlacedBetsViewImpl;
import org.casinocoin.dice.model.TransactionsViewImpl;
import org.casinocoin.dice.model.common.ActiveAvailableBetsView;
import org.casinocoin.dice.model.common.ActiveAvailableJackpotBetsView;
import org.casinocoin.dice.model.common.SecretsView;
import org.casinocoin.dice.model.common.SettingsView;
import org.casinocoin.params.MainNetParams;
import org.casinocoin.script.Script;

public class BetProcessor {
    
    private static ADFLogger log = ADFLogger.createADFLogger(BetProcessor.class);
    private ADFContext currentADFContext;
    private final String amDef = "org.casinocoin.dice.model.DiceAppModule";
    private final String config = "AppModuleLocal";
    private ApplicationModule diceAM;
    
    private BigDecimal betValue;
    private String receivedAddress;
    private String senderAddress;
    private String txId;
    private Integer confirmations;
    private Integer absId;
    private Integer pbsId;
    private String pbsTxId;
    private String jackpotTxId;
    private Timestamp betTime;
    private Integer gameValue;
    private String gameOperator;
    private BigDecimal winMultiplier;
    private BigDecimal minBet;
    private BigDecimal maxBet;
    private boolean jackpotBet;
    private String luckyHash;
    private Integer luckyNumber;
    private boolean refund = false;
    private String refundMessage;
    
    public BetProcessor(BigDecimal betValue, Timestamp betTime, String receivedAddress, String senderAddress, String txId, Integer confirmations) {
        log.info("Initialize DiceServerWalletEventListener()");
        currentADFContext = ADFContext.initADFContext(null, null, null, null);
        this.betValue = betValue;
        this.betTime = betTime;
        this.receivedAddress = receivedAddress;
        this.senderAddress = senderAddress;
        this.txId = txId;
        this.confirmations = confirmations;
    }
    
    public void process(){
        diceAM = Configuration.createRootApplicationModule(amDef,config);
        // get secret
        SecretsView scsVO = (SecretsView) diceAM.findViewObject("SecretsView1");
        String currentSecret = scsVO.getCurrentSecret();
        log.info("Secret: " + currentSecret);
        Integer currentSecretId = scsVO.getCurrentSecretId();
        // get the available bet with coin address
        ActiveAvailableBetsView aabVO = (ActiveAvailableBetsView) diceAM.findViewObject("ActiveAvailableBetsView1");
        aabVO.setparamCoinaddress(receivedAddress);
        aabVO.getViewCriteriaManager().setApplyViewCriteriaName("getAvailableBetForCoinaddress");
        aabVO.executeQuery();
        // get the available jackpot bet with  coin address
        ActiveAvailableJackpotBetsView aajbVO = (ActiveAvailableJackpotBetsView) diceAM.findViewObject("ActiveAvailableJackpotBetsView1");
        aajbVO.setparamCoinaddress(receivedAddress);
        aajbVO.getViewCriteriaManager().setApplyViewCriteriaName("getAvailableJackpotBetForCoinaddress");
        aajbVO.executeQuery();
        if(aabVO.hasNext()){
            // normal bet
            Row aabRow = aabVO.next();
            absId = (Integer) aabRow.getAttribute("Id");
            gameValue = (Integer) aabRow.getAttribute("GameValue");
            gameOperator = (String) aabRow.getAttribute("GameOperator");
            winMultiplier = (BigDecimal) aabRow.getAttribute("WinMultiplier");
            minBet = (BigDecimal) aabRow.getAttribute("MinBet");
            maxBet = (BigDecimal) aabRow.getAttribute("MaxBet");
            jackpotBet = ((Boolean)aabRow.getAttribute("JackpotBet")).booleanValue();
        } else if (aajbVO.hasNext()){
            // jackpot bet
            Row aajbRow = aabVO.next();
            absId = (Integer) aajbRow.getAttribute("Id");
            gameValue = (Integer) aajbRow.getAttribute("GameValue");
            gameOperator = (String) aajbRow.getAttribute("GameOperator");
            winMultiplier = (BigDecimal) aajbRow.getAttribute("WinMultiplier");
            minBet = (BigDecimal) aajbRow.getAttribute("MinBet");
            maxBet = (BigDecimal) aajbRow.getAttribute("MaxBet");
            jackpotBet = ((Boolean)aajbRow.getAttribute("JackpotBet")).booleanValue();
        } else {
            String errorMsg = "ERROR: Available bet for coinaddress " + receivedAddress + " not found in database.";
            log.severe(errorMsg);
            // return funds to sender
            returnFundsToSender(errorMsg);
            return;
        }
        // check against min/max values
        if((betValue.compareTo(minBet) == -1)|| (betValue.compareTo(maxBet) == 1)){
            String errorMsg = "ERROR: Amount not between MIN " + minBet + " and MAX " + maxBet + " values.";
            log.severe(errorMsg);
            // return funds to sender
            returnFundsToSender(errorMsg);
            return;
        }
        // move coin to jackpot if jackpot bet
        log.info("Jackpot Bet?: " + jackpotBet);
        if(jackpotBet){
            // reduce the betValue with 1 for the Jackpot
            betValue = betValue.subtract(new BigDecimal(1));
            // add 1 coin to the jackpot wallet address
            executeCoinTransaction(DiceServer.getServerJackpotAddress(), new BigDecimal(1), false, false);
            // save new jackpot to session and db
            BigInteger jackpot = DiceServer.getJackpotValue();
            jackpot.add(new BigInteger("100000000"));
            DiceServer.setJackpotValue(jackpot);
            SettingsView stsVO = (SettingsView) diceAM.findViewObject("SettingsView1");
            stsVO.setJackpotValue(jackpot.toString());
        }
        // concatenate transaction id with secret
        String betString = txId.concat(currentSecret);
        log.info("Bet string: " + betString);
        // get SHA256 hash for string
        try{
            luckyHash = sha256(betString);
            log.info("Luck Hash: " + luckyHash);
        } catch (Exception ex){
            String errorMsg = "ERROR: creating SHA-256 hash for bet.";
            log.severe(errorMsg);
            // return funds to sender
            returnFundsToSender(errorMsg);
            return;
        }
        // convert first 4 hex bytes to decimal number as the lucky number
        String hexNumber = "0x".concat(luckyHash.substring(0,4));
        log.info("Hex Number: " + hexNumber);
        luckyNumber = Integer.decode(hexNumber);
        log.info("Lucky Number: " + luckyNumber.toString());
        // check the lucky number against the bet
        String betExpression = "${luckyNumber " + gameOperator + " gameValue}";
        log.info("Bet expression: " + betExpression);
        log.info("Bet parsed: " + "${" + luckyNumber.toString() + " " + gameOperator + " " + gameValue.toString() + "}");
        ExpressionFactory factory = new ExpressionFactoryImpl();
        SimpleContext context = new SimpleContext();        
        // parse our expression
        ValueExpression valueExpr = factory.createValueExpression(context, betExpression, Boolean.class);
        // set values for luckyNumber and gameValue
        factory.createValueExpression(context, "${luckyNumber}", Integer.class).setValue(context, luckyNumber);
        factory.createValueExpression(context, "${gameValue}", Integer.class).setValue(context, gameValue);
        // get the result value for our expression
        Boolean betResult = (Boolean) valueExpr.getValue(context);
        log.info("Expression result: " + betResult.toString());
        // create result transaction(s)
        BigDecimal betResultValue = null;
        if(betResult){
            log.info("BET WON !!");
            // determine winning value
            betResultValue = betValue.multiply(winMultiplier);
            log.info("Value " + betValue.toString() + " multiplied with " + winMultiplier.toString() + " results in " + betResultValue.toString());
        } else {
            log.info("BET LOST !!");
            log.info("Wallet balance: " + DiceServer.getDiceService().wallet().getBalance());
            // create a small 0.001 coin transaction to confirm the bet loss processing
            betResultValue = new BigDecimal("0.001");
        }
        // If Jackpot bet than see if we win the Jackpot
        Boolean jackpotResult = Boolean.valueOf(false);
        BigInteger jackpotValue = BigInteger.valueOf(0);
        BigDecimal jackpotResultValue = null;
        if(jackpotBet){
            // convert first 2 hex bytes to decimal number as the lucky jackpot number
            String hexJackpotNumber = "0x".concat(luckyHash.substring(0,2));
            // convert to decimal
            Integer luckyJackpotNumber = Integer.decode(hexJackpotNumber);
            log.info("Jackpot lucky number: " + luckyJackpotNumber.toString());
            // get a random number between 0 and 255
            SecureRandom random = new SecureRandom();
            int randomJackpotNumber = random.nextInt(256);
            if(luckyJackpotNumber.equals(Integer.valueOf(randomJackpotNumber))){
                // We WON the JACKPOT !!!!
                jackpotResult = Boolean.valueOf(true);
                // get the current jackpot value
                jackpotValue = DiceServer.getJackpotValue();
                // reset the jackpot
                DiceServer.setJackpotValue(BigInteger.valueOf(0));
                // set value
                jackpotResultValue = new BigDecimal(Utils.bitcoinValueToFriendlyString(jackpotValue));
            }
        }
        // save the placed bet and results to the database
        ViewObject pbsVo = diceAM.findViewObject("PlacedBetsView1");
        // Set all attributes
        AttributeList al = new AttributeListImpl();
        al.setAttribute("AbsId", absId);
        al.setAttribute("ScsId", currentSecretId);
        al.setAttribute("BetTime", betTime);
        al.setAttribute("BetValue", betValue);
        al.setAttribute("SenderCoinAddress", senderAddress);
        al.setAttribute("TxId", txId);
        al.setAttribute("Confirmations", confirmations);
        al.setAttribute("GameValue", gameValue);
        al.setAttribute("LuckyHash", luckyHash);
        al.setAttribute("LuckyNumber", luckyNumber);
        al.setAttribute("BetResult", betResult);
        al.setAttribute("JackpotBet", jackpotBet);
        al.setAttribute("JackpotResult", jackpotResult);
        al.setAttribute("JackpotValue", jackpotResultValue);
        al.setAttribute("Payout", betResultValue);
        al.setAttribute("PayoutExecuted", 0);
        al.setAttribute("Refunded", refund);
        al.setAttribute("BetError", refundMessage);
        // Create a new row
        Row placedBet = pbsVo.createAndInitRow(al);
        log.info("Row for placed bet created.");
        // commit changes
        diceAM.getTransaction().commit();
        pbsId = (Integer) placedBet.getAttribute("Id");
        log.info("Placed bet id: " + pbsId.toString());
        // execute transaction for placed bet result
        executeCoinTransaction(senderAddress, betResultValue, false, false);
        // execute jackpot transaction if won
        if(jackpotResult){
            executeCoinTransaction(senderAddress, jackpotResultValue, false, true);
        }
    }
    
    private void returnFundsToSender(String message){
        log.info("Return funds to sender for reason: " + message);
        refund = true;
        refundMessage = message;
        // lower betValue with transaction fee
        BigDecimal refundValue = betValue.subtract(new BigDecimal("0.001"));
        executeCoinTransaction(senderAddress, refundValue, true, false);
    }
    
    private void executeCoinTransaction(String coinaddress, BigDecimal value, final boolean isRefundTx, final boolean isJackpotTx){
        // we need to convert human readable decimal value to satoshi
        log.info("Send " + value.toPlainString() + " to " + coinaddress);
        BigDecimal satoshiBigDecimalValue = value.multiply(new BigDecimal("100000000"));
        final BigInteger amountToSend = BigInteger.valueOf(satoshiBigDecimalValue.longValue());
        log.info("executeCoinTransaction: send " + amountToSend.toString() + " to " + coinaddress);
        NetworkParameters params = MainNetParams.get();
        try{
            Wallet.SendRequest sendRequest = Wallet.SendRequest.to(
                new Address(params, coinaddress), 
                amountToSend
            );
            // set fee to 0 as the transaction is only 0.001
            sendRequest.fee = new BigInteger("0");
            sendRequest.ensureMinRequiredFee = false;
            // execute the SendRequest
            final Wallet.SendResult sendResult = DiceServer.getDiceService().wallet().sendCoins(
                DiceServer.getDiceService().peerGroup(),
                sendRequest
            );
            // transaction is now created, register a callback that is invoked when the transaction has propagated across the network.
            sendResult.broadcastComplete.addListener(new Runnable() {
                @Override
                public void run() {
                    // The wallet has changed now, save transaction to database
                    log.info("Coins send back to user! Transaction hash is " + sendResult.tx.getHashAsString());
                    diceAM = Configuration.createRootApplicationModule(amDef,config);
                    // check if not already saved to database
                    TransactionsViewImpl transactionsVO = (TransactionsViewImpl)diceAM.findViewObject("TransactionsView1");            
                    transactionsVO.setparamTxId(sendResult.tx.getHashAsString());
                    transactionsVO.getViewCriteriaManager().setApplyViewCriteriaName("getTransactionForTxId");
                    transactionsVO.executeQuery();
                    if(!transactionsVO.hasNext()){
                        // Transaction not in database so proceed
                        NetworkParameters params = MainNetParams.get();
                        log.info("TXID: " + sendResult.tx.getHashAsString());                            
                        if(sendResult.tx.getInputs().size() > 0){
                            // get the first input of the transaction for the From Address
                            TransactionInput in = sendResult.tx.getInput(0);
                            Script script = in.getScriptSig();
                            Address fromAddress = new Address(params, Utils.sha256hash160(script.getPubKey()));
                            log.info("From Address: " + fromAddress.toString());
                            for(TransactionOutput out : sendResult.tx.getOutputs()){
                                Address toAddress = out.getScriptPubKey().getToAddress(params);
                                log.info("To Address: " + toAddress.toString());
                                if(toAddress.toString().equals(senderAddress)){
                                    log.info("value: " + Utils.bitcoinValueToFriendlyString(out.getValue()));
                                    // get transactions viewobject
                                    ViewObject ttsVo = diceAM.findViewObject("TransactionsView1");
                                    // get the blockhash
                                    String blockHash = "";
                                    Integer blockHeight = null;
                                    if(sendResult.tx.getAppearsInHashes() != null){
                                        if(sendResult.tx.getAppearsInHashes().entrySet().iterator().hasNext()){
                                            blockHash = sendResult.tx.getAppearsInHashes().entrySet().iterator().next().getKey().toString();
                                            blockHeight = sendResult.tx.getConfidence().getAppearedAtChainHeight();
                                        }
                                    }
                                    // determine the transaction type
                                    String txType = "BET_RESULT";
                                    if(isRefundTx){
                                        txType = "REFUND";
                                    } else if(isJackpotTx){
                                        txType = "JACKPOT";
                                    }
                                    // Set all attributes
                                    AttributeList al = new AttributeListImpl();
                                    al.setAttribute("TxType", txType);
                                    al.setAttribute("TxId", sendResult.tx.getHashAsString());
                                    al.setAttribute("TxTime", sendResult.tx.getUpdateTime());
                                    al.setAttribute("CoinAddress", toAddress.toString());
                                    al.setAttribute("Amount", Utils.bitcoinValueToFriendlyString(out.getValue()));
                                    al.setAttribute("Confirmations", sendResult.tx.getConfidence().getDepthInBlocks());
                                    al.setAttribute("Blockhash", blockHash);
                                    al.setAttribute("Blockheight", blockHeight);
                                    al.setAttribute("Executed", 1);
                                    al.setAttribute("ExecutedTime", new Timestamp(System.currentTimeMillis()));
                                    // Create a new row
                                    Row transaction = ttsVo.createAndInitRow(al);
                                    if(isJackpotTx){
                                        jackpotTxId = (String) transaction.getAttribute("TxId");
                                        log.info("Row for Jackpot TxId " + jackpotTxId + " created.");
                                    } else {
                                        pbsTxId = (String) transaction.getAttribute("TxId");    
                                        log.info("Row for TxId " + pbsTxId + " created.");
                                    }
                                    // commit changes
                                    diceAM.getTransaction().commit();
                                }
                            }
                            // update placed bet with transaction info
                            PlacedBetsViewImpl pbsVo = (PlacedBetsViewImpl) diceAM.findViewObject("PlacedBetsView1");
                            log.info("PbsId: " + pbsId);
                            pbsVo.setparamPbsId(pbsId);
                            pbsVo.getViewCriteriaManager().setApplyViewCriteriaName("getPlacedBetById");
                            pbsVo.executeQuery();
                            log.info("Placed Bets Row Count: " + pbsVo.getEstimatedRowCount());
                            Row placedBet = pbsVo.first();
                            log.info("Updating PbsId: " + placedBet.getAttribute("Id"));
                            // check if refund
                            if(isRefundTx){
                                placedBet.setAttribute("Refunded", 1);
                                placedBet.setAttribute("BetError", refundMessage);
                            }
                            // check if jackpot
                            if(isJackpotTx){
                                placedBet.setAttribute("JackpotPayoutTxId", jackpotTxId);
                            } else {
                                placedBet.setAttribute("PayoutExecuted", 1);
                                placedBet.setAttribute("PayoutExecutedTime", new Timestamp(System.currentTimeMillis()));
                                placedBet.setAttribute("PayoutTxId", pbsTxId);
                            }
                            // commit changes
                            diceAM.getTransaction().commit();
                        } else {
                            log.severe("Error: Transaction without inputs.");
                        }
                    } else {
                        // ignore transaction, already processed
                        log.info("Transaction already processed.");
                    }
                }
            }, MoreExecutors.sameThreadExecutor());
        } catch (InsufficientMoneyException e) {
            log.severe("Insufficient Money during transaction execution !!");
            throw new RuntimeException(e);
        } catch (AddressFormatException e) {
            log.severe("Address Format error during transaction execution !!");
            throw new RuntimeException(e);
        }
    }
    
    private String sha256(String base) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch(Exception ex){
           throw new RuntimeException(ex);
        }
    }
    
    public void setReceivedAddress(String receivedAddress) {
        this.receivedAddress = receivedAddress;
    }

    public String getReceivedAddress() {
        return receivedAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getTxId() {
        return txId;
    }

    public void setConfirmations(Integer confirmations) {
        this.confirmations = confirmations;
    }

    public Integer getConfirmations() {
        return confirmations;
    }
    
    @Override
    protected void finalize() throws Throwable {
        // release app module
        log.info("Release Application Module");
        Configuration.releaseRootApplicationModule(diceAM,true);
        ADFContext.resetADFContext(currentADFContext);
        // do super
        super.finalize();
    }
}
