package com.amobear.freevpn.utils

object Constants {
    const val NOTIFICATION_ID = 6
    
    const val TLS_AUTH_KEY_HEX =
        "6acef03f62675b4b1bbd03e53b187727\n" +
        "423cea742242106cb2916a8a4c829756\n" +
        "3d22c7e5cef430b1103c6f66eb1fc5b3\n" +
        "75a672f158e2e2e936c3faa48b035a6d\n" +
        "e17beaac23b5f03b10b868d53d03521d\n" +
        "8ba115059da777a60cbfd7b2c9c57472\n" +
        "78a15b8f6e68a3ef7fd583ec9f398c8b\n" +
        "d4735dab40cbd1e3c62a822e97489186\n" +
        "c30a0b48c7c38ea32ceb056d3fa5a710\n" +
        "e10ccc7a0ddb363b08c3d2777a3395e1\n" +
        "0c0b6080f56309192ab5aacd4b45f55d\n" +
        "a61fc77af39bd81a19218a79762c3386\n" +
        "2df55785075f37d8c71dc8a42097ee43\n" +
        "344739a0dd48d03025b0450cf1fb5e8c\n" +
        "aeb893d9a96d1f15519bb3c4dcb40ee3\n" +
        "16672ea16c012664f8a9f11255518deb\n"
    
    // Local Mac Server CA Certificate (for testing)
    const val LOCAL_VPN_ROOT_CERTS =
        "[[INLINE]]-----BEGIN CERTIFICATE-----\n" +
        "MIIDSzCCAjOgAwIBAgIUGtGS1lE01A59gW8gr66gFcLZ4NcwDQYJKoZIhvcNAQEL\n" +
        "BQAwFjEUMBIGA1UEAwwLRWFzeS1SU0EgQ0EwHhcNMjUxMjA0MTcwNDA2WhcNMzUx\n" +
        "MjAyMTcwNDA2WjAWMRQwEgYDVQQDDAtFYXN5LVJTQSBDQTCCASIwDQYJKoZIhvcN\n" +
        "AQEBBQADggEPADCCAQoCggEBANU8vEd4NI9J94IkH4y0xxiE1HhCoxCGPKdXfr5b\n" +
        "FRDnhAyUO1F2gmqoffRfRfIXeELmzLMcRQl2X/RFL9YwzH8NMhXr6ji5rdIpHuQl\n" +
        "jVKFeDsi3aAczRE9dSwaJ/e22iBwSsXCnXp0cNbBZDDNh7JGzgEKUvbzzrRzYNYt\n" +
        "OfuMxh/DphIUIAovC7SL/ynoDVIsG+09XXqzwAEb59cPkD99KEgNoPJOgfoBltSv\n" +
        "ivSEmu6yFutisLPRr09T3CnemKPitGs0jecdnZlg0+JxHNTRJ1UDX/85haEy1lbD\n" +
        "pLJ3iD+8C7xEUp6Km85jblRPF1BBt08RerJKMfz6W62OTyMCAwEAAaOBkDCBjTAM\n" +
        "BgNVHRMEBTADAQH/MB0GA1UdDgQWBBRlP6EqzfGlL6Iu/vG/6Y186fF5yzBRBgNV\n" +
        "HSMESjBIgBRlP6EqzfGlL6Iu/vG/6Y186fF5y6EapBgwFjEUMBIGA1UEAwwLRWFz\n" +
        "eS1SU0EgQ0GCFBrRktZRNNQOfYFvIK+uoBXC2eDXMAsGA1UdDwQEAwIBBjANBgkq\n" +
        "hkiG9w0BAQsFAAOCAQEAocxloBsEACDGCjGl2m88z07jKGe4JITay6srt8ZJeYgw\n" +
        "TrNF8SqZ9t1PZqLkzHLkqujclvFbUds2oa23vQhRUvUhWjhj0BqWq1kT6Zs48hiK\n" +
        "5/gmLbYGLIlrmvgteyekeTxoOSxP2P6cXiZOYKFqVjWbQ03uLehQB9ctQzS32GRi\n" +
        "zbUioR+pZqOc7cnU5SRW0pVIM+dyymwTXY/vB78VeIP9kwoFPEK9oERWLqCY5v8a\n" +
        "XCRP5N8ntcJ3BBILr2+2sxwxVxBiLwR8UUMHxdU2oXfdYN1rZCzaATV826zDdE7l\n" +
        "3MDsYdcMVRFj95eFkpGmcZ7i+kQgKjVX7INWROEC6g==\n" +
        "-----END CERTIFICATE-----"
    
    // Local Mac Server TLS Auth Key (for testing)
    const val LOCAL_TLS_AUTH_KEY_HEX =
        "e20720812991e4f80faaa07aa7b06046\n" +
        "4e3be9baba0310d9951eb5310c16d886\n" +
        "2fbc657635c8adad61a8162b17cd2bd2\n" +
        "ba773f3ca610f337c9fda277c5d61529\n" +
        "18e2e9fea590835a4feb3137ab50b541\n" +
        "c1e43e95076f30c50661c066e04ebb32\n" +
        "7b198f7a5428f8e57734f3b5d1fc660e\n" +
        "96c957c61d6c7aa03d79711d4a14c83f\n" +
        "4005027586cbd53090a36bd83ec701d4\n" +
        "ff786432a93f985bf5446ef84999f881\n" +
        "cce1adc4fa29f8dd75c94d991fd322e4\n" +
        "c631731e0b72112703d50f9767233f65\n" +
        "b2bbcf661d7624efaebc50e00912ac2d\n" +
        "368b6744dc9a081c83f1df0dc13dc802\n" +
        "a8b53ee58b68ae38e2a22820d0073a3b\n" +
        "c017558762ccaff01469035c1af01d1e\n"
    
    // Local Mac Server Client Certificate (for testing)
    const val LOCAL_CLIENT_CERT =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDXDCCAkSgAwIBAgIQaoZ+F8z27v6/XhtDzLvaaTANBgkqhkiG9w0BAQsFADAW\n" +
        "MRQwEgYDVQQDDAtFYXN5LVJTQSBDQTAeFw0yNTEyMDQxNzA0MTRaFw0yODAzMDgx\n" +
        "NzA0MTRaMBkxFzAVBgNVBAMMDmFuZHJvaWQtY2xpZW50MIIBIjANBgkqhkiG9w0B\n" +
        "AQEFAAOCAQ8AMIIBCgKCAQEAinGmT/DVMIS3ltWqaWqQypRGLuk27nQhGyvd6rhp\n" +
        "pYxgsUppSgiAu/aoype/+ItwIeIJPVB2OFJQicYQFDq0Ud6WzqgiM5DmYWdBQLTF\n" +
        "2lbxxW+l+NSlZMKblTZtkmk5jWbjEh3iMN6qHzX+DzbWSQTwFo+tyCI7HxSEeXxb\n" +
        "9igujPvL9YMVGis0cy/NvdsxREE8DrDU1i/Ls51fOWut3XnbBYQtLGYsVnGKy9bU\n" +
        "J67vKfFXFLTRTRuyQ4ptyEY1nfA0dvyLzT0ZDUL5qqLuR5O/Rl66px3jpjh0KdwO\n" +
        "bh/Cyt8xxik+dIb7vpvCauOQX3+bG+8+wMJYhZBj5g0L4wIDAQABo4GiMIGfMAkG\n" +
        "A1UdEwQCMAAwHQYDVR0OBBYEFOzN/0p2zv+5T3xk6pC0Q9Kvtu/gMFEGA1UdIwRK\n" +
        "MEiAFGU/oSrN8aUvoi7+8b/pjXzp8XnLoRqkGDAWMRQwEgYDVQQDDAtFYXN5LVJT\n" +
        "QSBDQYIUGtGS1lE01A59gW8gr66gFcLZ4NcwEwYDVR0lBAwwCgYIKwYBBQUHAwIw\n" +
        "CwYDVR0PBAQDAgeAMA0GCSqGSIb3DQEBCwUAA4IBAQBXrMa/mGoOhQ9oIhURJ+Fv\n" +
        "8mVhZBYD32udDKLZbhqVD+YiJ+Xwtb41TiPHF9NiGIhtP2b48DrzWnPvFdNI4jjy\n" +
        "KLjTQJCNVdRP/q2dbDlFKVKTI/sRA3sK0mcEYEKRFOtJGdPmH3n1wBkXjLhDl1gK\n" +
        "fyX/XYztv9oqzqa+AlMsKqXN6QOIfyRs9ub99gdkl0kYgO8/096YGPYJf11znmX+\n" +
        "7RXPnH/L81vuHPNG54xA4ddex26i/el70IKk5cGUFPfy6kNwxXQQi1eUWfc5BrkY\n" +
        "A1ToFXM+Mss+lsLgwJvLtf64XL4wckJgBXU5zBOqcwi7wJJ40/rpygFdbgDFrAiU\n" +
        "-----END CERTIFICATE-----"
    
    const val LOCAL_CLIENT_KEY =
        "-----BEGIN PRIVATE KEY-----\n" +
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCKcaZP8NUwhLeW\n" +
        "1appapDKlEYu6TbudCEbK93quGmljGCxSmlKCIC79qjKl7/4i3Ah4gk9UHY4UlCJ\n" +
        "xhAUOrRR3pbOqCIzkOZhZ0FAtMXaVvHFb6X41KVkwpuVNm2SaTmNZuMSHeIw3qof\n" +
        "Nf4PNtZJBPAWj63IIjsfFIR5fFv2KC6M+8v1gxUaKzRzL8292zFEQTwOsNTWL8uz\n" +
        "nV85a63dedsFhC0sZixWcYrL1tQnru8p8VcUtNFNG7JDim3IRjWd8DR2/IvNPRkN\n" +
        "Qvmqou5Hk79GXrqnHeOmOHQp3A5uH8LK3zHGKT50hvu+m8Jq45Bff5sb7z7AwliF\n" +
        "kGPmDQvjAgMBAAECggEAA7GJ2egiDwhtLz1wguTM7zcZCD55d8+U5E52aMEkiUxb\n" +
        "ryZMLiV00ORIM6aXAItpk1MseGATY8uALh6Snz6nCASSAzMzy7jWXmg3QQHKiRJ4\n" +
        "vwO3FWfzYaohQdARCL0ntbJ9c3ud83Un+hYUAgBY+Pf2FuscnDzxHC638vQBEduq\n" +
        "FRJRs+B3D0EgIVzAceYt8cmeha9W1n9lyWqHlvGIFqFjO1M/ijuUQcZk+wsfnnUD\n" +
        "J4rlnkHIfTH3fB+K8ri4vML/zQ5q9Dhy2NBfwndcy5fj7XXwFiCAahmjfGjJGfV+\n" +
        "7VoqnRPaw7Y0rhQaWh1gcgReiDUEDUMNwL6+XTjQwQKBgQDDExvuSi4C6WWIoDF+\n" +
        "u/4TsynlKjYjeluRldvqrwXZJvcZOyIYvkYN3zWn//gIm9RQ1Qdw4lg6ZcDejXxu\n" +
        "HO37x36PMa8pL1wydlVmJx4GggGOo6cMJabMvDmcpN7o81tTSxXOvDB0IORuvWag\n" +
        "Sr+rKznIcaXvnrKOhswGAuyRTQKBgQC1rrh7BXemmYOEJ8kS8lE62tGI+JMmKEnn\n" +
        "Wnz7BwuFK0wVLP9E0ssX9JFx4M8MQinFwAzN0gKUte39P19Yv8LUsO9Zxu38UzI/\n" +
        "0OSDFvl+1KICCk1R5XZkXHwjB8/1kut67PNMolamVuBxkSqHQDI6Jimhczww/uw1\n" +
        "M3gZwpR57wKBgE2LlxrOmiPGK/p307URfhrOqeTTNto7lZL2A4nzMVv1RVzeSNuV\n" +
        "J7vz8xxpgGvLpkbLqD2TfIcaU+UkUUZkcaYY05KQqZSxovDcYW5ONn1XyM6u94S4\n" +
        "ounYGP7P+1yXAMLHok+gN5KyUJxE5jrp++9LK5kmtFPgvhIyO4BYQToxAoGABsZ5\n" +
        "Qr01dRUMfoUtTnOS6+r1HXKHqkieWO5a8tGUcGbo83E2I87onAAW3HRRxFdDT4iV\n" +
        "8ab/SaRvN56BY8Hi9iOTCXoNrfc8THzxyKG2tDAhyomv0HoKLDv2tSe5baMI/RCR\n" +
        "Ei6LpLBgtlt/hoOyW9DIhKjTedBH5sQAmpVTDCkCgYEAjJDz3/BFXze+VpaUR2oh\n" +
        "Y9FLp2edhYB6Hrwo+A41PK/WqX1tKojephKqT86F573GGpUlLhLpifAzDi66AVPj\n" +
        "UO10eaFh7TcOj3AaLAAYUcR1iWhijS0VO5Tzz/LDKsbSkqhCfjOokuCTA4axAZSV\n" +
        "5juLnHCaUi/pihDrPcyWX0U=\n" +
        "-----END PRIVATE KEY-----"
    
    const val VPN_ROOT_CERTS =
        "[[INLINE]]-----BEGIN CERTIFICATE-----\n" +
        "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\n" +
        "A1gTTlpi7A==\n" +
        "-----END CERTIFICATE-----"
}

