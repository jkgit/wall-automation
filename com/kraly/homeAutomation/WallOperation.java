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

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/** Executes commands from remote contol button presses.
	Similar in function to the <code>irexec</code> program.

	@version $Revision: 1.1 $
	@author Bjorn Bringert (bjorn@mumblebee.com)
*/
public class WallOperation implements Runnable{
	GpioPinDigitalOutput pin = null;
	WallController controller = null;
	Thread t = null;
	boolean cancel=false;

	public WallOperation(WallController controller, GpioPinDigitalOutput pin) {
		this.pin=pin;
		this.controller=controller;
	}
	
	public void go() {
		t = new Thread(this);
		t.start();
	}

	public void stop() {
		t.interrupt();
	}

	public void waitUntilFinished() throws InterruptedException {
		t.join();
	}

	public void cancel() throws InterruptedException{
		System.out.println("Cancel request received, setting flag.");
		cancel=true;
		t.interrupt();
		// join the running operation thread so that this parent thread waits for it to finish.  this ensures that we don't send
		// conflicting requests
		waitUntilFinished();
	}

	public void run() {
		System.out.println("Operation starting, moving "+pin.getName()+" to high");
		pin.high();
		for (int i=0; i<60; i++) {
			if (cancel) {
				System.out.println("Cancel flag detected, breaking out of loop.");
				break;
			}	
			try {
 		                Thread.sleep(1000);
       	        	} catch (InterruptedException e) {
				System.out.println("Operation sleep interrupted.  Forcing cancel.  This may be redundant if the cancel method woke us up.");
				cancel=true;
                	}
		}
		System.out.println("Operation finished, moving "+pin.getName()+" to low");
                pin.low();	
		controller.operationFinished(this);
	}

	public String getOperationName() {
		return pin.getName();	
	}

	public GpioPinDigitalOutput getPin() {
		return pin;
	}
}
