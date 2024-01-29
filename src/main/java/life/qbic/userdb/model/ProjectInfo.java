package life.qbic.userdb.model;

import java.util.Objects;
import life.qbic.userdb.model.Person.PersonBuilder;

public class ProjectInfo {

  private String description;
  private String secondaryName;
  private Person investigator;
  private Person contact;
  private Person manager;
  private String space;
  private String projectCode;
  private int projectID;

  protected ProjectInfo(String space, String code, String description, String secondaryName) {
    this.space = space;
    this.projectCode = code;
    this.description = description;
    this.secondaryName = secondaryName;
  }

  public String getDescription() {
    return description;
  }

  public String getSecondaryName() {
    return secondaryName;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setSecondaryName(String secondaryName) {
    this.secondaryName = secondaryName;
  }

  public Person getInvestigator() {
    return investigator;
  }

  public Person getContact() {
    return contact;
  }

  public Person getManager() {
    return manager;
  }


  public void setManager(Person manager) {
    this.manager = manager;
  }

  public void setContact(Person contact) {
    this.contact = contact;
  }

  public void setInvestigator(Person investigator) {
    this.investigator = investigator;
  }

  @Override
  public String toString() {
    String res = projectCode + " (" + secondaryName + ")\n";
    res += "PI: " + investigator + ", ";
    res += "Ctct: " + contact + ", ";
    res += "Mngr: " + manager;
    return res;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ProjectInfo)) {
      return false;
    }

    ProjectInfo that = (ProjectInfo) o;

    if (projectID != that.projectID) {
      return false;
    }
    if (!Objects.equals(description, that.description)) {
      return false;
    }
    if (!Objects.equals(secondaryName, that.secondaryName)) {
      return false;
    }
    if (!Objects.equals(investigator, that.investigator)) {
      return false;
    }
    if (!Objects.equals(contact, that.contact)) {
      return false;
    }
    if (!Objects.equals(manager, that.manager)) {
      return false;
    }
    if (!Objects.equals(space, that.space)) {
      return false;
    }
    return Objects.equals(projectCode, that.projectCode);
  }

  @Override
  public int hashCode() {
    int result = description != null ? description.hashCode() : 0;
    result = 31 * result + (secondaryName != null ? secondaryName.hashCode() : 0);
    result = 31 * result + (investigator != null ? investigator.hashCode() : 0);
    result = 31 * result + (contact != null ? contact.hashCode() : 0);
    result = 31 * result + (manager != null ? manager.hashCode() : 0);
    result = 31 * result + (space != null ? space.hashCode() : 0);
    result = 31 * result + (projectCode != null ? projectCode.hashCode() : 0);
    result = 31 * result + projectID;
    return result;
  }

  public String getProjectCode() {
    return projectCode;
  }

  public String getSpace() {
    return space;
  }

  public int getProjectID() {
    return projectID;
  }

  public static class ProjectInfoBuilder {

    private ProjectInfo info;

    public ProjectInfo getProjectInfo() {
      return info;
    }

    public ProjectInfoBuilder createProjectInfo(String space, String code, String description, String secondaryName) {
      info = new ProjectInfo(space, code, description, secondaryName);
      return this;
    }

    public ProjectInfoBuilder withPersons(Person investigator, Person contact, Person manager) {
      info.setInvestigator(investigator);
      info.setContact(contact);
      info.setManager(manager);
      return this;
    }

    public ProjectInfoBuilder withId(int id) {
      info.setId(id);
      return this;
    }
}

  private void setId(int id) {
    this.projectID = id;
  }

  }

