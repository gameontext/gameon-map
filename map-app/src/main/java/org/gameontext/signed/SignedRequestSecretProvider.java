package org.gameontext.signed;

public interface SignedRequestSecretProvider {

    String getSecretForId(String userId);

}
