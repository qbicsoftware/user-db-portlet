package life.qbic.userdb.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import life.qbic.datamodel.persons.Affiliation;
import life.qbic.datamodel.persons.RoleAt;

public class Person {
  private int id;
  private String firstName;
  private String lastName;
  private String email;
  private String username;
  private String title;
  private String phone;
  private Map<Integer, RoleAt> affiliationInfo; // ids and roles
  private List<Affiliation> affiliations;

  protected Person(String title, String first, String last, String email) {
    this.title = title;
    this.firstName = first;
    this.lastName = last;
    this.email = email;
    affiliationInfo = new HashMap<>();
    affiliations = new ArrayList<>();
  }

  public String getUsername() {
    return username;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmail() {
    return email;
  }

  public String getTitle() {
    return title;
  }

  public String getPhone() {
    return phone;
  }

  public Map<Integer, RoleAt> getAffiliationInfos() {
    return affiliationInfo;
  }

  public void addAffiliationInfo(int id, String name, String role) {
    affiliationInfo.put(id, new RoleAt(name, role));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Person)) {
      return false;
    }

    Person person = (Person) o;

    if (id != person.id) {
      return false;
    }
    if (!firstName.equals(person.firstName)) {
      return false;
    }
    if (!lastName.equals(person.lastName)) {
      return false;
    }
    if (!email.equals(person.email)) {
      return false;
    }
    if (!Objects.equals(username, person.username)) {
      return false;
    }
    if (!Objects.equals(title, person.title)) {
      return false;
    }
    if (!Objects.equals(phone, person.phone)) {
      return false;
    }
    if (!Objects.equals(affiliationInfo, person.affiliationInfo)) {
      return false;
    }
    return Objects.equals(affiliations, person.affiliations);
  }

  @Override
  public int hashCode() {
    int result = id;
    result = 31 * result + firstName.hashCode();
    result = 31 * result + lastName.hashCode();
    result = 31 * result + email.hashCode();
    result = 31 * result + (username != null ? username.hashCode() : 0);
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (phone != null ? phone.hashCode() : 0);
    result = 31 * result + (affiliationInfo != null ? affiliationInfo.hashCode() : 0);
    result = 31 * result + (affiliations != null ? affiliations.hashCode() : 0);
    return result;
  }

  public void setAffiliationID(int affiID) {
    RoleAt affi = affiliationInfo.get(-1);
    affiliationInfo.remove(-1);
    affiliationInfo.put(affiID, affi);
  }

  public List<Affiliation> getAffiliations() {
    return affiliations;
  }

  protected void setAffiliations(List<Affiliation> affiliations) {
    this.affiliations = affiliations;
  }

  public int getId() { return id; }

  protected void setId(int id) {
    this.id = id;
  }
  protected void setPhone(String phone) {
    this.phone = phone;
  }

  protected void setUsername(String username) {
    this.username = username;
  }

  @Override
  public String toString() {
    return firstName + " " + lastName + " (" + email + ")";
  }

  /**
   * returns a random affiliation with its role for this user
   *
   * @return
   */
  public RoleAt getOneAffiliationWithRole() {
    Random random = new Random();
    List<Integer> keys = new ArrayList<Integer>(affiliationInfo.keySet());
    Integer randomKey = keys.get(random.nextInt(keys.size()));
    return affiliationInfo.get(randomKey);
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public static class PersonBuilder {

    private Person person;

    public Person getPerson() {
      return person;
    }

    public PersonBuilder createPerson(String title, String first, String last, String email) {
      person = new Person(title, first, last, email);
      return this;
    }

    public PersonBuilder withId(int id) {
      person.setId(id);
      return this;
    }

    public PersonBuilder withPhoneNumber(String phoneNumber) {
      person.setPhone(phoneNumber);
      return this;
    }

    public PersonBuilder withUsername(String username) {
      person.setUsername(username);
      return this;
    }

    public PersonBuilder withAffiliations(List<Affiliation> affiliations) {
      person.setAffiliations(affiliations);
      return this;
    }

    public PersonBuilder withRoleAtAffiliation(int affiliationId, String affiliationName, String role) {
      person.addAffiliationInfo(affiliationId, affiliationName, role);
      return this;
    }
  }

}
