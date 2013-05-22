/**
 * @author Dino Martin / Maki Tolentino
 */
package com.volenday.sms.ws;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import com.volenday.sms.ISmsGateway;
import com.volenday.sms.SmsGateway;

@Path("/ws")
public class TwoFactorAuthService {
    public static final int CHALLENGE_CODE_LENGTH = 6;
    public static final int EXPIRY_IN_MINUTES = 1;

    private ISmsGateway sms = SmsGateway.getInstance();
    
    @GET
    @Path("/{mobileNumber}/{challengeCode}/")
    public String sendChallenge(@PathParam("mobileNumber") String mobileNumber, @PathParam("challengeCode") String challengeCode ) {
    	sms = new SmsGateway();
        if (sms.sendSms(mobileNumber, challengeCode)) {
            return "SUCCESS";
        }
        return "FAILED";
    }
    
    @GET
    public String showHelloWorld() {
        return "Hello";
    }   

}
