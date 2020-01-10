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
package life.qbic.userdb.views;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tepi.filtertable.FilterTable;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import ch.systemsx.cisd.common.concurrent.TimerUtilities;
import life.qbic.datamodel.identifiers.SampleCodeFunctions;
import life.qbic.datamodel.persons.Affiliation;
import life.qbic.datamodel.persons.CollaboratorWithResponsibility;
import life.qbic.datamodel.persons.Person;
import life.qbic.datamodel.projects.ProjectInfo;
import life.qbic.portal.Styles;
import life.qbic.portal.portlet.ProjectFilterDecorator;
import life.qbic.portal.portlet.ProjectFilterGenerator;
import life.qbic.portal.utils.PortalUtils;
import life.qbic.utils.TimeUtils;

public class ProjectView extends VerticalLayout {
  Logger logger = LogManager.getLogger(ProjectView.class);

  private FilterTable projectTable;

  private Button downloadProjects;
  private FileDownloader tableDL;

  private Map<String, ProjectInfo> projectMap;
  private Map<String, Integer> personMap;

  private CheckBox showIncomplete;

  private TextField altName;
  private ComboBox investigatorBox;
  private ComboBox contactBox;
  private ComboBox managerBox;
  private Button submitInfo;

  private Button downloadProjectInfo;
  private FileDownloader tsvDL;

  private Table experimentPersons;
  private Button submitPersons;
  private Map<String, CollaboratorWithResponsibility> experimentMap;

  private VerticalLayout projectInfoLayout;

  public ProjectView(Map<String, ProjectInfo> projectMap, Map<String, Integer> personMap) {
    setSpacing(true);
    setMargin(true);

    this.personMap = personMap;
    this.projectMap = projectMap;

    showIncomplete = new CheckBox("Only show projects missing information");
    showIncomplete.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        initProjectTable(showIncomplete.getValue());
      }
    });
    addComponent(showIncomplete);
    downloadProjects = new Button("Download Table of projects");
    addComponent(downloadProjects);

    projectTable = new FilterTable("Projects");
    projectTable.setPageLength(Math.min(15, projectMap.size()));
    projectTable.setStyleName(ValoTheme.TABLE_SMALL);
    projectTable.addContainerProperty("Sub-Project", String.class, null);
    projectTable.addContainerProperty("Short Title", String.class, null);
    projectTable.setColumnWidth("Name", 300);
    projectTable.addContainerProperty("Project", String.class, null);
    projectTable.addContainerProperty("Principal Investigator", String.class, null);
    projectTable.addContainerProperty("Project Manager", String.class, null);
    projectTable.setSelectable(true);
    addComponent(projectTable);

    projectTable.setFilterDecorator(new ProjectFilterDecorator());
    projectTable.setFilterGenerator(new ProjectFilterGenerator());

    projectTable.setFilterBarVisible(true);

    projectTable.setImmediate(true);

    downloadProjectInfo = new Button("Download Project Information");
    downloadProjectInfo.setVisible(false);
    addComponent(downloadProjectInfo);
    initProjectTable(false);
    initView();

    experimentPersons = new Table("Experiment Collaborators (optional)");
    experimentPersons.setVisible(false);
    experimentPersons.setStyleName(ValoTheme.TABLE_SMALL);
    experimentPersons.addContainerProperty("Name", ComboBox.class, null);
    experimentPersons.addContainerProperty("Experiment", String.class, null);
    experimentPersons.addContainerProperty("Responsibility", String.class, null);
    experimentPersons.setColumnWidth("Responsibility", 150);
    experimentPersons.setPageLength(1);
    addComponent(experimentPersons);

    submitPersons = new Button("Submit Experiment");
    submitPersons.setVisible(false);
    addComponent(submitPersons);
  }

  private void initView() {
    projectInfoLayout = new VerticalLayout();
    projectInfoLayout.setVisible(false);
    altName = new TextField("Short Title");
    altName.setWidth("300px");
    altName.setStyleName(Styles.fieldTheme);
    investigatorBox = new ComboBox("Principal Investigator", personMap.keySet());
    investigatorBox.setStyleName(Styles.boxTheme);
    investigatorBox.setFilteringMode(FilteringMode.CONTAINS);
    contactBox = new ComboBox("Contact Person", personMap.keySet());
    contactBox.setStyleName(Styles.boxTheme);
    contactBox.setFilteringMode(FilteringMode.CONTAINS);
    managerBox = new ComboBox("Project Manager", personMap.keySet());
    managerBox.setStyleName(Styles.boxTheme);
    managerBox.setFilteringMode(FilteringMode.CONTAINS);
    submitInfo = new Button("Change Project Information");
    projectInfoLayout.addComponent(altName);
    projectInfoLayout.addComponent(investigatorBox);
    projectInfoLayout.addComponent(contactBox);
    projectInfoLayout.addComponent(managerBox);
    projectInfoLayout.addComponent(submitInfo);
    projectInfoLayout.setSpacing(true);
    addComponent(projectInfoLayout);
  }
  
  public void initProjectTable(boolean showIncompleteOnly) {
    projectTable.removeAllItems();
    for (String code : projectMap.keySet()) {
      ProjectInfo p = projectMap.get(code);
      List<Object> row = new ArrayList<Object>();

      String secName = p.getSecondaryName();
      String inv = p.getInvestigator();
      String mang = p.getManager();
      String cont = p.getContact();

      row.add(code);
      row.add(secName);
      row.add(p.getSpace());
      row.add(inv);
      row.add(mang);
      
      boolean complete = StringUtils.isNotBlank(secName) && StringUtils.isNotBlank(inv)
          && StringUtils.isNotBlank(mang) && StringUtils.isNotBlank(cont);

      if (showIncompleteOnly) {
        if (!complete) {
          projectTable.addItem(row.toArray(new Object[row.size()]), code);
        }
      } else {
        projectTable.addItem(row.toArray(new Object[row.size()]), code);
      }
      projectTable.setCaption(projectTable.size() + " Projects");
    }
    downloadProjects.setVisible(false);
    try {
      armProjectsDownloadButton();
      downloadProjects.setVisible(true);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  /**
   * Creates a tab separated values file of the available project information
   * 
   * @param manager
   * @param contact
   * @param PI
   * 
   * @param managerName
   * @param contactName
   * @param invName
   * @param space
   * @param secondaryName
   * @param string
   * 
   * @return
   * @throws FileNotFoundException
   * @throws UnsupportedEncodingException
   */
  private String createProjectTSV(String subProject, Person PI, Person contact, Person manager)
      throws FileNotFoundException, UnsupportedEncodingException {

    ProjectInfo proj = projectMap.get(subProject);
    String secondaryName = proj.getSecondaryName();
    secondaryName = secondaryName == null ? "" : secondaryName;
    String space = proj.getSpace();
    String description = proj.getDescription();

    List<String> summaryHeader =
        new ArrayList<>(Arrays.asList("Sub-Project", "Short Title", "Description", "Project",
            "Principal Investigator", "PI E-Mail", "PI Group", "PI Institute", "PI Organization",
            "PI Address", "Contact Person", "Contact E-Mail", "Contact Group", "Contact Institute",
            "Contact Organization", "Contact Address", "Project Manager", "Manager E-Mail",
            "Manager Group", "Manager Institute", "Manager Organization", "Manager Address"));
    List<String> data =
        new ArrayList<>(Arrays.asList(subProject, secondaryName, description, space));

    addPersonInfos(data, PI);
    addPersonInfos(data, contact);
    addPersonInfos(data, manager);

    // only filled entries are listed here and no address for now
    for (CollaboratorWithResponsibility col : experimentMap.values()) {
      String name = col.getPerson();
      if (name != null && !name.isEmpty()) {
        summaryHeader.add(col.getOpenbisCode() + " Experiment Collaborator");
        summaryHeader.add(col.getOpenbisCode() + " Experiment Role");
        data.add(name);
        data.add(col.getRole());
      }
    }

    String headerLine = String.join("\t", summaryHeader);
    String dataLine = replaceSpecialSymbols(String.join("\t", data));

    return headerLine + "\n" + dataLine + "\n";
  }

  private String createTSVForTable() throws FileNotFoundException, UnsupportedEncodingException {

    List<String> header = new ArrayList<>(Arrays.asList("Sub-Project", "Short Title", "Project",
        "Principal Investigator", "Contact Person", "Project Manager"));
    String headerLine = String.join("\t", header);

    StringBuilder builder = new StringBuilder(headerLine + "\n");

    for (Object item : projectTable.getItemIds()) {
      ProjectInfo proj = projectMap.get(item);
      String code = proj.getProjectCode();
      String name = proj.getSecondaryName();
      name = name == null ? "" : name;
      String space = proj.getSpace();
      String inv = proj.getInvestigator();
      inv = inv == null ? "" : inv;
      String cont = proj.getContact();
      cont = cont == null ? "" : cont;
      String mang = proj.getManager();
      mang = mang == null ? "" : mang;
      String row = replaceSpecialSymbols(
          String.join("\t", Arrays.asList(code, name, space, inv, cont, mang)));
      builder.append(row + "\n");
    }
    return builder.toString();
  }

  private String replaceSpecialSymbols(String string) {
    string = string.replace("ß", "ss");
    string = string.replace("ä", "ae").replace("ü", "ue").replace("ö", "oe");
    string = string.replace("Ä", "Ae").replace("Ü", "Ue").replace("Ö", "Oe");
    return string;
  }

  private void addPersonInfos(List<String> data, Person p) {
    if (p != null) {
      data.add(getFullName(p));
      data.add(p.getEmail());
      Affiliation affi = p.getAffiliations().get(0);
      data.add(Objects.toString(affi.getGroupName(), ""));
      data.add(Objects.toString(affi.getInstitute(), ""));
      data.add(Objects.toString(affi.getOrganization(), ""));
      data.add(generatePersonAddress(p));
    } else {
      data.add("");
      data.add("");
      data.add("");
      data.add("");
      data.add("");
      data.add("");
    }
  }

  private String getFullName(Person person) {
    return person.getFirstName() + " " + person.getLastName();
  }

  private String generatePersonAddress(Person person) {
    Affiliation af = person.getAffiliations().get(0);
    StringBuilder b = new StringBuilder(af.getStreet());
    b.append(" ");
    b.append(af.getZipCode());
    b.append(" ");
    b.append(af.getCountry());
    return b.toString();
  }

  // TODO move this to utils lib
  private StreamResource getTSVStream(final String content, String name) {
    StreamResource resource = new StreamResource(new StreamResource.StreamSource() {
      @Override
      public InputStream getStream() {
        try {
          InputStream is = new ByteArrayInputStream(content.getBytes());
          return is;
        } catch (Exception e) {
          e.printStackTrace();
          return null;
        }
      }
    }, String.format("%s.tsv", name));
    return resource;
  }

  private void armSingleProjectDownloadButton(Object item, Person PI, Person contact,
      Person manager) throws FileNotFoundException, UnsupportedEncodingException {
    String ts = TimeUtils.getCurrentTimestampString();
    String subProject = item.toString();
    StreamResource tsvStream = getTSVStream(createProjectTSV(subProject, PI, contact, manager),
        subProject + "_" + ts + "_summary");
    if (tsvDL == null) {
      tsvDL = new FileDownloader(tsvStream);
      tsvDL.extend(downloadProjectInfo);
    } else
      tsvDL.setFileDownloadResource(tsvStream);
  }

  private void armProjectsDownloadButton()
      throws FileNotFoundException, UnsupportedEncodingException {
    String ts = TimeUtils.getCurrentTimestampString();
    StreamResource tsvStream = getTSVStream(createTSVForTable(), ts + "projects_table");
    if (tableDL == null) {
      tableDL = new FileDownloader(tsvStream);
      tableDL.extend(downloadProjects);
    } else
      tableDL.setFileDownloadResource(tsvStream);
  }

  /**
   * returns null if there was no change
   * 
   * @return
   */
  public ProjectInfo getEditedInfo() {
    String code = (String) projectTable.getValue();
    ProjectInfo p = projectMap.get(code);
    String oldName = p.getSecondaryName();
    String newName = altName.getValue();

    String oldPI = p.getInvestigator();
    Object newPI = investigatorBox.getValue();
    boolean updatePI = oldPI != newPI;
    if (oldPI != null)
      updatePI = !oldPI.equals(newPI);

    String oldContact = p.getContact();
    Object newContact = contactBox.getValue();
    boolean updateContact = oldContact != newContact;
    if (oldContact != null)
      updateContact = !oldContact.equals(newContact);

    String oldManager = p.getManager();
    Object newManager = managerBox.getValue();
    boolean updateManager = oldManager != newManager;
    if (oldManager != null)
      updateManager = !oldManager.equals(newManager);

    boolean update = !oldName.equals(newName) || updatePI || updateContact || updateManager;
    if (update) {
      // initProjectInfos(projectMap.values());
      ProjectInfo newInfo =
          new ProjectInfo(p.getSpace(), code, p.getDescription(), newName, p.getProjectID());
      if (newPI != null)
        newInfo.setInvestigator(newPI.toString());
      else
        newInfo.setInvestigator("");
      if (newContact != null)
        newInfo.setContact(newContact.toString());
      else
        newInfo.setContact("");
      if (newManager != null)
        newInfo.setManager(newManager.toString());
      else
        newInfo.setManager("");
      return newInfo;
    } else {
      logger.debug("No changes to project info detected");
      return null;
    }
  }

  public Button getInfoCommitButton() {
    return submitInfo;
  }

  public Button getPeopleCommitButton() {
    return submitPersons;
  }

  public FilterTable getProjectTable() {
    return projectTable;
  }

  public Table getCollaboratorTable() {
    return experimentPersons;
  }

  public void setCollaboratorsOfProject(List<CollaboratorWithResponsibility> collaborators) {
    experimentMap = new HashMap<String, CollaboratorWithResponsibility>();
    experimentPersons.removeAllItems();
    experimentPersons.setVisible(false);

    for (CollaboratorWithResponsibility c : collaborators) {
      String code = c.getOpenbisCode();
      experimentMap.put(code, c);
      List<Object> row = new ArrayList<Object>();
      ComboBox persons = new ComboBox();
      persons.setStyleName(Styles.boxTheme);
      persons.setFilteringMode(FilteringMode.CONTAINS);
      persons.addItems(personMap.keySet());
      persons.setValue(c.getPerson());
      row.add(persons);
      row.add(c.getOpenbisCode());
      row.add(c.getOpenbisType());
      experimentPersons.addItem(row.toArray(new Object[row.size()]), code);
      experimentPersons.setVisible(true);
    }

    // sort ascending by Experiment ID
    Object[] properties = {"Experiment"};
    boolean[] ordering = {true};
    experimentPersons.sort(properties, ordering);

    experimentPersons.sort();
    experimentPersons.setPageLength(collaborators.size());
  }

  /**
   * 
   * @return list of new collaborators for experiments
   */
  public List<CollaboratorWithResponsibility> getNewResponsibilities() {
    List<CollaboratorWithResponsibility> res = new ArrayList<CollaboratorWithResponsibility>();
    logger.debug("ids: " + experimentPersons.getItemIds());
    for (Object code : experimentPersons.getItemIds()) {
      ComboBox personBox =
          (ComboBox) experimentPersons.getItem(code).getItemProperty("Name").getValue();
      String name = "";
      if (personBox.getValue() != null)
        name = personBox.getValue().toString();
      CollaboratorWithResponsibility old = experimentMap.get(code);
      CollaboratorWithResponsibility newColl = new CollaboratorWithResponsibility(
          old.getExperimentID(), name, old.getOpenbisIdentifier(), code.toString(), old.getRole());
      res.add(newColl);
    }
    return res;
  }

  public void updateChangedInfo(ProjectInfo info) {
    String code = info.getProjectCode();
    // projectTable.removeItem(code);
    projectMap.put(code, info);
    initProjectTable(showIncomplete.getValue());
    // List<Object> row = new ArrayList<Object>();
    // row.add(code);
    // row.add(info.getSecondaryName());
    // row.add(info.getSpace());
    // row.add(info.getInvestigator());
    // projectTable.addItem(row.toArray(new Object[row.size()]), code);
    // sort ascending by Project ID
    // projectTable.sort(new Object[] {"Sub-Project"}, new boolean[] {true});
  }

  public void handleProjectDeselect() {
    projectInfoLayout.setVisible(false);
    downloadProjectInfo.setVisible(false);
    experimentPersons.setVisible(false);
    submitPersons.setVisible(false);
  }

  public void handleProjectValueChange(Object item, Person PI, Person contact, Person manager) {
    projectInfoLayout.setVisible(false);
    downloadProjectInfo.setVisible(false);

    if (item != null) {
      String secondaryName = projectMap.get(item).getSecondaryName();

      projectInfoLayout.setCaption(projectMap.get(item).getProjectCode());
      logger.info("Selected project: " + projectMap.get(item));
      altName.setValue(secondaryName);
      investigatorBox.setValue(projectMap.get(item).getInvestigator());
      contactBox.setValue(projectMap.get(item).getContact());
      managerBox.setValue(projectMap.get(item).getManager());
      try {
        armSingleProjectDownloadButton(item, PI, contact, manager);
        downloadProjectInfo.setVisible(true);
        submitPersons.setVisible(true);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      projectInfoLayout.setVisible(true);
    }
  }

}
