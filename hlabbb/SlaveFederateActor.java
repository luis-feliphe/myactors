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
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.jlc.EncodingHelpers;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
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
	Queue<StringToken> myValue = new LinkedList();

	private String timeLog = "";
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
		// robotId.setExpression("");
		robotId = new StringParameter(this, "robot id");
		robotId.setExpression("1");

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

		inid = new TypedIOPort(this, "inid", true, false);
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

			long total = 0;
			for (long i : tempo) {
				total += i;
			}
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
	public TypedIOPort inid;
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
		/*
		 * Calendar data = Calendar.getInstance(); int hora =
		 * data.get(Calendar.HOUR_OF_DAY); int min = data.get(Calendar.MINUTE);
		 * int seg = data.get(Calendar.SECOND); int mseg =
		 * data.get(Calendar.MILLISECOND);
		 */
		// System.out.println("Robô "+ robotId.getValueAsString()
		// +" Time  = "+System.currentTimeMillis());
		// timeLog = String.valueOf(System.currentTimeMillis());
		getTimeMilis();
		// TODO Auto-generated method stub
		try {
			escritor(timeLog);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		 * try { Thread.sleep(10); } catch (InterruptedException e1) { // TODO
		 * Auto-generated catch block e1.printStackTrace(); }
		 */

		if (attributesToSend != null) {
			// System.out.println("Atributos recebidos via HLA");
			String[] v = new String[11];

			StringToken id, battery, temperature, sensor3, gps, compass, gotoM, rotate, activate;
			DoubleToken sensor2;
			DoubleToken sensor1;

			try {

				//System.out.println(" ----- Valores no Slave----- ");
				for (int i = 0; i < v.length; i++) {
					v[i] = EncodingHelpers.decodeString(attributesToSend
							.getReceivedData().getValue(i));
					//System.out.println("Indice: " + i + "  Valor: " + v[i]);
				}
				// System.out.println(" ----------------------------- ");
				// v = value.split(":");
				for (int i = 0; i < v.length; i++) {
					// comentar para testes
					// v[i] = v[i].split(":")[1];
					v[i] = v[i].replace("\"", "");
					v[i] = v[i].replace("\\", "");
					v[i] = v[i].replace(" ", "");
					// v[i] = v[i].replace(";", "");
				}
				id = new StringToken(v[0]);
				// TESTANDO A PARADA DO ID
				if (id.stringValue().contains(robotId.getValueAsString())) {

					battery = new StringToken(v[1]);
					temperature = new StringToken(v[2]);
					sensor1 = new DoubleToken(Double.parseDouble(v[10]));
					sensor2 = new DoubleToken(Double.parseDouble(v[4]));// new
																		// StringToken(v[4]);
					sensor3 = new StringToken(v[9]);
					//
					// System.out.println("Valor do sensor 2= " +
					// sensor2.toString());
					// String position [] = v[5].toString().split(";");
					gps = new StringToken(v[3]);
					//
					compass = new StringToken(v[7]);
					gotoM = new StringToken(v[8]);
					rotate = new StringToken(v[6]);
					activate = new StringToken(v[5]);

					String teste = sensor2.toString().replace("\"", "");
					teste = teste.replace("\"", "");
					teste = teste.replace("\"", "");
					teste = teste.replace("\'", "");
					teste = teste.replace("<", "");
					teste = teste.replace(">", "");

					// IntToken tmpSensor2 = new IntToken
					// (Integer.parseInt(teste));

					if (id.toString().contains(
							robotId.getValueAsString())) {
						outid.send(0, id);
						outbattery.send(0, battery);
						outTemperature.send(0, temperature);

						outSensor1.send(0, sensor1);
						outSensor2.send(0, sensor2);// sensor2));
						// System.out.println("enviando a saida - " + sensor2 );
						outSensor3.send(0, sensor3);
						// outGps.send(0, gps);
						// tratamento do gps para divisão em 3 partes x y e z
						String xyz[] = gps.toString().split(";");

						for (int i = 0; i < xyz.length; i++) {
							xyz[i] = xyz[i].replace("\"", "");
							xyz[i] = xyz[i].replace("\"", "");
							xyz[i] = xyz[i].replace("\"", "");
							xyz[i] = xyz[i].replace("\'", "");
							xyz[i] = xyz[i].replace("<", "");
							xyz[i] = xyz[i].replace(">", "");
							// System.out.println("indice " + i + " = " +
							// xyz[i]);
						}
						// System.out.println(" \t--------");
						if (xyz.length == 3) {
							// tres saidas para o gps
							outGps.send(0, new DoubleToken(xyz[0]));
							outGps.send(1, new DoubleToken(xyz[1]));
							outGps.send(2, new DoubleToken(xyz[2]));
						} else if (xyz.length == 2) {
							System.out.println("entrou no 2");
							outGps.send(0, new DoubleToken(xyz[0]));
							outGps.send(1, new DoubleToken(xyz[1]));
						} else if (xyz.length == 1) {
							// outGps.send(0, new DoubleToken(xyz[0]));
						}
						// ---

						outgoto.send(0, gotoM);
						outRotate.send(0, rotate);
						outActivate.send(0, activate);
					}
				}
			} catch (ArrayIndexOutOfBounds e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}

			attributesToSend = null;

		}// angelo - estava comentado - novo modelo
		boolean temp = false;
		if (input.hasToken(0)) {
			Token id = new StringToken("none");

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

			if (inid.getWidth() > 0&& inid.hasToken(0)) {
				// Otimiza, remove o inputValue
				id = inid.get(0);
			}
			if (inbattery.getWidth() > 0) {
				// Otimiza, remove o inputValue
				battery = inbattery.get(0);
				temp = true;
			}
			if (inTemperature.getWidth() > 0) {
				temperature = inTemperature.get(0);
				temp = true;
			}
			if (inSensor1.getWidth() > 0) {
				sensor1 = inSensor1.get(0);
				temp = true;
			}
			if (inSensor2.getWidth() > 0 && inSensor2.hasToken(0)) {
				sensor2 = inSensor2.get(0);
				temp = true;
			}

			if (inSensor3.getWidth() > 0) {
				sensor3 = inSensor3.get(0);
				temp = true;
			}

			if (inGps.getWidth() > 0) {
				gps = inGps.get(0);
				temp = true;
			}

			if (inCompass.getWidth() > 0) {
				compass = inCompass.get(0);
				temp = true;
			}

			if (ingoto.getWidth() > 0 && ingoto.hasToken(0)) {
				gotoM = ingoto.get(0);
				temp = true;
				System.out.println("I Got Something: " + gotoM.toString());
			}

			if (inRotate.getWidth() > 0) {
				rotate = inRotate.get(0);
				temp = true;
			}
			if (inActivate.getWidth() > 0) {
				activate = inActivate.get(0);
				temp = true;
			}

			StringToken value = new StringToken(id + " - " + battery + " - "
					+ temperature + " - " + sensor1 + " - " + sensor2 + " - "
					+ sensor3 + " - " + gps + " - " + compass + " - " + gotoM
					+ " - " + rotate + " - " + activate);

			if (temp) {
				double timeValue = getDirector().getModelTime()
						.getDoubleValue();
				this.setValue(value);
				this.setTime(timeValue);
				hasDataToSend = true;

				Token inputValue = input.get(0);
			}

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