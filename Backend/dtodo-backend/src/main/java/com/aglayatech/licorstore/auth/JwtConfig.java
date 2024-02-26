package com.aglayatech.licorstore.auth;

public class JwtConfig {
	
	public final static String LLAVE_SECRETA = "alguna.clave.secreta.12345678";
	
	public final static String RSA_PRIVATE = "-----BEGIN RSA PRIVATE KEY-----\r\n"
			+ "MIIEpAIBAAKCAQEA9BeWHGx3l6kjOPYCWUMCGSvNfs+qbV3fRK6POP48tCnGhvBF\r\n"
			+ "sQTSn/BVh5imepmaIaNeuF2kdqThiZVNFGMsCvONhY57HxdkLpooXyOL2930g9ta\r\n"
			+ "bR4i+0/bavbCeWyez+a8U++OMOntnrG37fTLZwMuD55KhOboFf47DBvUNlTecMCS\r\n"
			+ "YrW5Na9CzwumloXSvY8gpYLuxIkLRjDcEXoDyLNCbgWZdR+yDzMjIg6SEK5vBeV3\r\n"
			+ "Q94Y3D7ZylJ1xfvh20gI6ppils2CSU4dfqWapDDgt3AWn+3ZDhOpQxquql08AMOA\r\n"
			+ "1kKdD/vJAwKMEhXuHDPgv0EokYoANejzuyafkwIDAQABAoIBABgoNWz+tx9FV/0N\r\n"
			+ "K9JVtWF0WlQQjVdhkPUlKXuEfdF+yO0V3m/UgFvf1zciPmenx/9IQdmROvObAc8x\r\n"
			+ "/XzlC7jRvdHX9gtvrzSFgTVZOUA7STW7gcmG0AN7NcXyQfDudREyTrUn4jSpEiFA\r\n"
			+ "iWl9bpv1TpuxGMLqb1Gt5X4fSvNNZXMB7Rxo7C/8jiSR7Qcm5y/lKBHmiDEZP1gI\r\n"
			+ "9nxI30/EjnPkN+XU/m2pxSN/mn7fnLEW4ICDzCTfX507EhCJZQZ9dtJtDUXvQurj\r\n"
			+ "djF89sAPOfvJjEsgtRQfYwDg6OBudC1a7Wq5XRpIZN5fwpTcX4bmy88+Is4Rga16\r\n"
			+ "HK06kQECgYEA/HdIMKwqonkb7aPeh20HUEUD3O6SM7QN4RZm3JOcKHgA8vDtPynD\r\n"
			+ "cqnwNlpziHUleRW744enO0g+9fj9+1TrY7+jvNOE08rfpJE6FPLtS2TT+513iTHP\r\n"
			+ "YpN2pDuMmYF+r/nGvNxwkcxFLkbGjGa8gHg+6XBQbxAgEtUy+WDOvsECgYEA94JL\r\n"
			+ "7yiwKqwgeJFmXlmSJvLi43IpFvbZFcUNE2n/0QchXoxiAugZN8qckXnSN1+dqPHW\r\n"
			+ "T/NNVlQoITVl32ujkt+g7hPufqdwKUIMkgU9oOjEdibbQ0eOoJmKB5/A2apbKwS7\r\n"
			+ "xJ8CcMtZzixh2hHajr6gKD/JQMyhbnvLmepHh1MCgYBfmQtM4uSCAToCEjsBLNfI\r\n"
			+ "HVoJHo1d5qAay1DH1V0cNdSmKXSspjPMB21eJ4H3+ePmpcQvs/4vBHxSx6XktZMs\r\n"
			+ "gjSOG8oAKKHmvtcvd41DLzrV3BqXaLUOrqgOKD5lIhPGrt4CRbaafq7zhvlH0N8w\r\n"
			+ "BlhMLKtIYuLCPti+dVnUAQKBgQD0sgSQdRF1nqabjGRAcx1jFE3rvzmQUotcUmwS\r\n"
			+ "/YDEDuCWsQNVDFGiLtWLnXyO8iDUngLbGBcleh4SdtA4iho1YpGfzmTDIpZos7vp\r\n"
			+ "wPSk2HsitFehtGT1sdYco3vttdZcOVnJkcq/6MroYkh7Z5tD9IIySJ2XSrdum0U2\r\n"
			+ "7b+5sQKBgQDpRZ+3gLmehJaXGWxP2JTVKadJnnGTa1fbkUmcrbU7ikrCukHEfCc4\r\n"
			+ "JYHVlsN5OmYDI664eOs9FIfXXjijBZXsqATuq0hG4ZoTEx+PwsUTzf/pdxlXfdrw\r\n"
			+ "IP+GWR1jwGQLGQGWL51ZV9qmeCIHIGXgkcXpyZe7zMwXhj0elNmN/A==\r\n"
			+ "-----END RSA PRIVATE KEY-----";
	
	public final static String RSA_PUBLICA = "-----BEGIN PUBLIC KEY-----\r\n"
			+ "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA9BeWHGx3l6kjOPYCWUMC\r\n"
			+ "GSvNfs+qbV3fRK6POP48tCnGhvBFsQTSn/BVh5imepmaIaNeuF2kdqThiZVNFGMs\r\n"
			+ "CvONhY57HxdkLpooXyOL2930g9tabR4i+0/bavbCeWyez+a8U++OMOntnrG37fTL\r\n"
			+ "ZwMuD55KhOboFf47DBvUNlTecMCSYrW5Na9CzwumloXSvY8gpYLuxIkLRjDcEXoD\r\n"
			+ "yLNCbgWZdR+yDzMjIg6SEK5vBeV3Q94Y3D7ZylJ1xfvh20gI6ppils2CSU4dfqWa\r\n"
			+ "pDDgt3AWn+3ZDhOpQxquql08AMOA1kKdD/vJAwKMEhXuHDPgv0EokYoANejzuyaf\r\n"
			+ "kwIDAQAB\r\n"
			+ "-----END PUBLIC KEY-----";

}
