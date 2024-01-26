/*******************************************************************************
 * QBiC User DB Tools enables users to add people and affiliations to our mysql user database.
 * Copyright (C) 2016 Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.userdb;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import life.qbic.datamodel.persons.Affiliation;
import life.qbic.datamodel.persons.CollaboratorWithResponsibility;
import life.qbic.datamodel.persons.PersonAffiliationConnectionInfo;
import life.qbic.datamodel.persons.RoleAt;
import life.qbic.userdb.model.Minutes;
import life.qbic.userdb.model.Person;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DBManager {
  private Config config;

  Logger logger = LogManager.getLogger(DBManager.class);

  public DBManager(Config config) {
    this.config = config;
  }

  private void logout(Connection conn) {
    try {
      conn.close();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private String prepareStringInput(String input) {
    return input.trim();
  }

  @Deprecated
  private void printAffiliations() {
    String sql = "SELECT * FROM organizations";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        logger.info(Integer.toString(rs.getInt(1)) + " " + rs.getString(2) + " " + rs.getString(3)
            + " " + rs.getString(4) + " " + rs.getString(5) + " " + rs.getString(6) + " "
            + rs.getString(7) + " " + rs.getString(8) + " " + rs.getString(9) + " "
            + rs.getString(10) + " " + rs.getString(11));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
  }

  public String getProjectName(String projectIdentifier) {
    projectIdentifier = prepareStringInput(projectIdentifier);
    String sql = "SELECT short_title from projects WHERE openbis_project_identifier = ?";
    String res = "";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setString(1, projectIdentifier);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        res = rs.getString(1);
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  @Deprecated
  private void removePersonFromAllProjects(int userID) {
    logger.info("Trying to remove all project associations of user with ID " + userID);
    String sql = "DELETE FROM projects_persons WHERE person_id = ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, userID);
      statement.execute();
      logger.info("Successful.");
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
  }

  @Deprecated
  private void removePersonFromAllAffiliationRoles(int userID) {
    logger.info("Trying to remove all affiliation associations of user with ID " + userID);
    String sql = "DELETE FROM persons_organizations WHERE person_id = ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, userID);
      statement.execute();
      logger.info("Successful.");
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(null, statement);
    }
    logger.info("Trying to remove user from special affiliation roles");
    sql = "UPDATE organizations SET head=NULL WHERE head = ?";
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, userID);
      statement.execute();
      logger.info("Successful for head.");
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(null, statement);
    }

    sql = "UPDATE organizations SET main_contact=NULL WHERE main_contact = ?";
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, userID);
      statement.execute();
      logger.info("Successful for main contact.");
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
  }

  private Connection login() {
    String DB_URL = "jdbc:mariadb://" + config.getHostname() + ":" + config.getPort() + "/"
        + config.getSql_database();
    Connection conn = null;
    try {
      Class.forName("org.mariadb.jdbc.Driver");
      conn = DriverManager.getConnection(DB_URL, config.getUsername(), config.getPassword());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return conn;
  }

  public void addOrChangeSecondaryNameForProject(int projectID, String secondaryName) {
    secondaryName = prepareStringInput(secondaryName);
    logger.info(
        "Adding/Updating secondary name of project with id " + projectID + " to " + secondaryName);
    boolean saved = saveOldSecondaryNameForProjects(projectID);
    if (!saved)
      logger.warn("Could not save old project description to database!");
    String sql = "UPDATE projects SET short_title = ? WHERE id = ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setString(1, secondaryName);
      statement.setInt(2, projectID);
      statement.execute();
      logger.info("Successful.");
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
  }

  private boolean saveOldSecondaryNameForProjects(int id) {
    String sql = "SELECT * from projects WHERE id = ?";
    String oldDescription = "";
    String oldTitle = "";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, id);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        oldDescription = rs.getString("long_description");
        oldTitle = rs.getString("short_title");
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    }
    Date date = new java.util.Date();
    Timestamp timestamp = new Timestamp(date.getTime());
    sql =
        "INSERT INTO projects_history (project_id, timestamp, long_description, short_title) VALUES(?, ?, ?, ?)";
    statement = null;
    int res = -1;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, id);
      statement.setTimestamp(2, timestamp);
      statement.setString(3, oldDescription);
      statement.setString(4, oldTitle);
      statement.execute();
      res = statement.getUpdateCount();
      logger.info("Successful.");
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res != -1;
  }

  public int addMinutes(Minutes m) {
    logger.info("Adding minutes information.");
    String sql = "INSERT into project_minutes values (? ? ? ? ? ?)";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setString(1, m.getPurpose());
      statement.setString(2, m.getParticipants());
      statement.setString(3, m.getAgenda());
      statement.setString(4, m.getDiscussion());
      statement.setString(5, m.getResults());
      statement.setString(6, m.getNextSteps());
      statement.execute();
      ResultSet rs = statement.getGeneratedKeys();
      if (rs.next()) {
        logout(conn);
        logger.info("Successful.");
        return rs.getInt(1);
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return -1;
  }

  public Minutes getMinutesByID(int id) {
    logger.info("Looking for project minutes with id " + id + ".");
    String sql = "SELECT * from project_minutes WHERE id = ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, id);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        return new Minutes(id, rs.getString("purpose"), rs.getString("participants"),
            rs.getString("agenda"), rs.getString("discussion"), rs.getString("results"),
            rs.getString("next_steps"));
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return null;
  }

  public List<String> getPossibleEnumsForColumnsInTable(String table, String column) {
    String sql = "desc " + table + " " + column;
    Connection conn = login();
    List<String> res = new ArrayList<String>();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        for (String s : rs.getString(2).replace("enum('", "").replace("')", "").split("','"))
          res.add(s);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  public List<String> getPossibleSetOptionsForColumnsInTable(String table, String column) {
    String sql = "desc " + table + " " + column;
    Connection conn = login();
    List<String> res = new ArrayList<String>();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        for (String s : rs.getString(2).replace("set('", "").replace("')", "").split("','"))
          res.add(s);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  public boolean isProjectInDB(String projectIdentifier) {
    projectIdentifier = prepareStringInput(projectIdentifier);
    logger.info("Looking for project " + projectIdentifier + " in the DB");
    String sql = "SELECT * from projects WHERE openbis_project_identifier = ?";
    boolean res = false;
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setString(1, projectIdentifier);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        res = true;
        logger.info("project found!");
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  public int addProjectToDB(String projectIdentifier, String projectName) {
    projectIdentifier = prepareStringInput(projectIdentifier);
    projectName = prepareStringInput(projectName);
    if (!isProjectInDB(projectIdentifier)) {
      logger.info("Trying to add project " + projectIdentifier + " to the person DB");
      String sql = "INSERT INTO projects (openbis_project_identifier, short_title) VALUES(?, ?)";
      Connection conn = login();
      PreparedStatement statement = null;
      try {
        statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        statement.setString(1, projectIdentifier);
        statement.setString(2, projectName);
        statement.execute();
        ResultSet rs = statement.getGeneratedKeys();
        if (rs.next()) {
          logout(conn);
          logger.info("Successful.");
          return rs.getInt(1);
        }
      } catch (SQLException e) {
        logger.error("SQL operation unsuccessful: " + e.getMessage());
        e.printStackTrace();
      } finally {
        endQuery(conn, statement);
      }
      return -1;
    }
    return -1;
  }

  public boolean hasPersonRoleInProject(int personID, int projectID, String role) {
    role = prepareStringInput(role);
    logger.info("Checking if person already has this role in the project.");
    String sql =
        "SELECT * from projects_persons WHERE person_id = ? AND project_id = ? and project_role = ?";
    boolean res = false;
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, personID);
      statement.setInt(2, projectID);
      statement.setString(3, role);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        res = true;
        logger.info("person already has this role!");
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  public void addOrUpdatePersonToProject(int projectID, int personID, String role) {
    role = prepareStringInput(role);
    if (!hasPersonRoleInProject(personID, projectID, role)) {
      logger.info("Trying to add person with role " + role + " to a project.");
      if (!roleForProjectTaken(projectID, role)) {
        logger.info("Role " + role + " is not yet taken.");
        String sql =
            "INSERT INTO projects_persons (project_id, person_id, project_role) VALUES(?, ?, ?)";
        Connection conn = login();
        PreparedStatement statement = null;
        try {
          statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          statement.setInt(1, projectID);
          statement.setInt(2, personID);
          statement.setString(3, role);
          statement.execute();
          logger.info("Successful.");
        } catch (SQLException e) {
          logger.error("SQL operation unsuccessful: " + e.getMessage());
          e.printStackTrace();
        } finally {
          endQuery(conn, statement);
        }
      } else {
        logger.info("Role " + role + " is taken. Updating to new person.");
        String sql =
            "UPDATE projects_persons SET person_id = ? WHERE project_id = ? AND project_role = ?;";
        Connection conn = login();
        PreparedStatement statement = null;
        try {
          statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          statement.setInt(1, personID);
          statement.setInt(2, projectID);
          statement.setString(3, role);
          statement.execute();
          logger.info("Successful.");
        } catch (SQLException e) {
          logger.error("SQL operation unsuccessful: " + e.getMessage());
          e.printStackTrace();
        } finally {
          endQuery(conn, statement);
        }
      }
    }
  }

  private boolean roleForProjectTaken(int projectID, String role) {
    role = prepareStringInput(role);
    boolean res = false;
    String sql = "SELECT person_ID FROM projects_persons WHERE project_id = ? AND project_role = ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, projectID);
      statement.setString(2, role);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        res = true;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  private boolean roleForExperimentTaken(int experimentID, String role) {
    boolean res = false;
    String sql =
        "SELECT person_ID FROM experiments_persons WHERE experiment_id = ? AND experiment_role = ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, experimentID);
      statement.setString(2, role);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        res = true;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  public void printPeople() {
    String sql = "SELECT * FROM project_investigators";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        logger.info(Integer.toString(rs.getInt(1)) + " " + rs.getString(2) + " " + rs.getString(3)
            + " " + rs.getString(4) + " " + rs.getString(5) + " " + rs.getString(6) + " "
            + rs.getString(7) + " " + rs.getString(8) + " " + rs.getString(9) + " "
            + rs.getString(10) + " " + rs.getString(11));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
  }

  public void printProjects() {
    String sql = "SELECT pi_id, project_code FROM projects";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        int pi_id = rs.getInt("pi_id");
        String first = rs.getString("project_code");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
  }

  @Deprecated
  public Map<String, Integer> getAffiliationMap() {
    Map<String, Integer> res = new HashMap<String, Integer>();
    String sql = "SELECT * FROM organizations";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        int id = rs.getInt("id");
        String groupName = rs.getString("group_name");
        String acronym = rs.getString("group_acronym");
        String institute = rs.getString("institute");
        String organization = rs.getString("umbrella_organization");

        String resName = "";
        boolean group = !(groupName == null || groupName.isEmpty());
        boolean acr = !(acronym == null || acronym.isEmpty());
        boolean org = !(institute == null || institute.isEmpty());

        // no group
        if (!group) {
          // no acronym
          if (!acr) {
            // no org
            if (!org) {
              resName = "unknown";
            } else {
              resName = institute;
            }
            // acronym
          } else {
            // no org
            if (!org) {
              resName = acronym;
            } else {
              resName = acronym + " - " + institute;
            }
          }
          // group
        } else {
          // no acronym
          if (!acr) {
            // no org
            if (!org) {
              resName = groupName;
            } else {
              resName = groupName + " - " + institute;
            }
            // acronym
          } else {
            // no org
            if (!org) {
              resName = groupName + " (" + acronym + ")";
            } else {
              resName = groupName + " (" + acronym + ") - " + institute;
            }
          }
        }
        if (resName.isEmpty() || resName == null) {
          resName = organization;
        }
        res.put(resName, id);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  @Deprecated
  public int addNewAffiliation(Affiliation affiliation) {
    int res = -1;
    logger.info("Trying to add new affiliation to the DB");
    // TODO empty values are inserted as empty strings, ok?
    String insert =
        "INSERT INTO organizations (group_name,group_acronym,umbrella_organization,institute,faculty,street,zip_code,"
            + "city,country,webpage";
    String values = "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?";
    if (affiliation.getContactPersonID() > 0) {
      insert += ",main_contact";
      values += ", ?";
    }
    if (affiliation.getHeadID() > 0) {
      insert += ",head";
      values += ", ?";
    }
    String sql = insert + ") " + values + ")";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      statement.setString(1, affiliation.getGroupName());
      statement.setString(2, affiliation.getAcronym());
      statement.setString(3, affiliation.getOrganization());
      statement.setString(4, affiliation.getInstitute());
      statement.setString(5, affiliation.getFaculty());
      statement.setString(6, affiliation.getStreet());
      statement.setString(7, affiliation.getZipCode());
      statement.setString(8, affiliation.getCity());
      statement.setString(9, affiliation.getCountry());
      statement.setString(10, affiliation.getWebpage());
      int offset = 0;
      if (affiliation.getContactPersonID() > 0) {
        statement.setInt(11, affiliation.getContactPersonID());
        offset++;
      }
      if (affiliation.getHeadID() > 0)
        statement.setInt(11 + offset, affiliation.getHeadID());
      statement.execute();
      ResultSet rs = statement.getGeneratedKeys();
      if (rs.next()) {
        logout(conn);
        logger.info("Successful.");
        res = rs.getInt(1);
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  private String getFreeDummyUserName() {
    List<Person> existing = getPeopleWithDummyUserNames();
    int last = 0;
    for (Person p : existing) {
      String user = p.getUsername();
      try {
        int num = Integer.parseInt(user.substring(4));
        last = Math.max(last, num);
      } catch (NumberFormatException e) {
        logger.warn("Could not parse number from dummy username " + user);
      }
    }
    return "todo" + Integer.toString(last + 1);

  }

  public List<Person> getPeopleWithDummyUserNames() {
    String dummy = "todo";
    List<Person> existing = new ArrayList<Person>();
    String sql = "SELECT * FROM persons WHERE username LIKE ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setString(1, dummy + "%");
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String username = rs.getString("username");
        String title = rs.getString("title");
        String first = rs.getString("first_name");
        String last = rs.getString("family_name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        existing.add(new Person(username, title, first, last, email, phone, -1, "", ""));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return existing;
  }

  public boolean addNewPerson(Person person) {
    logger.info("Trying to add new person to the DB");
    // TODO empty values are inserted as empty strings, ok?
    boolean res = false;
    String sql =
        "INSERT INTO persons (username,title,first_name,family_name,email,phone,active) VALUES(?, ?, ?, ?, ?, ?, ?)";
    Connection conn = login();
    int person_id = -1;
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      String user = person.getUsername();
      if (user == null || user.isEmpty())
        user = getFreeDummyUserName();
      statement.setString(1, user);
      statement.setString(2, person.getTitle());
      statement.setString(3, person.getFirstName());
      statement.setString(4, person.getLastName());
      statement.setString(5, person.getEmail());
      statement.setString(6, person.getPhone());
      statement.setBoolean(7, true);
      statement.execute();
      ResultSet answer = statement.getGeneratedKeys();
      answer.next();
      person_id = answer.getInt(1);
      res = true;
      logger.info("Successfully added person to db.");
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      if (statement != null)
        endQuery(null, statement);
    }
    if (person_id > 0) {
      Map<Integer, RoleAt> affiliationInfos = person.getAffiliationInfos();
      for (int affiliation_id : affiliationInfos.keySet()) {
        sql =
            "INSERT INTO persons_organizations (person_id, organization_id, occupation) VALUES(?, ?, ?)";
        try {
          statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          statement.setInt(1, person_id);
          statement.setInt(2, affiliation_id);
          statement.setString(3, affiliationInfos.get(affiliation_id).getRole());
          statement.execute();
          logger.info("Successfully added person affiliation information to db.");
        } catch (SQLException e) {
          res = false;
          logger.error("SQL operation unsuccessful: " + e.getMessage());
          e.printStackTrace();
        } finally {
          endQuery(null, statement);
        }
      }
    }
    if (conn != null)
      logout(conn);
    return res;
  }

  public Map<String, Person> getPersonMap() {
    Map<String, Person> res = new HashMap<>();
    String sql = "SELECT * FROM person";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        int id = rs.getInt("id");
        String first = rs.getString("first_name");
        String last = rs.getString("last_name");
        String userID = rs.getString("user_id");
        String title = rs.getString("title");
        String email = rs.getString("email");
        res.put(first + " " + last, new Person(id, title, first, last, email, userID));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  public Map<String, Integer> getColsMaxLengthsForTable(String table) throws SQLException {
    Connection conn = login();
    DatabaseMetaData md = conn.getMetaData();
    Map<String, Integer> res = new HashMap<>();
    ResultSet rs = md.getColumns(null, null, table, null);
    while (rs.next()) {
      String cName = rs.getString("COLUMN_NAME");
      int length = rs.getInt("COLUMN_SIZE");
      res.put(cName, new Integer(length));
    }
    logout(conn);
    return res;
  }

  public void printTableNames() throws SQLException {
    Connection conn = login();
    DatabaseMetaData md = conn.getMetaData();
    ResultSet rs = md.getTables(null, null, "%", null);
    while (rs.next()) {
      String table = rs.getString(3);
      logger.info(table);
      String sql = "SELECT * FROM " + table;
      PreparedStatement statement = null;
      try {
        statement = conn.prepareStatement(sql);
        ResultSet r = statement.executeQuery();
        ResultSetMetaData metaData = r.getMetaData();
        int count = metaData.getColumnCount(); // number of column
        for (int i = 1; i <= count; i++) {
          logger.info("col: " + metaData.getColumnLabel(i));
        }
      } catch (Exception e) {
        // TODO: handle exception
      } finally {
        if (statement != null)
          statement.close();
      }
    }
    logout(conn);
  }

  @Deprecated
  public boolean addOrUpdatePersonAffiliationConnections(int personID,
      List<PersonAffiliationConnectionInfo> newConnections) {
    Connection conn = login();
    boolean res = false;
    String sql = "";
    PreparedStatement statement = null;
    List<Integer> knownAffiliationIDs = getPersonAffiliationIDs(personID);
    for (PersonAffiliationConnectionInfo data : newConnections) {
      int affiID = data.getAffiliation_id();
      String role = data.getRole();
      logger.debug("Trying to add user " + personID + " (" + role + ") to affiliation " + affiID);
      if (knownAffiliationIDs.contains(affiID)) {
        logger.debug("user is part of that affiliation, updating role");
        sql =
            "UPDATE persons_organizations SET occupation = ? WHERE person_id = ? and organization_id = ?";
        try {
          statement = conn.prepareStatement(sql);
          statement.setInt(2, personID);
          statement.setInt(3, affiID);
          statement.setString(1, role);
          statement.execute();
          logger.info("Successful.");
          res = true;
        } catch (SQLException e) {
          logger.error("SQL operation unsuccessful: " + e.getMessage());
          e.printStackTrace();
        } finally {
          // we updated this connection and can remove the id
          knownAffiliationIDs.remove(new Integer(affiID));
        }
      } else {
        sql =
            "INSERT INTO persons_organizations (person_id, organization_id, occupation) VALUES (?,?,?)";
        try {
          logger.debug("user is not part of that affiliation yet, inserting");
          statement = conn.prepareStatement(sql);
          statement.setInt(1, personID);
          statement.setInt(2, affiID);
          statement.setString(3, role);
          statement.execute();
          logger.info("Successful.");
          res = true;
        } catch (SQLException e) {
          logger.error("SQL operation unsuccessful: " + e.getMessage());
          e.printStackTrace();
        } finally {
          // we added this connection and can remove the id
          knownAffiliationIDs.remove(new Integer(affiID));
        }
      }
    }
    // any ID still in the list must be from an affiliation connection that the user removed in
    // the UI, thus we remove it from the DB
    for (int oldAffiID : knownAffiliationIDs) {
      sql = "DELETE FROM persons_organizations WHERE person_id = ? and organization_id = ?";
      try {
        logger.debug("user affiliation id " + oldAffiID
            + " found in DB, but not in updated connections, deleting this connection");
        statement = conn.prepareStatement(sql);
        statement.setInt(1, personID);
        statement.setInt(2, oldAffiID);
        statement.execute();
        logger.info("Successful.");
        res = true;
      } catch (SQLException e) {
        logger.error("SQL operation unsuccessful: " + e.getMessage());
        e.printStackTrace();
      }
    }
    endQuery(conn, statement);
    return res;
  }

  public List<Integer> getPersonAffiliationIDs(int person_id) {
    List<Integer> res = new ArrayList<Integer>();
    String sql = "SELECT * FROM person_affiliation WHERE person_id = ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, person_id);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        res.add(rs.getInt("affiliation_id"));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  @Deprecated
  public List<Person> getPersonTable() {
    List<Person> res = new ArrayList<Person>();
    String lnk = "persons_organizations";
    String sql =
        "SELECT persons.*, organizations.id, organizations.group_name, organizations.group_acronym, "
            + lnk + ".occupation FROM persons, organizations, " + lnk + " WHERE persons.id = " + lnk
            + ".person_id and organizations.id = " + lnk + ".organization_id";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String title = rs.getString("title");
        String first = rs.getString("first_name");
        String last = rs.getString("family_name");
        String eMail = rs.getString("email");
        String phone = rs.getString("phone");
        int affiliationID = rs.getInt("organizations.id");
        String affiliation =
            rs.getString("group_name") + " (" + rs.getString("group_acronym") + ")";
        String role = rs.getString(lnk + ".occupation");
        res.add(new Person(username, title, first, last, eMail, phone, affiliationID, affiliation,
            role)); // TODO add every affiliation!
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  @Deprecated
  public Set<String> getInstituteNames() {
    Set<String> res = new HashSet<String>();
    String sql = "SELECT institute FROM organizations";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String inst = rs.getString("institute");
        if (inst != null)
          res.add(inst);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  @Deprecated
  public Affiliation getOrganizationInfosFromInstitute(String institute) {
    institute = prepareStringInput(institute);
    Affiliation res = null;
    String sql = "SELECT * FROM organizations WHERE institute LIKE ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setString(1, institute + "%");
      ResultSet rs = statement.executeQuery();
      Set<String> streets = new HashSet<String>();
      Set<String> orgs = new HashSet<String>();
      Set<String> faculties = new HashSet<String>();
      Set<String> zips = new HashSet<String>();
      Set<String> cities = new HashSet<String>();
      Set<String> countries = new HashSet<String>();
      while (rs.next()) {
        faculties.add(rs.getString("faculty"));
        orgs.add(rs.getString("umbrella_organization"));
        streets.add(rs.getString("street"));
        zips.add(rs.getString("zip_code"));
        countries.add(rs.getString("country"));
        cities.add(rs.getString("city"));
        institute = rs.getString("institute");
      }
      String street = "";
      String faculty = "";
      String organization = "";
      String zipCode = "";
      String city = "";
      String country = "";
      if (streets.size() == 1)
        street = streets.iterator().next();
      if (orgs.size() == 1)
        organization = orgs.iterator().next();
      if (faculties.size() == 1)
        faculty = faculties.iterator().next();
      if (countries.size() == 1)
        country = countries.iterator().next();
      if (zips.size() == 1)
        zipCode = zips.iterator().next();
      if (cities.size() == 1)
        city = cities.iterator().next();
      res = new Affiliation(-1, "", "", organization, institute, faculty, "", "", street, zipCode,
          city, country, "");
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  @Deprecated
  public Affiliation getOrganizationInfosFromOrg(String organization) {
    organization = prepareStringInput(organization);
    Affiliation res = null, maybe = null;
    String sql = "SELECT * FROM organizations WHERE umbrella_organization LIKE ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setString(1, organization + "%");
      ResultSet rs = statement.executeQuery();
      String street = "";
      while (rs.next()) {
        String faculty = rs.getString("faculty");
        String institute = rs.getString("institute");
        if (!street.isEmpty() && !street.equals(rs.getString("street"))) {
          street = "";
          break;
        } else
          street = rs.getString("street");
        organization = rs.getString("umbrella_organization");
        String zipCode = rs.getString("zip_code");
        String city = rs.getString("city");
        String country = rs.getString("country");
        maybe = new Affiliation(-1, "", "", organization, institute, faculty, "", "", street,
            zipCode, city, country, "");
      }
      if (!street.isEmpty())
        res = maybe;
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  @Deprecated
  public List<Affiliation> getAffiliationTable() {
    List<Affiliation> res = new ArrayList<Affiliation>();
    String sql = "SELECT * from organizations";
    // String sql =
    // "SELECT organizations.*, persons.first_name, persons.family_name FROM organizations, persons"
    // + " WHERE persons.id = main_contact";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        int id = rs.getInt("id");
        String groupName = rs.getString("group_name");
        String acronym = rs.getString("group_acronym");
        if (acronym == null)
          acronym = "";
        String organization = rs.getString("umbrella_organization");
        String faculty = rs.getString("faculty");
        String institute = rs.getString("institute");
        if (institute == null)
          institute = "";
        String street = rs.getString("street");
        String zipCode = rs.getString("zip_code");
        String city = rs.getString("city");
        String country = rs.getString("country");
        String webpage = rs.getString("webpage");
        int contactID = rs.getInt("main_contact");
        int headID = rs.getInt("head");
        String contact = null;
        String head = null;
        if (contactID > 0) {
          Person c = getPerson(contactID);
          contact = c.getFirstName() + " " + c.getLastName();
        }
        if (headID > 0) {
          Person h = getPerson(headID);
          head = h.getFirstName() + " " + h.getLastName();
        }
        res.add(new Affiliation(id, groupName, acronym, organization, institute, faculty, contact,
            head, street, zipCode, city, country, webpage));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  public String getInvestigatorForProject(String projectIdentifier) {
    projectIdentifier = prepareStringInput(projectIdentifier);
    String details = getPersonDetailsForProject(projectIdentifier, "PI");
    return details.split("\n")[0].trim();
  }

  /**
   *
   * @param personID
   * @return AffiliationID, forwarded to getAffiliationWithID
   */
  public int getAffiliationIDForPersonID(Integer personID) {
    String lnk = "person_affiliation";
    String sql = "SELECT person.*, affiliation.* FROM person, affiliation, " + lnk
        + " WHERE person.id = " + Integer.toString(personID) + " AND person.id = " + lnk
        + ".person_id and affiliation.id = " + lnk + ".affiliation_id";
    Connection conn = login();

    int affiliationID = -1;

    try (PreparedStatement statement = conn.prepareStatement(sql)) {
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        affiliationID = rs.getInt("affiliation.id");

      }
      statement.close();
    } catch (SQLException e) {
      logger.error("Could not get affiliation ID", e);
    } finally {
      logout(conn);
    }

    return affiliationID;
  }

  public String getPersonDetailsForProject(String projectIdentifier, String role) {
    projectIdentifier = prepareStringInput(projectIdentifier);
    role = prepareStringInput(role);
    String sql =
        "SELECT projects_persons.*, projects.* FROM projects_persons, projects WHERE projects.openbis_project_identifier = ?"
            + " AND projects.id = projects_persons.project_id AND projects_persons.project_role = ?";

    int id = -1;

    List<Person> personWithAffiliations = new ArrayList<Person>();

    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setString(1, projectIdentifier);
      statement.setString(2, role);

      ResultSet rs = statement.executeQuery();

      while (rs.next()) {
        id = rs.getInt("person_id");
      }
      personWithAffiliations = getPersonWithAffiliations(id);
    } catch (SQLException e) {
      e.printStackTrace();
      logout(conn);
      // LOGGER.debug("Project not associated with Investigator. PI will be set to 'Unknown'");
    } finally {
      endQuery(conn, statement);
    }

    String details = "";
    if (personWithAffiliations.size() > 0) {
      Person p = personWithAffiliations.get(0);
      String institute = p.getOneAffiliationWithRole().getAffiliation();

      details = String.format("%s %s \n%s \n \n%s \n%s \n", p.getFirstName(), p.getLastName(),
          institute, p.getPhone(), p.getEmail());
      // TODO is address important?
    }
    return details;
  }

  /**
   * returns multiple person objects if they have multiple affiliations
   * 
   * @param personID
   * @return
   */
  public List<Person> getPersonWithAffiliations(Integer personID) {
    List<Person> res = new ArrayList<Person>();
    String lnk = "person_affiliation";
    String sql =
        "SELECT person.*, affiliation.id, affiliation.organization FROM person, affiliation, " + lnk
            + " WHERE person.id = " + Integer.toString(personID) + " AND person.id = " + lnk
            + ".person_id AND affiliation.id = " + lnk + ".affiliation_id";
    System.out.println(sql);
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      List<Affiliation> affiliations = new ArrayList<Affiliation>();
      while (rs.next()) {
        affiliations.add(getAffiliationWithID(rs.getInt("affiliation.id")));
        String username = rs.getString("user_id");
        String title = rs.getString("title");
        String first = rs.getString("first_name");
        String last = rs.getString("last_name");
        String eMail = rs.getString("email");
        int affiliationID = rs.getInt("affiliation.id");
        String affiliation = rs.getString("organization");
        // set phone number empty due to new table
        res.add(new Person(username, title, first, last, eMail, "", affiliationID, affiliation,
            "Member", affiliations));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  private Affiliation getAffiliationWithID(int id) {
    Affiliation res = null;
    String sql = "SELECT * from affiliation WHERE id = ?";

    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, id);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String groupName = rs.getString("organization");
        String acronym = "";
        String organization = rs.getString("address_addition");
        if (organization == null) {
          organization = "";
        }
        String faculty = rs.getString("category");
        String institute = "";
        String street = rs.getString("street");
        String zipCode = rs.getString("postal_code");
        String city = rs.getString("city");
        String country = rs.getString("country");
        String webpage = "";
        String contact = null;
        String head = null;
        res = new Affiliation(id, groupName, acronym, organization, institute, faculty, contact,
            head, street, zipCode, city, country, webpage);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  public Person getPerson(int id) {
    Person res = null;
    String sql = "SELECT * FROM person WHERE person.id = ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, id);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String username = rs.getString("user_id");
        String title = rs.getString("title");
        String first = rs.getString("first_name");
        String last = rs.getString("last_name");
        String eMail = rs.getString("email");
        res = new Person(username, title, first, last, eMail, "", -1, null, null);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  private void endQuery(Connection c, PreparedStatement p) {
    if (p != null)
      try {
        p.close();
      } catch (Exception e) {
        logger.error("PreparedStatement close problem");
      }
    if (c != null)
      try {
        logout(c);
      } catch (Exception e) {
        logger.error("Database Connection close problem");
      }
  }

  @Deprecated
  public void setAffiliationVIP(int affi, int person, String role) {
    role = prepareStringInput(role);
    logger.info("Trying to set/change affiliation-specific role " + role);
    String sql = "UPDATE organizations SET " + role + "=? WHERE id = ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, person);
      statement.setInt(2, affi);
      statement.execute();
      logger.info("Successful for " + role);
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(null, statement);
    }
  }

  public List<Person> getPersonsByName(String one, String two) {
    one = prepareStringInput(one);
    two = prepareStringInput(two);
    List<Person> res = new ArrayList<Person>();

    String sql = "SELECT * from person where (first_name LIKE ? AND last_name LIKE ?) OR "
        + "(last_name LIKE ? AND first_name LIKE ?)";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setString(1, "%" + one + "%");
      statement.setString(2, "%" + two + "%");
      statement.setString(3, "%" + one + "%");
      statement.setString(4, "%" + two + "%");
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        int id = rs.getInt("id");
        List<Person> found = getPersonWithAffiliations(id);
        if (found.isEmpty()) {
          String username = rs.getString("user_id");
          String title = rs.getString("title");
          String first = rs.getString("first_name");
          String last = rs.getString("last_name");
          String eMail = rs.getString("email");
          res.add(new Person(username, title, first, last, eMail, "", -1, "N/A", "N/A"));
        } else
          res.add(found.get(0));// TODO set all of them!
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  public List<Person> getPersonsContaining(String personQuery) {
    List<Person> res = new ArrayList<Person>();
    personQuery = personQuery.trim();
    if (personQuery.contains(" ")) {
      String one = personQuery.split(" ")[0];
      String two = personQuery.split(" ")[1];
      res.addAll(getPersonsByName(one, two));
    }

    String sql = "SELECT * from persons where first_name LIKE ? OR family_name LIKE ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setString(1, "%" + personQuery + "%");
      statement.setString(2, "%" + personQuery + "%");
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        int id = rs.getInt("id");
        List<Person> found = getPersonWithAffiliations(id);
        if (found.isEmpty()) {
          String username = rs.getString("user_id");
          String title = rs.getString("title");
          String first = rs.getString("first_name");
          String last = rs.getString("last_name");
          String eMail = rs.getString("email");
          String phone = "";
          res.add(new Person(username, title, first, last, eMail, phone, -1, "N/A", "N/A"));
        } else
          res.add(found.get(0));// TODO set all of them!
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  @Deprecated
  public List<Affiliation> getAffiliationsContaining(String affiQuery) {
    List<Affiliation> res = new ArrayList<Affiliation>();

    String sql =
        "SELECT * from organizations where group_name LIKE ? OR group_acronym LIKE ? OR umbrella_organization LIKE ? "
            + "or institute LIKE ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      for (int i = 1; i < 5; i++)
        statement.setString(i, "%" + affiQuery + "%");
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        int id = rs.getInt("id");
        String groupName = rs.getString("group_name");
        String acronym = rs.getString("group_acronym");
        if (acronym == null)
          acronym = "";
        String organization = rs.getString("umbrella_organization");
        String faculty = rs.getString("faculty");
        String institute = rs.getString("institute");
        if (institute == null)
          institute = "";
        String street = rs.getString("street");
        String zipCode = rs.getString("zip_code");
        String city = rs.getString("city");
        String country = rs.getString("country");
        String webpage = rs.getString("webpage");
        int contactID = rs.getInt("main_contact");
        int headID = rs.getInt("head");
        String contact = null;
        String head = null;
        if (contactID > 0) {
          Person c = getPerson(contactID);
          contact = c.getFirstName() + " " + c.getLastName();
        }
        if (headID > 0) {
          Person h = getPerson(headID);
          head = h.getFirstName() + " " + h.getLastName();
        }
        res.add(new Affiliation(id, groupName, acronym, organization, institute, faculty, contact,
            head, street, zipCode, city, country, webpage));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  public Map<String, life.qbic.userdb.model.ProjectInfo> getProjectMap() {
    Map<String, life.qbic.userdb.model.ProjectInfo> res = new HashMap<>();
    // since there are at the moment 2 different roles, this query will return two rows per project
    String sql =
        "SELECT projects.*, projects.id, projects_persons.*, person.* FROM projects INNER JOIN projects_persons ON "
            + "projects.id = projects_persons.project_id INNER JOIN person ON projects_persons.person_id = person.id";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String projectID = rs.getString("openbis_project_identifier");
        String[] openbisIDSplit = projectID.split("/");
        String project = "";
        try {
          project = openbisIDSplit[2];
        } catch (ArrayIndexOutOfBoundsException e) {
          logger
              .warn("Project identifier " + projectID + " is not correct. Skipping this project.");
          continue;
        }
        String role = rs.getString("project_role");

        int personID = rs.getInt("person_id");
        String first = rs.getString("first_name");
        String last = rs.getString("last_name");
        String userID = rs.getString("user_id");
        String title = rs.getString("title");
        String email = rs.getString("email");
        Person person = new Person(personID, title, first, last, email, userID);


        if (!res.containsKey(projectID)) {
          // first result row
          String space = openbisIDSplit[1];
          int id = rs.getInt("project_id");
          String shortName = rs.getString("short_title");
          res.put(projectID, new life.qbic.userdb.model.ProjectInfo(space, project, "", shortName, id));
        }
        // setting person for different role rows
        life.qbic.userdb.model.ProjectInfo info = res.get(projectID);
        switch (role) {
          case "PI":
            info.setInvestigator(person);
            break;
          case "Contact":
            info.setContact(person);
            break;
          case "Manager":
            info.setManager(person);
            break;
          default:
            logger.error("Unknown/unimplemented project role: " + role);
            break;
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    // now we need to add all projects without person connections
    sql =
        "SELECT t1.* FROM projects t1 LEFT JOIN projects_persons t2 ON t1.id = t2.project_id WHERE t2.project_id IS NULL";
    conn = login();
    statement = null;
    try {
      statement = conn.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String projID = rs.getString("openbis_project_identifier");
        String[] openbisIDSplit = projID.split("/");
        try {
          String project = openbisIDSplit[2];
          String space = openbisIDSplit[1];
          int id = rs.getInt("id");
          String shortName = rs.getString("short_title");
          res.put(projID, new life.qbic.userdb.model.ProjectInfo(space, project, "", shortName, id));
        } catch (Exception e) {
          logger.error("Could not parse project from openbis identifier " + projID
              + ". It seems this database entry is incorrect. Ignoring project.");
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  public List<CollaboratorWithResponsibility> getCollaboratorsOfProject(String project) {
    project = prepareStringInput(project);
    List<CollaboratorWithResponsibility> res = new ArrayList<CollaboratorWithResponsibility>();
    // for experiments
    String sql =
        "SELECT experiments.*, experiments.id, experiments_persons.*, person.first_name, person.last_name FROM experiments INNER JOIN experiments_persons ON "
            + "experiments.id = experiments_persons.experiment_id INNER JOIN person ON experiments_persons.person_id = person.id "
            + "WHERE openbis_experiment_identifier LIKE ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setString(1, "%" + project + "%");
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String openbisID = rs.getString("openbis_experiment_identifier");
        String[] openbisIDSplit = openbisID.split("/");
        int id = rs.getInt("experiments.id");
        String exp = openbisIDSplit[3];
        String role = rs.getString("experiment_role");
        String name = rs.getString("first_name") + " " + rs.getString("last_name");
        res.add(new CollaboratorWithResponsibility(id, name, openbisID, exp, role));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }

    return res;
  }

  public int addExperimentToDB(String openbisIdentifier) {
    openbisIdentifier = prepareStringInput(openbisIdentifier);
    int exists = isExpInDB(openbisIdentifier);
    if (exists < 0) {
      logger.info("Trying to add experiment " + openbisIdentifier + " to the person DB");
      String sql = "INSERT INTO experiments (openbis_experiment_identifier) VALUES(?)";
      Connection conn = login();
      try (PreparedStatement statement =
          conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setString(1, openbisIdentifier);
        statement.execute();
        ResultSet rs = statement.getGeneratedKeys();
        if (rs.next()) {
          logout(conn);
          logger.info("Successful.");
          return rs.getInt(1);
        }
      } catch (SQLException e) {
        logger.error("SQL operation unsuccessful: " + e.getMessage());
        e.printStackTrace();
      }
      logout(conn);
      return -1;
    }
    return exists;
  }

  private int isExpInDB(String expID) {
    expID = prepareStringInput(expID);
    logger.info("Looking for experiment " + expID + " in the DB");
    String sql = "SELECT * from experiments WHERE openbis_experiment_identifier = ?";
    int res = -1;
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, expID);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        logger.info("project found!");
        res = rs.getInt("id");
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    }
    logout(conn);
    return res;
  }

  public void addPersonToExperiment(int expID, int personID, String role) {
    role = prepareStringInput(role);
    if (expID == 0 || personID == 0)
      return;

    if (!hasPersonRoleInExperiment(personID, expID, role)) {
      logger.info("Trying to add person with role " + role + " to an experiment.");
      String sql =
          "INSERT INTO experiments_persons (experiment_id, person_id, experiment_role) VALUES(?, ?, ?)";
      Connection conn = login();
      try (PreparedStatement statement =
          conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setInt(1, expID);
        statement.setInt(2, personID);
        statement.setString(3, role);
        statement.execute();
        logger.info("Successful.");
      } catch (SQLException e) {
        logger.error("SQL operation unsuccessful: " + e.getMessage());
        e.printStackTrace();
      }
      logout(conn);
    }
  }

  private boolean hasPersonRoleInExperiment(int personID, int expID, String role) {
    logger.info("Checking if person already has this role in the experiment.");
    String sql =
        "SELECT * from experiments_persons WHERE person_id = ? AND experiment_id = ? and experiment_role = ?";
    boolean res = false;
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setInt(1, personID);
      statement.setInt(2, expID);
      statement.setString(3, role);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        res = true;
        logger.info("person already has this role!");
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    }
    logout(conn);
    return res;
  }

  public int getProjectIDFromCode(String code) {
    code = prepareStringInput(code);
    int res = -1;
    String sql = "SELECT id from projects WHERE openbis_project_identifier LIKE ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setString(1, "%" + code);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        res = rs.getInt("id");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
    return res;
  }

  public void removePersonFromProject(int id, String role) {
    role = prepareStringInput(role);
    logger.info("Trying to remove person with role " + role + " from project with id " + id);
    String sql = "DELETE FROM projects_persons WHERE project_id = ? AND project_role = ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, id);
      statement.setString(2, role);
      statement.executeQuery();
      logger.info("Successful.");
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
  }

  public void removePersonFromExperiment(int experimentID) {
    logger.info("Trying to remove person from experiment with id " + experimentID);
    String sql = "DELETE FROM experiments_persons WHERE experiment_id = ?";
    Connection conn = login();
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(sql);
      statement.setInt(1, experimentID);
      statement.executeQuery();
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    } finally {
      endQuery(conn, statement);
    }
  }

  public void addOrUpdatePersonToExperiment(int experimentID, int personID, String role) {
    role = prepareStringInput(role);
    if (!hasPersonRoleInExperiment(personID, experimentID, role)) {
      logger.info("Trying to add person with role " + role + " to an experiment.");
      if (!roleForExperimentTaken(experimentID, role)) {
        logger.info("Role " + role + " is not yet taken.");
        String sql =
            "INSERT INTO experiments_persons (experiment_id, person_id, experiment_role) VALUES(?, ?, ?)";
        Connection conn = login();
        PreparedStatement statement = null;
        try {
          statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          statement.setInt(1, experimentID);
          statement.setInt(2, personID);
          statement.setString(3, role);
          statement.execute();
          logger.info("Successful.");
        } catch (SQLException e) {
          logger.error("SQL operation unsuccessful: " + e.getMessage());
          e.printStackTrace();
        } finally {
          endQuery(conn, statement);
        }
      } else {
        logger.info("Role " + role + " is taken. Updating to new person.");
        String sql =
            "UPDATE experiments_persons SET person_id = ? WHERE experiment_id = ? AND experiment_role = ?;";
        Connection conn = login();
        PreparedStatement statement = null;
        try {
          statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          statement.setInt(1, personID);
          statement.setInt(2, experimentID);
          statement.setString(3, role);
          statement.execute();
          logger.info("Successful.");
        } catch (SQLException e) {
          logger.error("SQL operation unsuccessful: " + e.getMessage());
          e.printStackTrace();
        } finally {
          endQuery(conn, statement);
        }
      }
    }
  }

  public boolean personExists(Person p) {
    String username = p.getUsername();
    String email = p.getEmail();
    logger.info("Looking for user " + username + " in the DB");
    String sql = "SELECT * from persons WHERE UPPER(username) = UPPER(?)";
    boolean res = false;
    Connection conn = login();
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, username);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        logger.info("user found!");
        res = true;
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    }
    if (res)
      return res;
    logger.info("Looking for user with email " + email + " in the DB");
    sql = "SELECT * from persons WHERE UPPER(email) = UPPER(?)";
    try {
      PreparedStatement statement = conn.prepareStatement(sql);
      statement.setString(1, email);
      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        logger.info("email found!");
        res = true;
      }
    } catch (SQLException e) {
      logger.error("SQL operation unsuccessful: " + e.getMessage());
      e.printStackTrace();
    }
    logout(conn);
    return res;
  }
}
