/* 
 *   Copyright 2007 The Portico Project 
 * 
 *   This file is part of portico. 
 * 
 *   portico is free software; you can redistribute it and/or modify 
 *   it under the terms of the Common Developer and Distribution License (CDDL)  
 *   as published by Sun Microsystems. For more information see the LICENSE file. 
 *    
 *   Use of this software is strictly AT YOUR OWN RISK!!! 
 *   If something bad happens you do not have permission to come crying to me. 
 *   (that goes for your lawyer as well) 
 * 
 */
package ptolemy.myactors.hlabbb;
/* 
 *   Copyright 2007 The Portico Project 
 * 
 *   This file is part of portico. 
 * 
 *   portico is free software; you can redistribute it and/or modify 
 *   it under the terms of the Common Developer and Distribution License (CDDL)  
 *   as published by Sun Microsystems. For more information see the LICENSE file. 
 *    
 *   Use of this software is strictly AT YOUR OWN RISK!!! 
 *   If something bad happens you do not have permission to come crying to me. 
 *   (that goes for your lawyer as well) 
 * 
 */


import hla.rti.AttributeHandleSet;
import hla.rti.FederatesCurrentlyJoined;
import hla.rti.FederationExecutionAlreadyExists;
import hla.rti.FederationExecutionDoesNotExist;
import hla.rti.LogicalTime;
import hla.rti.RTIambassador;
import hla.rti.RTIexception;
import hla.rti.ResignAction;
import hla.rti.SuppliedAttributes;
import hla.rti.SuppliedParameters;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.util.Calendar;

import javax.management.MBeanServerConnection;
import javax.swing.JOptionPane;

//import org.hyperic.sigar.CpuPerc;
//import org.hyperic.sigar.Mem;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.cmd.SigarCommandBase;

import certi.rti.impl.CertiLogicalTime;
import certi.rti.impl.CertiLogicalTimeInterval;
import certi.rti.impl.CertiRtiAmbassador;

//import com.sun.management.OperatingSystemMXBean;

/**
 * This is an example federate demonstrating how to properly use the HLA 1.3
 * Java interface supplied with Portico.
 * 
 * As it is intended for example purposes, this is a rather simple federate. The
 * process is goes through is as follows:
 * 
 * 1. Create the RTIambassador 2. Try to create the federation (nofail) 3. Join
 * the federation 4. Announce a Synchronization Point (nofail) 5. Wait for the
 * federation to Synchronized on the point 6. Enable Time Regulation and
 * Constrained 7. Publish and Subscribe 8. Register an Object Instance 9. Main
 * Simulation Loop (executes 20 times) 9.1 Update attributes of registered
 * object 9.2 Send an Interaction 9.3 Advance time by 1.0 10. Delete the Object
 * Instance 11. Resign from Federation 12. Try to destroy the federation
 * (nofail)
 * 
 * 
 * NOTE: Those items marked with (nofail) deal with situations where multiple
 * federates may be working in the federation. In this sitaution, the federate
 * will attempt to carry out the tasks defined, but it won't stop or exit if
 * they fail. For example, if another federate has already created the
 * federation, the call to create it again will result in an exception. The
 * example federate expects this and will not fail. NOTE: Between actions 4. and
 * 5., the federate will pause until the uses presses the enter key. This will
 * give other federates a chance to enter the federation and prevent other
 * federates from racing ahead.
 * 
 * 
 * The main method to take notice of is {@link #runFederate(String)}. It
 * controls the main simulation loop and triggers most of the important
 * behaviour. To make the code simpler to read and navigate, many of the
 * important HLA activities are broken down into separate methods. For example,
 * if you want to know how to send an interaction, see the
 * {@link #sendInteraction()} method.
 * 
 * With regard to the FederateAmbassador, it will log all incoming information.
 * Thus, if it receives any reflects or interactions etc... you will be notified
 * of them.
 * 
 * Note that all of the methods throw an RTIexception. This class is the parent
 * of all HLA exceptions. The HLA Java interface is full of exceptions, with
 * only a handful being actually useful. To make matters worse, they're all
 * checked exceptions, so unlike C++, we are forced to handle them by the
 * compiler. This is unnecessary in this small example, so we'll just throw all
 * exceptions out to the main method and handle them there, rather than handling
 * each exception independently as they arise.
 */
public class SlaveFederate extends SigarCommandBase implements PtolemyFederate {
	// ----------------------------------------------------------
	// STATIC VARIABLES
	// ----------------------------------------------------------
	/**
	 * The number of times we will update our attributes and send an interaction
	 */
	public static final int ITERATIONS = 20;

	/** The sync point all federates will sync up on before starting */
	public static final String READY_TO_RUN = "ReadyToRun";

	// ----------------------------------------------------------
	// INSTANCE VARIABLES
	// ----------------------------------------------------------
	private RTIambassador rtiamb;
	private FederateAmbassador fedamb;

	private int objectHandle;

	/*
	 * Quando for executar mais de um slave no mesmo modelo, este nome a seguir
	 * deve ser diferente. E o ultimo slave a ser executado deve receber o nome
	 * ReadyToRun
	 */
	//private static SlaveManager sm = SlaveManager.getInstance();
	

	private static String federateName = "1";

	// ----------------------------------------------------------
	// CONSTRUCTORS
	// ----------------------------------------------------------
	public SlaveFederate() {
		fedamb = new FederateAmbassador();
	}

	// /////////////////////////////////////////////////////////////////////////
	// //////////////////////// Main Simulation Method /////////////////////////
	// /////////////////////////////////////////////////////////////////////////

	/**
	 * This is the main simulation loop. It can be thought of as the main method
	 * of the federate. For a description of the basic flow of this federate,
	 * see the class level comments
	 */
	//private OperatingSystemMXBean osMBean = null;
	private long nanoBefore, cpuBefore;

	public void createFederate(String fedName, String fedFileName)
			throws RTIexception {

		// ///////////////////////////////
		// 1. create the RTIambassador //
		// ///////////////////////////////

		createRTIAmbassador();

		// ////////////////////////////
		// 2. create the federation //
		// ////////////////////////////
		// create
		// NOTE: some other federate may have already created the federation,
		// in that case, we'll just try and join it
		try {
			createFederation(fedFileName);
			log("Created Federation");
		} catch (FederationExecutionAlreadyExists exists) {
			log("Didn't create federation, it already existed");
		} catch (MalformedURLException urle) {
			log("Exception processing fom: " + urle.getMessage());
			urle.printStackTrace();
			return;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// //////////////////////////
		// 3. join the federation //
		// //////////////////////////
		joinFederation(federateName);

		
		// //////////////////////////////
		// 4. announce the sync point //
		// //////////////////////////////
		// announce a sync point to get everyone on the same page. if the point
		// has already been registered, we'll get a callback saying it failed,
		// but we don't care about that, as long as someone registered it

		// Just present in SlaveFederate
		announceSynchronizationPoint();

		// WAIT FOR USER TO KICK US OFF
		// So that there is time to add other federates, we will wait until the
		// user hits enter before proceeding. That was, you have time to start
		// other federates.
		waitForUser();
		
	
		/*
		 * MBeanServerConnection mbsc =
		 * ManagementFactory.getPlatformMBeanServer();
		 * 
		 * try { osMBean = ManagementFactory.newPlatformMXBeanProxy( mbsc,
		 * ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
		 * OperatingSystemMXBean.class); } catch (IOException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); }
		 * 
		 * nanoBefore = System.nanoTime(); cpuBefore =
		 * osMBean.getProcessCpuTime();
		 */

		// Call an expensive task, or sleep if you are monitoring a remote
		// process

		// /////////////////////////////////////////////////////
		// 5. achieve the point and wait for synchronization //
		// /////////////////////////////////////////////////////
		// tell the RTI we are ready to move past the sync point and then wait
		// until the federation has synchronized on
		log("calling achieveSynchronizationPoint()");
		achieveSynchronizationPoint();
		
		
		
		// ///////////////////////////
		// 6. enable time policies //
		// ///////////////////////////
		// in this section we enable/disable all time policies
		// note that this step is optional!
		
		
		
		//comentando pela velocidade
		enableTimePolicy();
		log("Time Policy Enabled");

		// ////////////////////////////
		// 7. publish and subscribe //
		// ////////////////////////////
		// in this section we tell the RTI of all the data we are going to
		// produce, and all the data we want to know about

		// - Removendo para testes
		// publishAndSubscribe();
		publishAndSubscribe();
		log("Published and Subscribed");

		// ///////////////////////////////////
		// 8. register an object to update //
		// ///////////////////////////////////
		objectHandle = registerObject();
		log("Registered Object, handle=" + objectHandle);
	}

	public void sendData(String data) throws RTIexception {
		// 9.1 update the attribute values of the instance //
		// System.out.println("aaaaaaaa"); -ok
		
		
		updateAttributeValues(data);// angelo - era objecthandle
		//  --- Misterios da meia noite
		//advanceTime(1);
		// 9.2 send an interaction
		// sendInteraction(data); //angelo - comentei

	}

	public void finalizeFederate() throws RTIexception {
		// ////////////////////////////////////
		// 10. delete the object we created //
		// ////////////////////////////////////
		deleteObject(objectHandle);
		log("Deleted Object, handle=" + objectHandle);

		// //////////////////////////////////
		// 11. resign from the federation //
		// //////////////////////////////////
		rtiamb.resignFederationExecution(ResignAction.NO_ACTION);
		log("Resigned from Federation");

		// //////////////////////////////////////
		// 12. try and destroy the federation //
		// //////////////////////////////////////
		// NOTE: we won't die if we can't do this because other federates
		// remain. in that case we'll leave it for them to clean up
		try {
			rtiamb.destroyFederationExecution("ExampleFederation");
			log("Destroyed Federation");
		} catch (FederationExecutionDoesNotExist dne) {
			log("No need to destroy federation, it doesn't exist");
		} catch (FederatesCurrentlyJoined fcj) {
			log("Didn't destroy federation, federates still joined");
		}

	}

	// ----------------------------------------------------------
	// INSTANCE METHODS
	// ----------------------------------------------------------
	/**
	 * This is just a helper method to make sure all logging it output in the
	 * same form
	 */
	private void log(String message) {
		System.out.println("SlaveFederate   : " + message);
	}

	/**
	 * This method will block until the user presses enter
	 */
	private void waitForUser() {
		JOptionPane.showMessageDialog(null, "Wait for User...");
	}

	/*
	 * 
	 * angelo - mudando de Interaction para Attributes
	 * 
	 * public Interaction receivedData(double time){ LogicalTime t = new
	 * CertiLogicalTime(time); return fedamb.receivedData(t); }
	 * 
	 * public Interaction consumeReceivedData(double time){ LogicalTime t = new
	 * CertiLogicalTime(time); return fedamb.consumeReceivedData(t); }
	 */

	public Attributes receivedData(double time) {
		LogicalTime t = new CertiLogicalTime(time);
		return fedamb.receivedData(t);
	}

	public Attributes consumeReceivedData(double time) {
		LogicalTime t = new CertiLogicalTime(time);
		return fedamb.consumeReceivedData(t);
	}

	private void createRTIAmbassador() throws RTIexception {
		log("SlaveFederate - createAmbassador()");
		rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
	}

	private void createFederation(String fedFileName) throws RTIexception,
			MalformedURLException {
		log("SlaveFederate - createFederation()");
		File fom = new File(fedFileName);
		rtiamb.createFederationExecution("ExampleFederation", fom.toURI()
				.toURL());
	}

	private void joinFederation(String fedName) throws RTIexception {
		// create the federate ambassador and join the federation
		fedamb = new FederateAmbassador();
		rtiamb.joinFederationExecution(federateName, "ExampleFederation",
				fedamb);
		log("Joined Federation as " + federateName);
	}

	private void announceSynchronizationPoint() throws RTIexception {
		log("SlaveFederate - announceSynPoint()");

		byte[] tag = EncodingHelpers.encodeString("hi!");

		rtiamb.registerFederationSynchronizationPoint(federateName, tag);

		// wait until the point is announced
		while (fedamb.isRegistered == false || fedamb.isAnnounced == false) {
			log("tick () !!");
			((CertiRtiAmbassador) rtiamb).tick2();
		}
	}

	private void achieveSynchronizationPoint() throws RTIexception {
		// Not present in Slave
		 while( fedamb.isAnnounced == false ) {
		 log("isAnnounced = false");
		 ((CertiRtiAmbassador) rtiamb).tick2();
		 }
		//
		rtiamb.synchronizationPointAchieved(READY_TO_RUN);
		log("Achieved sync point: " + READY_TO_RUN
				+ ", waiting for federation...");

		while (fedamb.isReadyToRun == false) {
			log("isReadyToRun == false");
			((CertiRtiAmbassador) rtiamb).tick2();
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// //////////////////////////// Helper Methods
	// //////////////////////////////
	// //////////////////////////////////////////////////////////////////////////
	/**
	 * This method will attempt to enable the various time related properties
	 * for the federate
	 */
	private void enableTimePolicy() throws RTIexception {
		// NOTE: Unfortunately, the LogicalTime/LogicalTimeInterval create code
		// is
		// Portico specific. You will have to alter this if you move to a
		// different RTI implementation. As such, we've isolated it into a
		// method so that any change only needs to happen in a couple of spots
		CertiLogicalTime currentTime = new CertiLogicalTime(fedamb.federateTime);
		CertiLogicalTimeInterval lookahead = new CertiLogicalTimeInterval(
				fedamb.federateLookahead);

		// //////////////////////////
		// enable time regulation //
		// //////////////////////////
		this.rtiamb.enableTimeRegulation(currentTime, lookahead);

		// tick until we get the callback
		while (fedamb.isRegulating == false) {
			((CertiRtiAmbassador) rtiamb).tick2();
		}

		// ///////////////////////////
		// enable time constrained //
		// ///////////////////////////
		this.rtiamb.enableTimeConstrained();

		// tick until we get the callback
		while (fedamb.isConstrained == false) {
			((CertiRtiAmbassador) rtiamb).tick2();
		}
	}

	/**
	 * This method will inform the RTI about the types of data that the federate
	 * will be creating, and the types of data we are interested in hearing
	 * about as other federates produce it.
	 */
	private void publishAndSubscribe() throws RTIexception {
		// //////////////////////////////////////////
		// publish all attributes of ObjectRoot.A //
		// //////////////////////////////////////////
		// before we can register instance of the object class ObjectRoot.A and
		// update the values of the various attributes, we need to tell the RTI
		// that we intend to publish this information

		// get all the handle information for the attributes of ObjectRoot.A

		int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.robot"+ federateName);
		// int classHandle = rtiamb.getObjectClassHandle( "ObjectRoot.string" );
		int idHandle = rtiamb.getAttributeHandle("id", classHandle);
		int batteryHandle = rtiamb.getAttributeHandle("battery", classHandle);
		int temperatureHandle = rtiamb.getAttributeHandle("temperature", classHandle);
		int sensor1Handle = rtiamb.getAttributeHandle("sensor1", classHandle);
		int sensor2Handle = rtiamb.getAttributeHandle("sensor2", classHandle);
		int sensor3Handle = rtiamb.getAttributeHandle("sensor3", classHandle);
		int gpsHandle = rtiamb.getAttributeHandle("gps", classHandle);
		int compassHandle = rtiamb.getAttributeHandle("compass", classHandle);
		int gotoHandle = rtiamb.getAttributeHandle("goto", classHandle);
		int rotateHandle = rtiamb.getAttributeHandle("rotate", classHandle);
		int activateHandle = rtiamb.getAttributeHandle("activate", classHandle);
		// int classHandle = rtiamb.getObjectClassHandle("InteractionRoot.X");
		// int aaHandle = rtiamb.getAttributeHandle("xa", classHandle);

		// package the information into a handle set
		AttributeHandleSet attributes = RtiFactoryFactory.getRtiFactory()
				.createAttributeHandleSet();
		
		//adding handles
		attributes.add(idHandle);

		attributes.add(batteryHandle);
		attributes.add(temperatureHandle);
		attributes.add(sensor1Handle);
		attributes.add(sensor2Handle);
		attributes.add(sensor3Handle);
		attributes.add(gpsHandle);
//		attributes.add(gpsHandle);
		attributes.add(compassHandle);
		attributes.add(gotoHandle);
		attributes.add(rotateHandle);
		attributes.add(activateHandle);

		// do the actual publication
		rtiamb.publishObjectClass(classHandle, attributes);
		// ///////////////////////////////////////////////
		// subscribe to all attributes of ObjectRoot.A //
		// ///////////////////////////////////////////////
		// we also want to hear about the same sort of information as it is
		// created and altered in other federates, so we need to subscribe to it

		rtiamb.subscribeObjectClassAttributes(classHandle, attributes);
		// ///////////////////////////////////////////////////
		// publish the interaction class InteractionRoot.X //
		// ///////////////////////////////////////////////////
		// we want to send interactions of type InteractionRoot.X, so we need
		// to tell the RTI that we're publishing it first. We don't need to
		// inform it of the parameters, only the class, making it much simpler
		//int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.X");

		// do the publication
		//rtiamb.publishInteractionClass(interactionHandle);

		// //////////////////////////////////////////////////
		// subscribe to the InteractionRoot.X interaction //
		// //////////////////////////////////////////////////
		// we also want to receive other interaction of the same type that are
		// sent out by other federates, so we have to subscribe to it first
		//rtiamb.subscribeInteractionClass(interactionHandle);
	}


	/**
	 * This method will register an instance of the class ObjectRoot.A and will
	 * return the federation-wide unique handle for that instance. Later in the
	 * simulation, we will update the attribute values for this instance
	 */
	private int registerObject() throws RTIexception {
		int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.robot"+federateName);
		// int classHandle = rtiamb.getObjectClassHandle("InteractionRoot.X");
		return rtiamb.registerObjectInstance(classHandle);
	}

	/**
	 * This method will update all the values of the given object instance. It
	 * will set each of the values to be a string which is equal to the name of
	 * the attribute plus the current time. eg "aa:10.0" if the time is 10.0.
	 * <p/>
	 * Note that we don't actually have to update all the attributes at once, we
	 * could update them individually, in groups or not at all!
	 */
	private void updateAttributeValues(String data) throws RTIexception {
		/*
		 * This part of code are comented cause Slave do not send data, but after this can change  
		 */

		SuppliedAttributes attributes = RtiFactoryFactory.getRtiFactory()
				.createSuppliedAttributes();

		String[] tokens = data.split(" - ");

		
		/*
		 * Recebendo valores (Utilizar ordem crescente
		 */
		byte[] id = EncodingHelpers.encodeString("id:" + tokens[0]);

		byte[] battery = EncodingHelpers.encodeString("battery:" + tokens[1]);
		byte[] temperature = EncodingHelpers.encodeString("temperature:"+ tokens[2]);
		byte[] sensor1 = EncodingHelpers.encodeString("sensor1:" + tokens[3]);
		byte[] sensor2 = EncodingHelpers.encodeString("sensor2:" + tokens[4]);
		byte[] sensor3 = EncodingHelpers.encodeString("sensor3:" + tokens[5]);
		byte[] gps = EncodingHelpers.encodeString("gps:" + tokens[6]);
		byte[] compass = EncodingHelpers.encodeString("compass:" + tokens[7]);
		byte[] gotom = EncodingHelpers.encodeString("goto:" + tokens[8]);
		byte[] rotate = EncodingHelpers.encodeString("rotate:" + tokens[9]);
		//byte[] activate = EncodingHelpers.encodeString("activate:" + tokens[10]);
		byte[] activate = EncodingHelpers.encodeString(tokens[10]);
		//System.out.println("----- Mostrando valores -------");
		//for (int i = 0 ; i < tokens.length; i++ ){
		//	System.out.println(tokens[i]);
		//}
		//System.out.println(" ----- Fim de valores ---------");
		
		int classHandle = rtiamb.getObjectClass(objectHandle);
/*
 * Adicionando valores recebidos a variavel attributes
 */
		attributes.add(rtiamb.getAttributeHandle("id", classHandle),id);
		attributes.add(rtiamb.getAttributeHandle("battery", classHandle),battery);
		attributes.add(rtiamb.getAttributeHandle("temperature", classHandle),	temperature);
		attributes.add(rtiamb.getAttributeHandle("sensor1", classHandle),sensor1);
		attributes.add(rtiamb.getAttributeHandle("sensor2", classHandle),sensor2);
		attributes.add(rtiamb.getAttributeHandle("sensor3", classHandle),sensor3);
		attributes.add(rtiamb.getAttributeHandle("gps", classHandle), gps);
		attributes.add(rtiamb.getAttributeHandle("compass", classHandle), compass);
		attributes.add(rtiamb.getAttributeHandle("goto", classHandle), gotom);
		attributes.add(rtiamb.getAttributeHandle("rotate", classHandle), rotate);
		//TODO : FEITA ESSA MUGANGA AQUI PRA VER SE ERA AQUI O ERRO
		attributes.add(rtiamb.getAttributeHandle("activate", classHandle),
				activate);

		/*
		 * Enviando via HLA
		 */
		byte[] tag = EncodingHelpers.encodeString("hi!");
		
		// TODO:ESSA LINHA ABAIXO GERA INCONSISTENCIA DOS DADOS (UPDATE ATTRIBUTE VALUES REPTIDO GERA REFLECTEd REPETIDO 
		//rtiamb.updateAttributeValues(objectHandle, attributes, tag);
		CertiLogicalTime time = new CertiLogicalTime(fedamb.federateTime+ fedamb.federateLookahead);
	
		rtiamb.updateAttributeValues(objectHandle, attributes, tag, time);

	}

	/**
	 * This method will request a time advance to the current time, plus the
	 * given timestep. It will then wait until a notification of the time
	 * advance grant has been received.
	 */
	public void advanceTime(double timestep) throws RTIexception {
		// request the advance
		fedamb.isAdvancing = true;
		
		
		
		LogicalTime newTime = new CertiLogicalTime(fedamb.federateTime
				+ timestep);
		rtiamb.timeAdvanceRequest(newTime);

		// syso
		// log( "Time Advanced to " + fedamb.federateTime );

		// wait for the time advance to be granted. ticking will tell the
		// LRC to start delivering callbacks to the federate
		while (fedamb.isAdvancing) {
			((CertiRtiAmbassador) rtiamb).tick();//tick2();
		}
	}

	public void advanceTimeTo(double nextStep) throws RTIexception {
		// request the advance
		fedamb.isAdvancing = true;
		LogicalTime newTime = new CertiLogicalTime(nextStep);
		
		rtiamb.timeAdvanceRequest(newTime);
		
		// syso
		// log( "Time Advanced to " + newTime );

		// wait for the time advance to be granted. ticking will tell the
		// LRC to start delivering callbacks to the federate
		//modificado durante os testes
		while (fedamb.isAdvancing) {
			((CertiRtiAmbassador) rtiamb).tick();//tick2();
		}
	}

	/**
	 * This method will attempt to delete the object instance of the given
	 * handle. We can only delete objects we created, or for which we own the
	 * privilegeToDelete attribute.
	 */
	private void deleteObject(int handle) throws RTIexception {
		byte[] tag = EncodingHelpers.encodeString("hi!");
		rtiamb.deleteObjectInstance(handle, tag); // no tag, we're lazy
	}

	// ----------------------------------------------------------
	// STATIC METHODS
	// ----------------------------------------------------------
	public static void main(String[] args) {
		// get a federate name, use "exampleFederate" as default
		// federateName = "teste 1";
		if (args.length != 0) {
			federateName = args[0];
		}

		try {
			// run the example federate
			new SlaveFederate().createFederate(federateName, "PyhlaToPtolemy.fed");
		} catch (RTIexception rtie) {
			// an exception occurred, just log the information and exit
			rtie.printStackTrace();
		}
	}

	public int getObjectHandle() {
		// TODO Auto-generated method stub
		return this.objectHandle;
	}

	
	@Override
	public double getRTINextTime() {
		// TODO Auto-generated method stub
		return fedamb.federateTime + fedamb.federateLookahead;
	}

	@Override
	public void output(String[] arg0) throws SigarException {
		// TODO Auto-generated method stub

	}

}