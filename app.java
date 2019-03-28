package com.hubwiz.demo;

import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.io.File;
import java.lang.System;

public class App{
  private User user;
  private HFClient client;
  private Channel channel;
  
  public User loadUser(String name,String mspId) throws Exception{
    String mspDir = "../network/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/";
    
    File keystore = new File(mspDir + "keystore");
    File[] keyFiles = keystore.listFiles();
    if(keyFiles.length == 0 ) throw new Exception("no key found");
    String keyFileName = mspDir + "keystore/" + keyFiles[0].getName();
    
    File certstore = new File(mspDir + "signcerts");
    File[] certFiles = certstore.listFiles();
    if(certFiles.length == 0 ) throw new Exception("no cert found");
    String certFileName = mspDir + "signcerts/" + certFiles[0].getName();
    
    this.user = new LocalUser(name,mspId,keyFileName,certFileName);
    return this.user;
  }
  
  public void initChannel() throws Exception{
    if(this.user == null) throw new Exception("user not loaded");
    
    HFClient client = HFClient.createNewInstance();
    client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
    client.setUserContext(this.user);
    
    Channel channel = client.newChannel("ch1");
    Peer peer = client.newPeer("peer1`","grpc://127.0.0.1:7051");
    channel.addPeer(peer);
    Orderer orderer = client.newOrderer("orderer1","grpc://127.0.0.1:7050");
    channel.addOrderer(orderer);
    channel.initialize();
    
    this.channel = channel;
    this.client = client;
  }
 
  public void query(String ccname,String fcn,String...args) throws Exception{
    System.out.format("query %s %s...\n",ccname,fcn);
    
    QueryByChaincodeRequest req = this.client.newQueryProposalRequest();    
    ChaincodeID cid = ChaincodeID.newBuilder().setName(ccname).build();
    req.setChaincodeID(cid);
    req.setFcn(fcn);
    req.setArgs(args);
    
    Collection<ProposalResponse> rspc = channel.queryByChaincode(req);
    
    for(ProposalResponse rsp: rspc){
      System.out.format("status: %d \n",rsp.getStatus().getStatus());
      System.out.format("message: %s\n",rsp.getMessage());
      System.out.format("payload: %s\n",rsp.getProposalResponse().getResponse().getPayload().toStringUtf8());
    }
  }
  
  public void invoke(String ccname,String fcn,String... args) throws Exception{
    System.out.format("invoke %s %s...\n",ccname,fcn);
    
    TransactionProposalRequest req = this.client.newTransactionProposalRequest();   
    ChaincodeID cid = ChaincodeID.newBuilder().setName(ccname).build();    
    req.setChaincodeID(cid);
    req.setFcn(fcn);
    req.setArgs(args);
    
    Collection<ProposalResponse> rspc = channel.sendTransactionProposal(req);
    TransactionEvent event = channel.sendTransaction(rspc).get();

    System.out.format("txid: %s\n", event.getTransactionID());
    System.out.format("valid: %b\n", event.isValid());    
  }
  
  public void start() throws Exception{
    loadUser("admin","Org1MSP");
    initChannel();

    query("wizcc","test");
    invoke("wizcc","test");
  }
  
  public static void main(String[] args) throws Exception{
    System.out.println("wiz dapp");
    new App().start();         
  }
}
