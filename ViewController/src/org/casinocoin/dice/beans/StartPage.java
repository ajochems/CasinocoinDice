package org.casinocoin.dice.beans;

import java.math.BigInteger;

import oracle.adf.controller.TaskFlowId;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.share.logging.ADFLogger;

import oracle.adf.model.OperationBinding;

import oracle.adf.model.binding.DCBindingContainer;

import oracle.adf.model.binding.DCBindingContainerValueChangeEvent;
import oracle.adf.share.ADFContext;

import oracle.jbo.Row;

import org.apache.myfaces.trinidad.event.PollEvent;

import org.casinocoin.core.Address;
import org.casinocoin.core.ECKey;
import org.casinocoin.dice.DiceServer;

public class StartPage {
    
    private static ADFLogger log = ADFLogger.createADFLogger(StartPage.class);
    
    private String taskFlowId = "/WEB-INF/initializingTF.xml#initializingTF";
    private static boolean serverInitComplete = false;
    
    private DCBindingContainer bindings;

    public StartPage() {
        log.info("StartPage Bean Init");
        // initialize bindings
        BindingContext bc = BindingContext.getCurrent();
        bindings = (DCBindingContainer) bc.getCurrentBindingsEntry();
        // check if server init is complete
        if(checkServerInitComplete()){
            processServerInitComplete();
        }
    }

    public TaskFlowId getDynamicTaskFlowId() {
        if(isServerInitComplete()){
            showDiceStart();
        }
        return TaskFlowId.parse(taskFlowId);
    }
    
    public void showDiceStart(){
        this.taskFlowId = "/WEB-INF/diceTF.xml#diceTF";
    }

    public void setDynamicTaskFlowId(String taskFlowId) {
        this.taskFlowId = taskFlowId;
    }
    
    private void processServerInitComplete(){
        log.info("processServerInitComplete()");
        // set server init complete
        serverInitComplete = true;
        // set taskflow to diceTF
        showDiceStart();
        // turn diff checks on again
        DiceServer.getDiceService().params().setSkipDifficultyChecks(false);
        // set server wallet address in database if not yet done so
        OperationBinding operationBinding = (OperationBinding) bindings.getOperationBinding("getServerWalletAddress");
        operationBinding.execute();
        String dbServerWalletAddress = (String)operationBinding.getResult();
        if(!dbServerWalletAddress.equals(DiceServer.getServerWalletAddress())){
            operationBinding = (OperationBinding) bindings.getOperationBinding("setServerWalletAddress");
            operationBinding.getParamsMap().put("address",DiceServer.getServerWalletAddress());
            operationBinding.execute();
        }
        // set the jackpot wallet address in database if not yet done so
        operationBinding = (OperationBinding) bindings.getOperationBinding("getJackpotWalletAddress");
        operationBinding.execute();
        String dbJackpotWalletAddress = (String)operationBinding.getResult();
        if(dbJackpotWalletAddress.length()==0){
            // Create new Jackpot Wallet Address
            ECKey jackpotKey = new ECKey();
            log.info("Jackpot Key: " + jackpotKey.toString());
            Address jackpotAddress = jackpotKey.toAddress(DiceServer.getDiceService().params());
            log.info("jackpotKey address: " + jackpotAddress.toString());
            // add to wallet
            DiceServer.getDiceService().wallet().addKey(jackpotKey);
            // save to session
            DiceServer.setServerJackpotAddress(jackpotAddress.toString());
            // save to database
            operationBinding = (OperationBinding) bindings.getOperationBinding("setJackpotWalletAddress");
            operationBinding.getParamsMap().put("address", jackpotAddress.toString());
            operationBinding.execute();
        } else {
            DiceServer.setServerJackpotAddress(dbJackpotWalletAddress);
        }
        // Set the Jackpot value
        operationBinding = (OperationBinding) bindings.getOperationBinding("getJackpotValue");
        operationBinding.execute();
        String dbJackpotValue = (String)operationBinding.getResult();
        DiceServer.setJackpotValue(new BigInteger(dbJackpotValue));
        // Set the confirmations count
        operationBinding = (OperationBinding) bindings.getOperationBinding("getConfirmations");
        operationBinding.execute();
        Integer confirmations = (Integer)operationBinding.getResult();
        DiceServer.setConfirmations(confirmations);
        // Check if all bets have (valid) addresses
        DCIteratorBinding availableBetsIter = (DCIteratorBinding)bindings.get("AvailableBetsView1Iterator");
        for (int i=0; i<availableBetsIter.getViewObject().getEstimatedRowCount(); i++ ) {
            Row r = availableBetsIter.getRowAtRangeIndex(i);
            String curAddress = (String)r.getAttribute("CoinAddress");
            if(curAddress.length() == 0){
                // create new address
                ECKey betKey = new ECKey();
                log.info("betKey: " + betKey.toString());
                Address betAddress = betKey.toAddress(DiceServer.getDiceService().params());
                log.info("betKey address: " + betAddress.toString());
                // add to wallet
                DiceServer.getDiceService().wallet().addKey(betKey);
                // save to database bet
                r.setAttribute("CoinAddress", betAddress.toString());
            } else {
                log.info("Address set for Id "+ ((Integer)r.getAttribute("Id")).toString() + ": " + curAddress);
            }
        }
        // commit changes
        operationBinding = (OperationBinding) bindings.getOperationBinding("Commit");
        operationBinding.execute();
    }
    
    public static boolean isServerInitComplete() {
        return serverInitComplete;
    }
    
    private boolean checkServerInitComplete(){
        OperationBinding operationBinding = (OperationBinding)bindings.getOperationBinding("getServerInitComplete");
        operationBinding.execute();
        String initComplete = (String)operationBinding.getResult();
        log.info("checkServerInitComplete: " + initComplete);
        if(initComplete.equals("1")){
            log.info("checkServerInitComplete: true");
            return true;
        } else {
            return false;
        }
    }

}
