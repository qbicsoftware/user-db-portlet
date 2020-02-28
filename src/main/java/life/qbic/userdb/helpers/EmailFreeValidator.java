
package life.qbic.userdb.helpers;

import java.util.Set;
import java.util.stream.Collectors;
import com.vaadin.data.Validator;

public class EmailFreeValidator implements Validator {

  Set<String> emailAddresses;

  public EmailFreeValidator(Set<String> emailAddresses) {
    this.emailAddresses =
        emailAddresses.stream().map(String::toLowerCase).collect(Collectors.toSet());
  }

  @Override
  public void validate(Object value) throws InvalidValueException {
    String val = (String) value;
    if (emailAddresses.contains(val.toLowerCase())) {
      throw new InvalidValueException("A person with this e-mail address already exists.");
    }
  }
}
