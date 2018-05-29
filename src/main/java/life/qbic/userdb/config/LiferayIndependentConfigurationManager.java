/*******************************************************************************
 * QBiC User DB Tools enables users to add people and affiliations to our mysql user database.
 * Copyright (C) 2016  Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.userdb.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Implements {@see ConfigurationManager}. Does not need Portal environment.
 * 
 * @author wojnar
 * 
 */

public enum LiferayIndependentConfigurationManager implements ConfigurationManager {
  Instance;
  public static final String CONFIGURATION_SUFFIX = ".configuration";
  public static final String DATASOURCE_KEY = "datasource";
  public static final String DATASOURCE_USER = "datasource.user";
  public static final String DATASOURCE_PASS = "datasource.password";
  public static final String DATASOURCE_URL = "datasource.url";

  public static final String GENOMEVIEWER_URL = "genomeviewer.url";
  public static final String GENOMEVIEWER_RESTAPI = "genomeviewer.restapi";

  public static final String TMP_FOLDER = "tmp.folder";
  public static final String SCRIPTS_FOLDER = "barcode.scripts";
  public static final String PATH_VARIABLE = "path.variable";

  public static final String PATH_TO_GUSE_WORKFLOWS = "path_to_guse_workflows";
  public static final String PATH_TO_GUSE_CERTIFICATE = "path_to_certificate";
  public static final String PATH_TO_WF_CONFIG = "path_to_wf_config";

  public static final String PATH_TO_DROPBOXES = "path_to_dropboxes";

  public static final String GUSE_REMOTE_API_URL = "guse_remoteapi_url";
  public static final String GUSE_REMOTE_API_PASS = "guse_remoteapi_password";

  public static final String ATTACHMENT_URI = "attachment.uri";
  public static final String ATTACHMENT_USER = "attachment.user";
  public static final String ATTACHMENT_PASS = "attachment.password";
  public static final String ATTACHMENT_MAX_SIZE = "max.attachment.size";

  public static final String MSQL_HOST = "mysql.host";
  public static final String MSQL_DB = "mysql.db";
  public static final String MSQL_USER = "mysql.user";
  public static final String MSQL_PORT = "mysql.port";
  public static final String MSQL_PASS = "mysql.pass";

  public static final String DB_INPUT_USER_GROUPS = "mysql.input.usergrp";
  public static final String DB_INPUT_ADMIN_GROUPS = "mysql.input.admingrp";

  public static final String UNI_LDAP_URI = "uni.ldap.uri";
  public static final String UNI_LDAP_USER = "uni.ldap.user";
  public static final String UNI_LDAP_DN = "uni.ldap.dn";
  public static final String UNI_LDAP_PASS = "uni.ldap.pass";
  public static final String UNI_LDAP_BASE = "uni.ldap.base";

  private String LABELING_METHODS = "vocabulary.ms.labeling";

  private String configurationFileName;
  private String dataSourceUser;
  private String dataSourcePass;
  private String dataSourceUrl;

  private String tmpFolder;
  private String scriptsFolder;
  private String pathVariable;

  private String attachmentURI;
  private String attachmentUser;
  private String attachmentPass;
  private String attachmentMaxSize;

  private String msqlHost;
  private String msqlDB;
  private String msqlUser;
  private String msqlPort;
  private String msqlPass;

  private List<String> dbInputUserGrps;
  private List<String> dbInputAdminGrps;

  private String uniLdapUri;
  private String uniLdapUser;
  private String uniLdapPass;
  private String uniLdapDN;
  private String uniLdapBase;

  private String labelingMethods;

  // private String portletPropertiesFileName = "portlet.properties";

  private boolean initialized = false;


  public boolean isInitialized() {
    return initialized;
  }

  // /**
  // * init the portlet with the following properties file.
  // *
  // * @param portletPropertiesFileName
  // */
  // public void init(String portletPropertiesFileName) {
  // this.portletPropertiesFileName = portletPropertiesFileName;
  // init();
  // }

  public void init() {
    Properties portletConfig = new Properties();
    try {
      List<String> configs =
          new ArrayList<String>(Arrays.asList("/Users/frieda/Desktop/testing/portlet.properties",
              "/home/luser/liferay-portal-6.2-ce-ga4/portlets.properties",
              "/usr/local/share/guse/portlets.properties",
              "/home/tomcat-liferay/liferay_production/portlets.properties"));
      for (String s : configs) {
        File f = new File(s);
        if (f.exists())
          portletConfig.load(new FileReader(s));
      }
      dataSourceUser = portletConfig.getProperty(DATASOURCE_USER);
      dataSourcePass = portletConfig.getProperty(DATASOURCE_PASS);
      dataSourceUrl = portletConfig.getProperty(DATASOURCE_URL);

      tmpFolder = portletConfig.getProperty(TMP_FOLDER);
      scriptsFolder = portletConfig.getProperty(SCRIPTS_FOLDER);
      pathVariable = portletConfig.getProperty(PATH_VARIABLE);

      attachmentURI = portletConfig.getProperty(ATTACHMENT_URI);
      attachmentUser = portletConfig.getProperty(ATTACHMENT_USER);
      attachmentPass = portletConfig.getProperty(ATTACHMENT_PASS);
      attachmentMaxSize = portletConfig.getProperty(ATTACHMENT_MAX_SIZE);

      msqlHost = portletConfig.getProperty(MSQL_HOST);
      msqlDB = portletConfig.getProperty(MSQL_DB);
      msqlPort = portletConfig.getProperty(MSQL_PORT);
      msqlUser = portletConfig.getProperty(MSQL_USER);
      msqlPass = portletConfig.getProperty(MSQL_PASS);

      dbInputUserGrps = new ArrayList<String>(
          Arrays.asList(portletConfig.getProperty(DB_INPUT_USER_GROUPS).split(",")));
      for (int i = 0; i < dbInputUserGrps.size(); i++)
        dbInputUserGrps.set(i, dbInputUserGrps.get(i).trim());
      dbInputAdminGrps = new ArrayList<String>(
          Arrays.asList(portletConfig.getProperty(DB_INPUT_ADMIN_GROUPS).split(",")));
      for (int i = 0; i < dbInputAdminGrps.size(); i++)
        dbInputAdminGrps.set(i, dbInputAdminGrps.get(i).trim());

      uniLdapUri = portletConfig.getProperty(UNI_LDAP_URI);
      uniLdapUser = portletConfig.getProperty(UNI_LDAP_USER);
      uniLdapPass = portletConfig.getProperty(UNI_LDAP_PASS);
      uniLdapDN = portletConfig.getProperty(UNI_LDAP_DN);
      uniLdapBase = portletConfig.getProperty(UNI_LDAP_BASE);

      labelingMethods = portletConfig.getProperty(LABELING_METHODS);

    } catch (IOException ex) {
      ex.printStackTrace();
    }
    initialized = true;
  }

  @Override
  public String getConfigurationFileName() {
    return configurationFileName;
  }

  @Override
  public String getDataSourceUser() {
    return dataSourceUser;
  }

  @Override
  public String getDataSourcePassword() {
    return dataSourcePass;
  }

  @Override
  public String getDataSourceUrl() {
    return dataSourceUrl;
  }

  @Override
  public String getBarcodeScriptsFolder() {
    return scriptsFolder;
  }

  @Override
  public String getTmpFolder() {
    return tmpFolder;
  }

  @Override
  public String getBarcodePathVariable() {
    return pathVariable;
  }

  @Override
  public String getAttachmentURI() {
    return attachmentURI;
  }

  @Override
  public String getAttachmentUser() {
    return attachmentUser;
  }

  @Override
  public String getAttachmenPassword() {
    return attachmentPass;
  }

  @Override
  public String getAttachmentMaxSize() {
    return attachmentMaxSize;
  }

  @Override
  public String getMysqlHost() {
    return msqlHost;
  }

  @Override
  public String getMysqlPort() {
    return msqlPort;
  }

  @Override
  public String getMysqlDB() {
    return msqlDB;
  }

  @Override
  public String getMysqlUser() {
    return msqlUser;
  }

  @Override
  public String getMysqlPass() {
    return msqlPass;
  }

  @Override
  public List<String> getDBInputUserGrps() {
    return dbInputUserGrps;
  }
  
  @Override
  public List<String> getDBInputAdminGrps() {
    return dbInputAdminGrps;
  }
  
  @Override
  public String getVocabularyMSLabeling() {
    return labelingMethods;
  }

  @Override
  public String getLdapHost() {
    return uniLdapUri;
  }

  @Override
  public String getLdapBase() {
    return uniLdapBase;
  }

  @Override
  public String getLdapUser() {
    return uniLdapUser;
  }

  @Override
  public String getLdapPass() {
    return uniLdapPass;
  }
}
