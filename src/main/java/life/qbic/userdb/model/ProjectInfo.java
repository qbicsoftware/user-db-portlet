package life.qbic.userdb.model;

public class ProjectInfo {

  private String description;
  private String secondaryName;
  private Person investigator;
  private Person contact;
  private Person manager;
  private boolean isPilot;
  private String space;
  private String projectCode;
  private int projectID;

  public ProjectInfo(String space, String code, String description, String secondaryName,
      boolean isPilot, Person investigator, Person contact, Person manager) {
    this.space = space;
    this.projectCode = code;
    this.description = description;
    this.secondaryName = secondaryName;
    this.isPilot = isPilot;
    this.investigator = investigator;
    this.contact = contact;
    this.manager = manager;
  }

  public ProjectInfo(String space, String code, String description, String secondaryName, int id) {
    this.space = space;
    this.projectCode = code;
    this.description = description;
    this.secondaryName = secondaryName;
    this.projectID = id;
    this.isPilot = false;
  }

  public boolean isPilot() {
    return isPilot;
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

  public String getProjectCode() {
    return projectCode;
  }

  public String getSpace() {
    return space;
  }

  public int getProjectID() {
    return projectID;
  }
}

