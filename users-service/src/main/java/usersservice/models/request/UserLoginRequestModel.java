package usersservice.models.request;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserLoginRequestModel {
	private String email;
	private String password;


}
