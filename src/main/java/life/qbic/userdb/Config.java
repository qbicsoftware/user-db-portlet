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

import java.util.List;

public class Config {

  private String hostname;
  private String port;
  private String sql_database;
  private String username;
  private String password;
  private String openbisURL;
  private String openbisUser;
  private String openbisPass;
  private List<String> userGrps;
  private List<String> adminGrps;
  private String tmpFolder;

  public Config(String hostname, String port, String sql_database, String username, String password,
      List<String> userGrps, List<String> adminGrps, String openbisURL, String openbisUser,
      String openbisPass, String tmpFolder) {
    this.hostname = hostname;
    this.port = port;
    this.sql_database = sql_database;
    this.username = username;
    this.password = password;
    this.adminGrps = adminGrps;
    this.userGrps = userGrps;
    this.openbisURL = openbisURL;
    this.openbisUser = openbisUser;
    this.openbisPass = openbisPass;
    this.tmpFolder = tmpFolder;
  }

  public String getTmpFolder() {
    return tmpFolder;
  }
  
  public String getOpenbisURL() {
    return openbisURL;
  }

  public String getOpenbisUser() {
    return openbisUser;
  }

  public String getOpenbisPass() {
    return openbisPass;
  }

  public List<String> getUserGrps() {
    return userGrps;
  }

  public List<String> getAdminGrps() {
    return adminGrps;
  }

  public String getHostname() {
    return hostname;
  }

  public String getPort() {
    return port;
  }

  public String getSql_database() {
    return sql_database;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }



}
