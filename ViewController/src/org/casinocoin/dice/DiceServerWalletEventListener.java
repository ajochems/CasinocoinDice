package org.casinocoin.dice;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.sql.Timestamp;

import java.util.List;

import oracle.adf.share.ADFContext;
import oracle.adf.share.logging.ADFLogger;

import oracle.jbo.ApplicationModule;
import oracle.jbo.AttributeList;
import oracle.jbo.Row;
import oracle.jbo.ViewObject;
import oracle.jbo.client.Configuration;
import oracle.jbo.server.AttributeListImpl;

import org.casinocoin.core.Address;
import org.casinocoin.core.ECKey;
import org.casinocoin.core.NetworkParameters;
import org.casinocoin.core.WalletEventListener;
import org.casinocoin.core.Transaction;
import org.casinocoin.core.TransactionInput;
import org.casinocoin.core.TransactionOutput;
import org.casinocoin.core.Utils;
import org.casinocoin.core.Wallet;
import org.casinocoin.dice.beans.BetProcessor;
import org.casinocoin.dice.model.TransactionsViewImpl;
import org.casinocoin.params.MainNetParams;
import org.casinocoin.script.Script;

public class DiceServerWalletEventListener implements WalletEventListener {

    private static ADFLogger log = ADFLogger.createADFLogger(WalletEventListener.class);
    private Wallet vWallet;
    private final String amDef = "org.casinocoin.dice.model.DiceAppModule";
    private final String config = "AppModuleLocal";
    private ApplicationModule diceAM;
    
    public DiceServerWalletEventListener(){
        log.info("Initialize DiceServerWalletEventListener()");
    }

    @Override
    public void onCoinsReceived(Wallet w, Transaction tx, BigInteger prevBalance, BigInteger newBalance) {
        // The transaction "tx" can either be pending, or included into a block (we didn't see the broadcast).
        // we also check if we actually received something
        if(newBalance.compareTo(prevBalance) == 1){
            vWallet = w;
            log.info("Previous balance: " + Utils.bitcoinValueToFriendlyString(prevBalance) + " - New Balance: " + Utils.bitcoinValueToFriendlyString(newBalance));
            BigInteger value = tx.getValueSentToMe(vWallet);
            log.info("Received tx for " + Utils.bitcoinValueToFriendlyString(value) + ": " + tx.getHashAsString());
            // Wait until it's made it into the block chain (may run immediately if it's already there).
            Futures.addCallback(tx.getConfidence().getDepthFuture(1), new FutureCallback<Transaction>() {
                @Override
                public void onSuccess(Transaction confirmedTx) {
                    // "confirmedTx" here is the same as "tx" above, but we use it anyway for clarity.
                    log.info("Transaction confirmed, process bet");
                    ADFContext currentADFContext = null;
                    try {
                        currentADFContext = ADFContext.initADFContext(null, null, null, null);
                        diceAM = Configuration.createRootApplicationModule(amDef,config);
                        // check if not already saved to database
                        TransactionsViewImpl transactionsVO = (TransactionsViewImpl)diceAM.findViewObject("TransactionsView1");            
                        transactionsVO.setparamTxId(confirmedTx.getHashAsString());
                        transactionsVO.getViewCriteriaManager().setApplyViewCriteriaName("getTransactionForTxId");
                        transactionsVO.executeQuery();
                        if(!transactionsVO.hasNext()){
                            // Transaction not in database so proceed
                            NetworkParameters params = MainNetParams.get();
                            log.info("TXID: " + confirmedTx.getHashAsString());
                            for(TransactionOutput out : confirmedTx.getOutputs()){
                                if(out.isMine(vWallet)){
                                    if(confirmedTx.getInputs().size() > 0){
                                        // get the first input of the transaction for the From Address
                                        TransactionInput in = confirmedTx.getInput(0);
                                        Script script = in.getScriptSig();
                                        Address fromAddress = new Address(params, Utils.sha256hash160(script.getPubKey()));
                                        Address toAddress = out.getScriptPubKey().getToAddress(params);
                                        log.info("To Address: " + toAddress.toString());
                                        log.info("From Address: " + fromAddress.toString());
                                        log.info("value: " + Utils.bitcoinValueToFriendlyString(out.getValue()));
                                        // get transactions viewobject
                                        ViewObject ttsVo = diceAM.findViewObject("TransactionsView1");
                                        // get the blockhash
                                        String blockHash = "";
                                        if(confirmedTx.getAppearsInHashes().entrySet().iterator().hasNext()){
                                            blockHash = confirmedTx.getAppearsInHashes().entrySet().iterator().next().getKey().toString();
                                        }
                                        // determine the tx type
                                        String txType = "BET";
                                        if(toAddress.toString() == DiceServer.getServerWalletAddress()){
                                            txType = "WALLET";
                                        }
                                        // Set all attributes
                                        AttributeList al = new AttributeListImpl();
                                        al.setAttribute("TxType", txType);
                                        al.setAttribute("TxId", confirmedTx.getHashAsString());
                                        al.setAttribute("TxTime", confirmedTx.getUpdateTime());
                                        al.setAttribute("CoinAddress", toAddress.toString());
                                        al.setAttribute("Amount", Utils.bitcoinValueToFriendlyString(out.getValue()));
                                        al.setAttribute("Confirmations", confirmedTx.getConfidence().getDepthInBlocks());
                                        al.setAttribute("Blockhash", blockHash);
                                        al.setAttribute("Blockheight", confirmedTx.getConfidence().getAppearedAtChainHeight());
                                        // Create a new row
                                        Row transaction = ttsVo.createAndInitRow(al);
                                        // commit changes
                                        diceAM.getTransaction().commit();
                                        log.info("Row with Id " + transaction.getAttribute("Id") + " created.");
                                        // if BET then process
                                        if(txType.equals("BET")){
                                            BetProcessor betProc = new BetProcessor(new BigDecimal(Utils.bitcoinValueToFriendlyString(out.getValue())),
                                                                                    new Timestamp(confirmedTx.getUpdateTime().getTime()),
                                                                                    toAddress.toString(), 
                                                                                    fromAddress.toString(), 
                                                                                    confirmedTx.getHashAsString(), 
                                                                                    confirmedTx.getConfidence().getDepthInBlocks()
                                                                                    );
                                            betProc.process();
                                        }
                                    } else {
                                        log.severe("Error: Transaction without inputs.");
                                    }
                                }
                            }
                        } else {
                            // ignore transaction, already processed
                            log.info("Transaction already processed.");
                        }
                    } finally {
                        Configuration.releaseRootApplicationModule(diceAM,true);
                        ADFContext.resetADFContext(currentADFContext);
                    }
                }
    
                @Override
                public void onFailure(Throwable t) {
                    // This kind of future can't fail, just rethrow in case something weird happens.
                    throw new RuntimeException(t);
                }
            });
        }
    }

    @Override
    public void onCoinsSent(Wallet wallet, Transaction transaction, BigInteger prevBalance, BigInteger newBalance) {
        log.info("onCoinsSent");
        log.info("Previous balance: " + Utils.bitcoinValueToFriendlyString(prevBalance) + " - New Balance: " + Utils.bitcoinValueToFriendlyString(newBalance));
    }

    @Override
    public void onTransactionConfidenceChanged(Wallet wallet, Transaction transaction) {
        Integer transactionConfirmations = transaction.getConfidence().getDepthInBlocks();
        Integer registerConfirmations = DiceServer.getConfirmations();
        if(transactionConfirmations.compareTo(registerConfirmations) <= 0){
            log.info("onTransactionConfidenceChanged: " + transaction.getConfidence().toString());
            ADFContext currentADFContext = null;
            try {
                currentADFContext = ADFContext.initADFContext(null, null, null, null);
                diceAM = Configuration.createRootApplicationModule(amDef,config);
                TransactionsViewImpl transactionsVO = (TransactionsViewImpl)diceAM.findViewObject("TransactionsView1");            
                transactionsVO.setparamTxId(transaction.getHashAsString());
                transactionsVO.getViewCriteriaManager().setApplyViewCriteriaName("getTransactionForTxId");
                transactionsVO.executeQuery();
                if(transactionsVO.hasNext()){
                    Row transactionsRow = transactionsVO.next();
                    // set confirmations
                    transactionsRow.setAttribute("Confirmations", transaction.getConfidence().getDepthInBlocks());
                    // set blockhash if not set
                    if(transactionsRow.getAttribute("Blockhash").toString().length() == 0){
                        if(transaction.getAppearsInHashes() != null){
                            if(transaction.getAppearsInHashes().entrySet().iterator().hasNext()){
                                transactionsRow.setAttribute("Blockhash", transaction.getAppearsInHashes().entrySet().iterator().next().getKey().toString());
                                transactionsRow.setAttribute("Blockheight", transaction.getConfidence().getAppearedAtChainHeight());
                            }
                        }
                    }
                    diceAM.getTransaction().commit();
                } else {
                    log.severe("ERROR: transaction with id " + transaction.getHashAsString() + " not found in database.");
                }
            } finally {
                Configuration.releaseRootApplicationModule(diceAM,true);
                ADFContext.resetADFContext(currentADFContext);
            }
        }
    }

    @Override
    public void onReorganize(Wallet wallet) {
        onChange();
    }

    @Override
    public void onWalletChanged(Wallet wallet) {
        onChange();
    }

    @Override
    public void onKeysAdded(Wallet wallet, List<ECKey> list) {
        onChange();
    }

    @Override
    public void onScriptsAdded(Wallet wallet, List<Script> list) {
        onChange();
    }
    
    private void onChange() {
    }
}
