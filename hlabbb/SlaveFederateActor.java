/* A class modeling a sensor that transmits location information.

 Copyright (c) 2003-2005 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 PT_COPYRIGHT_VERSION_2
 COPYRIGHTENDKEY

 */
package ptolemy.myactors.hlabbb;

import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.jlc.EncodingHelpers;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.Location;
import ptolemy.kernel.util.NameDuplicationException;

//////////////////////////////////////////////////////////////////////////
////Locator

/**
 * This is a wireless sensor node that reacts to an input event by transmitting
 * an output with the current location of this node and the time of the input.
 * The output is a record token with type {location={double}, time=double}. The
 * location is an array with two doubles representing the X and Y positions of
 * the sensor. The location of the sensor is determined by the _getLocation()
 * protected method, which in this base class returns the location of the icon
 * in the visual editor, which is determined from the _location attribute of the
 * actor. If there is no _location attribute, then an exception is thrown.
 * Derived classes may override this protected method to specify the location in
 * some other way (or in more dimensions).
 * 
 * @author Philip Baldwin, Xiaojun Liu and Edward A. Lee
 * @version $Id: Locator.java,v 1.22 2005/10/27 15:36:09 cxh Exp $
 * @since Ptolemy II 4.0
 * @Pt.ProposedRating Yellow (eal)
 * @Pt.AcceptedRating Red (pjb2e)
 */
public class SlaveFederateActor extends TypedAtomicActor implements
		PtolemyFederateActor {

	private static final long serialVersionUID = 1L;

	// angelo - mudei
	// private IntToken myValue;
	private StringToken myValue;

	private double myTime;

	private double lastSentTime;

	// angelo - mudando de Interaction para Attributes
	// private Interaction interactionToSend = null; //comentei

	private Attributes attributesToSend = null;

	private boolean hasDataToSend = false;

	// angelo
	private boolean hasDataToReceive = false;

	/**
	 * Construct an actor with the specified container and name.
	 * 
	 * @param container
	 *            The container.
	 * @param name
	 *            The name.
	 * @exception IllegalActionException
	 *                If the entity cannot be contained by the proposed
	 *                container.
	 * @exception NameDuplicationException
	 *                If the container already has an actor with this name.
	 */
	public SlaveFederateActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// rtiFederation = new SlaveFederate();

		// Create and configure the parameters.

		// federateName = new StringParameter(this, "federateName");
		// federateName.setExpression("PtolemyFederate");

		// Create and configure the ports.
		input = new TypedIOPort(this, "signal", true, false);

		// variaveis do .fed
		outbattery = new TypedIOPort(this, "battery", false, true);
		outTemperature = new TypedIOPort(this, "temperature", false, true);
		outSensor1 = new TypedIOPort(this, "sensor1", false, true);
		outSensor2 = new TypedIOPort(this, "sensor2", false, true);
		outSensor3 = new TypedIOPort(this, "sensor3", false, true);
		outGps = new TypedIOPort(this, "gps", false, true);
		outCompass = new TypedIOPort(this, "compass", false, true);
		outgoto = new TypedIOPort(this, "goto", false, true);
		outRotate = new TypedIOPort(this, "rotate", false, true);
		outActivate = new TypedIOPort(this, "activate", false, true);

		myValue = new StringToken("");
		myTime = 0;

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	public boolean hasDataToSend() {

		return hasDataToSend;
	}

	public boolean hasDataToReceive() {
		return hasDataToReceive;
	}

	/**
	 * @return the myValue
	 */
	public StringToken getValue() {
		return myValue;
	}

	public StringToken getDataToSend() {
		hasDataToSend = false;
		return myValue;
	}

	/**
	 * @param myValue
	 *            the myValue to set
	 */
	public void setValue(StringToken myValue) {
		this.myValue = myValue;
	}

	/**
	 * @return the myTime
	 */
	public double getTime() {
		return myTime;
	}

	/**
	 * @param myTime
	 *            the myTime to set
	 */
	public void setTime(double myTime) {
		this.myTime = myTime;
	}

	/**
	 * Port that receives a trigger input that causes transmission of location
	 * and time information on the <i>output</i> port.
	 */
	public TypedIOPort input;

	/**
	 * Name of the input channel. This is a string that defaults to
	 * "InputChannel".
	 */
	// public StringParameter federateName;

	/**
	 * Port that transmits the current location and the time of the event on the
	 * <i>input</i> port. This has type {location={double}, time=double}, a
	 * record token.
	 */

	public TypedIOPort outbattery;
	public TypedIOPort outTemperature;
	public TypedIOPort outSensor1;
	public TypedIOPort outSensor2;
	public TypedIOPort outSensor3;
	public TypedIOPort outGps;
	public TypedIOPort outCompass;
	public TypedIOPort outgoto;
	public TypedIOPort outRotate;
	public TypedIOPort outActivate;

	// private SlaveFederate rtiFederation;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	/**
	 * Generate an event on the <i>output</i> port that indicates the current
	 * position and time of the last input on the <i>input</i> port. The value
	 * of the input is ignored.
	 */
	int aux = 0, aux1 = 0;

	public void fire() throws IllegalActionException {
		super.fire();

		try {
			Thread.sleep(10);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// System.out.println("SlaveFederateActor - fire() at " +
		// this.getDirector().getModelTime());

		// angelo - mudar essa variavel - dados recebidos do master
		// angelo - estava comentado - novo modelo
		if (attributesToSend != null) {

			String[] v = new String[10];

			StringToken battery, temperature, sensor1, sensor2, sensor3, gps, compass, gotoM, rotate, activate;

			try {

				System.out.println(" ----- Valores no Slave----- ");
				for (int i = 0; i < v.length; i++) {
					v[i] = EncodingHelpers.decodeString(attributesToSend
							.getReceivedData().getValue(i));
					System.out.println("Indice: " + i + "  Valor: " + v[i]);
				}
				System.out.println(" ----------------------------- ");
				// v = value.split(":");
				for (int i = 0; i < v.length; i++) {
					v[i] = v[i].split(":")[1];
					v[i] = v[i].replace("\"", "");
					v[i] = v[i].replace("\\", "");
					v[i] = v[i].replace(" ", "");
					v[i] = v[i].replace(";", "");
				}

				battery = new StringToken(v[0]);
				temperature = new StringToken(v[1]);
				sensor1 = new StringToken(v[2]);
				sensor2 = new StringToken(v[3]);
				sensor3 = new StringToken(v[4]);
				gps = new StringToken(v[8]);
				compass = new StringToken(v[6]);
				gotoM = new StringToken(v[7]);
				rotate = new StringToken(v[5]);
				activate = new StringToken(v[9]);

				outbattery.send(0, battery);
				outTemperature.send(0, temperature);
				outSensor1.send(0, sensor1);
				outSensor2.send(0, sensor2);
				outSensor3.send(0, sensor3);
				outGps.send(0, gps);
				outgoto.send(0, gotoM);
				outRotate.send(0, rotate);
				outActivate.send(0, activate);

			} catch (ArrayIndexOutOfBounds e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}

			attributesToSend = null;

		}// angelo - estava comentado - novo modelo

		if (input.hasToken(0)) {

			Token inputValue = input.get(0);
			/*
			 * StringToken t = (StringToken) StringToken.convert(inputValue);//
			 * fazendo gambis StringToken string = StringToken.convert(t);
			 * double timeValue = getDirector().getModelTime().getDoubleValue();
			 * 
			 * Parameter finalTime = (Parameter)getAttribute("timeWindow");
			 * double finalT = 1.0 ;
			 * 
			 * 
			 * if(aux1 < finalT){ aux1++; hasDataToSend = false; }else{ aux1 =
			 * 0; this.setValue(new StringToken(""+string));
			 * 
			 * this.setTime(timeValue); hasDataToSend = true; }
			 */
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // protected methods ////

	/**
	 * Return the location of this sensor. In this base class, this is
	 * determined by looking for an attribute with name "_location" and class
	 * Location. Normally, a visual editor such as Vergil will create this icon,
	 * so the location will be determined by the visual editor. Derived classes
	 * can override this method to specify the location in some other way.
	 * 
	 * @return An array identifying the location.
	 * @exception IllegalActionException
	 *                If the location attribute does not exist or cannot be
	 *                evaluated.
	 */
	protected double[] _getLocation() throws IllegalActionException {
		Location locationAttribute = (Location) getAttribute("_location",
				Location.class);

		if (locationAttribute == null) {
			throw new IllegalActionException(this,
					"Cannot find a _location attribute of class Location.");
		}

		return locationAttribute.getLocation();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ptolemy.actor.AtomicActor#initialize()
	 */
	@Override
	public void initialize() throws IllegalActionException {
		// TODO Auto-generated method stub
		super.initialize();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ptolemy.actor.AtomicActor#terminate()
	 */
	@Override
	public void terminate() {
		// TODO Auto-generated method stub
		super.terminate();

	}

	public void addInteractionToSend(Interaction inter) {
		System.out.println("Data received by MasterFederateActor at "
				+ inter.getReceivedTime() + " : " + inter.getReceivedData());
		// interactionToSend = inter; //comentei

	}

	@Override
	public void updateAtributesToSend(Attributes attr) {
		// syso
		// System.out.println("Data received by MasterFederateActor at "+
		// attr.getReceivedTime()+" : "+attr.getReceivedData() );
		attributesToSend = attr;

	}

}