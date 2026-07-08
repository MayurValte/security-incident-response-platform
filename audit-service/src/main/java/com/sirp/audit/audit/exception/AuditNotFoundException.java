package com.sirp.audit.audit.exception;

import java.util.UUID;

public class AuditNotFoundException extends RuntimeException {

  public AuditNotFoundException(UUID id) {

    super(

        "Audit event not found : " + id

         );

  }

}