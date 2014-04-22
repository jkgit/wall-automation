/**************************************************************************/
/*
/* Irexec.java -- Part of the org.lirc.test package
/* Copyright (C) 2001 Bjorn Bringert (bjorn@mumblebee.com)
/*
/* This program is free software; you can redistribute it and/or
/* modify it under the terms of the GNU General Public License
/* as published by the Free Software Foundation; either version 2
/* of the License, or (at your option) any later version.
/*
/* This program is distributed in the hope that it will be useful,
/* but WITHOUT ANY WARRANTY; without even the implied warranty of
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
/* GNU General Public License for more details.
/*
/* You should have received a copy of the GNU General Public License
/* along with this program; if not, write to the Free Software
/* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
/*
/**************************************************************************/

package com.kraly.homeAutomation;

import org.lirc.*;
import org.lirc.util.*;

import java.net.*;
import java.io.*;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

/** Executes commands from remote contol button presses.
	Similar in function to the <code>irexec</code> program.

	@version $Revision: 1.1 $
	@author Bjorn Bringert (bjorn@mumblebee.com)
*/
public class WallController {

	private SimpleLIRCClient client;
	private SocketListener socket;
	private GpioPinDigitalOutput upPin;
	private GpioPinDigitalOutput downPin;
	private WallOperation runningOperation;

	public WallController(File configFile) throws LIRCException, IOException, FileNotFoundException {
		System.out.println("Getting gpio controller...");
		// create gpio controller
		GpioController gpio = GpioFactory.getInstance();
	
		System.out.println("Provisioning up pin...");	
		// provision gpio pin #00 as an output pin and turn off
		upPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "MyUpPin", PinState.LOW);
		System.out.println("Provisioning down pin...");	
		// provision gpio pin #01 as an output pin and turn off
		downPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "MyDownPin", PinState.LOW);

		System.out.println("Connecting to lirc daemon...");
		// connect to the lircd and add ourselves as a listener
		client = new SimpleLIRCClient("wallcontrol", configFile);

		System.out.println("Adding remote listener...");
		client.addIRActionListener(new RemoteListener());
		
		System.out.println("Listening on port 5000...");
		// open a udp socket to accept tcp-ip requests (could do this on port 80? to provide api?)
		socket = new SocketListener();

		System.out.println("Finished initialization of WallController");
	}

	/** Exits this program. */
	public void quit(){
		client.stopListening();
		System.exit(0);
	}

	private class SocketListener {
		byte[] receive_data = new byte[1024];
         	byte[] send_data = new byte[1024];
         
         	int recv_port;
         
         	DatagramSocket server_socket = new DatagramSocket(5000);
         
         	System.out.println ("UDPServer Waiting for client on port 5000");
              
         	while(true) {
          		DatagramPacket receive_packet = new DatagramPacket(receive_data,
                                              receive_data.length);
                                              
                  	server_socket.receive(receive_packet);
                  
                  	String command = new String(receive_packet.getData(),0,0
                                           ,receive_packet.getLength());
                  
                  	InetAddress IPAddress = receive_packet.getAddress();
                  	recv_port = receive_packet.getPort();
                  
                  	System.out.println("Received:" + command);
			new Throwable().printStackTrace();
			if (command.equals("UP")) {
				startOperation(upPin);
			} else if (command.equals("DOWN")) {
				startOperation(downPin);
			}
			System.out.println("Finished:" + command);
      		}
	}
	
	private class RemoteListener implements IRActionListener{
		public void action(String command){
			System.out.println("Received:" + command);
			new Throwable().printStackTrace();
			if (command.equals("UP")) {
				startOperation(upPin);
			} else if (command.equals("DOWN")) {
				startOperation(downPin);
			}
			System.out.println("Finished:" + command);
		}
	}

	public void startOperation(GpioPinDigitalOutput pin) {
		boolean cancelStart = false;
		if (runningOperation!=null) {
			System.out.println("There is a currently running operation.");
			if (runningOperation.getPin().equals(pin)) {
				// if the running operation is the same as the operation that was just started, cancel the current operation
				// don't do anything.
				System.out.println("The currently running operation is the same as the requested operation, canceling operation and not starting new operation.");
				cancelStart=true;
			}
			// the requested pin is different from the pin of the running operation so we need to cancel the running operation
			// the cancel method will not return until the operation is canceled
			// once it is canceled, sleep for a couple seconds, and then start the new operation

			try {
				runningOperation.cancel();
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// not sure why this thread would be interrupted.  maybe IO error?  in any case, something weird is going on
				// so lets not do anything just in case
				cancelStart=true;
			}
		}

		// don't start a new operation if we couldn't cancel the running operation or the logic above decided we shouldn't start a new
		// operation
		if (runningOperation==null && !cancelStart) {
			WallOperation operation = new WallOperation(this, pin);
			operation.go();
			runningOperation=operation;
		}
	}

	protected void operationFinished(WallOperation finishedOperation) {
		// make sure that the thread that thinks it is finished is
		// the same thread that we think is running.  if it isn't
		// then something is very messed up and we should not do 
		// anything
		if (finishedOperation==runningOperation) {
			System.out.println("Operation is finished");
			runningOperation=null;
		}
	}

	public static void main(String[] args) {
		try {
			File config = new File(System.getProperty("user.home"), ".lircrc");
			System.out.println("Creating WallController");
			WallController p = new WallController(config);
			
			System.out.println("Looping until break");
			while(true) { Thread.currentThread().sleep(1000); } // loop until Ctrl-C
		} catch (LIRCException ex) {
			System.err.println(ex.toString());
		} catch (IOException ex) {
			System.err.println(ex.toString());
		} catch (InterruptedException ex) {
			System.err.println(ex.toString());
		}
	}

}
