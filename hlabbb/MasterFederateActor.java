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

import java.io.BufferedWriter;
import java.util.LinkedList;
import java.util.Queue;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;

//import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

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
public class MasterFederateActor extends TypedAtomicActor implements
		PtolemyFederateActor {

	private static final long serialVersionUID = 1L;

	// angelo - mudei
	// private IntToken myValue;
	// private StringToken myValue;

	Queue<StringToken> myValue = new LinkedList();

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
	public MasterFederateActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		// rtiFederation = new SlaveFederate();

		// Create and configure the parameters.

		// federateName = new StringParameter(this, "federateName");
		// federateName.setExpression("PtolemyFederate");

		// Create and configure the ports.
	

		// Variables of BBB <=> Ptolemy Model

		inbattery = new TypedIOPort(this, "inBattery", true, false);
		inTemperature = new TypedIOPort(this, "inTemperature", true, false);
		inSensor1 = new TypedIOPort(this, "inSensor1", true, false);
		inSensor2 = new TypedIOPort(this, "inSensor2", true, false);
		inSensor3 = new TypedIOPort(this, "inSensor3", true, false);
		inGps = new TypedIOPort(this, "inGps", true, false);
		inCompass = new TypedIOPort(this, "inCompass", true, false);
		ingoto = new TypedIOPort(this, "inGoto", true, false);
		inRotate = new TypedIOPort(this, "inRotate", true, false);
		inActivate = new TypedIOPort(this, "inActivate", true, false);

		// TypeAttribute outputType = new TypeAttribute(output, "type");
		// outputType.setExpression("String");

		// Modificando
		// myValue.add(new StringToken(""));
		myTime = 0;

	}

	// /////////////////////////////////////////////////////////////////
	// // ports and parameters ////

	public boolean hasDataToSend() {
		if (myValue.size() == 0)
			return false;
		else
			return true;

		// return hasDataToSend;
	}

	public boolean hasDataToReceive() {
		return hasDataToReceive;
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
	 * pots used by HLA with BBB
	 */

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
	 * Name of the input channel. This is a string that defaults to
	 * "InputChannel".
	 */
	// public StringParameter federateName;

	/**
	 * Port that transmits the current location and the time of the event on the
	 * <i>input</i> port. This has type {location={double}, time=double}, a
	 * record token.
	 */

	// private SlaveFederate rtiFederation;

	// /////////////////////////////////////////////////////////////////
	// // public methods ////

	public String capturarEstatisticas() {
		try {
			Sigar sigar = new Sigar();

			BufferedWriter arquivo;
			String str = "";
			CpuPerc cpu;
			cpu = sigar.getCpuPerc();
			Mem memory = sigar.getMem();

			CpuPerc cpus[] = sigar.getCpuPercList();

			double totalMemory = (double) memory.getTotal()
					/ (1024 * 1024 * 1024);
			double usedMemory = (double) memory.getUsed()
					/ (1024 * 1024 * 1024); // vira GB - origianl eh bytes
			double freeMemory = (double) memory.getFree()
					/ (1024 * 1024 * 1024);
			double usedMemoryPercent = (double) memory.getUsedPercent();

			Runtime runtime = Runtime.getRuntime();
			double totalMemoryJVM = (double) runtime.totalMemory()
					/ (1024 * 1024); // valor utilizado pela JVM - em bytes
			double freeMemoryJVM = (double) runtime.freeMemory()
					/ (1024 * 1024);
			double maxMemory = (double) runtime.maxMemory() / 1024;

			String cpu_pc = CpuPerc.format(cpu.getUser() + cpu.getSys());
			String cpu_jvm = CpuPerc.format(cpu.getSys());

			String cpu_pc_0 = CpuPerc.format(cpus[0].getUser()
					+ cpus[0].getSys());
			String cpu_jvm_0 = CpuPerc.format(cpus[0].getSys());

			String cpu_pc_1 = CpuPerc.format(cpus[1].getUser()
					+ cpus[1].getSys());
			String cpu_jvm_1 = CpuPerc.format(cpus[1].getSys());

			str = usedMemoryPercent + "% - "
					+ ((totalMemoryJVM - freeMemoryJVM) / totalMemoryJVM) * 100
					+ "% - " + cpu_pc + " - " + cpu_jvm + "% - " + cpu_pc_0
					+ " - " + cpu_jvm_0 + "% - " + cpu_pc_1 + " - " + cpu_jvm_1;

			// arquivo = new BufferedWriter(new
			// FileWriter("config/experimentos_mem_used.txt", true));
			// arquivo.write(str);
			// arquivo.newLine();
			// arquivo.close();

			return str;
		} catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}
	}

	/**
	 * Generate an event on the <i>output</i> port that indicates the current
	 * position and time of the last input on the <i>input</i> port. The value
	 * of the input is ignored.
	 */
	int aux = 0, aux1 = 0;

	public void fire() throws IllegalActionException {
		super.fire();

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

		if (inbattery.getWidth()>0) {
			// Otimiza, remove o inputValue
			battery = inbattery.get(0);
		}
		if (inTemperature.getWidth()>0) {
			temperature = inTemperature.get(0);
		}
		if (inSensor1.getWidth()>0) {
			sensor1 = inSensor1.get(0);
		}
		if (inSensor2.getWidth()>0) {
			sensor2 = inSensor2.get(0);
		}

		if (inSensor3.getWidth()>0) {
			sensor3 = inSensor3.get(0);
		}

		if (inGps.getWidth()>0) {
			gps = inGps.get(0);
		}

		if (inCompass.getWidth()>0) {
			compass = inCompass.get(0);
		}

		if (ingoto.getWidth()>0) {
			gotoM = ingoto.get(0);
		}

		if (inRotate.getWidth()>0) {
			rotate = inRotate.get(0);
		}
		if (inActivate.getWidth()>0) {
			activate = inActivate.get(0);
		}

	
		// Criando string para ser processada pelo Master Federate
		StringToken value = new StringToken(battery + " - " + temperature + " - "
				+ sensor1 + " - " + sensor2 + " - " + sensor3 + " - " + gps
				+ " - " + compass + " - " + gotoM + " - " + rotate + " - "
				+ activate);

		double timeValue = getDirector().getModelTime().getDoubleValue();
		this.setValue(value);
		this.setTime(timeValue);
		hasDataToSend = true;

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
		// System.out.println("Data received by MasterFederateActor " +
		// inter.getReceivedData() + " at "+ inter.getReceivedTime());
		// interactionToSend = inter; //comentei
	}

	@Override
	public void updateAtributesToSend(Attributes attr) {
		// System.out.println("Data received by MasterFederateActor " +
		// inter.getReceivedData() + " at "+ inter.getReceivedTime());
		attributesToSend = attr;

	}

}
