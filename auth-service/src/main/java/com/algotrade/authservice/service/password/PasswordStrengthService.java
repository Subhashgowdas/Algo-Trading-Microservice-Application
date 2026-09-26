package com.algotrade.authservice.service.password;

import org.springframework.stereotype.Service;

import com.algotrade.authservice.service.otp.OtpGenerator;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;

@Service
public class PasswordStrengthService {

    private final OtpGenerator otpGenerator;

	private final Zxcvbn zxcvbn = new Zxcvbn();
	
    PasswordStrengthService(OtpGenerator otpGenerator) {
        this.otpGenerator = otpGenerator;
    }

	public boolean isStrong(String password) {
		if(password == null || password.isBlank()) {
			return false;
		}

		Strength strength = zxcvbn.measure(password);
		return strength.getScore() >= 3;
	}

	public String getFeedback(String password) {
		Strength strenght = zxcvbn.measure(password);
		var feedback = strenght.getFeedback();
		if(feedback != null && feedback.getWarning() != null)
			return feedback.getWarning();

		return "password is too weak";
	}
	
	public static void main(String[] args) {
		PasswordStrengthService pass = new PasswordStrengthService(null);
		System.out.println(pass.getFeedback("1"));
	}

}
