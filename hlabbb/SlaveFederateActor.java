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

import java.io.BufferedWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.jlc.EncodingHelpers;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.DoubleToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.StringParameter;
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
	Queue<StringToken> myValue = new LinkedList<StringToken>();

	private String timeLog = "";
	private double myTime;

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
		// robotId.setExpression("");
		robotId = new StringParameter(this, "robot id");
		robotId.setExpression("1");
		robotIdRecv = new StringParameter(this, "Receive From");
		robotIdRecv.setExpression("1");

		// Create and configure the ports.
		input = new TypedIOPort(this, "signal", true, false);

		// variaveis do .fed
		outid = new TypedIOPort(this, "id", false, true);
		outbattery = new TypedIOPort(this, "battery", false, true);
		outTemperature = new TypedIOPort(this, "temperature", false, true);
		outSensor1 = new TypedIOPort(this, "sensor1", false, true);
		outSensor2 = new TypedIOPort(this, "sensor2", false, true);
		outSensor3 = new TypedIOPort(this, "sensor3", false, true);
		outGps = new TypedIOPort(this, "position", false, true);
		outCompass = new TypedIOPort(this, "compass", false, true);
		outgoto = new TypedIOPort(this, "goto", false, true);
		outRotate = new TypedIOPort(this, "rotate", false, true);
		outActivate = new TypedIOPort(this, "activate", false, true);
		outGps.setMultiport(true);
		myTime = 0;

		inbattery = new TypedIOPort(this, "inBattery", true, false);
		inTemperature = new TypedIOPort(this, "inTemperature", true, false);
		inSensor1 = new TypedIOPort(this, "inSensor1", true, false);
		inSensor2 = new TypedIOPort(this, "inSensor2", true, false);
		inSensor3 = new TypedIOPort(this, "inSensor3", true, false);
		inGps = new TypedIOPort(this, "inPosition", true, false);

		inCompass = new TypedIOPort(this, "inCompass", true, false);
		ingoto = new TypedIOPort(this, "inGoto", true, false);
		inRotate = new TypedIOPort(this, "inRotate", true, false);
		inActivate = new TypedIOPort(this, "inActivate", true, false);

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	public static void escritor(String data) throws IOException {
		File arquivo = new File("timeLog.txt");
		FileWriter fw = new FileWriter(arquivo, true);
		BufferedWriter buffWrite = new BufferedWriter(fw);

		buffWrite.append(data);
		buffWrite.newLine();
		buffWrite.flush();
		buffWrite.close();
	}

	public boolean hasDataToSend() {

		return hasDataToSend;
	}

	public boolean hasDataToReceive() {
		return hasDataToReceive;
	}

	private LinkedList<Long> tempo = new LinkedList<Long>();
	long lastOcurrence = 0;

	public void getTimeMilis() {
		Calendar lCDateTime = Calendar.getInstance();
		if (lastOcurrence == 0) {
			lastOcurrence = lCDateTime.getTimeInMillis();
		} else {
			long temp = lCDateTime.getTimeInMillis();
			tempo.add(temp - lastOcurrence);
			lastOcurrence = temp;

			// System.out.println(" Valor médio - " + total/tempo.size());

		}

	}

	/**
	 * @return the myValue
	 */
	public StringToken getValue() {
		// return myValue;
		return myValue.peek();
	}

	public StringToken getDataToSend() {

		hasDataToSend = false;

		String out = "";
		for (StringToken valor : myValue) {
			out += valor + " ; ";
		}

		myValue.clear();
		return new StringToken(out);

	}

	/**
	 * @param myValue
	 *            the myValue to set
	 */
	public void setValue(StringToken myValue) {
		// this.myValue = myValue;
		this.myValue.add(myValue);
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

	/**
	 * Name of the input channel. This is a string that defaults to
	 * "InputChannel".
	 */
	// public StringParameter federateName;
	public StringParameter robotId;
	public StringParameter robotIdRecv;
	/**
	 * Port that transmits the current location and the time of the event on the
	 * <i>input</i> port. This has type {location={double}, time=double}, a
	 * record token.
	 */
	public TypedIOPort input;
	// Outputs of FED. File
	public TypedIOPort outid;
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
	// inputs of fed file
	public TypedIOPort inbattery;
	public TypedIOPort inTemperature;
	public TypedIOPort inSensor1;
	public TypedIOPort inSensor2;
	public TypedIOPort inSensor3;
	public TypedIOPort inGps;
	public TypedIOPort inCompass;
	public TypedIOPort ingoto;
	public TypedIOPort inRotate;
	public TypedIOPort inActivate;

	/**
	 * Generate an event on the <i>output</i> port that indicates the current
	 * position and time of the last input on the <i>input</i> port. The value
	 * of the input is ignored.
	 */
	int aux = 0, aux1 = 0;

	public void fire() throws IllegalActionException {
		super.fire();

		getTimeMilis();
		try {
			escritor(timeLog);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Verifica se chegaram artigos via HLA
		if (attributesToSend != null) {
			String[] v = new String[11];

			StringToken id, battery, temperature, sensor3, gps, compass, gotoM, rotate, activate;
			DoubleToken sensor1, sensor2;

			try {
				for (int i = 0; i < v.length; i++) {
					v[i] = EncodingHelpers.decodeString(attributesToSend
							.getReceivedData().getValue(i));
				}

				for (int i = 0; i < v.length; i++) {
					v[i] = v[i].replace("\"", "");
					v[i] = v[i].replace("\\", "");
					v[i] = v[i].replace(" ", "");
				}
				id = new StringToken(v[0]);

				if (id.stringValue()
						.replace("\"", "")
						.replace("\"", "")
						.contains(
								robotIdRecv.getValueAsString()
										.replace("\"", "").replace("\"", ""))) {

					// iniciando variaveis nos locais adequados
					battery = new StringToken(v[1]);
					temperature = new StringToken(v[2]);
					sensor2 = new DoubleToken(Double.parseDouble(v[4].replace(
							";", "")));
					sensor1 = new DoubleToken(Double.parseDouble(v[10].replace(
							";", "")));
					sensor3 = new StringToken(v[9]);
					gps = new StringToken(v[3]);
					compass = new StringToken(v[7]);
					gotoM = new StringToken(v[8]);
					rotate = new StringToken(v[6]);
					activate = new StringToken(v[5]);
					// System.out.println(robotIdRecv.getValueAsString()+":"+id.stringValue()+" "+
					// robotIdRecv.getValueAsString().equals(id.stringValue()));
					// if
					// (robotIdRecv.getValueAsString().equals(id.stringValue()))
					// {
					System.out.println("enviando dados as portas"+ id.toString()+ " bateria : " + battery.stringValue() + " sensor 3: "+ sensor3.stringValue());
					// enviando dados as portas de saida do ator
					outid.send(0, id);
					outbattery.send(0, battery);
					outTemperature.send(0, temperature);
					outSensor1.send(0, sensor1);
					outSensor2.send(0, sensor2);
					outSensor3.send(0, sensor3);
					outgoto.send(0, gotoM);
					outCompass.send(0, compass);
					outRotate.send(0, rotate);
					outActivate.send(0, activate);

					String xyz[] = gps.toString().split(";");

					// Limpando caracteres
					for (int i = 0; i < xyz.length; i++) {
						xyz[i] = xyz[i].replace("\"", "");
						xyz[i] = xyz[i].replace("\"", "");
						xyz[i] = xyz[i].replace("\"", "");
						xyz[i] = xyz[i].replace("\'", "");
						xyz[i] = xyz[i].replace("<", "");
						xyz[i] = xyz[i].replace(">", "");
					}

					// envia dados a porta de posicao (x, y , z)
					if (xyz.length == 3) {
						outGps.send(0, new DoubleToken(xyz[0]));
						outGps.send(1, new DoubleToken(xyz[1]));
						outGps.send(2, new DoubleToken(xyz[2]));
					} else if (xyz.length == 2) {
						outGps.send(0, new DoubleToken(xyz[0]));
						outGps.send(1, new DoubleToken(xyz[1]));
					} else if (xyz.length == 1) {
						outGps.send(0, new DoubleToken(xyz[0]));
					}

				}

			} catch (ArrayIndexOutOfBounds e) {
				e.printStackTrace();
			}

			attributesToSend = null;

		}

		if (input.hasToken(0)) {

			Token battery = new StringToken("none");
			Token temperature = new StringToken("none");
			Token sensor1 = new StringToken("none");
			Token sensor2 = new StringToken("none");
			Token sensor3 = new StringToken("none");
			Token gps = new StringToken("none");
			Token compass = new StringToken("none");
			Token gotoM = new StringToken("none");
			Token rotate = new StringToken("none");
			Token activate = new StringToken("none");

			if (checkDataReceived(inbattery)) {
				battery = inbattery.get(0);
			}
			if (checkDataReceived(inTemperature)) {
				temperature = inTemperature.get(0);
			}
			if (checkDataReceived(inSensor1)) {
				sensor1 = inSensor1.get(0);
			}
			if (checkDataReceived(inSensor2)) {
				sensor2 = inSensor2.get(0);
			}

			if (checkDataReceived(inSensor3)) {
				sensor3 = inSensor3.get(0);
			}

			if (checkDataReceived(inGps)) {
				gps = inGps.get(0);
			}

			if (checkDataReceived(inCompass)) {
				compass = inCompass.get(0);
			}

			if (checkDataReceived(ingoto)) {
				gotoM = ingoto.get(0);
			}

			if (checkDataReceived(inRotate)) {
				rotate = inRotate.get(0);
			}
			if (checkDataReceived(inActivate)) {
				activate = inActivate.get(0);
			}

			// Verificar se não vai dar erro
			StringToken value = new StringToken(robotId.getValueAsString()
					+ " - " + battery + " - " + temperature + " - " + sensor1
					+ " - " + sensor2 + " - " + sensor3 + " - " + gps + " - "
					+ compass + " - " + gotoM + " - " + rotate + " - "
					+ activate);

			if (!gotoM.toString().contains("none")) {
				double timeValue = getDirector().getModelTime()
						.getDoubleValue();
				this.setValue(value);
				this.setTime(timeValue);
				hasDataToSend = true;

			}
			input.get(0);
		}
	}

	// /////////////////////////////////////////////////////////////////
	// // protected methods ////

	public boolean checkDataReceived(TypedIOPort porta)
			throws IllegalActionException {
		return ((porta.getWidth() > 0) && porta.hasToken(0));
	}

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

	@Override
	public void initialize() throws IllegalActionException {
		super.initialize();

	}

	@Override
	public void terminate() {

		super.terminate();

	}

	public void addInteractionToSend(Interaction inter) {
		System.out.println("Data received by MasterFederateActor at "
				+ inter.getReceivedTime() + " : " + inter.getReceivedData());

	}

	@Override
	public void updateAtributesToSend(Attributes attr) {

		attributesToSend = attr;

	}

}