package life.qbic.userdb.model;

public class Minutes {
  
  private int id;
  private String purpose;
  private String participants;
  private String agenda;
  private String discussion;
  private String results;
  private String nextSteps;
  
  public Minutes(int id, String purpose, String participants, String agenda, String discussion,
      String results, String nextSteps) {
    super();
    this.id = id;
    this.purpose = purpose;
    this.participants = participants;
    this.agenda = agenda;
    this.discussion = discussion;
    this.results = results;
    this.nextSteps = nextSteps;
  }

  public int getId() {
    return id;
  }

  public String getPurpose() {
    return purpose;
  }

  public String getParticipants() {
    return participants;
  }

  public String getAgenda() {
    return agenda;
  }

  public String getDiscussion() {
    return discussion;
  }

  public String getResults() {
    return results;
  }

  public String getNextSteps() {
    return nextSteps;
  }
  
  
}
