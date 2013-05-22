/**
 * @author Dino Martin / Maki Tolentino
 */
package com.volenday.sms;

import com.sun.jersey.spi.resource.Singleton;

@Singleton
public interface ISmsGateway {
	boolean sendSms(String mobileNumber, String challengeCode);
}