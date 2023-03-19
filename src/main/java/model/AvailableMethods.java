package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
public class AvailableMethods {

    private List<AuthenticationMethod> authenticationMethods;

    @Data
    public static class AuthenticationMethod {

        @JsonIgnore
        private String location;
        @JsonIgnore
        private String message;
        private String code;
    }


}
