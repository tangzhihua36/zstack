package org.zstack.header.identity.role.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIAddPolicyStatementsToRoleEvent extends APIEvent {
    public APIAddPolicyStatementsToRoleEvent() {
    }

    public APIAddPolicyStatementsToRoleEvent(String apiId) {
        super(apiId);
    }
}
