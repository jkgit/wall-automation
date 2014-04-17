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

public class Blink {

public static void main(String[] args) throws InterruptedException {

    // create gpio controller
    GpioController gpio = GpioFactory.getInstance();
        
    // provision gpio pin #01 as an output pin and turn off
    GpioPinDigitalOutput outputPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "MyLED", PinState.LOW);
    
    // turn output to LOW/OFF state
    outputPin.low();

    // turn output to HIGH/ON state
    outputPin.high();

    Thread.sleep(10000);

    outputPin.low();

}
}
