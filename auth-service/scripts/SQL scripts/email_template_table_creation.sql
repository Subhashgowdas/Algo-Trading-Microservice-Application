
CREATE TABLE email_templates (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTV'
	);

-- Insert default templates
INSERT INTO email_templates (code, subject, body, status) VALUES
('REG_OTP', 'Your Login OTP',
'<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><title>OTP</title></head>
<body>
  <h2>Your One‑Time Password</h2>
  <p>Use the following OTP to complete your registration:</p>
  <h1 th:text="${otp}">123456</h1>
  <p>This code expires in <span th:text="${expiryMinutes}">5</span> minutes.</p>
</body>
</html>', 'ACTV');

INSERT INTO email_templates (code, subject, body, status) VALUES
('RESET_PWD', 'Password Reset Token',
'<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><title>Reset Password</title></head>
<body>
  <h2>Password Reset Request</h2>
  <p>Use the following token to reset your password:</p>
  <h1 th:text="${token}">654321</h1>
  <p>This token expires in <span th:text="${expiryMinutes}">15</span> minutes.</p>
</body>
</html>', 'ACTV');
