package org.casinocoin.dice;

import com.google.common.util.concurrent.AbstractIdleService;

import org.casinocoin.core.BlockChain;
import org.casinocoin.core.NetworkParameters;
import org.casinocoin.core.PeerGroup;
import org.casinocoin.core.Wallet;
import org.casinocoin.store.MySQLFullPrunedBlockStore;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import oracle.adf.share.logging.ADFLogger;

import org.casinocoin.core.DownloadListener;
import org.casinocoin.core.ECKey;
import org.casinocoin.core.PeerAddress;
import org.casinocoin.core.PeerEventListener;
import org.casinocoin.net.discovery.DnsDiscovery;
import org.casinocoin.store.BlockStoreException;
import org.casinocoin.store.WalletProtobufSerializer;

public class CasinocoinService extends AbstractIdleService {
    
    private static ADFLogger log = ADFLogger.createADFLogger(CasinocoinService.class);
    
    protected final NetworkParameters params;
    protected volatile BlockChain vChain;
    protected volatile MySQLFullPrunedBlockStore vStore;
    protected volatile Wallet vWallet;
    protected volatile PeerGroup vPeerGroup;
    protected PeerAddress[] peerAddresses;
    protected final String walletLocation = System.getProperty("org.casinocoin.dicehome");
    
    private File directory;
    protected volatile File vWalletFile;
    
    private String datasource;
    private String hostname;
    private String dbname;
    private String username;
    private String password;
    
    protected String userAgent;
    protected String version;
    protected PeerEventListener downloadListener;
    protected boolean useAutoSave = true;
    protected boolean blockingStartup = true;
    protected boolean autoStop = true;
    
    private boolean runWithDatasource = true;
    
    public CasinocoinService(NetworkParameters params, String datasource) {
        this.params = checkNotNull(params);
        this.datasource = checkNotNull(datasource);
        this.runWithDatasource = true;
    }
    
    public CasinocoinService(NetworkParameters params, String hostname, String dbname, String username, String password) {
        this.params = checkNotNull(params);
        this.hostname = checkNotNull(hostname);
        this.dbname = checkNotNull(dbname);
        this.username = checkNotNull(username);
        this.password = checkNotNull(password);
        this.runWithDatasource = false;
    }

    public void setWalletDirectory(String walletDirectory){
        this.directory = new File(walletDirectory);
    }
    
    /** Will only connect to the given addresses. Cannot be called after startup. */
    public CasinocoinService setPeerNodes(PeerAddress... addresses) {
        checkState(state() == State.NEW, "Cannot call after startup");
        this.peerAddresses = addresses;
        return this;
    }
    
    /**
     * If you want to learn about the sync process, you can provide a listener here. For instance, a
     * {@link org.casinocoin.core.DownloadListener} is a good choice.
     */
    public CasinocoinService setDownloadListener(PeerEventListener listener) {
        this.downloadListener = listener;
        return this;
    }
    
    /** If true, will register a shutdown hook to stop the library. Defaults to true. */
    public CasinocoinService setAutoStop(boolean autoStop) {
        this.autoStop = autoStop;
        return this;
    }
    
    public CasinocoinService setBlockingStartup(boolean blockingStartup) {
        this.blockingStartup = blockingStartup;
        return this;
    }
    
    public CasinocoinService setUserAgent(String userAgent, String version) {
        this.userAgent = checkNotNull(userAgent);
        this.version = checkNotNull(version);
        return this;
    }
    
    protected PeerGroup createPeerGroup() throws TimeoutException {
        return new PeerGroup(params, vChain);
    }
    
    /** If true, the wallet will save itself to disk automatically whenever it changes. */
    public CasinocoinService setAutoSave(boolean value) {
        checkState(state() == State.NEW, "Cannot call after startup");
        useAutoSave = value;
        return this;
    }
    
    /**
     * <p>Override this to load all wallet extensions if any are necessary.</p>
     *
     * <p>When this is called, chain(), store(), and peerGroup() will return the created objects, however they are not
     * initialized/started</p>
     */
    protected void addWalletExtensions() throws Exception { }

    @Override
    protected void startUp() throws Exception {
        // Runs in a separate thread.
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                throw new IOException("Could not create named directory.");
            }
        }
        FileInputStream walletStream = null;
        try {
            // open/create the MySQL block store with 10.000 full blocks history
            if(runWithDatasource){
                vStore = new MySQLFullPrunedBlockStore(params, datasource, 10000);
            } else {
                vStore = new MySQLFullPrunedBlockStore(params, 10000, hostname, dbname, username, password);
            }
            vWalletFile = new File(directory, "casino-dice.wallet");
            // boolean shouldReplayWallet = vWalletFile.exists() && !chainFileExists;
            // boolean shouldReplayWallet = vWalletFile.exists();
            // check if store contains no blocks then we need to replay the wallet transactions
            boolean shouldReplayWallet = (vStore.getChainHead().getHeight() <= 0) && vWalletFile.exists();
            vChain = new BlockChain(params, vStore);
            vPeerGroup = createPeerGroup();
            if (this.userAgent != null)
                vPeerGroup.setUserAgent(userAgent, version);
            if (vWalletFile.exists()) {
                walletStream = new FileInputStream(vWalletFile);
                vWallet = new Wallet(params);
                addWalletExtensions(); // All extensions must be present before we deserialize
                new WalletProtobufSerializer().readWallet(WalletProtobufSerializer.parseToProto(walletStream), vWallet);
                if (shouldReplayWallet)
                    vWallet.clearTransactions(0);
            } else {
                vWallet = new Wallet(params);
                vWallet.addKey(new ECKey());
                addWalletExtensions();
                vWallet.saveToFile(vWalletFile);
            }
            if (useAutoSave) {
                vWallet.autosaveToFile(vWalletFile, 1000, TimeUnit.MILLISECONDS, null);
            }
            // Set up peer addresses or discovery first, so if wallet extensions try to broadcast a transaction
            // before we're actually connected the broadcast waits for an appropriate number of connections.
            if (peerAddresses != null) {
                for (PeerAddress addr : peerAddresses) vPeerGroup.addAddress(addr);
                peerAddresses = null;
            } else {
                vPeerGroup.addPeerDiscovery(new DnsDiscovery(params));
            }
            vChain.addWallet(vWallet);
            vPeerGroup.addWallet(vWallet);
            // Now all is ready to start the PeerGroup
            if (blockingStartup) {
                vPeerGroup.startAsync();
                vPeerGroup.awaitRunning();
                // Make sure we shut down cleanly.
                installShutdownHook();
                // TODO: Be able to use the provided download listener when doing a blocking startup.
                final DownloadListener listener = new DownloadListener();
                vPeerGroup.startBlockChainDownload(listener);
                listener.await();
            } else {
                vPeerGroup.startAsync();
                vPeerGroup.addListener(new Service.Listener() {
                    @Override
                    public void running() {
                        final PeerEventListener l = downloadListener == null ? new DownloadListener() : downloadListener;
                        vPeerGroup.startBlockChainDownload(l);
                    }

                    @Override
                    public void failed(State from, Throwable failure) {
                        throw new RuntimeException(failure);
                    }
                }, MoreExecutors.sameThreadExecutor());
            }
            // show peers chain height
            log.info("Peers most common chain height: " + vPeerGroup.getMostCommonChainHeight());
        } catch (BlockStoreException e) {
            throw new IOException(e);
        } finally {
            if (walletStream != null) walletStream.close();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        // Runs in a separate thread.
        vPeerGroup.stopAsync();
        vPeerGroup.awaitTerminated();
        vWallet.saveToFile(vWalletFile);
        vStore.close();

        vPeerGroup = null;
        vWallet = null;
        vStore = null;
        vChain = null;
    }
    
    private void installShutdownHook() {
        if (autoStop) Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override public void run() {
                try {
                    CasinocoinService.this.stopAsync();
                    CasinocoinService.this.awaitTerminated();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    
    public NetworkParameters params() {
        return params;
    }

    public BlockChain chain() {
        checkState(state() == State.STARTING || state() == State.RUNNING, "Cannot call until startup is complete");
        return vChain;
    }

    public MySQLFullPrunedBlockStore store() {
        checkState(state() == State.STARTING || state() == State.RUNNING, "Cannot call until startup is complete");
        return vStore;
    }

    public Wallet wallet() {
        checkState(state() == State.STARTING || state() == State.RUNNING, "Cannot call until startup is complete");
        return vWallet;
    }

    public PeerGroup peerGroup() {
        checkState(state() == State.STARTING || state() == State.RUNNING, "Cannot call until startup is complete");
        return vPeerGroup;
    }

    public File directory() {
        return directory;
    }
    
    public String getWalletLocation(){
        return walletLocation;
    }
    
}
