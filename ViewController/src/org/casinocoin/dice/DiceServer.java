package org.casinocoin.dice;

import java.math.BigInteger;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import oracle.adf.share.logging.ADFLogger;

import org.casinocoin.core.Address;
import org.casinocoin.core.ECKey;
import org.casinocoin.core.NetworkParameters;

import org.casinocoin.core.Utils;
import org.casinocoin.core.Wallet;
import org.casinocoin.params.MainNetParams;

@ManagedBean(eager=true)
@ApplicationScoped
public class DiceServer {
    
    private static ADFLogger log = ADFLogger.createADFLogger(DiceServer.class);
    
    private String walletLocation;
    private static CasinocoinService diceService;
    private static int blockHeight = 0;
    private static String serverWalletAddress;
    private static Integer confirmations = 10;
    private static String serverJackpotAddress;
    private static BigInteger jackpotValue;

    public DiceServer(){
        log.info("Casinocoin Dice Server is starting up ...");
        NetworkParameters params = MainNetParams.get();
        // set skip diff checks to true
        params.setSkipDifficultyChecks(true);
        // Startup wallet
        log.info("Starting up Wallet Service");
        // diceService = new DiceService(params, walletLocation,"162.242.217.6", "dicedb", "diceuser", "st19RH2Eq2qGGaurk7Pe");
        diceService = new CasinocoinService(params, "jdbc/DiceDS");
        walletLocation = diceService.getWalletLocation();
        log.info("walletLocation from server property: " + walletLocation);
        if(walletLocation == null){
            String userHome = System.getProperty("user.home");
            String seperator = System.getProperty("file.separator");
            walletLocation = userHome + seperator + "csc-wallet-data" + seperator;
            log.info("walletLocation from userhome: " + walletLocation);
        }
        diceService.setWalletDirectory(walletLocation);
        log.info("Wallet location: " + walletLocation);
        // disable blocking startup
        diceService.setBlockingStartup(false);
        // turn on auto save of wallet
        diceService.setAutoSave(true);
        // Start downloading the block chain
        log.info("Starting block chain download");
        diceService.startAsync();
        // wait till diceService startup has finished
        diceService.awaitRunning();
        // We want to know when we receive money so register listener
        diceService.wallet().addEventListener(new DiceServerWalletEventListener());
        // Add download listener
        diceService.chain().addListener(new WebBlockChainListener());
        log.info("Address count: " + diceService.wallet().getKeys().size());
        int counter = 0;
        for(ECKey key : diceService.wallet().getKeys()){
            Address sendToAddress = key.toAddress(params);
            log.info("Wallet Address[" + counter + "]: " + sendToAddress);
            counter = counter + 1;
        }
        serverWalletAddress = diceService.wallet().getKeys().get(0).toAddress(params).toString();
        log.info("Saved server wallet address: " + serverWalletAddress);
        log.info("First wallet address creation time: " + diceService.wallet().getEarliestKeyCreationTime());
        log.info("Wallet balance: " + Utils.bitcoinValueToFriendlyString(diceService.wallet().getBalance(Wallet.BalanceType.ESTIMATED)));
    }
    
    public static CasinocoinService getDiceService(){
        return diceService;
    }
    
    public void setWalletLocation(String walletLocation) {
        this.walletLocation = walletLocation;
    }

    public String getWalletLocation() {
        return walletLocation;
    }
    
    public static void setBlockHeight(int height){
        blockHeight = height;
    }
    
    public int getBlockHeight(){
        return blockHeight;
    }
    
    public static int getStaticBlockHeight(){
        return blockHeight;
    }

    public static String getServerWalletAddress() {
        return serverWalletAddress;
    }
    
    public int getPeerMaxChainHeight(){
        return diceService.peerGroup().getMostCommonChainHeight();
    }
    
    public static int peerMaxChainHeight(){
        return diceService.peerGroup().getMostCommonChainHeight();
    }
    
    public static String getServerWalletBalance(){
        return Utils.bitcoinValueToFriendlyString(diceService.wallet().getBalance(Wallet.BalanceType.AVAILABLE));
    }
    
    public static void setConfirmations(Integer confirmations) {
        DiceServer.confirmations = confirmations;
    }

    public static Integer getConfirmations() {
        return confirmations;
    }
    
    public static void setServerJackpotAddress(String serverJackpotAddress) {
        DiceServer.serverJackpotAddress = serverJackpotAddress;
    }

    public static String getServerJackpotAddress() {
        return serverJackpotAddress;
    }
    
    public static void setJackpotValue(BigInteger value) {
        jackpotValue = value;
    }
    
    public static BigInteger getJackpotValue() {
        return jackpotValue;
    }
    
    public static String getFormattedJackpotValue(){
        return Utils.bitcoinValueToFriendlyString(jackpotValue);
    }
}
